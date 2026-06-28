package com.example.viajesgvr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesReportItemDto {

    private String operationDate;
    private Long reservationId;
    private Long paymentId;

    private String customerFullName;
    private String customerEmail;

    private String tourPackageName;
    private String destination;
    private Integer passengerCount;

    private Double reservationTotalAmount;
    private Double paidAmount;
    private String reservationStatus;
}