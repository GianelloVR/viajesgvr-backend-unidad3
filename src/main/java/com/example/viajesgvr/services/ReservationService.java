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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReservationService {

    private static final long EXPIRATION_CHECK_RATE_MS = 10000;

    private static final int GROUP_DISCOUNT_MIN_PASSENGERS = 4;
    private static final double GROUP_DISCOUNT_PERCENTAGE = 0.10;

    private static final int FREQUENT_CLIENT_MIN_CONFIRMED_RESERVATIONS = 3;
    private static final double FREQUENT_CLIENT_DISCOUNT_PERCENTAGE = 0.05;

    private static final int MULTIPLE_PACKAGE_MIN_DISTINCT_PACKAGES = 2;
    private static final double MULTIPLE_PACKAGE_DISCOUNT_PERCENTAGE = 0.05;

    private static final double MAX_TOTAL_DISCOUNT_PERCENTAGE = 0.20;

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final TourPackageRepository tourPackageRepository;
    private final PaymentRepository paymentRepository;

    public ReservationService(ReservationRepository reservationRepository,
                              UserRepository userRepository,
                              TourPackageRepository tourPackageRepository,
                              PaymentRepository paymentRepository) {
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.tourPackageRepository = tourPackageRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public List<ReservationEntity> getAllReservations() {
        expirePendingReservations();
        return reservationRepository.findAll();
    }

    @Transactional
    public Optional<ReservationEntity> getReservationById(Long id) {
        return reservationRepository.findById(id)
                .map(this::expireReservationIfNeeded);
    }

    @Transactional
    public List<ReservationEntity> getReservationsByUserId(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        expirePendingReservations();

        return reservationRepository.findByUser(user);
    }

    @Transactional
    public List<ReservationEntity> getReservationsByTourPackageId(Long tourPackageId) {
        TourPackageEntity tourPackage = tourPackageRepository.findById(tourPackageId)
                .orElseThrow(() -> new RuntimeException("Paquete turístico no encontrado"));

        expirePendingReservations();

        return reservationRepository.findByTourPackage(tourPackage);
    }

    @Transactional
    public List<ReservationEntity> getReservationsByStatus(String status) {
        expirePendingReservations();
        return reservationRepository.findByStatus(status);
    }

    @Transactional
    public List<ReservationEntity> getReservationsByPurchaseGroupCode(String purchaseGroupCode) {
        expirePendingReservations();
        return reservationRepository.findByPurchaseGroupCode(purchaseGroupCode);
    }

    @Transactional
    public ReservationEntity createReservation(ReservationEntity reservation) {
        validateReservationRequest(reservation);

        Long userId = reservation.getUser().getId();
        Long tourPackageId = reservation.getTourPackage().getId();

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        TourPackageEntity tourPackage = tourPackageRepository.findById(tourPackageId)
                .orElseThrow(() -> new RuntimeException("Paquete turístico no encontrado"));

        validateUser(user);
        validateTourPackageForReservation(tourPackage, reservation.getPassengerCount());
        validateTourDates(reservation, tourPackage);

        LocalDateTime reservationDate = LocalDateTime.now();
        LocalDateTime paymentDeadline = reservationDate.plusMinutes(
                ReservationEntity.PAYMENT_EXPIRATION_MINUTES
        );

        ReservationEntity newReservation = buildReservation(
                reservation,
                user,
                tourPackage,
                reservationDate,
                paymentDeadline,
                null,
                false
        );

        discountTourPackageQuota(tourPackage, reservation.getPassengerCount());

        tourPackageRepository.save(tourPackage);

        return reservationRepository.save(newReservation);
    }

    @Transactional
    public List<ReservationEntity> createMultipleReservations(List<ReservationEntity> reservations) {
        validateMultipleReservationRequest(reservations);

        for (ReservationEntity reservation : reservations) {
            validateReservationRequest(reservation);
        }

        Long userId = reservations.get(0).getUser().getId();

        validateAllReservationsBelongToSameUser(reservations, userId);
        validateMultiplePackageSelection(reservations);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        validateUser(user);

        Map<Long, TourPackageEntity> tourPackagesById = new HashMap<>();
        Map<Long, Integer> requestedPassengersByPackage = new HashMap<>();

        for (ReservationEntity reservation : reservations) {
            Long tourPackageId = reservation.getTourPackage().getId();

            TourPackageEntity tourPackage = tourPackagesById.computeIfAbsent(
                    tourPackageId,
                    id -> tourPackageRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("Paquete turístico no encontrado"))
            );

            validateTourPackageForReservation(tourPackage, reservation.getPassengerCount());
            validateTourDates(reservation, tourPackage);

            requestedPassengersByPackage.merge(
                    tourPackageId,
                    reservation.getPassengerCount(),
                    Integer::sum
            );
        }

        validateAggregatedQuota(tourPackagesById, requestedPassengersByPackage);

        String purchaseGroupCode = generatePurchaseGroupCode();

        LocalDateTime reservationDate = LocalDateTime.now();
        LocalDateTime paymentDeadline = reservationDate.plusMinutes(
                ReservationEntity.PAYMENT_EXPIRATION_MINUTES
        );

        List<ReservationEntity> createdReservations = new ArrayList<>();

        for (ReservationEntity reservation : reservations) {
            TourPackageEntity tourPackage = tourPackagesById.get(reservation.getTourPackage().getId());

            ReservationEntity newReservation = buildReservation(
                    reservation,
                    user,
                    tourPackage,
                    reservationDate,
                    paymentDeadline,
                    purchaseGroupCode,
                    true
            );

            discountTourPackageQuota(tourPackage, reservation.getPassengerCount());
            tourPackageRepository.save(tourPackage);

            createdReservations.add(reservationRepository.save(newReservation));
        }

        return createdReservations;
    }

    @Transactional
    public ReservationEntity cancelReservation(Long id) {
        ReservationEntity existingReservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        existingReservation = expireReservationIfNeeded(existingReservation);

        if (ReservationEntity.STATUS_CANCELED.equals(existingReservation.getStatus())) {
            return existingReservation;
        }

        if (ReservationEntity.STATUS_EXPIRED.equals(existingReservation.getStatus())) {
            return existingReservation;
        }

        if (ReservationEntity.STATUS_CONFIRMED.equals(existingReservation.getStatus())) {
            throw new RuntimeException("No se puede cancelar una reserva confirmada");
        }

        restoreQuota(existingReservation);

        existingReservation.setStatus(ReservationEntity.STATUS_CANCELED);

        return reservationRepository.save(existingReservation);
    }

    @Transactional
    public ReservationSummaryDto getReservationSummary(Long id) {
        ReservationEntity reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        reservation = expireReservationIfNeeded(reservation);

        ReservationSummaryDto summary = new ReservationSummaryDto();

        summary.setReservationId(reservation.getId());
        summary.setReservationStatus(reservation.getStatus());
        summary.setReservationDate(reservation.getReservationDate().toString());
        summary.setPurchaseGroupCode(reservation.getPurchaseGroupCode());

        summary.setUserId(reservation.getUser().getId());
        summary.setUserFullName(reservation.getUser().getFullName());
        summary.setUserEmail(reservation.getUser().getEmail());

        summary.setTourPackageId(reservation.getTourPackage().getId());
        summary.setTourPackageName(reservation.getTourPackage().getName());
        summary.setDestination(reservation.getTourPackage().getDestination());
        summary.setStartDate(reservation.getTourPackage().getStartDate().toString());
        summary.setEndDate(reservation.getTourPackage().getEndDate().toString());
        summary.setPassengerCount(reservation.getPassengerCount());

        summary.setOriginalTotalAmount(reservation.getOriginalTotalAmount());
        summary.setDiscountAmount(reservation.getDiscountAmount());
        summary.setDiscountDescription(reservation.getDiscountDescription());
        summary.setFinalTotalAmount(reservation.getFinalTotalAmount());

        PaymentEntity payment = paymentRepository.findByReservation(reservation).orElse(null);

        if (payment != null) {
            summary.setPaymentRegistered(true);
            summary.setPaymentId(payment.getId());
            summary.setPaymentStatus(payment.getPaymentStatus());
            summary.setPaymentMethod(payment.getPaymentMethod());
            summary.setPaymentDate(payment.getPaymentDate().toString());
        } else {
            summary.setPaymentRegistered(false);
            summary.setPaymentId(null);
            summary.setPaymentStatus(null);
            summary.setPaymentMethod(null);
            summary.setPaymentDate(null);
        }

        return summary;
    }

    @Scheduled(fixedRate = EXPIRATION_CHECK_RATE_MS)
    @Transactional
    public void expirePendingReservations() {
        List<ReservationEntity> pendingReservations =
                reservationRepository.findByStatus(ReservationEntity.STATUS_PENDING_PAYMENT);

        for (ReservationEntity reservation : pendingReservations) {
            expireReservationIfNeeded(reservation);
        }
    }

    @Transactional
    public ReservationEntity expireReservationIfNeeded(ReservationEntity reservation) {
        if (reservation == null) {
            return null;
        }

        if (!ReservationEntity.STATUS_PENDING_PAYMENT.equals(reservation.getStatus())) {
            return reservation;
        }

        LocalDateTime deadline = getPaymentDeadlineOrDefault(reservation);

        if (LocalDateTime.now().isBefore(deadline)) {
            if (reservation.getPaymentDeadline() == null) {
                reservation.setPaymentDeadline(deadline);
                return reservationRepository.save(reservation);
            }

            return reservation;
        }

        restoreQuota(reservation);

        reservation.setStatus(ReservationEntity.STATUS_EXPIRED);
        reservation.setPaymentDeadline(deadline);

        return reservationRepository.save(reservation);
    }

    private ReservationEntity buildReservation(ReservationEntity reservation,
                                               UserEntity user,
                                               TourPackageEntity tourPackage,
                                               LocalDateTime reservationDate,
                                               LocalDateTime paymentDeadline,
                                               String purchaseGroupCode,
                                               boolean multiplePackagePurchase) {
        double originalTotalAmount = tourPackage.getPrice() * reservation.getPassengerCount();

        DiscountCalculation discountCalculation = calculateDiscount(
                user,
                tourPackage,
                reservation.getPassengerCount(),
                originalTotalAmount,
                multiplePackagePurchase
        );

        double discountAmount = discountCalculation.discountAmount;
        String discountDescription = discountCalculation.discountDescription;
        double finalTotalAmount = roundCurrency(Math.max(0.0, originalTotalAmount - discountAmount));

        ReservationEntity newReservation = new ReservationEntity();
        newReservation.setUser(user);
        newReservation.setTourPackage(tourPackage);
        newReservation.setPassengerCount(reservation.getPassengerCount());
        newReservation.setOriginalTotalAmount(originalTotalAmount);
        newReservation.setDiscountAmount(discountAmount);
        newReservation.setDiscountDescription(discountDescription);
        newReservation.setFinalTotalAmount(finalTotalAmount);
        newReservation.setStatus(ReservationEntity.STATUS_PENDING_PAYMENT);
        newReservation.setReservationDate(reservationDate);
        newReservation.setPaymentDeadline(paymentDeadline);
        newReservation.setPurchaseGroupCode(purchaseGroupCode);
        newReservation.setSpecialRequests(reservation.getSpecialRequests());
        newReservation.setTourStartDate(reservation.getTourStartDate());
        newReservation.setTourEndDate(reservation.getTourEndDate());

        return newReservation;
    }

    private void discountTourPackageQuota(TourPackageEntity tourPackage, Integer passengerCount) {
        tourPackage.setAvailableQuota(tourPackage.getAvailableQuota() - passengerCount);

        if (tourPackage.getAvailableQuota() == 0) {
            tourPackage.setStatus(TourPackageEntity.STATUS_SOLD_OUT);
        }
    }

    private String generatePurchaseGroupCode() {
        return "COMPRA-" + UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();
    }

    private LocalDateTime getPaymentDeadlineOrDefault(ReservationEntity reservation) {
        if (reservation.getPaymentDeadline() != null) {
            return reservation.getPaymentDeadline();
        }

        if (reservation.getReservationDate() != null) {
            return reservation.getReservationDate().plusMinutes(
                    ReservationEntity.PAYMENT_EXPIRATION_MINUTES
            );
        }

        return LocalDateTime.now().plusMinutes(ReservationEntity.PAYMENT_EXPIRATION_MINUTES);
    }

    private void restoreQuota(ReservationEntity reservation) {
        TourPackageEntity reservationTourPackage = reservation.getTourPackage();

        if (reservationTourPackage == null || reservation.getPassengerCount() == null) {
            return;
        }

        TourPackageEntity tourPackage = getFreshTourPackage(reservationTourPackage);

        int currentAvailableQuota = tourPackage.getAvailableQuota() != null
                ? tourPackage.getAvailableQuota()
                : 0;

        int totalQuota = tourPackage.getTotalQuota() != null
                ? tourPackage.getTotalQuota()
                : currentAvailableQuota + reservation.getPassengerCount();

        int restoredQuota = currentAvailableQuota + reservation.getPassengerCount();

        if (restoredQuota > totalQuota) {
            restoredQuota = totalQuota;
        }

        tourPackage.setAvailableQuota(restoredQuota);

        if (TourPackageEntity.STATUS_SOLD_OUT.equals(tourPackage.getStatus())
                && restoredQuota > 0) {
            tourPackage.setStatus(TourPackageEntity.STATUS_AVAILABLE);
        }

        tourPackageRepository.save(tourPackage);
        reservation.setTourPackage(tourPackage);
    }

    private TourPackageEntity getFreshTourPackage(TourPackageEntity tourPackage) {
        if (tourPackage.getId() == null) {
            return tourPackage;
        }

        return tourPackageRepository.findById(tourPackage.getId())
                .orElse(tourPackage);
    }

    private void validateReservationRequest(ReservationEntity reservation) {
        if (reservation.getUser() == null || reservation.getUser().getId() == null) {
            throw new RuntimeException("El usuario es obligatorio");
        }

        if (reservation.getTourPackage() == null || reservation.getTourPackage().getId() == null) {
            throw new RuntimeException("El paquete turístico es obligatorio");
        }

        if (reservation.getPassengerCount() == null || reservation.getPassengerCount() <= 0) {
            throw new RuntimeException("La cantidad de pasajeros debe ser mayor que cero");
        }

        if (reservation.getTourStartDate() == null) {
            throw new RuntimeException("La fecha de inicio del tour es obligatoria");
        }

        if (reservation.getTourEndDate() == null) {
            throw new RuntimeException("La fecha de término del tour es obligatoria");
        }
    }

    private void validateMultipleReservationRequest(List<ReservationEntity> reservations) {
        if (reservations == null || reservations.isEmpty()) {
            throw new RuntimeException("Debe existir al menos una reserva para la compra múltiple");
        }

        if (reservations.size() < 2) {
            throw new RuntimeException("La compra múltiple debe incluir al menos dos reservas");
        }
    }

    private void validateAllReservationsBelongToSameUser(List<ReservationEntity> reservations, Long userId) {
        for (ReservationEntity reservation : reservations) {
            if (!userId.equals(reservation.getUser().getId())) {
                throw new RuntimeException("Todas las reservas de la compra múltiple deben pertenecer al mismo usuario");
            }
        }
    }

    private void validateMultiplePackageSelection(List<ReservationEntity> reservations) {
        long distinctPackageCount = reservations.stream()
                .map(reservation -> reservation.getTourPackage().getId())
                .distinct()
                .count();

        if (distinctPackageCount < MULTIPLE_PACKAGE_MIN_DISTINCT_PACKAGES) {
            throw new RuntimeException("La compra múltiple debe incluir al menos dos paquetes turísticos distintos");
        }
    }

    private void validateAggregatedQuota(Map<Long, TourPackageEntity> tourPackagesById,
                                         Map<Long, Integer> requestedPassengersByPackage) {
        for (Map.Entry<Long, Integer> entry : requestedPassengersByPackage.entrySet()) {
            TourPackageEntity tourPackage = tourPackagesById.get(entry.getKey());
            Integer requestedPassengers = entry.getValue();

            if (requestedPassengers > tourPackage.getAvailableQuota()) {
                throw new RuntimeException("La cantidad total de pasajeros excede los cupos disponibles del paquete "
                        + tourPackage.getName());
            }
        }
    }

    private void validateUser(UserEntity user) {
        if (user.getActive() == null || !user.getActive()) {
            throw new RuntimeException("El usuario debe estar activo para realizar una reserva");
        }
    }

    private void validateTourPackageForReservation(TourPackageEntity tourPackage, Integer passengerCount) {
        if (!TourPackageEntity.STATUS_AVAILABLE.equals(tourPackage.getStatus())) {
            throw new RuntimeException("El paquete turístico no está disponible para reserva");
        }

        if (tourPackage.getEndDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("El paquete turístico ya no está vigente");
        }

        if (tourPackage.getAvailableQuota() == null || tourPackage.getAvailableQuota() <= 0) {
            throw new RuntimeException("El paquete turístico no tiene cupos disponibles");
        }

        if (passengerCount > tourPackage.getAvailableQuota()) {
            throw new RuntimeException("La cantidad de pasajeros excede los cupos disponibles");
        }
    }

    private void validateTourDates(ReservationEntity reservation, TourPackageEntity tourPackage) {
        if (reservation.getTourStartDate().isBefore(tourPackage.getStartDate())
                || reservation.getTourStartDate().isAfter(tourPackage.getEndDate())) {
            throw new RuntimeException("La fecha de inicio del tour debe estar dentro de la vigencia del paquete");
        }

        if (!reservation.getTourEndDate().isAfter(reservation.getTourStartDate())) {
            throw new RuntimeException("La fecha de término del tour debe ser posterior a la fecha de inicio del tour");
        }
    }

    private DiscountCalculation calculateDiscount(UserEntity user,
                                                  TourPackageEntity tourPackage,
                                                  Integer passengerCount,
                                                  double originalTotalAmount,
                                                  boolean multiplePackagePurchase) {
        double totalDiscountPercentage = 0.0;
        StringBuilder discountDescription = new StringBuilder();

        int promotionDiscountPercentage = getPromotionDiscountPercentage(tourPackage);

        if (promotionDiscountPercentage > 0) {
            totalDiscountPercentage += promotionDiscountPercentage / 100.0;
            appendDiscountDescription(
                    discountDescription,
                    "Promoción del paquete (" + promotionDiscountPercentage + "%)"
            );
        }

        if (passengerCount >= GROUP_DISCOUNT_MIN_PASSENGERS) {
            totalDiscountPercentage += GROUP_DISCOUNT_PERCENTAGE;
            appendDiscountDescription(discountDescription, "Descuento por grupo (10%)");
        }

        if (isFrequentClient(user)) {
            totalDiscountPercentage += FREQUENT_CLIENT_DISCOUNT_PERCENTAGE;
            appendDiscountDescription(discountDescription, "Descuento cliente frecuente (5%)");
        }

        if (multiplePackagePurchase) {
            totalDiscountPercentage += MULTIPLE_PACKAGE_DISCOUNT_PERCENTAGE;
            appendDiscountDescription(discountDescription, "Descuento por compra múltiple (5%)");
        }

        if (totalDiscountPercentage > MAX_TOTAL_DISCOUNT_PERCENTAGE) {
            totalDiscountPercentage = MAX_TOTAL_DISCOUNT_PERCENTAGE;

            if (!discountDescription.isEmpty()) {
                discountDescription.append(" - límite máximo aplicado (20%)");
            }
        }

        double discountAmount = roundCurrency(originalTotalAmount * totalDiscountPercentage);

        if (discountDescription.isEmpty()) {
            return new DiscountCalculation(0.0, "Sin descuento");
        }

        return new DiscountCalculation(discountAmount, discountDescription.toString());
    }

    private int getPromotionDiscountPercentage(TourPackageEntity tourPackage) {
        if (tourPackage == null || tourPackage.getPromotionDiscountPercentage() == null) {
            return 0;
        }

        if (tourPackage.getPromotionDiscountPercentage() < 0
                || tourPackage.getPromotionDiscountPercentage() > 20) {
            throw new RuntimeException("La promoción debe estar entre 0 y 20");
        }

        return tourPackage.getPromotionDiscountPercentage();
    }

    private void appendDiscountDescription(StringBuilder discountDescription, String description) {
        if (!discountDescription.isEmpty()) {
            discountDescription.append(" + ");
        }

        discountDescription.append(description);
    }



    private boolean isFrequentClient(UserEntity user) {
        List<ReservationEntity> userReservations = reservationRepository.findByUser(user);

        long confirmedReservations = userReservations.stream()
                .filter(reservation -> ReservationEntity.STATUS_CONFIRMED.equals(reservation.getStatus()))
                .count();

        return confirmedReservations >= FREQUENT_CLIENT_MIN_CONFIRMED_RESERVATIONS;
    }

    private static class DiscountCalculation {
        private final double discountAmount;
        private final String discountDescription;

        private DiscountCalculation(double discountAmount, String discountDescription) {
            this.discountAmount = discountAmount;
            this.discountDescription = discountDescription;
        }
    }

    private double roundCurrency(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }

}