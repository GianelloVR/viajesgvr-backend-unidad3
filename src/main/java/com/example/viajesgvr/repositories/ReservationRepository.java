package com.example.viajesgvr.repositories;

import com.example.viajesgvr.entities.ReservationEntity;
import com.example.viajesgvr.entities.TourPackageEntity;
import com.example.viajesgvr.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<ReservationEntity, Long> {

    List<ReservationEntity> findByUser(UserEntity user);

    List<ReservationEntity> findByTourPackage(TourPackageEntity tourPackage);

    List<ReservationEntity> findByStatus(String status);

    List<ReservationEntity> findByPurchaseGroupCode(String purchaseGroupCode);
}