package com.example.viajesgvr.controllers;

import com.example.viajesgvr.entities.TourPackageEntity;
import com.example.viajesgvr.services.TourPackageService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tour-packages")
@CrossOrigin("*")
public class TourPackageController {

    private final TourPackageService tourPackageService;

    public TourPackageController(TourPackageService tourPackageService) {
        this.tourPackageService = tourPackageService;
    }

    @GetMapping("/")
    public ResponseEntity<List<TourPackageEntity>> getAllTourPackages() {
        List<TourPackageEntity> tourPackages = tourPackageService.getAllTourPackages();
        return ResponseEntity.ok(tourPackages);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TourPackageEntity> getTourPackageById(@PathVariable Long id) {
        return tourPackageService.getTourPackageById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/destination/{destination}")
    public ResponseEntity<List<TourPackageEntity>> getTourPackagesByDestination(@PathVariable String destination) {
        List<TourPackageEntity> tourPackages = tourPackageService.getTourPackagesByDestination(destination);
        return ResponseEntity.ok(tourPackages);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<TourPackageEntity>> getTourPackagesByStatus(@PathVariable String status) {
        List<TourPackageEntity> tourPackages = tourPackageService.getTourPackagesByStatus(status);
        return ResponseEntity.ok(tourPackages);
    }

    @GetMapping("/price")
    public ResponseEntity<List<TourPackageEntity>> getTourPackagesByPriceRange(
            @RequestParam Double minPrice,
            @RequestParam Double maxPrice) {
        List<TourPackageEntity> tourPackages = tourPackageService.getTourPackagesByPriceRange(minPrice, maxPrice);
        return ResponseEntity.ok(tourPackages);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchAvailableTourPackages(
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate travelDate,
            @RequestParam(required = false) String travelType,
            @RequestParam(required = false) String season,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer minDuration,
            @RequestParam(required = false) Integer maxDuration) {
        try {
            List<TourPackageEntity> tourPackages = tourPackageService.searchAvailableTourPackages(
                    destination,
                    minPrice,
                    maxPrice,
                    travelDate,
                    travelType,
                    season,
                    category,
                    minDuration,
                    maxDuration
            );
            return ResponseEntity.ok(tourPackages);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/")
    public ResponseEntity<?> saveTourPackage(@RequestBody TourPackageEntity tourPackage) {
        try {
            TourPackageEntity newTourPackage = tourPackageService.saveTourPackage(tourPackage);
            return ResponseEntity.status(HttpStatus.CREATED).body(newTourPackage);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTourPackage(@PathVariable Long id, @RequestBody TourPackageEntity tourPackage) {
        try {
            TourPackageEntity updatedTourPackage = tourPackageService.updateTourPackage(id, tourPackage);
            return ResponseEntity.ok(updatedTourPackage);
        } catch (RuntimeException e) {
            if (e.getMessage().equals("Paquete turístico no encontrado")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivateTourPackage(@PathVariable Long id) {
        try {
            TourPackageEntity deactivatedTourPackage = tourPackageService.deactivateTourPackage(id);
            return ResponseEntity.ok(deactivatedTourPackage);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}