package com.example.viajesgvr.services;

import com.example.viajesgvr.entities.TourPackageEntity;
import com.example.viajesgvr.repositories.TourPackageRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class TourPackageService {

    private final TourPackageRepository tourPackageRepository;

    public TourPackageService(TourPackageRepository tourPackageRepository) {
        this.tourPackageRepository = tourPackageRepository;
    }

    public List<TourPackageEntity> getAllTourPackages() {
        return tourPackageRepository.findAll();
    }

    public Optional<TourPackageEntity> getTourPackageById(Long id) {
        return tourPackageRepository.findById(id);
    }

    public List<TourPackageEntity> getTourPackagesByDestination(String destination) {
        return tourPackageRepository.findByDestination(destination);
    }

    public List<TourPackageEntity> getTourPackagesByStatus(String status) {
        return tourPackageRepository.findByStatus(status);
    }

    public List<TourPackageEntity> getTourPackagesByPriceRange(Double minPrice, Double maxPrice) {
        return tourPackageRepository.findByPriceBetween(minPrice, maxPrice);
    }

    public List<TourPackageEntity> searchAvailableTourPackages(
            String destination,
            Double minPrice,
            Double maxPrice,
            LocalDate travelDate,
            String travelType,
            String season,
            String category,
            Integer minDuration,
            Integer maxDuration) {

        if (minPrice != null && minPrice < 0) {
            throw new RuntimeException("El precio mínimo no puede ser negativo");
        }

        if (maxPrice != null && maxPrice < 0) {
            throw new RuntimeException("El precio máximo no puede ser negativo");
        }

        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new RuntimeException("El precio mínimo no puede ser mayor que el precio máximo");
        }

        if (minDuration != null && minDuration <= 0) {
            throw new RuntimeException("La duración mínima debe ser mayor que cero");
        }

        if (maxDuration != null && maxDuration <= 0) {
            throw new RuntimeException("La duración máxima debe ser mayor que cero");
        }

        if (minDuration != null && maxDuration != null && minDuration > maxDuration) {
            throw new RuntimeException("La duración mínima no puede ser mayor que la duración máxima");
        }

        return tourPackageRepository.searchAvailableTourPackages(
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
    }

    public TourPackageEntity saveTourPackage(TourPackageEntity tourPackage) {
        applyDefaultValuesForNewPackage(tourPackage);

        validateTourPackageData(tourPackage);
        validateControlledStatus(tourPackage.getStatus());
        validateAvailablePackageHasQuota(tourPackage.getStatus(), tourPackage.getAvailableQuota());

        return tourPackageRepository.save(tourPackage);
    }

    public TourPackageEntity updateTourPackage(Long id, TourPackageEntity tourPackage) {
        TourPackageEntity existingTourPackage = tourPackageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Paquete turístico no encontrado"));

        String statusToApply = getStatusOrDefault(tourPackage.getStatus());

        if (tourPackage.getAvailableQuota() == null) {
            tourPackage.setAvailableQuota(existingTourPackage.getAvailableQuota());
        }

        if (tourPackage.getPromotionDiscountPercentage() == null) {
            tourPackage.setPromotionDiscountPercentage(
                    existingTourPackage.getPromotionDiscountPercentage() != null
                            ? existingTourPackage.getPromotionDiscountPercentage()
                            : 0
            );
        }

        validateTourPackageData(tourPackage);
        validateControlledStatus(statusToApply);
        validateAvailablePackageHasQuota(statusToApply, tourPackage.getAvailableQuota());

        existingTourPackage.setName(tourPackage.getName());
        existingTourPackage.setDestination(tourPackage.getDestination());
        existingTourPackage.setDescription(tourPackage.getDescription());
        existingTourPackage.setStartDate(tourPackage.getStartDate());
        existingTourPackage.setEndDate(tourPackage.getEndDate());
        existingTourPackage.setDurationDays(tourPackage.getDurationDays());
        existingTourPackage.setPrice(tourPackage.getPrice());
        existingTourPackage.setTotalQuota(tourPackage.getTotalQuota());
        existingTourPackage.setAvailableQuota(tourPackage.getAvailableQuota());
        existingTourPackage.setIncludedServices(tourPackage.getIncludedServices());
        existingTourPackage.setConditions(tourPackage.getConditions());
        existingTourPackage.setRestrictions(tourPackage.getRestrictions());
        existingTourPackage.setTravelType(tourPackage.getTravelType());
        existingTourPackage.setSeason(tourPackage.getSeason());
        existingTourPackage.setCategory(tourPackage.getCategory());
        existingTourPackage.setPromotionDiscountPercentage(tourPackage.getPromotionDiscountPercentage());
        existingTourPackage.setStatus(statusToApply);

        return tourPackageRepository.save(existingTourPackage);
    }

    public TourPackageEntity deactivateTourPackage(Long id) {
        TourPackageEntity existingTourPackage = tourPackageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Paquete turístico no encontrado"));

        existingTourPackage.setStatus(TourPackageEntity.STATUS_NOT_VALID);

        return tourPackageRepository.save(existingTourPackage);
    }

    private void applyDefaultValuesForNewPackage(TourPackageEntity tourPackage) {
        tourPackage.setStatus(getStatusOrDefault(tourPackage.getStatus()));

        if (tourPackage.getAvailableQuota() == null) {
            tourPackage.setAvailableQuota(tourPackage.getTotalQuota());
        }

        if (tourPackage.getPromotionDiscountPercentage() == null) {
            tourPackage.setPromotionDiscountPercentage(0);
        }
    }

    private String getStatusOrDefault(String status) {
        if (status == null || status.isBlank()) {
            return TourPackageEntity.STATUS_AVAILABLE;
        }

        return status;
    }

    private void validateTourPackageData(TourPackageEntity tourPackage) {
        if (tourPackage.getName() == null || tourPackage.getName().isBlank()) {
            throw new RuntimeException("El nombre es obligatorio");
        }

        if (tourPackage.getDestination() == null || tourPackage.getDestination().isBlank()) {
            throw new RuntimeException("El destino es obligatorio");
        }

        if (tourPackage.getDescription() == null || tourPackage.getDescription().isBlank()) {
            throw new RuntimeException("La descripción es obligatoria");
        }

        if (tourPackage.getStartDate() == null) {
            throw new RuntimeException("La fecha de inicio de vigencia es obligatoria");
        }

        if (tourPackage.getEndDate() == null) {
            throw new RuntimeException("La fecha de término de vigencia es obligatoria");
        }

        if (!tourPackage.getEndDate().isAfter(tourPackage.getStartDate())) {
            throw new RuntimeException("La fecha de término de vigencia debe ser posterior a la fecha de inicio de vigencia");
        }

        if (tourPackage.getDurationDays() == null || tourPackage.getDurationDays() <= 0) {
            throw new RuntimeException("La duración del paquete debe ser mayor que cero");
        }

        if (tourPackage.getPrice() == null || tourPackage.getPrice() <= 0) {
            throw new RuntimeException("El precio debe ser mayor que cero");
        }

        if (tourPackage.getTotalQuota() == null || tourPackage.getTotalQuota() <= 0) {
            throw new RuntimeException("Los cupos totales deben ser mayores que cero");
        }

        if (tourPackage.getAvailableQuota() == null) {
            throw new RuntimeException("Los cupos disponibles son obligatorios");
        }

        if (tourPackage.getAvailableQuota() < 0) {
            throw new RuntimeException("Los cupos disponibles no pueden ser negativos");
        }

        if (tourPackage.getAvailableQuota() > tourPackage.getTotalQuota()) {
            throw new RuntimeException("Los cupos disponibles no pueden ser mayores que los cupos totales");
        }

        if (tourPackage.getPromotionDiscountPercentage() == null) {
            throw new RuntimeException("La promoción es obligatoria");
        }

        if (tourPackage.getPromotionDiscountPercentage() < 0 || tourPackage.getPromotionDiscountPercentage() > 20) {
            throw new RuntimeException("La promoción debe estar entre 0 y 20");
        }

    }

    private void validateControlledStatus(String status) {
        if (!TourPackageEntity.STATUS_AVAILABLE.equals(status)
                && !TourPackageEntity.STATUS_SOLD_OUT.equals(status)
                && !TourPackageEntity.STATUS_NOT_VALID.equals(status)
                && !TourPackageEntity.STATUS_CANCELED.equals(status)) {
            throw new RuntimeException("El estado del paquete turístico no es válido");
        }
    }

    private void validateAvailablePackageHasQuota(String status, Integer availableQuota) {
        if (TourPackageEntity.STATUS_AVAILABLE.equals(status)
                && (availableQuota == null || availableQuota <= 0)) {
            throw new RuntimeException("Un paquete disponible debe tener cupos disponibles");
        }
    }
}