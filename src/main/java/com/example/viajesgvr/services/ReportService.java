package com.example.viajesgvr.services;

import com.example.viajesgvr.dto.SalesReportItemDto;
import com.example.viajesgvr.dto.TourPackageRankingDto;
import com.example.viajesgvr.entities.PaymentEntity;
import com.example.viajesgvr.entities.ReservationEntity;
import com.example.viajesgvr.repositories.PaymentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ReportService {

    private final PaymentRepository paymentRepository;

    public ReportService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public List<SalesReportItemDto> getSalesReportByPeriod(LocalDate startDate, LocalDate endDate) {
        validatePeriod(startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        List<PaymentEntity> payments = paymentRepository.findByPaymentDateBetween(startDateTime, endDateTime);

        return payments.stream()
                .filter(payment -> !ReservationEntity.STATUS_CANCELED.equals(payment.getReservation().getStatus()))
                .map(this::mapToSalesReportItem)
                .toList();
    }

    private void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new RuntimeException("La fecha de inicio es obligatoria");
        }

        if (endDate == null) {
            throw new RuntimeException("La fecha de termino es obligatoria");
        }

        if (startDate.isAfter(endDate)) {
            throw new RuntimeException("La fecha de inicio no puede ser posterior a la fecha de termino");
        }
    }

    private SalesReportItemDto mapToSalesReportItem(PaymentEntity payment) {
        ReservationEntity reservation = payment.getReservation();

        return new SalesReportItemDto(
                payment.getPaymentDate().toString(),
                reservation.getId(),
                payment.getId(),
                reservation.getUser().getFullName(),
                reservation.getUser().getEmail(),
                reservation.getTourPackage().getName(),
                reservation.getTourPackage().getDestination(),
                reservation.getPassengerCount(),
                reservation.getFinalTotalAmount(),
                payment.getAmount(),
                reservation.getStatus()
        );
    }

    public List<TourPackageRankingDto> getTourPackageRankingByPeriod(LocalDate startDate, LocalDate endDate) {
        validatePeriod(startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        List<PaymentEntity> payments = paymentRepository.findByPaymentDateBetween(startDateTime, endDateTime);

        Map<Long, TourPackageRankingDto> rankingMap = new LinkedHashMap<>();

        for (PaymentEntity payment : payments) {
            ReservationEntity reservation = payment.getReservation();

            if (ReservationEntity.STATUS_CANCELED.equals(reservation.getStatus())) {
                continue;
            }

            Long tourPackageId = reservation.getTourPackage().getId();

            if (!rankingMap.containsKey(tourPackageId)) {
                rankingMap.put(tourPackageId, new TourPackageRankingDto(
                        tourPackageId,
                        reservation.getTourPackage().getName(),
                        reservation.getTourPackage().getDestination(),
                        0L,
                        0,
                        0.0
                ));
            }

            TourPackageRankingDto item = rankingMap.get(tourPackageId);
            item.setReservationCount(item.getReservationCount() + 1);
            item.setTotalPassengers(item.getTotalPassengers() + reservation.getPassengerCount());
            item.setTotalRevenue(item.getTotalRevenue() + payment.getAmount());
        }

        return rankingMap.values().stream()
                .sorted(Comparator
                        .comparing(TourPackageRankingDto::getTotalRevenue).reversed()
                        .thenComparing(TourPackageRankingDto::getReservationCount).reversed()
                        .thenComparing(TourPackageRankingDto::getTotalPassengers).reversed()
                        .thenComparing(TourPackageRankingDto::getTourPackageName))
                .toList();
    }
}