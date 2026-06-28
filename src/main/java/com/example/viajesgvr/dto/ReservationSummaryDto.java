package com.example.viajesgvr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationSummaryDto {

    private Long reservationId;
    private String reservationStatus;
    private String reservationDate;
    private String purchaseGroupCode;

    private Long userId;
    private String userFullName;
    private String userEmail;

    private Long tourPackageId;
    private String tourPackageName;
    private String destination;
    private String startDate;
    private String endDate;
    private Integer passengerCount;

    private Double originalTotalAmount;
    private Double discountAmount;
    private String discountDescription;
    private Double finalTotalAmount;

    private Boolean paymentRegistered;
    private Long paymentId;
    private String paymentStatus;
    private String paymentMethod;
    private String paymentDate;
}