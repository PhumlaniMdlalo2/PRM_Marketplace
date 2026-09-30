package za.ac.cput.prm_marketplace.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.prm_marketplace.domain.Order;
import za.ac.cput.prm_marketplace.domain.OrderStatus;
import za.ac.cput.prm_marketplace.service.IOrderService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final IOrderService orderService;

    @Autowired
    public OrderController(IOrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<Order> create(@RequestBody Order order) {
        Order created = orderService.create(order);
        if (created == null) {
            return ResponseEntity.badRequest().build();
        }
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> read(@PathVariable UUID id) {
        Order order = orderService.read(id);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(order);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Order> update(@PathVariable UUID id, @RequestBody Order order) {
        Order existing = orderService.read(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        Order toUpdate = new Order.Builder().copy(order).setId(id).build();
        Order updated = orderService.update(toUpdate);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        boolean deleted = orderService.delete(id);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<Order>> getAll() {
        return ResponseEntity.ok(orderService.getAll());
    }

    @GetMapping("/buyer/{buyerId}")
    public ResponseEntity<List<Order>> getByBuyer(@PathVariable UUID buyerId) {
        return ResponseEntity.ok(orderService.getByBuyer(buyerId));
    }

    @GetMapping("/buyer/{buyerId}/status/{status}")
    public ResponseEntity<List<Order>> getByBuyerAndStatus(@PathVariable UUID buyerId,
                                                            @PathVariable OrderStatus status) {
        return ResponseEntity.ok(orderService.getByBuyerAndStatus(buyerId, status));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Order> updateStatus(@PathVariable UUID id,
                                               @RequestParam OrderStatus status) {
        Order updated = orderService.updateStatus(id, status);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable UUID id, @RequestParam UUID buyerId) {
        boolean cancelled = orderService.cancel(id, buyerId);
        if (!cancelled) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.noContent().build();
    }
}