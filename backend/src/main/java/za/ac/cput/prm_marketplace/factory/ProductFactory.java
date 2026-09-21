package za.ac.cput.prm_marketplace.factory;

import za.ac.cput.prm_marketplace.domain.Product;

import java.math.BigDecimal;
import java.util.UUID;

public class ProductFactory {

    public static Product createProduct(String name, String description, BigDecimal price,
                                        String category, int stockQuantity, UUID vendorId) {

        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Product name is required");
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Price must be greater than zero");
        if (category == null || category.isBlank())
            throw new IllegalArgumentException("Category is required");
        if (stockQuantity < 0)
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        if (vendorId == null)
            throw new IllegalArgumentException("Vendor ID is required");

        return new Product.Builder()
                .name(name)
                .description(description)
                .price(price)
                .category(category)
                .stockQuantity(stockQuantity)
                .build();
    }
}
