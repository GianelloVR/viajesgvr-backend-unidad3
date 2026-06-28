package com.example.viajesgvr.config;

import com.example.viajesgvr.entities.PaymentEntity;
import com.example.viajesgvr.entities.ReservationEntity;
import com.example.viajesgvr.entities.TourPackageEntity;
import com.example.viajesgvr.entities.UserEntity;
import com.example.viajesgvr.repositories.PaymentRepository;
import com.example.viajesgvr.repositories.ReservationRepository;
import com.example.viajesgvr.repositories.TourPackageRepository;
import com.example.viajesgvr.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Configuration
public class LocalDataSeeder {

    @Bean
    @Transactional
    CommandLineRunner seedLocalData(
            UserRepository userRepository,
            TourPackageRepository tourPackageRepository,
            ReservationRepository reservationRepository,
            PaymentRepository paymentRepository
    ) {
        return args -> {
            UserEntity client = getOrCreateUser(
                    userRepository,
                    "cliente.demo@viajesgvr.cl",
                    "Cliente Demo",
                    "CLIENT"
            );

            UserEntity admin = getOrCreateUser(
                    userRepository,
                    "admin.demo@viajesgvr.cl",
                    "Administrador Demo",
                    "ADMIN"
            );

            if (tourPackageRepository.count() == 0) {
                List<TourPackageEntity> packages = List.of(
                        buildPackage(
                                "Escapada a San Pedro de Atacama",
                                "San Pedro de Atacama",
                                "Paquete turístico al norte de Chile con tours al Valle de la Luna, geysers y lagunas altiplánicas.",
                                7,
                                650000.0,
                                40,
                                "Turismo",
                                "Alta",
                                "Nacional",
                                10
                        ),
                        buildPackage(
                                "Aventura en Torres del Paine",
                                "Puerto Natales",
                                "Experiencia de naturaleza en la Patagonia con alojamiento, traslados y excursiones guiadas.",
                                8,
                                980000.0,
                                35,
                                "Aventura",
                                "Alta",
                                "Nacional",
                                5
                        ),
                        buildPackage(
                                "Caribe Todo Incluido",
                                "Punta Cana",
                                "Viaje internacional con hotel todo incluido, traslados y asistencia al viajero.",
                                6,
                                1250000.0,
                                50,
                                "Vacacional",
                                "Alta",
                                "Internacional",
                                15
                        ),
                        buildPackage(
                                "Buenos Aires Cultural",
                                "Buenos Aires",
                                "Paquete urbano con hotel, city tour, experiencia gastronómica y actividades culturales.",
                                4,
                                420000.0,
                                45,
                                "Cultural",
                                "Media",
                                "Internacional",
                                0
                        ),
                        buildPackage(
                                "Lagos del Sur",
                                "Puerto Varas",
                                "Recorrido por la zona lacustre del sur de Chile, incluyendo alojamiento y excursiones.",
                                5,
                                520000.0,
                                30,
                                "Turismo",
                                "Media",
                                "Nacional",
                                0
                        ),
                        buildPackage(
                                "Gira de Estudios Bariloche",
                                "Bariloche",
                                "Paquete grupal para estudiantes con alojamiento, alimentación y actividades recreativas.",
                                7,
                                780000.0,
                                80,
                                "Gira",
                                "Alta",
                                "Internacional",
                                10
                        )
                );

                tourPackageRepository.saveAll(packages);
            }

            if (reservationRepository.count() == 0 && paymentRepository.count() == 0) {
                List<TourPackageEntity> packages = tourPackageRepository.findAll();

                if (packages.size() >= 3) {
                    createConfirmedReservationWithPayment(
                            reservationRepository,
                            paymentRepository,
                            client,
                            packages.get(0),
                            2,
                            LocalDateTime.now().minusDays(10)
                    );

                    createConfirmedReservationWithPayment(
                            reservationRepository,
                            paymentRepository,
                            client,
                            packages.get(1),
                            4,
                            LocalDateTime.now().minusDays(8)
                    );

                    createConfirmedReservationWithPayment(
                            reservationRepository,
                            paymentRepository,
                            client,
                            packages.get(2),
                            3,
                            LocalDateTime.now().minusDays(5)
                    );

                    createConfirmedReservationWithPayment(
                            reservationRepository,
                            paymentRepository,
                            admin,
                            packages.get(0),
                            1,
                            LocalDateTime.now().minusDays(2)
                    );

                    createConfirmedReservationWithPayment(
                            reservationRepository,
                            paymentRepository,
                            admin,
                            packages.get(1),
                            5,
                            LocalDateTime.now().minusDays(1)
                    );
                }
            }
        };
    }

