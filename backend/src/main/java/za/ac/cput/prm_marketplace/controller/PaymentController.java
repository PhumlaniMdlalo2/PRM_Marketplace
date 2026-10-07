package za.ac.cput.prm_marketplace.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.prm_marketplace.domain.Payment;
import za.ac.cput.prm_marketplace.domain.PaymentStatus;
import za.ac.cput.prm_marketplace.domain.PaymentSimulationOutcome;
import za.ac.cput.prm_marketplace.dto.PaymentInstruction;
import za.ac.cput.prm_marketplace.security.CurrentCaller;
import za.ac.cput.prm_marketplace.service.IPaymentService;

import java.util.List;
import java.util.UUID;

/**
 * Mounted under "/api" to match the rest of the application; the old "/payments" mapping sat
 * outside the group the security rules are written against.
 *
 * <p>Every route here is scoped to the caller. The previous controller accepted a user id in the
 * path ({@code GET /payments/user/{userId}}), let any caller read or edit any payment by id, and
 * returned every payment in the system from a bare {@code GET /payments}.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final IPaymentService paymentService;

    public PaymentController(IPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Starts a payment against one of the caller's own orders. The payer is taken from the token;
     * the body's {@code userId} is ignored because the entity marks it read-only.
     */
    @PostMapping
    public ResponseEntity<Payment> create(@RequestBody Payment payment, Authentication authentication) {
        Payment created = paymentService.create(payment, CurrentCaller.id(authentication));
        if (created == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** The caller's own payments. This route replaces {@code GET /payments}, which returned everyone's. */
    @GetMapping
    public ResponseEntity<List<Payment>> getAll(Authentication authentication) {
        return ResponseEntity.ok(paymentService.getByUserId(CurrentCaller.id(authentication)));
    }

    @GetMapping("/simulation")
    public ResponseEntity<Boolean> simulationEnabled() {
        return ResponseEntity.ok(paymentService.isSimulationEnabled());
    }

    /**
     * One of the caller's own payments. A payment belonging to another account is reported as
     * not found rather than forbidden, so the response cannot be used to probe for valid ids.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Payment> read(@PathVariable UUID id, Authentication authentication) {
        Payment payment = paymentService.read(id, CurrentCaller.id(authentication));
        if (payment == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(payment);
    }

    /**
     * Changes the method on a PENDING payment of the caller's own. The payment is named by the
     * path, so a body id is never used; the old route took the id from the body, which let a
     * caller name any payment.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Payment> update(@PathVariable UUID id,
                                          @RequestBody Payment payment,
                                          Authentication authentication) {
        Payment updated = paymentService.update(id, payment, CurrentCaller.id(authentication));
        if (updated == null) {
            // Either it is not the caller's payment, or it is no longer PENDING.
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    /**
     * Payments for one of the caller's own orders. Returns an empty list for an order that is not
     * theirs, so this cannot be used to discover another account's orders.
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<Payment>> getByOrderId(@PathVariable UUID orderId,
                                                      Authentication authentication) {
        return ResponseEntity.ok(paymentService.getByOrderId(orderId, CurrentCaller.id(authentication)));
    }

    @GetMapping("/order/{orderId}/instructions")
    public ResponseEntity<List<PaymentInstruction>> getPaymentInstructions(
            @PathVariable UUID orderId, Authentication authentication) {
        return ResponseEntity.ok(paymentService.getPaymentInstructions(orderId, CurrentCaller.id(authentication)));
    }

    @GetMapping("/seller")
    public ResponseEntity<List<PaymentInstruction>> getSellerPayments(Authentication authentication) {
        return ResponseEntity.ok(paymentService.getSellerPayments(
                CurrentCaller.id(authentication), CurrentCaller.role(authentication)));
    }

    @PostMapping("/{id}/confirm-receipt")
    public ResponseEntity<Payment> confirmReceipt(@PathVariable UUID id, Authentication authentication) {
        Payment payment = paymentService.confirmReceipt(id,
                CurrentCaller.id(authentication), CurrentCaller.role(authentication));
        if (payment == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(payment);
    }

    @PostMapping("/{id}/simulate")
    public ResponseEntity<Payment> simulate(@PathVariable UUID id,
                                            @RequestParam PaymentSimulationOutcome outcome,
                                            Authentication authentication) {
        Payment payment = paymentService.simulate(id, outcome, CurrentCaller.id(authentication));
        if (payment == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(payment);
    }

    /**
     * Moves the caller's own payment through its lifecycle.
     *
     * <p>Settling a payment -- COMPLETED, FAILED or REFUNDED -- requires ADMIN. For anyone else the
     * service refuses the change and this reports the payment as not found, so a buyer cannot mark
     * their own attempt as paid or failed. Retrying (PENDING) stays open to the payer.
     *
     * <p>The role comes from the validated token via {@link CurrentCaller}, never from the request,
     * so naming a role in the body or query string cannot elevate the caller.
     *
     * <p>The previous version let any authenticated caller move any payment to any status.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Payment> updateStatus(@PathVariable UUID id,
                                                @RequestParam PaymentStatus status,
                                                Authentication authentication) {
        Payment updated = paymentService.updateStatus(id, status,
                CurrentCaller.id(authentication), CurrentCaller.role(authentication));
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }
}