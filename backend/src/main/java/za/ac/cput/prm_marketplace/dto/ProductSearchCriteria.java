package za.ac.cput.prm_marketplace.dto;

import za.ac.cput.prm_marketplace.domain.ProductCondition;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Optional filter set for the product catalogue search. Every field is nullable so a single
 * endpoint can serve "browse everything" as well as targeted queries. Category accepts one exact
 * value or a comma-separated set of exact values.
 */
public record ProductSearchCriteria(
        String keyword,
        String category,
        String city,
        String campus,
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
        if (category != null && !category.isBlank()
                && Arrays.stream(category.split(",", -1)).anyMatch(value -> value.isBlank())) {
            throw new IllegalArgumentException("category values must not be blank");
        }
    }

    public boolean hasKeyword() {
        return keyword != null && !keyword.isBlank();
    }

    public boolean hasCategory() {
        return category != null && !category.isBlank();
    }

    /** A single category or a comma-separated set of exact category values. */
    public List<String> categoryValues() {
        if (!hasCategory()) {
            return List.of();
        }
        return Arrays.stream(category.split(",", -1))
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .toList();
    }

    public boolean hasCity() {
        return city != null && !city.isBlank();
    }

    public boolean hasCampus() {
        return campus != null && !campus.isBlank();
    }
}
