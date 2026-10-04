package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.OrderItem;

import java.util.List;
import java.util.UUID;

/**
 * Read-only view of the line items on the caller's own orders.
 *
 * <p>The create, update, delete and list-everything methods that used to be here were removed
 * rather than guarded, because each one was a way to change what an order costs:
 * <ul>
 *   <li>create accepted a whole {@code OrderItem} from the body, so a caller could attach a line
 *       to somebody else's order at a price they chose;</li>
 *   <li>update replaced a stored row wholesale, including its order, product and
 *       {@code priceAtPurchase};</li>
 *   <li>delete removed lines from a live order;</li>
 *   <li>getAll returned every line item in the system, which is every customer's purchase history.</li>
 * </ul>
 *
 * <p>Lines are created by checkout in {@code OrderServiceImpl} from the cart and the catalogue,
 * and the order total is derived from them. Restricting writes to that one path is what keeps an
 * order total equal to the sum of its lines.
 */
public interface IOrderItemService {

    /** A line item on one of the caller's own orders, or null if it does not exist or is not theirs. */
    OrderItem read(UUID id, UUID requesterId);

    /**
     * The lines on one of the caller's own orders, oldest first. Returns an empty list for an
     * order that is not theirs, so this cannot be used to discover another account's orders.
     */
    List<OrderItem> getByOrderId(UUID orderId, UUID requesterId);
}