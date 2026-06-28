package com.example.viajesgvr.services;

import com.example.viajesgvr.dto.SalesReportItemDto;
import com.example.viajesgvr.dto.TourPackageRankingDto;
import com.example.viajesgvr.entities.PaymentEntity;
import com.example.viajesgvr.entities.ReservationEntity;
import com.example.viajesgvr.entities.TourPackageEntity;
import com.example.viajesgvr.entities.UserEntity;
import com.example.viajesgvr.repositories.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private ReportService reportService;

    @Test
    void getSalesReportByPeriodShouldReturnOnlyNonCanceledReservations() {
        LocalDate startDate = LocalDate.of(2026, 5, 1);
        LocalDate endDate = LocalDate.of(2026, 5, 31);

        PaymentEntity validPayment = payment(
                1L,
                1L,
                "Cliente Uno",
                "cliente1@test.com",
                1L,
                "Paquete Santiago",
                "Santiago",
                2,
                200000.0,
                200000.0,
                ReservationEntity.STATUS_CONFIRMED,
                LocalDateTime.of(2026, 5, 10, 12, 0)
        );

        PaymentEntity canceledPayment = payment(
                2L,
                2L,
                "Cliente Dos",
                "cliente2@test.com",
                2L,
                "Paquete Norte",
                "Antofagasta",
                3,
                300000.0,
                300000.0,
                ReservationEntity.STATUS_CANCELED,
                LocalDateTime.of(2026, 5, 12, 12, 0)
        );

        when(paymentRepository.findByPaymentDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(validPayment, canceledPayment));

        List<SalesReportItemDto> result = reportService.getSalesReportByPeriod(startDate, endDate);

        assertEquals(1, result.size());

        SalesReportItemDto item = result.get(0);

        assertEquals(1L, item.getReservationId());
        assertEquals(1L, item.getPaymentId());
        assertEquals("Cliente Uno", item.getCustomerFullName());
        assertEquals("cliente1@test.com", item.getCustomerEmail());
        assertEquals("Paquete Santiago", item.getTourPackageName());
        assertEquals("Santiago", item.getDestination());
        assertEquals(2, item.getPassengerCount());
        assertEquals(200000.0, item.getReservationTotalAmount());
        assertEquals(200000.0, item.getPaidAmount());
        assertEquals(ReservationEntity.STATUS_CONFIRMED, item.getReservationStatus());

        verify(paymentRepository).findByPaymentDateBetween(
                LocalDateTime.of(2026, 5, 1, 0, 0),
                LocalDateTime.of(2026, 5, 31, 23, 59, 59)
        );
    }

    @Test
    void getSalesReportByPeriodShouldThrowExceptionWhenStartDateIsNull() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reportService.getSalesReportByPeriod(null, LocalDate.of(2026, 5, 31)));

        assertEquals("La fecha de inicio es obligatoria", exception.getMessage());

        verify(paymentRepository, never()).findByPaymentDateBetween(any(), any());
    }

    @Test
    void getSalesReportByPeriodShouldThrowExceptionWhenEndDateIsNull() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reportService.getSalesReportByPeriod(LocalDate.of(2026, 5, 1), null));

        assertEquals("La fecha de termino es obligatoria", exception.getMessage());

        verify(paymentRepository, never()).findByPaymentDateBetween(any(), any());
    }

    @Test
    void getSalesReportByPeriodShouldThrowExceptionWhenStartDateIsAfterEndDate() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reportService.getSalesReportByPeriod(
                        LocalDate.of(2026, 6, 1),
                        LocalDate.of(2026, 5, 1)
                ));

        assertEquals("La fecha de inicio no puede ser posterior a la fecha de termino",
                exception.getMessage());

        verify(paymentRepository, never()).findByPaymentDateBetween(any(), any());
    }

    @Test
    void getTourPackageRankingByPeriodShouldGroupPaymentsByTourPackageAndExcludeCanceledReservations() {
        LocalDate startDate = LocalDate.of(2026, 5, 1);
        LocalDate endDate = LocalDate.of(2026, 5, 31);

        PaymentEntity firstPayment = payment(
                1L,
                1L,
                "Cliente Uno",
                "cliente1@test.com",
                10L,
                "Paquete Santiago",
                "Santiago",
                2,
                200000.0,
                200000.0,
                ReservationEntity.STATUS_CONFIRMED,
                LocalDateTime.of(2026, 5, 10, 12, 0)
        );

        PaymentEntity secondPaymentSamePackage = payment(
                2L,
                2L,
                "Cliente Dos",
                "cliente2@test.com",
                10L,
                "Paquete Santiago",
                "Santiago",
                3,
                300000.0,
                300000.0,
                ReservationEntity.STATUS_CONFIRMED,
                LocalDateTime.of(2026, 5, 11, 12, 0)
        );

        PaymentEntity canceledPayment = payment(
                3L,
                3L,
                "Cliente Tres",
                "cliente3@test.com",
                20L,
                "Paquete Norte",
                "Antofagasta",
                4,
                400000.0,
                400000.0,
                ReservationEntity.STATUS_CANCELED,
                LocalDateTime.of(2026, 5, 12, 12, 0)
        );

        when(paymentRepository.findByPaymentDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(firstPayment, secondPaymentSamePackage, canceledPayment));

        List<TourPackageRankingDto> result = reportService.getTourPackageRankingByPeriod(startDate, endDate);

        assertEquals(1, result.size());

        TourPackageRankingDto item = result.get(0);

        assertEquals(10L, item.getTourPackageId());
        assertEquals("Paquete Santiago", item.getTourPackageName());
        assertEquals("Santiago", item.getDestination());
        assertEquals(2L, item.getReservationCount());
        assertEquals(5, item.getTotalPassengers());
        assertEquals(500000.0, item.getTotalRevenue());

        verify(paymentRepository).findByPaymentDateBetween(
                LocalDateTime.of(2026, 5, 1, 0, 0),
                LocalDateTime.of(2026, 5, 31, 23, 59, 59)
        );
    }

    @Test
    void getTourPackageRankingByPeriodShouldThrowExceptionWhenPeriodIsInvalid() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reportService.getTourPackageRankingByPeriod(
                        LocalDate.of(2026, 6, 1),
                        LocalDate.of(2026, 5, 1)
                ));

        assertEquals("La fecha de inicio no puede ser posterior a la fecha de termino",
                exception.getMessage());

        verify(paymentRepository, never()).findByPaymentDateBetween(any(), any());
    }

    @Test
    void getTourPackageRankingByPeriodShouldReturnEmptyListWhenThereAreNoPayments() {
        when(paymentRepository.findByPaymentDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        List<TourPackageRankingDto> result = reportService.getTourPackageRankingByPeriod(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31)
        );

        assertTrue(result.isEmpty());

        verify(paymentRepository).findByPaymentDateBetween(any(LocalDateTime.class), any(LocalDateTime.class));
    }

    private PaymentEntity payment(
            Long paymentId,
            Long reservationId,
            String customerFullName,
            String customerEmail,
            Long tourPackageId,
            String tourPackageName,
            String destination,
            Integer passengerCount,
            Double reservationTotalAmount,
            Double paidAmount,
            String reservationStatus,
            LocalDateTime paymentDate
    ) {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setFullName(customerFullName);
        user.setEmail(customerEmail);

        TourPackageEntity tourPackage = new TourPackageEntity();
        tourPackage.setId(tourPackageId);
        tourPackage.setName(tourPackageName);
        tourPackage.setDestination(destination);

        ReservationEntity reservation = new ReservationEntity();
        reservation.setId(reservationId);
        reservation.setUser(user);
        reservation.setTourPackage(tourPackage);
        reservation.setPassengerCount(passengerCount);
        reservation.setFinalTotalAmount(reservationTotalAmount);
        reservation.setStatus(reservationStatus);

        PaymentEntity payment = new PaymentEntity();
        payment.setId(paymentId);
        payment.setReservation(reservation);
        payment.setAmount(paidAmount);
        payment.setPaymentDate(paymentDate);

        return payment;
    }
}