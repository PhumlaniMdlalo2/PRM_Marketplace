package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.CartItem;

import java.util.List;
import java.util.UUID;

public interface ICartItemService {

    CartItem create(CartItem cartItem);
    CartItem read(UUID id);
    CartItem update(CartItem cartItem);
    boolean delete(UUID id);
    List<CartItem> getAll();

    //Cart-specific
    List<CartItem> getByUser(UUID userId);
    CartItem addToCart(UUID userId, UUID productId, int quantity);
    CartItem updateQuantity(UUID cartItemId, int quantity);
    void clearCart(UUID userId);
}
