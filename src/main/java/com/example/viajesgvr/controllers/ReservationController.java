package com.example.viajesgvr.controllers;

import com.example.viajesgvr.entities.ReservationEntity;
import com.example.viajesgvr.services.ReservationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.viajesgvr.dto.ReservationSummaryDto;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
@CrossOrigin("*")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping("/")
    public ResponseEntity<List<ReservationEntity>> getAllReservations() {
        List<ReservationEntity> reservations = reservationService.getAllReservations();
        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationEntity> getReservationById(@PathVariable Long id) {
        return reservationService.getReservationById(id)
                .map(reservation -> ResponseEntity.ok(reservation))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getReservationsByUserId(@PathVariable Long userId) {
        try {
            List<ReservationEntity> reservations = reservationService.getReservationsByUserId(userId);
            return ResponseEntity.ok(reservations);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/tour-package/{tourPackageId}")
    public ResponseEntity<?> getReservationsByTourPackageId(@PathVariable Long tourPackageId) {
        try {
            List<ReservationEntity> reservations = reservationService.getReservationsByTourPackageId(tourPackageId);
            return ResponseEntity.ok(reservations);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<ReservationEntity>> getReservationsByStatus(@PathVariable String status) {
        List<ReservationEntity> reservations = reservationService.getReservationsByStatus(status);
        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/purchase-group/{purchaseGroupCode}")
    public ResponseEntity<List<ReservationEntity>> getReservationsByPurchaseGroupCode(@PathVariable String purchaseGroupCode) {
        List<ReservationEntity> reservations = reservationService.getReservationsByPurchaseGroupCode(purchaseGroupCode);
        return ResponseEntity.ok(reservations);
    }

    @PostMapping("/")
    public ResponseEntity<?> createReservation(@RequestBody ReservationEntity reservation) {
        try {
            ReservationEntity newReservation = reservationService.createReservation(reservation);
            return ResponseEntity.status(HttpStatus.CREATED).body(newReservation);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/multiple")
    public ResponseEntity<?> createMultipleReservations(@RequestBody List<ReservationEntity> reservations) {
        try {
            List<ReservationEntity> newReservations = reservationService.createMultipleReservations(reservations);
            return ResponseEntity.status(HttpStatus.CREATED).body(newReservations);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<?> getReservationSummary(@PathVariable Long id) {
        try {
            ReservationSummaryDto summary = reservationService.getReservationSummary(id);
            return ResponseEntity.ok(summary);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }


    @PatchMapping("/{id}/cancel")
    public ResponseEntity<?> cancelReservation(@PathVariable Long id) {
        try {
            ReservationEntity canceledReservation = reservationService.cancelReservation(id);
            return ResponseEntity.ok(canceledReservation);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}