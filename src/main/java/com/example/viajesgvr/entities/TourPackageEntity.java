package com.example.viajesgvr.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "tour_packages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TourPackageEntity {

    public static final String STATUS_AVAILABLE = "AVAILABLE";
    public static final String STATUS_SOLD_OUT = "SOLD_OUT";
    public static final String STATUS_NOT_VALID = "NOT_VALID";
    public static final String STATUS_CANCELED = "CANCELED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String destination;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private Integer durationDays;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private Integer totalQuota;

    @Column(nullable = false)
    private Integer availableQuota;

    @Column(columnDefinition = "TEXT")
    private String includedServices;

    @Column(columnDefinition = "TEXT")
    private String conditions;

    @Column(columnDefinition = "TEXT")
    private String restrictions;

    private String travelType;
    private String season;
    private String category;

    private Integer promotionDiscountPercentage = 0;

    @Column(nullable = false)
    private String status;
}