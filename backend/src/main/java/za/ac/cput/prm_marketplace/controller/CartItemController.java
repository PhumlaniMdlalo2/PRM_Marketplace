package za.ac.cput.prm_marketplace.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.prm_marketplace.domain.CartItem;
import za.ac.cput.prm_marketplace.service.ICartItemService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cart-items")
public class CartItemController {
    private final ICartItemService cartItemService;

    @Autowired
    public CartItemController(ICartItemService cartItemService) {
        this.cartItemService = cartItemService;
    }

    @PostMapping
    public ResponseEntity<CartItem> create(@RequestBody CartItem cartItem) {
        CartItem created = cartItemService.create(cartItem);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CartItem> read(@PathVariable UUID id) {
        CartItem cartItem = cartItemService.read(id);
        if (cartItem == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(cartItem);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CartItem> update(@PathVariable UUID id, @RequestBody CartItem cartItem) {
        CartItem existing = cartItemService.read(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        CartItem toUpdate = CartItem.builder().copy(cartItem).id(id).build();
        CartItem updated = cartItemService.update(toUpdate);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        CartItem existing = cartItemService.read(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        cartItemService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<CartItem>> getAll() {
        return ResponseEntity.ok(cartItemService.getAll());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CartItem>> getByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(cartItemService.getByUser(userId));
    }

    @PatchMapping("/{id}/quantity")
    public ResponseEntity<CartItem> updateQuantity(@PathVariable UUID id, @RequestParam int quantity) {
        CartItem existing = cartItemService.read(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        CartItem toUpdate = CartItem.builder().copy(existing).quantity(quantity).build();
        CartItem updated = cartItemService.update(toUpdate);
        return ResponseEntity.ok(updated);
    }
}
