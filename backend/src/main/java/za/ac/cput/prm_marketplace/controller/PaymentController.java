package za.ac.cput.prm_marketplace.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.prm_marketplace.domain.Payment;
import za.ac.cput.prm_marketplace.domain.PaymentStatus;
import za.ac.cput.prm_marketplace.service.IPaymentService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final IPaymentService paymentService;

    public PaymentController(IPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<Payment> create(@RequestBody Payment payment) {
        Payment created = paymentService.create(payment);

        if (created == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payment> read(@PathVariable UUID id) {
        Payment payment = paymentService.read(id);

        if (payment == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(payment);
    }

    @PutMapping
    public ResponseEntity<Payment> update(@RequestBody Payment payment) {
        if (payment.getId() == null || paymentService.read(payment.getId()) == null) {
            return ResponseEntity.notFound().build();
        }

        Payment updated = paymentService.update(payment);

        if (updated == null) {
            // payment exists but is no longer PENDING
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        boolean deleted = paymentService.delete(id);

        if (!deleted) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<Payment>> getAll() {
        return ResponseEntity.ok(paymentService.getAll());
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<Payment>> getByOrderId(@PathVariable UUID orderId) {
        return ResponseEntity.ok(paymentService.getByOrderId(orderId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Payment>> getByUserId(@PathVariable UUID userId) {
        return ResponseEntity.ok(paymentService.getByUserId(userId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Payment> updateStatus(@PathVariable UUID id,
                                                @RequestParam PaymentStatus status) {
        if (paymentService.read(id) == null) {
            return ResponseEntity.notFound().build();
        }

        Payment updated = paymentService.updateStatus(id, status);

        if (updated == null) {
            // payment exists but the requested status change is not allowed
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        return ResponseEntity.ok(updated);
    }
}