    private UserEntity getOrCreateUser(
            UserRepository userRepository,
            String email,
            String fullName,
            String role
    ) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    UserEntity user = new UserEntity();

                    user.setFullName(fullName);
                    user.setEmail(email);
                    user.setPassword("KEYCLOAK_USER");
                    user.setPhone("+56912345678");
                    user.setNationality("Chilena");
                    user.setDocumentNumber("12345678-9");
                    user.setActive(true);
                    user.setRole(role);

                    return userRepository.save(user);
                });
    }

    private TourPackageEntity buildPackage(
            String name,
            String destination,
            String description,
            Integer durationDays,
            Double price,
            Integer quota,
            String travelType,
            String season,
            String category,
            Integer promotionDiscountPercentage
    ) {
        LocalDate startDate = LocalDate.now().plusDays(15);
        LocalDate endDate = LocalDate.now().plusMonths(8);

        TourPackageEntity tourPackage = new TourPackageEntity();

        tourPackage.setName(name);
        tourPackage.setDestination(destination);
        tourPackage.setDescription(description);
        tourPackage.setStartDate(startDate);
        tourPackage.setEndDate(endDate);
        tourPackage.setDurationDays(durationDays);
        tourPackage.setPrice(price);
        tourPackage.setTotalQuota(quota);
        tourPackage.setAvailableQuota(quota);
        tourPackage.setIncludedServices("Alojamiento, traslados, asistencia al viajero y actividades programadas.");
        tourPackage.setConditions("Reserva sujeta a disponibilidad de cupos.");
        tourPackage.setRestrictions("No incluye gastos personales ni servicios no indicados.");
        tourPackage.setTravelType(travelType);
        tourPackage.setSeason(season);
        tourPackage.setCategory(category);
        tourPackage.setPromotionDiscountPercentage(promotionDiscountPercentage);
        tourPackage.setStatus(TourPackageEntity.STATUS_AVAILABLE);

        return tourPackage;
    }

    private void createConfirmedReservationWithPayment(
            ReservationRepository reservationRepository,
            PaymentRepository paymentRepository,
            UserEntity user,
            TourPackageEntity tourPackage,
            Integer passengerCount,
            LocalDateTime operationDate
    ) {
        double originalAmount = tourPackage.getPrice() * passengerCount;
        double discountPercentage = passengerCount >= 4 ? 0.10 : 0.0;
        double discountAmount = originalAmount * discountPercentage;
        double finalAmount = originalAmount - discountAmount;

        ReservationEntity reservation = new ReservationEntity();

        reservation.setUser(user);
        reservation.setTourPackage(tourPackage);
        reservation.setPassengerCount(passengerCount);
        reservation.setOriginalTotalAmount(originalAmount);
        reservation.setDiscountAmount(discountAmount);
        reservation.setFinalTotalAmount(finalAmount);
        reservation.setDiscountDescription(
                discountAmount > 0 ? "Descuento por grupo" : "Sin descuento"
        );
        reservation.setStatus(ReservationEntity.STATUS_CONFIRMED);
        reservation.setReservationDate(operationDate);
        reservation.setPaymentDeadline(operationDate.plusMinutes(1));
        reservation.setPurchaseGroupCode(null);
        reservation.setSpecialRequests("Reserva de prueba para evaluación 3.");
        reservation.setTourStartDate(tourPackage.getStartDate().plusDays(10));
        reservation.setTourEndDate(tourPackage.getStartDate().plusDays(10 + tourPackage.getDurationDays()));

        ReservationEntity savedReservation = reservationRepository.save(reservation);

        PaymentEntity payment = new PaymentEntity();

        payment.setReservation(savedReservation);
        payment.setAmount(finalAmount);
        payment.setPaymentMethod(PaymentEntity.PAYMENT_METHOD_CREDIT_CARD);
        payment.setPaymentStatus(PaymentEntity.PAYMENT_STATUS_APPROVED);
        payment.setPaymentDate(operationDate);
        payment.setCardNumber("4111111111111111");
        payment.setCardExpiration("12/30");
        payment.setCardCvv("123");

        paymentRepository.save(payment);
    }
}