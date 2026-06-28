package com.example.viajesgvr.services;

import com.example.viajesgvr.entities.PaymentEntity;
import com.example.viajesgvr.entities.ReservationEntity;
import com.example.viajesgvr.entities.TourPackageEntity;
import com.example.viajesgvr.entities.UserEntity;
import com.example.viajesgvr.repositories.PaymentRepository;
import com.example.viajesgvr.repositories.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservationService reservationService;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void getAllPaymentsShouldReturnPayments() {
        PaymentEntity payment = savedPayment();

        when(paymentRepository.findAll()).thenReturn(List.of(payment));

        List<PaymentEntity> result = paymentService.getAllPayments();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());

        verify(paymentRepository).findAll();
    }

    @Test
    void getPaymentByIdShouldReturnPayment() {
        PaymentEntity payment = savedPayment();

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        Optional<PaymentEntity> result = paymentService.getPaymentById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());

        verify(paymentRepository).findById(1L);
    }

    @Test
    void getPaymentByReservationIdShouldReturnPayment() {
        ReservationEntity reservation = pendingReservation();
        PaymentEntity payment = savedPayment();

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(paymentRepository.findByReservation(reservation)).thenReturn(Optional.of(payment));

        Optional<PaymentEntity> result = paymentService.getPaymentByReservationId(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());

        verify(reservationRepository).findById(1L);
        verify(paymentRepository).findByReservation(reservation);
    }

    @Test
    void getPaymentByReservationIdShouldThrowExceptionWhenReservationDoesNotExist() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paymentService.getPaymentByReservationId(1L));

        assertEquals("Reserva no encontrada", exception.getMessage());

        verify(reservationRepository).findById(1L);
        verify(paymentRepository, never()).findByReservation(any(ReservationEntity.class));
    }

    @Test
    void createPaymentShouldApprovePaymentAndConfirmReservation() {
        ReservationEntity reservation = pendingReservation();
        PaymentEntity request = paymentRequest();

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(reservationService.expireReservationIfNeeded(reservation)).thenReturn(reservation);
        when(paymentRepository.existsByReservation(reservation)).thenReturn(false);
        when(paymentRepository.save(any(PaymentEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentEntity result = paymentService.createPayment(request);

        assertEquals(reservation, result.getReservation());
        assertEquals(200000.0, result.getAmount());
        assertEquals(PaymentEntity.PAYMENT_METHOD_CREDIT_CARD, result.getPaymentMethod());
        assertEquals(PaymentEntity.PAYMENT_STATUS_APPROVED, result.getPaymentStatus());
        assertNotNull(result.getPaymentDate());
        assertEquals("4111111111111111", result.getCardNumber());
        assertEquals("12/30", result.getCardExpiration());
        assertEquals("123", result.getCardCvv());

        assertEquals(ReservationEntity.STATUS_CONFIRMED, reservation.getStatus());

        verify(reservationService).expireReservationIfNeeded(reservation);
        verify(reservationRepository).save(reservation);
        verify(paymentRepository).save(any(PaymentEntity.class));
    }

    @Test
    void createPaymentShouldThrowExceptionWhenReservationIsMissing() {
        PaymentEntity request = paymentRequest();
        request.setReservation(null);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paymentService.createPayment(request));

        assertEquals("La reserva es obligatoria", exception.getMessage());

        verify(reservationRepository, never()).findById(any());
        verify(paymentRepository, never()).save(any(PaymentEntity.class));
    }

    @Test
    void createPaymentShouldThrowExceptionWhenCardNumberIsMissing() {
        PaymentEntity request = paymentRequest();
        request.setCardNumber(" ");

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paymentService.createPayment(request));

        assertEquals("El numero de tarjeta es obligatorio", exception.getMessage());

        verify(reservationRepository, never()).findById(any());
        verify(paymentRepository, never()).save(any(PaymentEntity.class));
    }

    @Test
    void createPaymentShouldThrowExceptionWhenCardExpirationIsMissing() {
        PaymentEntity request = paymentRequest();
        request.setCardExpiration(" ");

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paymentService.createPayment(request));

        assertEquals("La fecha de expiracion es obligatoria", exception.getMessage());

        verify(reservationRepository, never()).findById(any());
        verify(paymentRepository, never()).save(any(PaymentEntity.class));
    }

    @Test
    void createPaymentShouldThrowExceptionWhenCardCvvIsMissing() {
        PaymentEntity request = paymentRequest();
        request.setCardCvv(" ");

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paymentService.createPayment(request));

        assertEquals("El CVV es obligatorio", exception.getMessage());

        verify(reservationRepository, never()).findById(any());
        verify(paymentRepository, never()).save(any(PaymentEntity.class));
    }

    @Test
    void createPaymentShouldThrowExceptionWhenReservationIsCanceled() {
        ReservationEntity reservation = pendingReservation();
        reservation.setStatus(ReservationEntity.STATUS_CANCELED);

        PaymentEntity request = paymentRequest();

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(reservationService.expireReservationIfNeeded(reservation)).thenReturn(reservation);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paymentService.createPayment(request));

        assertEquals("No se puede registrar un pago para una reserva cancelada", exception.getMessage());

        verify(reservationService).expireReservationIfNeeded(reservation);
        verify(paymentRepository, never()).save(any(PaymentEntity.class));
    }

    @Test
    void createPaymentShouldThrowExceptionWhenReservationIsExpired() {
        ReservationEntity reservation = pendingReservation();
        reservation.setStatus(ReservationEntity.STATUS_EXPIRED);

        PaymentEntity request = paymentRequest();

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(reservationService.expireReservationIfNeeded(reservation)).thenReturn(reservation);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paymentService.createPayment(request));

        assertEquals("No se puede registrar un pago para una reserva expirada", exception.getMessage());

        verify(reservationService).expireReservationIfNeeded(reservation);
        verify(paymentRepository, never()).save(any(PaymentEntity.class));
    }

    @Test
    void createPaymentShouldThrowExceptionWhenPendingReservationExpiresBeforePayment() {
        ReservationEntity reservation = pendingReservation();
        ReservationEntity expiredReservation = pendingReservation();
        expiredReservation.setStatus(ReservationEntity.STATUS_EXPIRED);

        PaymentEntity request = paymentRequest();

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(reservationService.expireReservationIfNeeded(reservation)).thenReturn(expiredReservation);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paymentService.createPayment(request));

        assertEquals("No se puede registrar un pago para una reserva expirada", exception.getMessage());

        verify(reservationService).expireReservationIfNeeded(reservation);
        verify(paymentRepository, never()).save(any(PaymentEntity.class));
    }

    @Test
    void createPaymentShouldThrowExceptionWhenReservationIsAlreadyConfirmed() {
        ReservationEntity reservation = pendingReservation();
        reservation.setStatus(ReservationEntity.STATUS_CONFIRMED);

        PaymentEntity request = paymentRequest();

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(reservationService.expireReservationIfNeeded(reservation)).thenReturn(reservation);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paymentService.createPayment(request));

        assertEquals("La reserva ya se encuentra confirmada", exception.getMessage());

        verify(reservationService).expireReservationIfNeeded(reservation);
        verify(paymentRepository, never()).save(any(PaymentEntity.class));
    }

    @Test
    void createPaymentShouldThrowExceptionWhenReservationAlreadyHasPayment() {
        ReservationEntity reservation = pendingReservation();
        PaymentEntity request = paymentRequest();

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(reservationService.expireReservationIfNeeded(reservation)).thenReturn(reservation);
        when(paymentRepository.existsByReservation(reservation)).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paymentService.createPayment(request));

        assertEquals("La reserva ya tiene un pago registrado", exception.getMessage());

        verify(reservationService).expireReservationIfNeeded(reservation);
        verify(paymentRepository, never()).save(any(PaymentEntity.class));
    }

    @Test
    void createPaymentShouldThrowExceptionWhenAmountIsInvalid() {
        ReservationEntity reservation = pendingReservation();
        PaymentEntity request = paymentRequest();
        request.setAmount(0.0);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(reservationService.expireReservationIfNeeded(reservation)).thenReturn(reservation);
        when(paymentRepository.existsByReservation(reservation)).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paymentService.createPayment(request));

        assertEquals("El monto del pago debe ser mayor que cero", exception.getMessage());

        verify(reservationService).expireReservationIfNeeded(reservation);
        verify(paymentRepository, never()).save(any(PaymentEntity.class));
    }

    @Test
    void createPaymentShouldThrowExceptionWhenAmountDoesNotMatchReservationTotal() {
        ReservationEntity reservation = pendingReservation();
        PaymentEntity request = paymentRequest();
        request.setAmount(100000.0);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(reservationService.expireReservationIfNeeded(reservation)).thenReturn(reservation);
        when(paymentRepository.existsByReservation(reservation)).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paymentService.createPayment(request));

        assertEquals("El monto del pago debe coincidir con el total de la reserva", exception.getMessage());

        verify(reservationService).expireReservationIfNeeded(reservation);
        verify(paymentRepository, never()).save(any(PaymentEntity.class));
    }


    @Test
    void createPaymentsForPurchaseGroupShouldApprovePaymentsAndConfirmReservations() {
        ReservationEntity reservationOne = pendingReservation();
        reservationOne.setId(1L);
        reservationOne.setPurchaseGroupCode("COMPRA-TEST");
        reservationOne.setFinalTotalAmount(200000.0);

        ReservationEntity reservationTwo = pendingReservation();
        reservationTwo.setId(2L);
        reservationTwo.setPurchaseGroupCode("COMPRA-TEST");
        reservationTwo.setFinalTotalAmount(100000.0);

        PaymentEntity request = groupPaymentRequest();

        when(reservationRepository.findByPurchaseGroupCode("COMPRA-TEST"))
                .thenReturn(List.of(reservationOne, reservationTwo));
        when(reservationService.expireReservationIfNeeded(reservationOne)).thenReturn(reservationOne);
        when(reservationService.expireReservationIfNeeded(reservationTwo)).thenReturn(reservationTwo);
        when(paymentRepository.existsByReservation(any(ReservationEntity.class))).thenReturn(false);
        when(paymentRepository.save(any(PaymentEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<PaymentEntity> result = paymentService.createPaymentsForPurchaseGroup("COMPRA-TEST", request);

        assertEquals(2, result.size());
        assertEquals(200000.0, result.get(0).getAmount());
        assertEquals(100000.0, result.get(1).getAmount());
        assertEquals(PaymentEntity.PAYMENT_METHOD_CREDIT_CARD, result.get(0).getPaymentMethod());
        assertEquals(PaymentEntity.PAYMENT_STATUS_APPROVED, result.get(0).getPaymentStatus());
        assertEquals(PaymentEntity.PAYMENT_STATUS_APPROVED, result.get(1).getPaymentStatus());
        assertEquals(ReservationEntity.STATUS_CONFIRMED, reservationOne.getStatus());
        assertEquals(ReservationEntity.STATUS_CONFIRMED, reservationTwo.getStatus());

        verify(reservationRepository).findByPurchaseGroupCode("COMPRA-TEST");
        verify(reservationService).expireReservationIfNeeded(reservationOne);
        verify(reservationService).expireReservationIfNeeded(reservationTwo);
        verify(reservationRepository).save(reservationOne);
        verify(reservationRepository).save(reservationTwo);
        verify(paymentRepository, times(2)).save(any(PaymentEntity.class));
    }

    @Test
    void createPaymentsForPurchaseGroupShouldThrowExceptionWhenGroupDoesNotExist() {
        PaymentEntity request = groupPaymentRequest();

        when(reservationRepository.findByPurchaseGroupCode("COMPRA-NO-EXISTE"))
                .thenReturn(List.of());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paymentService.createPaymentsForPurchaseGroup("COMPRA-NO-EXISTE", request));

        assertEquals("Compra multiple no encontrada", exception.getMessage());

        verify(reservationRepository).findByPurchaseGroupCode("COMPRA-NO-EXISTE");
        verify(paymentRepository, never()).save(any(PaymentEntity.class));
    }

    @Test
    void createPaymentsForPurchaseGroupShouldThrowExceptionWhenAmountDoesNotMatchGroupTotal() {
        ReservationEntity reservationOne = pendingReservation();
        reservationOne.setId(1L);
        reservationOne.setFinalTotalAmount(200000.0);

        ReservationEntity reservationTwo = pendingReservation();
        reservationTwo.setId(2L);
        reservationTwo.setFinalTotalAmount(100000.0);

        PaymentEntity request = groupPaymentRequest();
        request.setAmount(250000.0);

        when(reservationRepository.findByPurchaseGroupCode("COMPRA-TEST"))
                .thenReturn(List.of(reservationOne, reservationTwo));
        when(reservationService.expireReservationIfNeeded(reservationOne)).thenReturn(reservationOne);
        when(reservationService.expireReservationIfNeeded(reservationTwo)).thenReturn(reservationTwo);
        when(paymentRepository.existsByReservation(any(ReservationEntity.class))).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paymentService.createPaymentsForPurchaseGroup("COMPRA-TEST", request));

        assertEquals("El monto del pago debe coincidir con el total de la compra multiple", exception.getMessage());

        verify(paymentRepository, never()).save(any(PaymentEntity.class));
        verify(reservationRepository, never()).save(any(ReservationEntity.class));
    }

    @Test
    void createPaymentsForPurchaseGroupShouldThrowExceptionWhenAnyReservationExpiresBeforePayment() {
        ReservationEntity reservationOne = pendingReservation();
        reservationOne.setId(1L);
        reservationOne.setFinalTotalAmount(200000.0);

        ReservationEntity expiredReservation = pendingReservation();
        expiredReservation.setId(2L);
        expiredReservation.setFinalTotalAmount(100000.0);
        expiredReservation.setStatus(ReservationEntity.STATUS_EXPIRED);

        PaymentEntity request = groupPaymentRequest();

        when(reservationRepository.findByPurchaseGroupCode("COMPRA-TEST"))
                .thenReturn(List.of(reservationOne, expiredReservation));
        when(reservationService.expireReservationIfNeeded(reservationOne)).thenReturn(reservationOne);
        when(reservationService.expireReservationIfNeeded(expiredReservation)).thenReturn(expiredReservation);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paymentService.createPaymentsForPurchaseGroup("COMPRA-TEST", request));

        assertEquals("No se puede registrar un pago para una reserva expirada", exception.getMessage());

        verify(paymentRepository, never()).save(any(PaymentEntity.class));
        verify(reservationRepository, never()).save(any(ReservationEntity.class));
    }

    @Test
    void createPaymentsForPurchaseGroupShouldThrowExceptionWhenAnyReservationAlreadyHasPayment() {
        ReservationEntity reservationOne = pendingReservation();
        reservationOne.setId(1L);
        reservationOne.setFinalTotalAmount(200000.0);

        ReservationEntity reservationTwo = pendingReservation();
        reservationTwo.setId(2L);
        reservationTwo.setFinalTotalAmount(100000.0);

        PaymentEntity request = groupPaymentRequest();

        when(reservationRepository.findByPurchaseGroupCode("COMPRA-TEST"))
                .thenReturn(List.of(reservationOne, reservationTwo));
        when(reservationService.expireReservationIfNeeded(reservationOne)).thenReturn(reservationOne);
        when(reservationService.expireReservationIfNeeded(reservationTwo)).thenReturn(reservationTwo);
        when(paymentRepository.existsByReservation(reservationOne)).thenReturn(false);
        when(paymentRepository.existsByReservation(reservationTwo)).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paymentService.createPaymentsForPurchaseGroup("COMPRA-TEST", request));

        assertEquals("Una reserva del grupo ya tiene un pago registrado", exception.getMessage());

        verify(paymentRepository, never()).save(any(PaymentEntity.class));
        verify(reservationRepository, never()).save(any(ReservationEntity.class));
    }

    private PaymentEntity paymentRequest() {
        PaymentEntity payment = new PaymentEntity();

        ReservationEntity reservation = new ReservationEntity();
        reservation.setId(1L);

        payment.setReservation(reservation);
        payment.setAmount(200000.0);
        payment.setCardNumber("4111111111111111");
        payment.setCardExpiration("12/30");
        payment.setCardCvv("123");

        return payment;
    }


    private PaymentEntity groupPaymentRequest() {
        PaymentEntity payment = new PaymentEntity();

        payment.setAmount(300000.0);
        payment.setCardNumber("4111111111111111");
        payment.setCardExpiration("12/30");
        payment.setCardCvv("123");

        return payment;
    }

    private PaymentEntity savedPayment() {
        PaymentEntity payment = new PaymentEntity();

        payment.setId(1L);
        payment.setReservation(pendingReservation());
        payment.setAmount(200000.0);
        payment.setPaymentMethod(PaymentEntity.PAYMENT_METHOD_CREDIT_CARD);
        payment.setPaymentStatus(PaymentEntity.PAYMENT_STATUS_APPROVED);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setCardNumber("4111111111111111");
        payment.setCardExpiration("12/30");
        payment.setCardCvv("123");

        return payment;
    }

    private ReservationEntity pendingReservation() {
        ReservationEntity reservation = new ReservationEntity();

        reservation.setId(1L);
        reservation.setUser(activeUser());
        reservation.setTourPackage(availableTourPackage());
        reservation.setPassengerCount(2);
        reservation.setOriginalTotalAmount(200000.0);
        reservation.setDiscountAmount(0.0);
        reservation.setDiscountDescription("Sin descuento");
        reservation.setFinalTotalAmount(200000.0);
        reservation.setStatus(ReservationEntity.STATUS_PENDING_PAYMENT);
        reservation.setReservationDate(LocalDateTime.now());
        reservation.setPaymentDeadline(LocalDateTime.now().plusMinutes(5));
        reservation.setTourStartDate(LocalDate.now().plusDays(3));
        reservation.setTourEndDate(LocalDate.now().plusDays(7));

        return reservation;
    }

    private UserEntity activeUser() {
        UserEntity user = new UserEntity();

        user.setId(1L);
        user.setFullName("Cliente Prueba");
        user.setEmail("cliente@test.com");
        user.setPassword("123456");
        user.setActive(true);
        user.setRole(UserEntity.ROLE_CLIENT);

        return user;
    }

    private TourPackageEntity availableTourPackage() {
        TourPackageEntity tourPackage = new TourPackageEntity();

        tourPackage.setId(1L);
        tourPackage.setName("Paquete Santiago");
        tourPackage.setDestination("Santiago");
        tourPackage.setDescription("Descripción");
        tourPackage.setStartDate(LocalDate.now().plusDays(1));
        tourPackage.setEndDate(LocalDate.now().plusDays(10));
        tourPackage.setDurationDays(5);
        tourPackage.setPrice(100000.0);
        tourPackage.setTotalQuota(10);
        tourPackage.setAvailableQuota(10);
        tourPackage.setStatus(TourPackageEntity.STATUS_AVAILABLE);

        return tourPackage;
    }
}