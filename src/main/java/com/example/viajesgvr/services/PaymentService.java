package com.example.viajesgvr.services;

import com.example.viajesgvr.entities.PaymentEntity;
import com.example.viajesgvr.entities.ReservationEntity;
import com.example.viajesgvr.repositories.PaymentRepository;
import com.example.viajesgvr.repositories.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;

    public PaymentService(PaymentRepository paymentRepository,
                          ReservationRepository reservationRepository,
                          ReservationService reservationService) {
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
        this.reservationService = reservationService;
    }

    public List<PaymentEntity> getAllPayments() {
        return paymentRepository.findAll();
    }

    public Optional<PaymentEntity> getPaymentById(Long id) {
        return paymentRepository.findById(id);
    }

    public Optional<PaymentEntity> getPaymentByReservationId(Long reservationId) {
        ReservationEntity reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        return paymentRepository.findByReservation(reservation);
    }

    @Transactional
    public PaymentEntity createPayment(PaymentEntity payment) {
        validatePaymentRequest(payment);

        Long reservationId = payment.getReservation().getId();

        ReservationEntity reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        reservation = reservationService.expireReservationIfNeeded(reservation);

        validateReservationForPayment(reservation);

        if (paymentRepository.existsByReservation(reservation)) {
            throw new RuntimeException("La reserva ya tiene un pago registrado");
        }

        validatePaymentAmount(payment.getAmount(), reservation.getFinalTotalAmount(),
                "El monto del pago debe coincidir con el total de la reserva");

        PaymentEntity newPayment = buildApprovedPayment(reservation, payment, reservation.getFinalTotalAmount());

        reservation.setStatus(ReservationEntity.STATUS_CONFIRMED);
        reservationRepository.save(reservation);

        return paymentRepository.save(newPayment);
    }

    @Transactional
    public List<PaymentEntity> createPaymentsForPurchaseGroup(String purchaseGroupCode, PaymentEntity payment) {
        validatePurchaseGroupPaymentRequest(purchaseGroupCode, payment);

        List<ReservationEntity> reservations = reservationRepository.findByPurchaseGroupCode(purchaseGroupCode);

        if (reservations.isEmpty()) {
            throw new RuntimeException("Compra multiple no encontrada");
        }

        List<ReservationEntity> updatedReservations = new ArrayList<>();

        for (ReservationEntity reservation : reservations) {
            updatedReservations.add(reservationService.expireReservationIfNeeded(reservation));
        }

        for (ReservationEntity reservation : updatedReservations) {
            validateReservationForPayment(reservation);

            if (paymentRepository.existsByReservation(reservation)) {
                throw new RuntimeException("Una reserva del grupo ya tiene un pago registrado");
            }
        }

        double groupTotalAmount = roundCurrency(updatedReservations.stream()
                .mapToDouble(reservation -> reservation.getFinalTotalAmount() != null
                        ? reservation.getFinalTotalAmount()
                        : 0.0)
                .sum());

        validatePaymentAmount(payment.getAmount(), groupTotalAmount,
                "El monto del pago debe coincidir con el total de la compra multiple");

        List<PaymentEntity> createdPayments = new ArrayList<>();

        for (ReservationEntity reservation : updatedReservations) {
            PaymentEntity newPayment = buildApprovedPayment(
                    reservation,
                    payment,
                    reservation.getFinalTotalAmount()
            );

            reservation.setStatus(ReservationEntity.STATUS_CONFIRMED);
            reservationRepository.save(reservation);

            createdPayments.add(paymentRepository.save(newPayment));
        }

        return createdPayments;
    }

    private PaymentEntity buildApprovedPayment(ReservationEntity reservation,
                                               PaymentEntity payment,
                                               Double amount) {
        PaymentEntity newPayment = new PaymentEntity();
        newPayment.setReservation(reservation);
        newPayment.setAmount(amount);
        newPayment.setPaymentMethod(PaymentEntity.PAYMENT_METHOD_CREDIT_CARD);
        newPayment.setPaymentStatus(PaymentEntity.PAYMENT_STATUS_APPROVED);
        newPayment.setPaymentDate(LocalDateTime.now());
        newPayment.setCardNumber(payment.getCardNumber());
        newPayment.setCardExpiration(payment.getCardExpiration());
        newPayment.setCardCvv(payment.getCardCvv());

        return newPayment;
    }

    private void validatePaymentRequest(PaymentEntity payment) {
        if (payment.getReservation() == null || payment.getReservation().getId() == null) {
            throw new RuntimeException("La reserva es obligatoria");
        }

        validateCardData(payment);
    }

    private void validatePurchaseGroupPaymentRequest(String purchaseGroupCode, PaymentEntity payment) {
        if (purchaseGroupCode == null || purchaseGroupCode.isBlank()) {
            throw new RuntimeException("El codigo de compra es obligatorio");
        }

        validateCardData(payment);
    }

    private void validateCardData(PaymentEntity payment) {
        if (payment.getCardNumber() == null || payment.getCardNumber().isBlank()) {
            throw new RuntimeException("El numero de tarjeta es obligatorio");
        }

        if (payment.getCardExpiration() == null || payment.getCardExpiration().isBlank()) {
            throw new RuntimeException("La fecha de expiracion es obligatoria");
        }

        if (payment.getCardCvv() == null || payment.getCardCvv().isBlank()) {
            throw new RuntimeException("El CVV es obligatorio");
        }
    }

    private void validatePaymentAmount(Double paymentAmount, Double expectedAmount, String errorMessage) {
        if (paymentAmount == null || paymentAmount <= 0) {
            throw new RuntimeException("El monto del pago debe ser mayor que cero");
        }

        if (Double.compare(roundCurrency(paymentAmount), roundCurrency(expectedAmount)) != 0) {
            throw new RuntimeException(errorMessage);
        }
    }

    private void validateReservationForPayment(ReservationEntity reservation) {
        if (ReservationEntity.STATUS_CANCELED.equals(reservation.getStatus())) {
            throw new RuntimeException("No se puede registrar un pago para una reserva cancelada");
        }

        if (ReservationEntity.STATUS_EXPIRED.equals(reservation.getStatus())) {
            throw new RuntimeException("No se puede registrar un pago para una reserva expirada");
        }

        if (ReservationEntity.STATUS_CONFIRMED.equals(reservation.getStatus())) {
            throw new RuntimeException("La reserva ya se encuentra confirmada");
        }

        if (!ReservationEntity.STATUS_PENDING_PAYMENT.equals(reservation.getStatus())) {
            throw new RuntimeException("La reserva no esta disponible para pago");
        }
    }

    private double roundCurrency(Double amount) {
        if (amount == null) {
            return 0.0;
        }

        return Math.round(amount * 100.0) / 100.0;
    }
}
