package za.ac.cput.prm_marketplace.service;

import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.domain.VendorProfile;
import za.ac.cput.prm_marketplace.dto.ProductSearchCriteria;
import za.ac.cput.prm_marketplace.factory.ProductFactory;
import za.ac.cput.prm_marketplace.repository.ProductRepository;
import za.ac.cput.prm_marketplace.repository.VendorProfileRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ProductServiceImpl implements IProductService {

    private static final String DEFAULT_SORT = "createdAt";

    /** Escape character used when a caller-supplied keyword is embedded in a LIKE pattern. */
    private static final char LIKE_ESCAPE = '\\';

    /** Guards against unknown sort keys, which would otherwise surface as a 500. */
    private static final Map<String, String> SORTABLE_FIELDS = Map.of(
            "createdat", "createdAt",
            "name", "name",
            "price", "price",
            "stockquantity", "stockQuantity",
            "category", "category",
            "city", "city");

    private final ProductRepository productRepository;
    private final VendorProfileRepository vendorProfileRepository;

    @Autowired
    public ProductServiceImpl(ProductRepository productRepository,
                              VendorProfileRepository vendorProfileRepository) {
        this.productRepository = productRepository;
        this.vendorProfileRepository = vendorProfileRepository;
    }

    @Override
    @Transactional
    public Product create(Product product, UUID requesterId) {
        if (product == null || requesterId == null) {
            return null;
        }

        // The listing belongs to the caller's own vendor profile. Resolving it here is what stops
        // a request from publishing a product under somebody else's business.
        VendorProfile vendor = vendorProfileRepository.findByUserId(requesterId).orElse(null);
        if (vendor == null) {
            return null;
        }

        // The factory enforces the rules a listing has to satisfy: a name, a price above zero,
        // a category and non-negative stock. A request that breaks one of them is rejected rather
        // than written.
        try {
            ProductFactory.createProduct(product.getName(), product.getDescription(),
                    product.getPrice(), product.getCategory(), product.getStockQuantity(), vendor.getId());
        } catch (IllegalArgumentException e) {
            return null;
        }

        // Built fresh rather than saving the submitted object: the vendor is the caller's, the
        // listing starts active, and the images collection is left empty because it is
        // WRITE_ONLY, so a request cannot smuggle image rows in through the cascaded collection.
        return productRepository.save(Product.builder()
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .category(product.getCategory())
                .vendor(vendor)
                .imageUrl(product.getImageUrl())
                .condition(product.getCondition())
                .city(product.getCity())
                .province(product.getProvince())
                .active(true)
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public Product read(UUID id) {
        if (id == null) {
            return null;
        }
        return productRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public Product update(UUID id, Product product, UUID requesterId) {
        if (id == null || product == null || requesterId == null) {
            return null;
        }

        Product existing = productRepository.findByIdAndVendorUserId(id, requesterId).orElse(null);
        if (existing == null) {
            return null;
        }

        // Rebuilt from the stored row. The vendor and creation time come from the database rather
        // than the request, so a listing cannot be sold on to another vendor or backdated, and the
        // stock change here is a deliberate vendor edit rather than a checkout reservation.
        Product updated = Product.builder()
                .copy(existing)
                .name(product.getName() != null && !product.getName().isBlank()
                        ? product.getName() : existing.getName())
                .description(product.getDescription() != null
                        ? product.getDescription() : existing.getDescription())
                .price(product.getPrice() != null
                        && product.getPrice().compareTo(BigDecimal.ZERO) > 0
                        ? product.getPrice() : existing.getPrice())
                .stockQuantity(product.getStockQuantity() >= 0
                        ? product.getStockQuantity() : existing.getStockQuantity())
                .category(product.getCategory() != null && !product.getCategory().isBlank()
                        ? product.getCategory() : existing.getCategory())
                .imageUrl(product.getImageUrl() != null
                        ? product.getImageUrl() : existing.getImageUrl())
                .condition(product.getCondition() != null
                        ? product.getCondition() : existing.getCondition())
                .city(product.getCity() != null ? product.getCity() : existing.getCity())
                .province(product.getProvince() != null
                        ? product.getProvince() : existing.getProvince())
                .active(product.isActive())
                .build();

        return productRepository.save(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> getMine(UUID requesterId) {
        if (requesterId == null) {
            return List.of();
        }
        return productRepository.findByVendorUserIdOrderByCreatedAtDesc(requesterId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> getAll() {
        return productRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> getByVendor(UUID vendorId) {
        if (vendorId == null) {
            return List.of();
        }
        return productRepository.findByVendorId(vendorId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> getByCategory(String category) {
        if (category == null) {
            return List.of();
        }
        return productRepository.findByCategory(category);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> search(ProductSearchCriteria criteria) {
        PageRequest pageRequest = PageRequest.of(
                criteria.page(),
                criteria.size(),
                resolveSort(criteria.sortBy(), criteria.direction()));

        return productRepository.findAll(buildSpecification(criteria), pageRequest);
    }

    /**
     * Wraps the keyword in wildcards, escaping the LIKE metacharacters first so that a
     * caller cannot turn a narrow search into "match everything" by passing {@code %} or {@code _}.
     */
    private static String likePattern(String keyword) {
        String escaped = keyword.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private static Sort resolveSort(String sortBy, String direction) {
        String requested = sortBy == null || sortBy.isBlank()
                ? DEFAULT_SORT
                : sortBy;

        String property = SORTABLE_FIELDS.get(requested.trim().toLowerCase(Locale.ROOT));
        if (property == null) {
            throw new IllegalArgumentException("Unsupported sort field: " + sortBy);
        }

        Sort.Direction resolved =
                "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(resolved, property);
    }

    private static Specification<Product> buildSpecification(ProductSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.hasKeyword()) {
                String pattern = likePattern(criteria.keyword());
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(root.get("description")), pattern, LIKE_ESCAPE)));
            }
            if (criteria.hasCategory()) {
                predicates.add(cb.equal(cb.lower(root.get("category")),
                        criteria.category().trim().toLowerCase(Locale.ROOT)));
            }
            if (criteria.hasCity()) {
                predicates.add(cb.equal(cb.lower(root.get("city")),
                        criteria.city().trim().toLowerCase(Locale.ROOT)));
            }
            if (criteria.minPrice() != null) {
                predicates.add(cb.ge(root.get("price"), criteria.minPrice()));
            }
            if (criteria.maxPrice() != null) {
                predicates.add(cb.le(root.get("price"), criteria.maxPrice()));
            }
            if (criteria.condition() != null) {
                predicates.add(cb.equal(root.get("condition"), criteria.condition()));
            }
            if (Boolean.TRUE.equals(criteria.activeOnly())) {
                predicates.add(cb.isTrue(root.get("active")));
            }

            return predicates.isEmpty()
                    ? cb.conjunction()
                    : cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
