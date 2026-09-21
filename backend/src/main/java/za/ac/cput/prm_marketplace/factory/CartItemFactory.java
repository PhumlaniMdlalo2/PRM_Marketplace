package za.ac.cput.prm_marketplace.factory;

import za.ac.cput.prm_marketplace.domain.CartItem;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.domain.User;

import java.time.LocalDateTime;

public class CartItemFactory {

    public static CartItem createCartItem(User user, Product product, int quantity) {

        if (user == null)
            throw new IllegalArgumentException("User is required");
        if (product == null)
            throw new IllegalArgumentException("Product is required");
        if (quantity <= 0)
            throw new IllegalArgumentException("Quantity must be atleast 1");

        return new CartItem.Builder()
                .user(user)
                .product(product)
                .quantity(quantity)
                .addedAt(LocalDateTime.now())
                .build();
    }
}
