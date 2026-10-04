package za.ac.cput.prm_marketplace.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.prm_marketplace.domain.OrderItem;
import za.ac.cput.prm_marketplace.security.CurrentCaller;
import za.ac.cput.prm_marketplace.service.IOrderItemService;

import java.util.List;
import java.util.UUID;

/**
 * A read-only view of the lines on the caller's orders.
 *
 * <p>The POST, PUT and DELETE routes here are gone, along with the bare {@code GET} that returned
 * every line item in the system. Line items are created by checkout and the order total is
 * calculated from them, so accepting them over the API let a caller set their own price on a line,
 * move a line onto somebody else's order, or remove a line from a live order.
 */
@RestController
@RequestMapping("/api/order-items")
public class OrderItemController {

    private final IOrderItemService orderItemService;

    public OrderItemController(IOrderItemService orderItemService) {
        this.orderItemService = orderItemService;
    }

    /** A line item on one of the caller's own orders. Somebody else's reads as not found. */
    @GetMapping("/{id}")
    public ResponseEntity<OrderItem> read(@PathVariable UUID id, Authentication authentication) {
        OrderItem orderItem = orderItemService.read(id, CurrentCaller.id(authentication));
        if (orderItem == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(orderItem);
    }

    /**
     * The lines on one of the caller's own orders. Returns an empty list for an order that is not
     * theirs, rather than revealing that the order exists.
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<OrderItem>> getByOrderId(@PathVariable UUID orderId,
                                                         Authentication authentication) {
        return ResponseEntity.ok(orderItemService.getByOrderId(orderId, CurrentCaller.id(authentication)));
    }
}