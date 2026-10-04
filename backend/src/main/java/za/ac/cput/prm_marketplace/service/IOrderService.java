package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.Address;
import za.ac.cput.prm_marketplace.domain.CartItem;
import za.ac.cput.prm_marketplace.domain.Order;
import za.ac.cput.prm_marketplace.domain.OrderStatus;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Every method that touches a single order takes the id of the caller making the request. That is
 * deliberate: an order belongs to exactly one buyer, and a signature that only accepts an order id
 * cannot tell "your order" apart from "anybody's order".
 *
 * <p>Methods return {@code null} or {@code false} when the caller does not own the order rather
 * than throwing, so that a request for someone else's order is indistinguishable from a request
 * for an order that does not exist. Answering 403 instead would confirm the order is real.
 */
public interface IOrderService {

    /**
     * Reads an order only if {@code requesterId} is the buyer.
     */
    Order read(UUID id, UUID requesterId);

    /**
     * Applies the buyer-editable part of an order. Status, total, buyer and timestamps are not
     * editable here; status moves through {@link #updateStatus} instead.
     */
    Order update(Order order, UUID requesterId);

    /** Deletes an order only if {@code requesterId} is the buyer. */
    boolean delete(UUID id, UUID requesterId);

    /** Every order belonging to {@code requesterId}. Never another user's orders. */
    List<Order> getAll(UUID requesterId);

    /** Kept for callers that already know the buyer id is the caller's own. */
    List<Order> getByBuyer(UUID buyerId);

    List<Order> getByBuyerAndStatus(UUID buyerId, OrderStatus status);

    /**
     * Turns the caller's stored cart into an order.
     *
     * <p>This is the only way an order comes into existence. There used to be a second path that
     * built an order from a request body, which let a client choose the line items, the prices and
     * the buyer; removing it removes the whole class of problem rather than filtering the fields.
     *
     * <p>Prices are read from the catalogue rather than from the request, stock is reserved
     * atomically, and the cart is emptied only once the order exists.
     */
    Order checkout(UUID buyerId, UUID shippingAddressId);

    /**
     * Moves an order through its lifecycle.
     *
     * <p>Two things are checked before anything is written. First, authority: faculty may move any
     * order, and a vendor may only move an order that contains one of their own products. Holding
     * a vendor account is not on its own enough — otherwise every seller could advance every other
     * seller's orders by guessing ids. Second, legality: the move has to be a real step in the
     * lifecycle, so a delivered or cancelled order cannot be reopened.
     *
     * @param requesterId the caller, resolved from the token
     * @return the updated order, or null when the caller may not make this move
     */
    Order updateStatus(UUID id, OrderStatus status, UUID requesterId, Role role);

    /** Cancels an order the caller owns, returning any reserved stock. */
    boolean cancel(UUID id, UUID requesterId);

    BigDecimal calculateTotal(List<CartItem> cartItems);
}