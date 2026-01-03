package com.driveGuard.dataProducer.service;

import com.driveGuard.dataProducer.dto.TripDTO;
import com.driveGuard.dataProducer.entity.Trip;
import com.driveGuard.dataProducer.exception.TripNotFoundException;
import com.driveGuard.dataProducer.repository.TripRepository;
import com.driveGuard.dataProducer.utility.Mapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TripService {
    private final TripRepository tripRepository;


    private final Mapper mapper;

    private static final String TRIP_NOT_FOUND_MESSAGE = "TRIP WITH ID : %s NOT FOUND.";

    TripService(TripRepository tripRepository, Mapper mapper) {
        this.tripRepository = tripRepository;
        this.mapper = mapper;
    }

    public TripDTO addTrip(Trip trip) {
        return mapper.tripTOTripDTO(tripRepository.save(trip));
    }

    public TripDTO getTripByTripNumber(String tripNumber) {
        Optional<Trip> tripOptional = tripRepository.getTripByTripNumber(tripNumber);
        if (tripOptional.isEmpty()) {
            throw new TripNotFoundException(String.format(TRIP_NOT_FOUND_MESSAGE, tripNumber));
        }
        return mapper.tripTOTripDTO(tripOptional.get());
    }

    public TripDTO updateTripEndTime(String tripNumber) {
        Optional<Trip> tripOptional = tripRepository.getTripByTripNumber(tripNumber);
        if (tripOptional.isEmpty()) {
            throw new TripNotFoundException(String.format(TRIP_NOT_FOUND_MESSAGE, tripNumber));
        }
        Trip trip = tripOptional.get();
        trip.setEndTime(Instant.now().toString());
        return mapper.tripTOTripDTO(tripRepository.save(trip));
    }

    public void updateTripStartLocation(String tripNumber, String startLocationLatitude, String startLocationLongitude) {
        Optional<Trip> tripOptional = tripRepository.getTripByTripNumber(tripNumber);
        if (tripOptional.isEmpty()) {
            throw new TripNotFoundException(String.format(TRIP_NOT_FOUND_MESSAGE, tripNumber));
        }
        Trip trip = tripOptional.get();
        trip.setStartLocationLatitude(startLocationLatitude);
        trip.setStartLocationLongitude(startLocationLongitude);
        tripRepository.save(trip);
    }

    public void updateTripEndLocationAndDistance(String tripNumber, String endLocationLatitude, String endLocationLongitude, Double totalDistanceKm) {
        Optional<Trip> tripOptional = tripRepository.getTripByTripNumber(tripNumber);
        if (tripOptional.isEmpty()) {
            throw new TripNotFoundException(String.format(TRIP_NOT_FOUND_MESSAGE, tripNumber));
        }
        Trip trip = tripOptional.get();
        trip.setEndLocationLatitude(endLocationLatitude);
        trip.setEndLocationLongitude(endLocationLongitude);
        trip.setTotalDistanceKm(totalDistanceKm);
        trip.setEndTime(Instant.now().toString());
        tripRepository.save(trip);
    }

    @Transactional(readOnly = true)
    public Page<TripDTO> getTripsByCarId(Integer carId, Pageable pageable) {
        // 1. Fetch paged results directly
        Page<Trip> tripPage = tripRepository.findByCarCnr(carId, pageable);

        // 2. Map entities to DTOs
        List<TripDTO> tripDTOList = mapper.tripListToTripDTOList(tripPage.getContent());

        if (tripDTOList.isEmpty()) {
            throw new TripNotFoundException("NO MORE TRIPS FOUND.");
        }

        return tripPage.map(mapper::tripTOTripDTO);
    }
}
