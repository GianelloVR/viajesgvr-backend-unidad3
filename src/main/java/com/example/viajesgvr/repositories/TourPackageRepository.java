package com.example.viajesgvr.repositories;

import com.example.viajesgvr.entities.TourPackageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TourPackageRepository extends JpaRepository<TourPackageEntity, Long> {

    List<TourPackageEntity> findByDestination(String destination);

    List<TourPackageEntity> findByStatus(String status);

    List<TourPackageEntity> findByPriceBetween(Double minPrice, Double maxPrice);

    @Query("""
            SELECT tp
            FROM TourPackageEntity tp
            WHERE tp.status = 'AVAILABLE'
              AND tp.availableQuota > 0
              AND tp.endDate >= CURRENT_DATE
              AND (:destination IS NULL OR LOWER(tp.destination) LIKE LOWER(CONCAT('%', :destination, '%')))
              AND (:minPrice IS NULL OR tp.price >= :minPrice)
              AND (:maxPrice IS NULL OR tp.price <= :maxPrice)
              AND (:travelDate IS NULL OR :travelDate BETWEEN tp.startDate AND tp.endDate)
              AND (:travelType IS NULL OR LOWER(tp.travelType) = LOWER(:travelType))
              AND (:season IS NULL OR LOWER(tp.season) = LOWER(:season))
              AND (:category IS NULL OR LOWER(tp.category) = LOWER(:category))
              AND (:minDuration IS NULL OR tp.durationDays >= :minDuration)
              AND (:maxDuration IS NULL OR tp.durationDays <= :maxDuration)
            """)
    List<TourPackageEntity> searchAvailableTourPackages(
            @Param("destination") String destination,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            @Param("travelDate") LocalDate travelDate,
            @Param("travelType") String travelType,
            @Param("season") String season,
            @Param("category") String category,
            @Param("minDuration") Integer minDuration,
            @Param("maxDuration") Integer maxDuration
    );
}