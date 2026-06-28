package com.example.viajesgvr.repositories;

import com.example.viajesgvr.entities.PaymentEntity;
import com.example.viajesgvr.entities.ReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {

    Optional<PaymentEntity> findByReservation(ReservationEntity reservation);

    boolean existsByReservation(ReservationEntity reservation);

    List<PaymentEntity> findByPaymentDateBetween(LocalDateTime startDateTime, LocalDateTime endDateTime);
}