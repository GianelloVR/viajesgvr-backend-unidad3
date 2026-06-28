package com.example.viajesgvr.controllers;

import com.example.viajesgvr.entities.PaymentEntity;
import com.example.viajesgvr.services.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin("*")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/")
    public ResponseEntity<List<PaymentEntity>> getAllPayments() {
        List<PaymentEntity> payments = paymentService.getAllPayments();
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentEntity> getPaymentById(@PathVariable Long id) {
        return paymentService.getPaymentById(id)
                .map(payment -> ResponseEntity.ok(payment))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/reservation/{reservationId}")
    public ResponseEntity<?> getPaymentByReservationId(@PathVariable Long reservationId) {
        try {
            return paymentService.getPaymentByReservationId(reservationId)
                    .<ResponseEntity<?>>map(payment -> ResponseEntity.ok(payment))
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "Pago no encontrado para la reserva indicada")));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/")
    public ResponseEntity<?> createPayment(@RequestBody PaymentEntity payment) {
        try {
            PaymentEntity newPayment = paymentService.createPayment(payment);
            return ResponseEntity.status(HttpStatus.CREATED).body(newPayment);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/purchase-group/{purchaseGroupCode}")
    public ResponseEntity<?> createPaymentsForPurchaseGroup(@PathVariable String purchaseGroupCode,
                                                            @RequestBody PaymentEntity payment) {
        try {
            List<PaymentEntity> newPayments = paymentService.createPaymentsForPurchaseGroup(
                    purchaseGroupCode,
                    payment
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(newPayments);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
