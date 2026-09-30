package za.ac.cput.prm_marketplace.service;

import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.dto.ProductSearchCriteria;
import za.ac.cput.prm_marketplace.repository.ProductRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ProductServiceImpl implements IProductService {

    private static final String DEFAULT_SORT = "createdAt";

    /** Guards against unknown sort keys, which would otherwise surface as a 500. */
    private static final Map<String, String> SORTABLE_FIELDS = Map.of(
            "createdat", "createdAt",
            "name", "name",
            "price", "price",
            "stockquantity", "stockQuantity",
            "category", "category",
            "city", "city");

    private final ProductRepository productRepository;

    @Autowired
    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Product create(Product product) {
        return productRepository.save(product);
    }

    @Override
    public Product read(UUID id) {
        return productRepository.findById(id).orElse(null);
    }

    @Override
    public Product update(Product product) {
        if (productRepository.existsById(product.getId())) {
            return productRepository.save(product);
        }
        return null;
    }

    @Override
    public boolean delete(UUID id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Override
    public List<Product> getAll() {
        return productRepository.findAll();
    }

    @Override
    public List<Product> getByVendor(UUID vendorId) {
        return productRepository.findByVendorId(vendorId);
    }

    @Override
    public List<Product> getByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    @Override
    public Page<Product> search(ProductSearchCriteria criteria) {
        PageRequest pageRequest = PageRequest.of(
                criteria.page(),
                criteria.size(),
                resolveSort(criteria.sortBy(), criteria.direction()));

        return productRepository.findAll(buildSpecification(criteria), pageRequest);
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
                String pattern = "%" + criteria.keyword().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)));
            }
            if (criteria.hasCategory()) {
                predicates.add(cb.equal(root.get("category"), criteria.category().trim()));
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
