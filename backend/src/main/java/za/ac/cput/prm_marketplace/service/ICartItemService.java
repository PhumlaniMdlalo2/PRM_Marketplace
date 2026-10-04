package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.CartItem;

import java.util.List;
import java.util.UUID;

/**
 * A cart belongs to exactly one account. Every operation is scoped by the caller's id, so there is
 * no way to read, change or empty somebody else's cart.
 */
public interface ICartItemService {

    /**
     * Adds a product to the caller's cart, or increases the quantity if it is already there.
     * Prices and stock are never taken from the request; checkout re-reads them from the catalogue.
     */
    CartItem addToCart(UUID requesterId, UUID productId, int quantity);

    /** @return the cart line, or null when it does not exist or belongs to someone else */
    CartItem read(UUID id, UUID requesterId);

    /**
     * Sets the quantity on one of the caller's cart lines. A quantity of zero or less removes the
     * line, which is how the "remove from cart" action is expressed.
     */
    CartItem updateQuantity(UUID id, UUID requesterId, int quantity);

    /** @return false when the line does not exist or belongs to someone else */
    boolean delete(UUID id, UUID requesterId);

    List<CartItem> getByUser(UUID requesterId);

    void clearCart(UUID requesterId);
}