package com.driveguard.rule_engine.service;

import com.driveguard.rule_engine.AppLogger;
import com.driveguard.rule_engine.Mapper;
import com.driveguard.rule_engine.dto.Alert;
import com.driveguard.rule_engine.dto.CarResponseDTO;
import com.driveguard.rule_engine.entity.TripAlerts;
import com.driveguard.rule_engine.exception.NoAlertsFoundException;
import com.driveguard.rule_engine.repository.TripAlertsRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class AlertPersistenceService {

    private final TripAlertsRepository repository;

    private final AlertCacheService alertCacheService;

    private final CarCacheService carCacheService;

    private final Mapper mapper;

    private AlertPersistenceService self;

    private static final AppLogger log = AppLogger.getLogger(AlertPersistenceService.class);

    // Constructor Injection
    public AlertPersistenceService(TripAlertsRepository repository, Mapper mapper, AlertCacheService alertCacheService, CarCacheService carCacheService) {
        this.carCacheService = carCacheService;
        this.alertCacheService = alertCacheService;
        this.repository = repository;
        this.mapper = mapper;
    }

    @PostConstruct
    public void init() {
        this.self = this;
    }

    @Transactional
    public void saveAlertBatch(List<TripAlerts> alerts) {
        if (!alerts.isEmpty()) {
            // This triggers the MySQL batching (rewriteBatchedStatements=true)
            repository.saveAll(alerts);
        }
    }

    @Transactional(readOnly = true)
    public Page<Alert> getAlertsByTripNumber(String tripNumber, Pageable pageable) {
        // 1. Check Redis (Pass 2 args)
        Page<Alert> cachedPage = alertCacheService.getCachedPage(tripNumber, pageable);
        if (cachedPage != null) {
            return cachedPage;
        }

        // 2. Database Fallback
        Page<TripAlerts> tripAlerts = repository.findByTripNumber(tripNumber, pageable);
        if (tripAlerts.isEmpty()) {
            throw new NoAlertsFoundException("NO ALERTS FOUND FOR TRIP: " + tripNumber);
        }

        Page<Alert> resultPage = tripAlerts.map(mapper::mapToDTO);

        // 3. Save to Redis (Pass 3 args)
        alertCacheService.cacheAlertPage(tripNumber, pageable, resultPage);

        return resultPage;
    }

    @Scheduled(fixedRate = 60000)
    public void refreshActiveTripCaches() {
        // Get all trips that are currently active
        Set<String> activeTrips = alertCacheService.getAllActiveTrips();

        if (activeTrips == null || activeTrips.isEmpty()) return;

        log.info(String.format("Cron: Warming cache for %s active trips", activeTrips.size()));

        // We typically refresh Page 0 (the most viewed page)
        Pageable firstPage = PageRequest.of(0, 20);

        for (String tripNumber : activeTrips) {
            try {
                // Call through self-reference to properly handle @Transactional
                if (self != null) {
                    self.getAlertsByTripNumber(tripNumber, firstPage);
                } else {
                    log.info(String.format("Self-reference is null while refreshing cache for trip: %s, Unable to refresh keys for active trips.", tripNumber));
                }
            } catch (NoAlertsFoundException ignored) {
                // It's okay if an active trip doesn't have alerts yet
            }
        }
    }

    public List<CarResponseDTO> activeCarsWithTrips(){
        Set<String> activeTrips = alertCacheService.getAllActiveTrips();
        List<CarResponseDTO> carResponseDTOList = new ArrayList<>();

        for(String tripNumber : activeTrips){
            CarResponseDTO car = carCacheService.getCarByTripNumberWithCache(tripNumber);
            if(car != null && car.getIsActiveTrip() && car.getActiveTripNumber() != null && car.getActiveTripNumber().equals(tripNumber)){
                carResponseDTOList.add(car);
            } else {
                log.warn(String.format("No car found for active trip: %s", tripNumber));
            }
        }
        return carResponseDTOList;
    }

    /**
     * Get active cars with pagination support.
     * Retrieves all active cars, applies sorting, and then applies pagination to the result set.
     *
     * @param pageable Pagination parameters (page, size, sort)
     * @return Page of CarResponseDTO with pagination metadata
     */
    @Transactional(readOnly = true)
    public Page<CarResponseDTO> activeCarsWithTrips(Pageable pageable) {
        // Step 1: Get all active cars
        List<CarResponseDTO> allActiveCars = activeCarsWithTrips();

        // Step 2: Apply sorting if requested
        if (pageable.getSort().isSorted()) {
            allActiveCars = sortCars(allActiveCars, pageable.getSort());
        }

        // Step 3: Apply pagination to the sorted list
        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();
        int fromIndex = pageNumber * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, allActiveCars.size());

        // Handle edge case where page number is beyond available data
        if (fromIndex >= allActiveCars.size()) {
            return new PageImpl<>(
                    new java.util.ArrayList<>(),
                    pageable,
                    allActiveCars.size()
            );
        }

        // Extract the page slice
        List<CarResponseDTO> pageContent = allActiveCars.subList(fromIndex, toIndex);

        log.info(String.format("Retrieved page %d with %d active cars out of %d total",
                pageNumber, pageContent.size(), allActiveCars.size()));

        // Step 4: Return as Page
        return new PageImpl<>(
                pageContent,
                pageable,
                allActiveCars.size()
        );
    }

    /**
     * Sort cars based on Sort parameters.
     * Supports sorting by: cnr, manufacturer, model, fuel, powerKw, transmission, weightKg, isActiveTrip
     *
     * @param cars List of cars to sort
     * @param sort Sort parameters
     * @return Sorted list of cars
     */
    private List<CarResponseDTO> sortCars(List<CarResponseDTO> cars, Sort sort) {
        return cars.stream()
                .sorted((car1, car2) -> {
                    for (Sort.Order order : sort) {
                        int comparison = compareByField(car1, car2, order.getProperty());
                        if (comparison != 0) {
                            return order.isAscending() ? comparison : -comparison;
                        }
                    }
                    return 0;
                })
                .toList();
    }

    /**
     * Compare two cars by a specific field.
     *
     * @param car1 First car
     * @param car2 Second car
     * @param field Field to compare
     * @return Comparison result (-1, 0, 1)
     */
    private int compareByField(CarResponseDTO car1, CarResponseDTO car2, String field) {
        return switch (field) {
            case "cnr" -> car1.getCnr().compareTo(car2.getCnr());
            case "manufacturer" -> car1.getManufacturer().compareTo(car2.getManufacturer());
            case "model" -> car1.getModel().compareTo(car2.getModel());
            case "fuel" -> car1.getFuel().compareTo(car2.getFuel());
            case "powerKw" -> car1.getPowerKw().compareTo(car2.getPowerKw());
            case "transmission" -> car1.getTransmission().compareTo(car2.getTransmission());
            case "weightKg" -> car1.getWeightKg().compareTo(car2.getWeightKg());
            case "isActiveTrip" -> car1.getIsActiveTrip().compareTo(car2.getIsActiveTrip());
            default -> {
                log.warn(String.format("Unknown sort field: %s, defaulting to cnr", field));
                yield car1.getCnr().compareTo(car2.getCnr());
            }
        };
    }
}
