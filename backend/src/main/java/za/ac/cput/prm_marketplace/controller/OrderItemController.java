package za.ac.cput.prm_marketplace.controller;

import za.ac.cput.prm_marketplace.domain.OrderItem;
import za.ac.cput.prm_marketplace.service.IOrderItemService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/order-items")
public class OrderItemController {

    private final IOrderItemService orderItemService;

    public OrderItemController(IOrderItemService orderItemService) {
        this.orderItemService = orderItemService;
    }

    @PostMapping
    public ResponseEntity<OrderItem> create(@RequestBody OrderItem orderItem) {
        OrderItem created = orderItemService.create(orderItem);

        if (created == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderItem> read(@PathVariable UUID id) {
        OrderItem orderItem = orderItemService.read(id);

        if (orderItem == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(orderItem);
    }

    @PutMapping
    public ResponseEntity<OrderItem> update(@RequestBody OrderItem orderItem) {
        OrderItem updated = orderItemService.update(orderItem);

        if (updated == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        boolean deleted = orderItemService.delete(id);

        if (!deleted) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<OrderItem>> getAll() {
        return ResponseEntity.ok(orderItemService.getAll());
    }
}