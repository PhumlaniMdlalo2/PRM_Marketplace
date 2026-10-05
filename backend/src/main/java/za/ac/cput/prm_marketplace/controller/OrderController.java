package za.ac.cput.prm_marketplace.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.prm_marketplace.domain.Order;
import za.ac.cput.prm_marketplace.domain.OrderStatus;
import za.ac.cput.prm_marketplace.domain.PaymentMethod;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.exception.UnauthorizedException;
import za.ac.cput.prm_marketplace.security.UserPrincipal;
import za.ac.cput.prm_marketplace.service.IOrderService;

import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * Every handler takes the caller from the validated token via {@link UserPrincipal}. Nothing here
 * reads an owner id out of the path, the query string or the body: those are all attacker-supplied,
 * and an order's owner is the one thing the client must never get to choose.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final IOrderService orderService;

    @Autowired
    public OrderController(IOrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Checks out the caller's stored cart.
     *
     * <p>This used to accept a full {@code Order} body and save it, which let a client pick the
     * line items, the prices, the buyer and the status. Ordering now happens only from the cart the
     * server holds, so the body is gone entirely and the caller only names an address they own.
     */
    @PostMapping
    public ResponseEntity<Order> create(Authentication caller,
                                        @RequestParam(required = false) UUID shippingAddressId,
                                        @RequestParam(required = false) PaymentMethod paymentMethod) {
        Order placed = orderService.checkout(callerId(caller), shippingAddressId, paymentMethod);
        if (placed == null) {
            return ResponseEntity.badRequest().build();
        }
        return new ResponseEntity<>(placed, HttpStatus.CREATED);
    }

    /**
     * Checks out the caller's stored cart.
     *
     * <p>The cart, the prices and the shipping address ownership are all resolved server-side, so
     * the body only needs to name an address the caller owns. {@code paymentMethod} is how the
     * buyer intends to pay and defaults to CARD; the amount is never taken from the client.
     */
    @PostMapping("/checkout")
    public ResponseEntity<Order> checkout(Authentication caller,
                                          @RequestParam(required = false) UUID shippingAddressId,
                                          @RequestParam(required = false) PaymentMethod paymentMethod) {
        Order placed = orderService.checkout(callerId(caller), shippingAddressId, paymentMethod);
        if (placed == null) {
            return ResponseEntity.badRequest().build();
        }
        return new ResponseEntity<>(placed, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> read(Authentication caller,
                                      @PathVariable UUID id) {
        Order order = orderService.read(id, callerId(caller));
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(order);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Order> update(Authentication caller,
                                        @PathVariable UUID id,
                                        @RequestBody Order order) {
        Order.Builder builder = new Order.Builder().copy(order).setId(id);
        Order updated = orderService.update(builder.build(), callerId(caller));
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Nothing to return: the change was applied and there is no state left to read.")
    })
    public ResponseEntity<Void> delete(Authentication caller,
                                       @PathVariable UUID id) {
        boolean deleted = orderService.delete(id, callerId(caller));
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    /** The caller's own orders. The previous version had no buyer filter and returned every row. */
    @GetMapping
    public ResponseEntity<List<Order>> getAll(Authentication caller) {
        return ResponseEntity.ok(orderService.getAll(callerId(caller)));
    }

    /**
     * Replaced by {@code GET /api/orders}. Kept so existing clients do not break, but it now only
     * answers when the path id is the caller's own; otherwise it reports nothing found rather than
     * another user's orders.
     */
    @GetMapping("/buyer/{buyerId}")
    public ResponseEntity<List<Order>> getByBuyer(Authentication caller,
                                                  @PathVariable UUID buyerId) {
        if (!callerId(caller).equals(buyerId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(orderService.getByBuyer(buyerId));
    }

    @GetMapping("/buyer/{buyerId}/status/{status}")
    public ResponseEntity<List<Order>> getByBuyerAndStatus(Authentication caller,
                                                            @PathVariable UUID buyerId,
                                                            @PathVariable OrderStatus status) {
        if (!callerId(caller).equals(buyerId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(orderService.getByBuyerAndStatus(buyerId, status));
    }

/**
     * Sellers and staff advancing a sale.
     *
     * <p>The caller id is now passed alongside the role. The role alone was not enough: any vendor
     * could move any order, because the service had no way to tell whose order it was. The service
     * now checks that a vendor actually sells something on the order, and that the move is a legal
     * step in the lifecycle.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Order> updateStatus(Authentication caller,
                                              @PathVariable UUID id,
                                              @RequestParam OrderStatus status) {
        Order updated = orderService.updateStatus(id, status, callerId(caller), callerRole(caller));
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    /**
     * The buyer id used to arrive as a query parameter, which meant anyone could cancel anybody's
     * order by passing the victim's id. It now comes from the token.
     */
    @PatchMapping("/{id}/cancel")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Nothing to return: the change was applied and there is no state left to read.")
    })
    public ResponseEntity<Void> cancel(Authentication caller,
                                       @PathVariable UUID id) {
        boolean cancelled = orderService.cancel(id, callerId(caller));
        if (!cancelled) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * The authenticated account behind the request. Resolving the principal here, rather than
     * trusting any id in the request, is what stops one user acting on another's data.
     */
    private static UserPrincipal principal(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal user) {
            return user;
        }
        throw new UnauthorizedException("Authentication is required");
    }

    private static UUID callerId(Authentication authentication) {
        return principal(authentication).getId();
    }

    private static Role callerRole(Authentication authentication) {
        return principal(authentication).getRole();
    }
}
