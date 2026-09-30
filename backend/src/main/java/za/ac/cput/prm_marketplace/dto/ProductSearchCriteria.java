package za.ac.cput.prm_marketplace.dto;

import za.ac.cput.prm_marketplace.domain.ProductCondition;

import java.math.BigDecimal;

/**
 * Optional filter set for the product catalogue search. Every field is nullable so a single
 * endpoint can serve "browse everything" as well as targeted queries.
 */
public record ProductSearchCriteria(
        String keyword,
        String category,
        String city,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        ProductCondition condition,
        Boolean activeOnly,
        int page,
        int size,
        String sortBy,
        String direction
) {

    public static final int MAX_PAGE_SIZE = 100;

    public ProductSearchCriteria {
        if (page < 0) {
            throw new IllegalArgumentException("page must be zero or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("minPrice must not be greater than maxPrice");
        }
    }

    public boolean hasKeyword() {
        return keyword != null && !keyword.isBlank();
    }

    public boolean hasCategory() {
        return category != null && !category.isBlank();
    }

    public boolean hasCity() {
        return city != null && !city.isBlank();
    }
}
