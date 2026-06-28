package com.example.viajesgvr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TourPackageRankingDto {

    private Long tourPackageId;
    private String tourPackageName;
    private String destination;
    private Long reservationCount;
    private Integer totalPassengers;
    private Double totalRevenue;
}
