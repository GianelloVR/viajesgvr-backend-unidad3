package com.example.viajesgvr.services;

import com.example.viajesgvr.dto.ReservationSummaryDto;
import com.example.viajesgvr.entities.PaymentEntity;
import com.example.viajesgvr.entities.ReservationEntity;
import com.example.viajesgvr.entities.TourPackageEntity;
import com.example.viajesgvr.entities.UserEntity;
import com.example.viajesgvr.repositories.PaymentRepository;
import com.example.viajesgvr.repositories.ReservationRepository;
import com.example.viajesgvr.repositories.TourPackageRepository;
import com.example.viajesgvr.repositories.UserRepository;
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
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TourPackageRepository tourPackageRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private ReservationService reservationService;

    @Test
    void createReservationShouldCreatePendingReservationWithoutDiscount() {
        UserEntity user = activeUser();
        TourPackageEntity tourPackage = availableTourPackage();

        ReservationEntity request = reservationRequest(2);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(tourPackageRepository.findById(1L)).thenReturn(Optional.of(tourPackage));
        when(reservationRepository.save(any(ReservationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservationEntity result = reservationService.createReservation(request);

        assertEquals(user, result.getUser());
        assertEquals(tourPackage, result.getTourPackage());
        assertEquals(2, result.getPassengerCount());
        assertEquals(200000.0, result.getOriginalTotalAmount());
        assertEquals(0.0, result.getDiscountAmount());
        assertEquals("Sin descuento", result.getDiscountDescription());
        assertEquals(200000.0, result.getFinalTotalAmount());
        assertEquals(ReservationEntity.STATUS_PENDING_PAYMENT, result.getStatus());
        assertNotNull(result.getReservationDate());
        assertNotNull(result.getPaymentDeadline());
        assertEquals(8, tourPackage.getAvailableQuota());

        verify(tourPackageRepository).save(tourPackage);
        verify(reservationRepository).save(any(ReservationEntity.class));
    }

    @Test
    void createReservationShouldSetPaymentDeadlineFiveMinutesAfterReservationDate() {
        UserEntity user = activeUser();
        TourPackageEntity tourPackage = availableTourPackage();

        ReservationEntity request = reservationRequest(2);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(tourPackageRepository.findById(1L)).thenReturn(Optional.of(tourPackage));
        when(reservationRepository.save(any(ReservationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservationEntity result = reservationService.createReservation(request);

        assertNotNull(result.getReservationDate());
        assertNotNull(result.getPaymentDeadline());
        assertEquals(
                result.getReservationDate().plusMinutes(ReservationEntity.PAYMENT_EXPIRATION_MINUTES),
                result.getPaymentDeadline()
        );
    }

    @Test
    void createReservationShouldApplyGroupDiscountWhenPassengerCountIsFourOrMore() {
        UserEntity user = activeUser();
        TourPackageEntity tourPackage = availableTourPackage();

        ReservationEntity request = reservationRequest(4);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(tourPackageRepository.findById(1L)).thenReturn(Optional.of(tourPackage));
        when(reservationRepository.save(any(ReservationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservationEntity result = reservationService.createReservation(request);

        assertEquals(400000.0, result.getOriginalTotalAmount());
        assertEquals(40000.0, result.getDiscountAmount());
        assertEquals("Descuento por grupo (10%)", result.getDiscountDescription());
        assertEquals(360000.0, result.getFinalTotalAmount());
        assertEquals(6, tourPackage.getAvailableQuota());
    }

    @Test
    void createReservationShouldApplyFrequentClientDiscountWhenUserHasThreeConfirmedReservations() {
        UserEntity user = activeUser();
        TourPackageEntity tourPackage = availableTourPackage();

        ReservationEntity request = reservationRequest(2);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(tourPackageRepository.findById(1L)).thenReturn(Optional.of(tourPackage));
        when(reservationRepository.findByUser(user)).thenReturn(List.of(
                confirmedReservation(),
                confirmedReservation(),
                confirmedReservation()
        ));
        when(reservationRepository.save(any(ReservationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservationEntity result = reservationService.createReservation(request);

        assertEquals(200000.0, result.getOriginalTotalAmount());
        assertEquals(10000.0, result.getDiscountAmount());
        assertEquals("Descuento cliente frecuente (5%)", result.getDiscountDescription());
        assertEquals(190000.0, result.getFinalTotalAmount());
        assertEquals(8, tourPackage.getAvailableQuota());

        verify(tourPackageRepository).save(tourPackage);
        verify(reservationRepository).save(any(ReservationEntity.class));
    }

    @Test
    void createReservationShouldApplyGroupAndFrequentClientDiscountsTogether() {
        UserEntity user = activeUser();
        TourPackageEntity tourPackage = availableTourPackage();

        ReservationEntity request = reservationRequest(4);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(tourPackageRepository.findById(1L)).thenReturn(Optional.of(tourPackage));
        when(reservationRepository.findByUser(user)).thenReturn(List.of(
                confirmedReservation(),
                confirmedReservation(),
                confirmedReservation()
        ));
        when(reservationRepository.save(any(ReservationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservationEntity result = reservationService.createReservation(request);

        assertEquals(400000.0, result.getOriginalTotalAmount());
        assertEquals(60000.0, result.getDiscountAmount());
        assertEquals("Descuento por grupo (10%) + Descuento cliente frecuente (5%)", result.getDiscountDescription());
        assertEquals(340000.0, result.getFinalTotalAmount());
        assertEquals(6, tourPackage.getAvailableQuota());

        verify(tourPackageRepository).save(tourPackage);
        verify(reservationRepository).save(any(ReservationEntity.class));
    }

    @Test
    void createReservationShouldApplyPackagePromotionDiscount() {
        UserEntity user = activeUser();
        TourPackageEntity tourPackage = availableTourPackage();
        tourPackage.setPromotionDiscountPercentage(15);

        ReservationEntity request = reservationRequest(2);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(tourPackageRepository.findById(1L)).thenReturn(Optional.of(tourPackage));
        when(reservationRepository.save(any(ReservationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservationEntity result = reservationService.createReservation(request);

        assertEquals(200000.0, result.getOriginalTotalAmount());
        assertEquals(30000.0, result.getDiscountAmount());
        assertEquals("Promoción del paquete (15%)", result.getDiscountDescription());
        assertEquals(170000.0, result.getFinalTotalAmount());
        assertEquals(8, tourPackage.getAvailableQuota());

        verify(tourPackageRepository).save(tourPackage);
        verify(reservationRepository).save(any(ReservationEntity.class));
    }

    @Test
    void createReservationShouldApplyPromotionGroupAndFrequentClientDiscountsWithMaximumLimit() {
        UserEntity user = activeUser();
        TourPackageEntity tourPackage = availableTourPackage();
        tourPackage.setPromotionDiscountPercentage(10);

        ReservationEntity request = reservationRequest(4);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(tourPackageRepository.findById(1L)).thenReturn(Optional.of(tourPackage));
        when(reservationRepository.findByUser(user)).thenReturn(List.of(
                confirmedReservation(),
                confirmedReservation(),
                confirmedReservation()
        ));
        when(reservationRepository.save(any(ReservationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservationEntity result = reservationService.createReservation(request);

        assertEquals(400000.0, result.getOriginalTotalAmount());
        assertEquals(80000.0, result.getDiscountAmount());
        assertEquals(
                "Promoción del paquete (10%) + Descuento por grupo (10%) + Descuento cliente frecuente (5%) - límite máximo aplicado (20%)",
                result.getDiscountDescription()
        );
        assertEquals(320000.0, result.getFinalTotalAmount());
        assertEquals(6, tourPackage.getAvailableQuota());

        verify(tourPackageRepository).save(tourPackage);
        verify(reservationRepository).save(any(ReservationEntity.class));
    }


    @Test
    void createMultipleReservationsShouldCreateReservationsWithSamePurchaseGroupAndApplyMultiplePackageDiscount() {
        UserEntity user = activeUser();
        TourPackageEntity firstPackage = availableTourPackage();
        TourPackageEntity secondPackage = availableTourPackageWithId(2L, "Paquete Valparaíso", 200000.0);

        ReservationEntity firstRequest = reservationRequestForPackage(1L, 1);
        ReservationEntity secondRequest = reservationRequestForPackage(2L, 1);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(tourPackageRepository.findById(1L)).thenReturn(Optional.of(firstPackage));
        when(tourPackageRepository.findById(2L)).thenReturn(Optional.of(secondPackage));
        when(reservationRepository.save(any(ReservationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<ReservationEntity> result = reservationService.createMultipleReservations(List.of(
                firstRequest,
                secondRequest
        ));

        assertEquals(2, result.size());

        ReservationEntity firstReservation = result.get(0);
        ReservationEntity secondReservation = result.get(1);

        assertNotNull(firstReservation.getPurchaseGroupCode());
        assertEquals(firstReservation.getPurchaseGroupCode(), secondReservation.getPurchaseGroupCode());

        assertEquals(100000.0, firstReservation.getOriginalTotalAmount());
        assertEquals(5000.0, firstReservation.getDiscountAmount());
        assertEquals("Descuento por compra múltiple (5%)", firstReservation.getDiscountDescription());
        assertEquals(95000.0, firstReservation.getFinalTotalAmount());

        assertEquals(200000.0, secondReservation.getOriginalTotalAmount());
        assertEquals(10000.0, secondReservation.getDiscountAmount());
        assertEquals("Descuento por compra múltiple (5%)", secondReservation.getDiscountDescription());
        assertEquals(190000.0, secondReservation.getFinalTotalAmount());

        assertEquals(9, firstPackage.getAvailableQuota());
        assertEquals(9, secondPackage.getAvailableQuota());

        verify(tourPackageRepository).save(firstPackage);
        verify(tourPackageRepository).save(secondPackage);
        verify(reservationRepository, times(2)).save(any(ReservationEntity.class));
    }

    @Test
    void createMultipleReservationsShouldApplyMaximumLimitWhenAllDiscountsApply() {
        UserEntity user = activeUser();
        TourPackageEntity firstPackage = availableTourPackage();
        firstPackage.setPromotionDiscountPercentage(10);

        TourPackageEntity secondPackage = availableTourPackageWithId(2L, "Paquete Valparaíso", 200000.0);
        secondPackage.setPromotionDiscountPercentage(10);

        ReservationEntity firstRequest = reservationRequestForPackage(1L, 4);
        ReservationEntity secondRequest = reservationRequestForPackage(2L, 4);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(tourPackageRepository.findById(1L)).thenReturn(Optional.of(firstPackage));
        when(tourPackageRepository.findById(2L)).thenReturn(Optional.of(secondPackage));
        when(reservationRepository.findByUser(user)).thenReturn(List.of(
                confirmedReservation(),
                confirmedReservation(),
                confirmedReservation()
        ));
        when(reservationRepository.save(any(ReservationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<ReservationEntity> result = reservationService.createMultipleReservations(List.of(
                firstRequest,
                secondRequest
        ));

        assertEquals(2, result.size());

        ReservationEntity firstReservation = result.get(0);
        ReservationEntity secondReservation = result.get(1);

        assertEquals(400000.0, firstReservation.getOriginalTotalAmount());
        assertEquals(80000.0, firstReservation.getDiscountAmount());
        assertEquals(
                "Promoción del paquete (10%) + Descuento por grupo (10%) + Descuento cliente frecuente (5%) + Descuento por compra múltiple (5%) - límite máximo aplicado (20%)",
                firstReservation.getDiscountDescription()
        );
        assertEquals(320000.0, firstReservation.getFinalTotalAmount());

        assertEquals(800000.0, secondReservation.getOriginalTotalAmount());
        assertEquals(160000.0, secondReservation.getDiscountAmount());
        assertEquals(
                "Promoción del paquete (10%) + Descuento por grupo (10%) + Descuento cliente frecuente (5%) + Descuento por compra múltiple (5%) - límite máximo aplicado (20%)",
                secondReservation.getDiscountDescription()
        );
        assertEquals(640000.0, secondReservation.getFinalTotalAmount());

        assertEquals(firstReservation.getPurchaseGroupCode(), secondReservation.getPurchaseGroupCode());
        assertEquals(6, firstPackage.getAvailableQuota());
        assertEquals(6, secondPackage.getAvailableQuota());

        verify(tourPackageRepository).save(firstPackage);
        verify(tourPackageRepository).save(secondPackage);
        verify(reservationRepository, times(2)).save(any(ReservationEntity.class));
    }

    @Test
    void createMultipleReservationsShouldThrowExceptionWhenPackagesAreNotDistinct() {
        ReservationEntity firstRequest = reservationRequestForPackage(1L, 1);
        ReservationEntity secondRequest = reservationRequestForPackage(1L, 1);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reservationService.createMultipleReservations(List.of(firstRequest, secondRequest)));

        assertEquals("La compra múltiple debe incluir al menos dos paquetes turísticos distintos", exception.getMessage());

        verify(userRepository, never()).findById(any());
        verify(tourPackageRepository, never()).findById(any());
        verify(reservationRepository, never()).save(any(ReservationEntity.class));
    }

    @Test
    void createReservationShouldSetPackageSoldOutWhenQuotaReachesZero() {
        UserEntity user = activeUser();
        TourPackageEntity tourPackage = availableTourPackage();
        tourPackage.setAvailableQuota(4);

        ReservationEntity request = reservationRequest(4);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(tourPackageRepository.findById(1L)).thenReturn(Optional.of(tourPackage));
        when(reservationRepository.save(any(ReservationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        reservationService.createReservation(request);

        assertEquals(0, tourPackage.getAvailableQuota());
        assertEquals(TourPackageEntity.STATUS_SOLD_OUT, tourPackage.getStatus());
    }

    @Test
    void createReservationShouldThrowExceptionWhenUserIsInactive() {
        UserEntity user = activeUser();
        user.setActive(false);

        ReservationEntity request = reservationRequest(2);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(tourPackageRepository.findById(1L)).thenReturn(Optional.of(availableTourPackage()));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reservationService.createReservation(request));

        assertEquals("El usuario debe estar activo para realizar una reserva", exception.getMessage());

        verify(reservationRepository, never()).save(any(ReservationEntity.class));
    }

    @Test
    void createReservationShouldThrowExceptionWhenTourPackageIsNotAvailable() {
        TourPackageEntity tourPackage = availableTourPackage();
        tourPackage.setStatus(TourPackageEntity.STATUS_CANCELED);

        ReservationEntity request = reservationRequest(2);

        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser()));
        when(tourPackageRepository.findById(1L)).thenReturn(Optional.of(tourPackage));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reservationService.createReservation(request));

        assertEquals("El paquete turístico no está disponible para reserva", exception.getMessage());

        verify(reservationRepository, never()).save(any(ReservationEntity.class));
    }

    @Test
    void createReservationShouldThrowExceptionWhenPassengerCountExceedsAvailableQuota() {
        TourPackageEntity tourPackage = availableTourPackage();
        tourPackage.setAvailableQuota(1);

        ReservationEntity request = reservationRequest(2);

        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser()));
        when(tourPackageRepository.findById(1L)).thenReturn(Optional.of(tourPackage));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reservationService.createReservation(request));

        assertEquals("La cantidad de pasajeros excede los cupos disponibles", exception.getMessage());

        verify(reservationRepository, never()).save(any(ReservationEntity.class));
    }

    @Test
    void createReservationShouldThrowExceptionWhenTourStartDateIsOutsidePackageValidity() {
        ReservationEntity request = reservationRequest(2);
        request.setTourStartDate(LocalDate.now().plusDays(30));
        request.setTourEndDate(LocalDate.now().plusDays(35));

        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser()));
        when(tourPackageRepository.findById(1L)).thenReturn(Optional.of(availableTourPackage()));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reservationService.createReservation(request));

        assertEquals("La fecha de inicio del tour debe estar dentro de la vigencia del paquete",
                exception.getMessage());

        verify(reservationRepository, never()).save(any(ReservationEntity.class));
    }

    @Test
    void createReservationShouldThrowExceptionWhenTourEndDateIsNotAfterStartDate() {
        ReservationEntity request = reservationRequest(2);
        request.setTourStartDate(LocalDate.now().plusDays(3));
        request.setTourEndDate(LocalDate.now().plusDays(3));

        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser()));
        when(tourPackageRepository.findById(1L)).thenReturn(Optional.of(availableTourPackage()));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reservationService.createReservation(request));

        assertEquals("La fecha de término del tour debe ser posterior a la fecha de inicio del tour",
                exception.getMessage());

        verify(reservationRepository, never()).save(any(ReservationEntity.class));
    }

    @Test
    void cancelReservationShouldCancelAndRestoreQuota() {
        TourPackageEntity tourPackage = availableTourPackage();
        tourPackage.setAvailableQuota(5);

        ReservationEntity reservation = savedReservation();
        reservation.setTourPackage(tourPackage);
        reservation.setPassengerCount(2);
        reservation.setStatus(ReservationEntity.STATUS_PENDING_PAYMENT);
        reservation.setPaymentDeadline(LocalDateTime.now().plusMinutes(5));

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(ReservationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservationEntity result = reservationService.cancelReservation(1L);

        assertEquals(ReservationEntity.STATUS_CANCELED, result.getStatus());
        assertEquals(7, tourPackage.getAvailableQuota());

        verify(tourPackageRepository).save(tourPackage);
        verify(reservationRepository).save(reservation);
    }

    @Test
    void cancelReservationShouldReturnSameReservationWhenAlreadyCanceled() {
        ReservationEntity reservation = savedReservation();
        reservation.setStatus(ReservationEntity.STATUS_CANCELED);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        ReservationEntity result = reservationService.cancelReservation(1L);

        assertEquals(ReservationEntity.STATUS_CANCELED, result.getStatus());

        verify(tourPackageRepository, never()).save(any(TourPackageEntity.class));
        verify(reservationRepository, never()).save(any(ReservationEntity.class));
    }

    @Test
    void cancelReservationShouldReturnExpiredReservationWhenReservationExpiredBeforeCancel() {
        TourPackageEntity tourPackage = availableTourPackage();
        tourPackage.setAvailableQuota(5);

        ReservationEntity reservation = savedReservation();
        reservation.setTourPackage(tourPackage);
        reservation.setPassengerCount(2);
        reservation.setStatus(ReservationEntity.STATUS_PENDING_PAYMENT);
        reservation.setPaymentDeadline(LocalDateTime.now().minusMinutes(1));

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(ReservationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservationEntity result = reservationService.cancelReservation(1L);

        assertEquals(ReservationEntity.STATUS_EXPIRED, result.getStatus());
        assertEquals(7, tourPackage.getAvailableQuota());

        verify(tourPackageRepository).save(tourPackage);
        verify(reservationRepository).save(reservation);
    }

    @Test
    void expireReservationIfNeededShouldExpirePendingReservationAndRestoreQuota() {
        TourPackageEntity tourPackage = availableTourPackage();
        tourPackage.setAvailableQuota(5);

        ReservationEntity reservation = savedReservation();
        reservation.setTourPackage(tourPackage);
        reservation.setPassengerCount(2);
        reservation.setStatus(ReservationEntity.STATUS_PENDING_PAYMENT);
        reservation.setPaymentDeadline(LocalDateTime.now().minusMinutes(1));

        when(reservationRepository.save(any(ReservationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservationEntity result = reservationService.expireReservationIfNeeded(reservation);

        assertEquals(ReservationEntity.STATUS_EXPIRED, result.getStatus());
        assertEquals(7, tourPackage.getAvailableQuota());

        verify(tourPackageRepository).save(tourPackage);
        verify(reservationRepository).save(reservation);
    }

    @Test
    void expireReservationIfNeededShouldNotExpireReservationBeforeDeadline() {
        TourPackageEntity tourPackage = availableTourPackage();
        tourPackage.setAvailableQuota(5);

        ReservationEntity reservation = savedReservation();
        reservation.setTourPackage(tourPackage);
        reservation.setPassengerCount(2);
        reservation.setStatus(ReservationEntity.STATUS_PENDING_PAYMENT);
        reservation.setPaymentDeadline(LocalDateTime.now().plusMinutes(5));

        ReservationEntity result = reservationService.expireReservationIfNeeded(reservation);

        assertEquals(ReservationEntity.STATUS_PENDING_PAYMENT, result.getStatus());
        assertEquals(5, tourPackage.getAvailableQuota());

        verify(tourPackageRepository, never()).save(any(TourPackageEntity.class));
        verify(reservationRepository, never()).save(any(ReservationEntity.class));
    }

    @Test
    void expireReservationIfNeededShouldNotRestoreQuotaTwiceWhenReservationIsAlreadyExpired() {
        TourPackageEntity tourPackage = availableTourPackage();
        tourPackage.setAvailableQuota(5);

        ReservationEntity reservation = savedReservation();
        reservation.setTourPackage(tourPackage);
        reservation.setPassengerCount(2);
        reservation.setStatus(ReservationEntity.STATUS_EXPIRED);
        reservation.setPaymentDeadline(LocalDateTime.now().minusMinutes(1));

        ReservationEntity result = reservationService.expireReservationIfNeeded(reservation);

        assertEquals(ReservationEntity.STATUS_EXPIRED, result.getStatus());
        assertEquals(5, tourPackage.getAvailableQuota());

        verify(tourPackageRepository, never()).save(any(TourPackageEntity.class));
        verify(reservationRepository, never()).save(any(ReservationEntity.class));
    }

    @Test
    void expireReservationIfNeededShouldSetDefaultDeadlineWhenDeadlineIsMissing() {
        ReservationEntity reservation = savedReservation();
        reservation.setStatus(ReservationEntity.STATUS_PENDING_PAYMENT);
        reservation.setReservationDate(LocalDateTime.now());
        reservation.setPaymentDeadline(null);

        when(reservationRepository.save(any(ReservationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservationEntity result = reservationService.expireReservationIfNeeded(reservation);

        assertEquals(ReservationEntity.STATUS_PENDING_PAYMENT, result.getStatus());
        assertNotNull(result.getPaymentDeadline());

        verify(reservationRepository).save(reservation);
    }

    @Test
    void expirePendingReservationsShouldExpireAllExpiredPendingReservations() {
        TourPackageEntity tourPackage = availableTourPackage();
        tourPackage.setAvailableQuota(5);

        ReservationEntity reservation = savedReservation();
        reservation.setTourPackage(tourPackage);
        reservation.setPassengerCount(2);
        reservation.setStatus(ReservationEntity.STATUS_PENDING_PAYMENT);
        reservation.setPaymentDeadline(LocalDateTime.now().minusMinutes(1));

        when(reservationRepository.findByStatus(ReservationEntity.STATUS_PENDING_PAYMENT))
                .thenReturn(List.of(reservation));
        when(reservationRepository.save(any(ReservationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        reservationService.expirePendingReservations();

        assertEquals(ReservationEntity.STATUS_EXPIRED, reservation.getStatus());
        assertEquals(7, tourPackage.getAvailableQuota());

        verify(reservationRepository).findByStatus(ReservationEntity.STATUS_PENDING_PAYMENT);
        verify(tourPackageRepository).save(tourPackage);
        verify(reservationRepository).save(reservation);
    }

    @Test
    void getReservationsByUserIdShouldReturnUserReservations() {
        UserEntity user = activeUser();
        ReservationEntity reservation = savedReservation();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(reservationRepository.findByStatus(ReservationEntity.STATUS_PENDING_PAYMENT))
                .thenReturn(List.of());
        when(reservationRepository.findByUser(user)).thenReturn(List.of(reservation));

        List<ReservationEntity> result = reservationService.getReservationsByUserId(1L);

        assertEquals(1, result.size());
        verify(userRepository).findById(1L);
        verify(reservationRepository).findByStatus(ReservationEntity.STATUS_PENDING_PAYMENT);
        verify(reservationRepository).findByUser(user);
    }

    @Test
    void getReservationSummaryShouldReturnSummaryWithoutPayment() {
        ReservationEntity reservation = savedReservation();

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(paymentRepository.findByReservation(reservation)).thenReturn(Optional.empty());

        ReservationSummaryDto result = reservationService.getReservationSummary(1L);

        assertEquals(1L, result.getReservationId());
        assertEquals(ReservationEntity.STATUS_PENDING_PAYMENT, result.getReservationStatus());
        assertEquals("Cliente Prueba", result.getUserFullName());
        assertEquals("Paquete Santiago", result.getTourPackageName());
        assertFalse(result.getPaymentRegistered());
        assertNull(result.getPaymentId());
    }

    @Test
    void getReservationSummaryShouldReturnSummaryWithPayment() {
        ReservationEntity reservation = savedReservation();

        PaymentEntity payment = new PaymentEntity();
        payment.setId(10L);
        payment.setPaymentStatus(PaymentEntity.PAYMENT_STATUS_APPROVED);
        payment.setPaymentMethod(PaymentEntity.PAYMENT_METHOD_CREDIT_CARD);
        payment.setPaymentDate(LocalDateTime.now());

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(paymentRepository.findByReservation(reservation)).thenReturn(Optional.of(payment));

        ReservationSummaryDto result = reservationService.getReservationSummary(1L);

        assertTrue(result.getPaymentRegistered());
        assertEquals(10L, result.getPaymentId());
        assertEquals(PaymentEntity.PAYMENT_STATUS_APPROVED, result.getPaymentStatus());
        assertEquals(PaymentEntity.PAYMENT_METHOD_CREDIT_CARD, result.getPaymentMethod());
        assertNotNull(result.getPaymentDate());
    }

    @Test
    void getAllReservationsShouldReturnAllReservationsAfterExpiringPendingReservations() {
        ReservationEntity reservation = savedReservation();

        when(reservationRepository.findByStatus(ReservationEntity.STATUS_PENDING_PAYMENT))
                .thenReturn(List.of());
        when(reservationRepository.findAll()).thenReturn(List.of(reservation));

        List<ReservationEntity> result = reservationService.getAllReservations();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());

        verify(reservationRepository).findByStatus(ReservationEntity.STATUS_PENDING_PAYMENT);
        verify(reservationRepository).findAll();
    }

    @Test
    void getReservationByIdShouldReturnReservationWhenExists() {
        ReservationEntity reservation = savedReservation();
        reservation.setStatus(ReservationEntity.STATUS_CONFIRMED);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        Optional<ReservationEntity> result = reservationService.getReservationById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        assertEquals(ReservationEntity.STATUS_CONFIRMED, result.get().getStatus());

        verify(reservationRepository).findById(1L);
    }

    @Test
    void getReservationsByTourPackageIdShouldReturnTourPackageReservations() {
        TourPackageEntity tourPackage = availableTourPackage();
        ReservationEntity reservation = savedReservation();

        when(tourPackageRepository.findById(1L)).thenReturn(Optional.of(tourPackage));
        when(reservationRepository.findByStatus(ReservationEntity.STATUS_PENDING_PAYMENT))
                .thenReturn(List.of());
        when(reservationRepository.findByTourPackage(tourPackage)).thenReturn(List.of(reservation));

        List<ReservationEntity> result = reservationService.getReservationsByTourPackageId(1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());

        verify(tourPackageRepository).findById(1L);
        verify(reservationRepository).findByStatus(ReservationEntity.STATUS_PENDING_PAYMENT);
        verify(reservationRepository).findByTourPackage(tourPackage);
    }

    @Test
    void getReservationsByStatusShouldReturnReservationsWithRequestedStatus() {
        ReservationEntity reservation = savedReservation();
        reservation.setStatus(ReservationEntity.STATUS_CONFIRMED);

        when(reservationRepository.findByStatus(ReservationEntity.STATUS_PENDING_PAYMENT))
                .thenReturn(List.of());
        when(reservationRepository.findByStatus(ReservationEntity.STATUS_CONFIRMED))
                .thenReturn(List.of(reservation));

        List<ReservationEntity> result = reservationService.getReservationsByStatus(
                ReservationEntity.STATUS_CONFIRMED
        );

        assertEquals(1, result.size());
        assertEquals(ReservationEntity.STATUS_CONFIRMED, result.get(0).getStatus());

        verify(reservationRepository).findByStatus(ReservationEntity.STATUS_PENDING_PAYMENT);
        verify(reservationRepository).findByStatus(ReservationEntity.STATUS_CONFIRMED);
    }

    @Test
    void getReservationsByPurchaseGroupCodeShouldReturnReservationsFromGroup() {
        ReservationEntity reservation = savedReservation();
        reservation.setPurchaseGroupCode("COMPRA-ABC12345");

        when(reservationRepository.findByStatus(ReservationEntity.STATUS_PENDING_PAYMENT))
                .thenReturn(List.of());
        when(reservationRepository.findByPurchaseGroupCode("COMPRA-ABC12345"))
                .thenReturn(List.of(reservation));

        List<ReservationEntity> result = reservationService.getReservationsByPurchaseGroupCode(
                "COMPRA-ABC12345"
        );

        assertEquals(1, result.size());
        assertEquals("COMPRA-ABC12345", result.get(0).getPurchaseGroupCode());

        verify(reservationRepository).findByStatus(ReservationEntity.STATUS_PENDING_PAYMENT);
        verify(reservationRepository).findByPurchaseGroupCode("COMPRA-ABC12345");
    }

    @Test
    void getReservationsByTourPackageIdShouldThrowExceptionWhenTourPackageDoesNotExist() {
        when(tourPackageRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reservationService.getReservationsByTourPackageId(99L));

        assertEquals("Paquete turístico no encontrado", exception.getMessage());

        verify(tourPackageRepository).findById(99L);
        verify(reservationRepository, never()).findByTourPackage(any(TourPackageEntity.class));
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
        tourPackage.setPromotionDiscountPercentage(0);
        tourPackage.setStatus(TourPackageEntity.STATUS_AVAILABLE);

        return tourPackage;
    }

    private ReservationEntity reservationRequest(Integer passengerCount) {
        ReservationEntity reservation = new ReservationEntity();

        UserEntity user = new UserEntity();
        user.setId(1L);

        TourPackageEntity tourPackage = new TourPackageEntity();
        tourPackage.setId(1L);

        reservation.setUser(user);
        reservation.setTourPackage(tourPackage);
        reservation.setPassengerCount(passengerCount);
        reservation.setSpecialRequests("Sin solicitudes especiales");
        reservation.setTourStartDate(LocalDate.now().plusDays(3));
        reservation.setTourEndDate(LocalDate.now().plusDays(7));

        return reservation;
    }

    private TourPackageEntity availableTourPackageWithId(Long id, String name, Double price) {
        TourPackageEntity tourPackage = availableTourPackage();

        tourPackage.setId(id);
        tourPackage.setName(name);
        tourPackage.setPrice(price);

        return tourPackage;
    }

    private ReservationEntity reservationRequestForPackage(Long tourPackageId, Integer passengerCount) {
        ReservationEntity reservation = reservationRequest(passengerCount);

        TourPackageEntity tourPackage = new TourPackageEntity();
        tourPackage.setId(tourPackageId);
        reservation.setTourPackage(tourPackage);

        return reservation;
    }

    private ReservationEntity savedReservation() {
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

    private ReservationEntity confirmedReservation() {
        ReservationEntity reservation = savedReservation();
        reservation.setStatus(ReservationEntity.STATUS_CONFIRMED);
        return reservation;
    }
}