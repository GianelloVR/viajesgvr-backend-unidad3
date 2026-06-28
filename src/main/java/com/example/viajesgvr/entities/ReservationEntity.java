package com.example.viajesgvr.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationEntity {

    public static final String STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_CANCELED = "CANCELED";
    public static final String STATUS_EXPIRED = "EXPIRED";

    public static final int PAYMENT_EXPIRATION_MINUTES = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne
    @JoinColumn(name = "tour_package_id", nullable = false)
    private TourPackageEntity tourPackage;

    @Column(nullable = false)
    private Integer passengerCount;

    @Column(nullable = false)
    private Double originalTotalAmount;

    @Column(nullable = false)
    private Double finalTotalAmount;

    @Column(nullable = false)
    private Double discountAmount;

    @Column(columnDefinition = "TEXT")
    private String discountDescription;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private LocalDateTime reservationDate;

    private LocalDateTime paymentDeadline;

    @Column(length = 50)
    private String purchaseGroupCode;

    @Column(columnDefinition = "TEXT")
    private String specialRequests;

    @Column(nullable = false)
    private LocalDate tourStartDate;

    @Column(nullable = false)
    private LocalDate tourEndDate;
}