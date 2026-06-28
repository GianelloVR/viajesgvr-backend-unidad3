package com.example.viajesgvr.services;

import com.example.viajesgvr.entities.TourPackageEntity;
import com.example.viajesgvr.repositories.TourPackageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TourPackageServiceTest {

    @Mock
    private TourPackageRepository tourPackageRepository;

    @InjectMocks
    private TourPackageService tourPackageService;

    @Test
    void saveTourPackageShouldSetDefaultStatusAndAvailableQuota() {
        TourPackageEntity tourPackage = validTourPackage();
        tourPackage.setStatus(null);
        tourPackage.setAvailableQuota(null);

        when(tourPackageRepository.save(any(TourPackageEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TourPackageEntity result = tourPackageService.saveTourPackage(tourPackage);

        assertEquals(TourPackageEntity.STATUS_AVAILABLE, result.getStatus());
        assertEquals(20, result.getAvailableQuota());

        verify(tourPackageRepository).save(tourPackage);
    }

    @Test
    void saveTourPackageShouldThrowExceptionWhenNameIsMissing() {
        TourPackageEntity tourPackage = validTourPackage();
        tourPackage.setName(" ");

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tourPackageService.saveTourPackage(tourPackage));

        assertEquals("El nombre es obligatorio", exception.getMessage());

        verify(tourPackageRepository, never()).save(any(TourPackageEntity.class));
    }

    @Test
    void saveTourPackageShouldThrowExceptionWhenEndDateIsNotAfterStartDate() {
        TourPackageEntity tourPackage = validTourPackage();
        tourPackage.setStartDate(LocalDate.now().plusDays(10));
        tourPackage.setEndDate(LocalDate.now().plusDays(5));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tourPackageService.saveTourPackage(tourPackage));

        assertEquals("La fecha de término de vigencia debe ser posterior a la fecha de inicio de vigencia",
                exception.getMessage());

        verify(tourPackageRepository, never()).save(any(TourPackageEntity.class));
    }

    @Test
    void saveTourPackageShouldThrowExceptionWhenPriceIsInvalid() {
        TourPackageEntity tourPackage = validTourPackage();
        tourPackage.setPrice(0.0);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tourPackageService.saveTourPackage(tourPackage));

        assertEquals("El precio debe ser mayor que cero", exception.getMessage());

        verify(tourPackageRepository, never()).save(any(TourPackageEntity.class));
    }

    @Test
    void saveTourPackageShouldThrowExceptionWhenAvailableQuotaIsGreaterThanTotalQuota() {
        TourPackageEntity tourPackage = validTourPackage();
        tourPackage.setTotalQuota(10);
        tourPackage.setAvailableQuota(15);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tourPackageService.saveTourPackage(tourPackage));

        assertEquals("Los cupos disponibles no pueden ser mayores que los cupos totales",
                exception.getMessage());

        verify(tourPackageRepository, never()).save(any(TourPackageEntity.class));
    }

    @Test
    void searchAvailableTourPackagesShouldCallRepositoryWithFilters() {
        LocalDate travelDate = LocalDate.now().plusDays(20);
        TourPackageEntity tourPackage = validTourPackage();

        when(tourPackageRepository.searchAvailableTourPackages(
                "Santiago",
                100000.0,
                300000.0,
                travelDate,
                "Aventura",
                "Verano",
                "Nacional",
                2,
                7
        )).thenReturn(List.of(tourPackage));

        List<TourPackageEntity> result = tourPackageService.searchAvailableTourPackages(
                "Santiago",
                100000.0,
                300000.0,
                travelDate,
                "Aventura",
                "Verano",
                "Nacional",
                2,
                7
        );

        assertEquals(1, result.size());
        assertEquals("Santiago", result.get(0).getDestination());

        verify(tourPackageRepository).searchAvailableTourPackages(
                "Santiago",
                100000.0,
                300000.0,
                travelDate,
                "Aventura",
                "Verano",
                "Nacional",
                2,
                7
        );
    }

    @Test
    void searchAvailableTourPackagesShouldThrowExceptionWhenMinPriceIsGreaterThanMaxPrice() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tourPackageService.searchAvailableTourPackages(
                        null,
                        500000.0,
                        100000.0,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ));

        assertEquals("El precio mínimo no puede ser mayor que el precio máximo",
                exception.getMessage());

        verify(tourPackageRepository, never()).searchAvailableTourPackages(
                any(), any(), any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void searchAvailableTourPackagesShouldThrowExceptionWhenMinDurationIsGreaterThanMaxDuration() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tourPackageService.searchAvailableTourPackages(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        10,
                        5
                ));

        assertEquals("La duración mínima no puede ser mayor que la duración máxima",
                exception.getMessage());

        verify(tourPackageRepository, never()).searchAvailableTourPackages(
                any(), any(), any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void updateTourPackageShouldCopyFieldsAndSetDefaultStatusWhenMissing() {
        TourPackageEntity existingTourPackage = validTourPackage();
        existingTourPackage.setId(1L);
        existingTourPackage.setName("Paquete antiguo");

        TourPackageEntity updatedData = validTourPackage();
        updatedData.setName("Paquete actualizado");
        updatedData.setDestination("Valparaíso");
        updatedData.setStatus(" ");

        when(tourPackageRepository.findById(1L)).thenReturn(Optional.of(existingTourPackage));
        when(tourPackageRepository.save(any(TourPackageEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TourPackageEntity result = tourPackageService.updateTourPackage(1L, updatedData);

        assertEquals("Paquete actualizado", result.getName());
        assertEquals("Valparaíso", result.getDestination());
        assertEquals(TourPackageEntity.STATUS_AVAILABLE, result.getStatus());

        verify(tourPackageRepository).findById(1L);
        verify(tourPackageRepository).save(existingTourPackage);
    }

    @Test
    void deactivateTourPackageShouldSetStatusNotValid() {
        TourPackageEntity existingTourPackage = validTourPackage();
        existingTourPackage.setId(1L);
        existingTourPackage.setStatus(TourPackageEntity.STATUS_AVAILABLE);

        when(tourPackageRepository.findById(1L)).thenReturn(Optional.of(existingTourPackage));
        when(tourPackageRepository.save(any(TourPackageEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TourPackageEntity result = tourPackageService.deactivateTourPackage(1L);

        assertEquals(TourPackageEntity.STATUS_NOT_VALID, result.getStatus());

        verify(tourPackageRepository).findById(1L);
        verify(tourPackageRepository).save(existingTourPackage);
    }

    @Test
    void saveTourPackageShouldThrowExceptionWhenDestinationIsMissing() {
        TourPackageEntity tourPackage = validTourPackage();
        tourPackage.setDestination(" ");

        assertThrows(RuntimeException.class,
                () -> tourPackageService.saveTourPackage(tourPackage));

        verify(tourPackageRepository, never()).save(any(TourPackageEntity.class));
    }

    @Test
    void saveTourPackageShouldThrowExceptionWhenDescriptionIsMissing() {
        TourPackageEntity tourPackage = validTourPackage();
        tourPackage.setDescription(" ");

        assertThrows(RuntimeException.class,
                () -> tourPackageService.saveTourPackage(tourPackage));

        verify(tourPackageRepository, never()).save(any(TourPackageEntity.class));
    }

    @Test
    void saveTourPackageShouldThrowExceptionWhenStartDateIsMissing() {
        TourPackageEntity tourPackage = validTourPackage();
        tourPackage.setStartDate(null);

        assertThrows(RuntimeException.class,
                () -> tourPackageService.saveTourPackage(tourPackage));

        verify(tourPackageRepository, never()).save(any(TourPackageEntity.class));
    }

    @Test
    void saveTourPackageShouldThrowExceptionWhenEndDateIsMissing() {
        TourPackageEntity tourPackage = validTourPackage();
        tourPackage.setEndDate(null);

        assertThrows(RuntimeException.class,
                () -> tourPackageService.saveTourPackage(tourPackage));

        verify(tourPackageRepository, never()).save(any(TourPackageEntity.class));
    }

    @Test
    void saveTourPackageShouldThrowExceptionWhenDurationIsInvalid() {
        TourPackageEntity tourPackage = validTourPackage();
        tourPackage.setDurationDays(0);

        assertThrows(RuntimeException.class,
                () -> tourPackageService.saveTourPackage(tourPackage));

        verify(tourPackageRepository, never()).save(any(TourPackageEntity.class));
    }

    @Test
    void saveTourPackageShouldThrowExceptionWhenTotalQuotaIsInvalid() {
        TourPackageEntity tourPackage = validTourPackage();
        tourPackage.setTotalQuota(0);

        assertThrows(RuntimeException.class,
                () -> tourPackageService.saveTourPackage(tourPackage));

        verify(tourPackageRepository, never()).save(any(TourPackageEntity.class));
    }

    @Test
    void saveTourPackageShouldThrowExceptionWhenAvailableQuotaIsNegative() {
        TourPackageEntity tourPackage = validTourPackage();
        tourPackage.setAvailableQuota(-1);

        assertThrows(RuntimeException.class,
                () -> tourPackageService.saveTourPackage(tourPackage));

        verify(tourPackageRepository, never()).save(any(TourPackageEntity.class));
    }

    @Test
    void updateTourPackageShouldThrowExceptionWhenTourPackageDoesNotExist() {
        TourPackageEntity updatedData = validTourPackage();

        when(tourPackageRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> tourPackageService.updateTourPackage(99L, updatedData));

        verify(tourPackageRepository).findById(99L);
        verify(tourPackageRepository, never()).save(any(TourPackageEntity.class));
    }

    @Test
    void deactivateTourPackageShouldThrowExceptionWhenTourPackageDoesNotExist() {
        when(tourPackageRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> tourPackageService.deactivateTourPackage(99L));

        verify(tourPackageRepository).findById(99L);
        verify(tourPackageRepository, never()).save(any(TourPackageEntity.class));
    }

    @Test
    void saveTourPackageShouldThrowExceptionWhenAvailableStatusHasNoAvailableQuota() {
        TourPackageEntity tourPackage = validTourPackage();
        tourPackage.setStatus(TourPackageEntity.STATUS_AVAILABLE);
        tourPackage.setAvailableQuota(0);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tourPackageService.saveTourPackage(tourPackage));

        assertEquals("Un paquete disponible debe tener cupos disponibles", exception.getMessage());

        verify(tourPackageRepository, never()).save(any(TourPackageEntity.class));
    }

    @Test
    void updateTourPackageShouldThrowExceptionWhenAvailableStatusHasNoAvailableQuota() {
        TourPackageEntity existingTourPackage = validTourPackage();
        existingTourPackage.setId(1L);

        TourPackageEntity updatedData = validTourPackage();
        updatedData.setStatus(TourPackageEntity.STATUS_AVAILABLE);
        updatedData.setAvailableQuota(0);

        when(tourPackageRepository.findById(1L)).thenReturn(Optional.of(existingTourPackage));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tourPackageService.updateTourPackage(1L, updatedData));

        assertEquals("Un paquete disponible debe tener cupos disponibles", exception.getMessage());

        verify(tourPackageRepository).findById(1L);
        verify(tourPackageRepository, never()).save(any(TourPackageEntity.class));
    }

    @Test
    void saveTourPackageShouldAllowSoldOutPackageWithoutAvailableQuota() {
        TourPackageEntity tourPackage = validTourPackage();
        tourPackage.setStatus(TourPackageEntity.STATUS_SOLD_OUT);
        tourPackage.setAvailableQuota(0);

        when(tourPackageRepository.save(any(TourPackageEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TourPackageEntity result = tourPackageService.saveTourPackage(tourPackage);

        assertEquals(TourPackageEntity.STATUS_SOLD_OUT, result.getStatus());
        assertEquals(0, result.getAvailableQuota());

        verify(tourPackageRepository).save(tourPackage);
    }

    @Test
    void getAllTourPackagesShouldReturnRepositoryResult() {
        TourPackageEntity tourPackage = validTourPackage();

        when(tourPackageRepository.findAll()).thenReturn(List.of(tourPackage));

        List<TourPackageEntity> result = tourPackageService.getAllTourPackages();

        assertEquals(1, result.size());
        assertEquals("Paquete Santiago", result.get(0).getName());

        verify(tourPackageRepository).findAll();
    }

    @Test
    void getTourPackageByIdShouldReturnRepositoryResult() {
        TourPackageEntity tourPackage = validTourPackage();
        tourPackage.setId(1L);

        when(tourPackageRepository.findById(1L)).thenReturn(Optional.of(tourPackage));

        Optional<TourPackageEntity> result = tourPackageService.getTourPackageById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());

        verify(tourPackageRepository).findById(1L);
    }

    @Test
    void getTourPackagesByDestinationShouldReturnRepositoryResult() {
        TourPackageEntity tourPackage = validTourPackage();

        when(tourPackageRepository.findByDestination("Santiago")).thenReturn(List.of(tourPackage));

        List<TourPackageEntity> result = tourPackageService.getTourPackagesByDestination("Santiago");

        assertEquals(1, result.size());
        assertEquals("Santiago", result.get(0).getDestination());

        verify(tourPackageRepository).findByDestination("Santiago");
    }

    @Test
    void getTourPackagesByStatusShouldReturnRepositoryResult() {
        TourPackageEntity tourPackage = validTourPackage();

        when(tourPackageRepository.findByStatus(TourPackageEntity.STATUS_AVAILABLE))
                .thenReturn(List.of(tourPackage));

        List<TourPackageEntity> result = tourPackageService.getTourPackagesByStatus(
                TourPackageEntity.STATUS_AVAILABLE
        );

        assertEquals(1, result.size());
        assertEquals(TourPackageEntity.STATUS_AVAILABLE, result.get(0).getStatus());

        verify(tourPackageRepository).findByStatus(TourPackageEntity.STATUS_AVAILABLE);
    }

    @Test
    void getTourPackagesByPriceRangeShouldReturnRepositoryResult() {
        TourPackageEntity tourPackage = validTourPackage();

        when(tourPackageRepository.findByPriceBetween(100000.0, 300000.0))
                .thenReturn(List.of(tourPackage));

        List<TourPackageEntity> result = tourPackageService.getTourPackagesByPriceRange(
                100000.0,
                300000.0
        );

        assertEquals(1, result.size());
        assertEquals(250000.0, result.get(0).getPrice());

        verify(tourPackageRepository).findByPriceBetween(100000.0, 300000.0);
    }

    @Test
    void searchAvailableTourPackagesShouldThrowExceptionWhenMinPriceIsNegative() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tourPackageService.searchAvailableTourPackages(
                        null,
                        -1.0,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ));

        assertEquals("El precio mínimo no puede ser negativo", exception.getMessage());

        verify(tourPackageRepository, never()).searchAvailableTourPackages(
                any(), any(), any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void saveTourPackageShouldThrowExceptionWhenStatusIsInvalid() {
        TourPackageEntity tourPackage = validTourPackage();
        tourPackage.setStatus("INVALID_STATUS");

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tourPackageService.saveTourPackage(tourPackage));

        assertEquals("El estado del paquete turístico no es válido", exception.getMessage());

        verify(tourPackageRepository, never()).save(any(TourPackageEntity.class));
    }

    @Test
    void saveTourPackageShouldThrowExceptionWhenPromotionIsInvalid() {
        TourPackageEntity tourPackage = validTourPackage();
        tourPackage.setPromotionDiscountPercentage(25);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tourPackageService.saveTourPackage(tourPackage));

        assertEquals("La promoción debe estar entre 0 y 20", exception.getMessage());

        verify(tourPackageRepository, never()).save(any(TourPackageEntity.class));
    }

    @Test
    void updateTourPackageShouldKeepExistingQuotaAndPromotionWhenMissing() {
        TourPackageEntity existingTourPackage = validTourPackage();
        existingTourPackage.setId(1L);
        existingTourPackage.setAvailableQuota(7);
        existingTourPackage.setPromotionDiscountPercentage(12);

        TourPackageEntity updatedData = validTourPackage();
        updatedData.setAvailableQuota(null);
        updatedData.setPromotionDiscountPercentage(null);

        when(tourPackageRepository.findById(1L)).thenReturn(Optional.of(existingTourPackage));
        when(tourPackageRepository.save(any(TourPackageEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TourPackageEntity result = tourPackageService.updateTourPackage(1L, updatedData);

        assertEquals(7, result.getAvailableQuota());
        assertEquals(12, result.getPromotionDiscountPercentage());

        verify(tourPackageRepository).findById(1L);
        verify(tourPackageRepository).save(existingTourPackage);
    }

    private TourPackageEntity validTourPackage() {
        TourPackageEntity tourPackage = new TourPackageEntity();

        tourPackage.setName("Paquete Santiago");
        tourPackage.setDestination("Santiago");
        tourPackage.setDescription("Descripción del paquete turístico");
        tourPackage.setStartDate(LocalDate.now().plusDays(10));
        tourPackage.setEndDate(LocalDate.now().plusDays(20));
        tourPackage.setDurationDays(5);
        tourPackage.setPrice(250000.0);
        tourPackage.setTotalQuota(20);
        tourPackage.setAvailableQuota(20);
        tourPackage.setIncludedServices("Hotel y desayuno");
        tourPackage.setConditions("Condiciones generales");
        tourPackage.setRestrictions("Sin restricciones especiales");
        tourPackage.setTravelType("Aventura");
        tourPackage.setSeason("Verano");
        tourPackage.setCategory("Nacional");
        tourPackage.setStatus(TourPackageEntity.STATUS_AVAILABLE);

        return tourPackage;
    }
}