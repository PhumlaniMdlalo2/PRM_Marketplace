package za.ac.cput.prm_marketplace.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.prm_marketplace.domain.CartItem;
import za.ac.cput.prm_marketplace.security.CurrentCaller;
import za.ac.cput.prm_marketplace.service.ICartItemService;

import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/cart-items")
public class CartItemController {

    private final ICartItemService cartItemService;

    @Autowired
    public CartItemController(ICartItemService cartItemService) {
        this.cartItemService = cartItemService;
    }

    /**
     * The caller's cart. This replaced both the old "/user/{userId}" route and the bare
     * "GET /api/cart-items", which returned every cart in the system.
     */
    @GetMapping
    public ResponseEntity<List<CartItem>> getAll(Authentication authentication) {
        return ResponseEntity.ok(cartItemService.getByUser(CurrentCaller.id(authentication)));
    }

    /**
     * Adds to the caller's own cart. The userId parameter is gone: it used to be taken from the
     * request, so anyone could add items to anybody's cart and influence their checkout total.
     */
    @PostMapping
    public ResponseEntity<CartItem> addToCart(@RequestParam UUID productId,
                                             @RequestParam int quantity,
                                             Authentication authentication) {
        CartItem created = cartItemService.addToCart(CurrentCaller.id(authentication), productId, quantity);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CartItem> read(@PathVariable UUID id, Authentication authentication) {
        CartItem cartItem = cartItemService.read(id, CurrentCaller.id(authentication));
        if (cartItem == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(cartItem);
    }

    /** Setting the quantity to zero or less removes the line. */
    @PutMapping("/{id}/quantity")
    public ResponseEntity<CartItem> updateQuantity(@PathVariable UUID id,
                                                   @RequestParam int quantity,
                                                   Authentication authentication) {
        CartItem updated = cartItemService.updateQuantity(id, CurrentCaller.id(authentication), quantity);
        if (updated == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Nothing to return: the change was applied and there is no state left to read.")
    })
    public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication authentication) {
        if (!cartItemService.delete(id, CurrentCaller.id(authentication))) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    /** Empties the caller's cart. The former "/user/{userId}" route is gone. */
    @DeleteMapping
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Nothing to return: the change was applied and there is no state left to read.")
    })
    public ResponseEntity<Void> clearCart(Authentication authentication) {
        cartItemService.clearCart(CurrentCaller.id(authentication));
        return ResponseEntity.noContent().build();
    }
}
