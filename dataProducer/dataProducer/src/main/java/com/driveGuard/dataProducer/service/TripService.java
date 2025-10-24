package com.driveGuard.dataProducer.service;

import com.driveGuard.dataProducer.dto.TripDTO;
import com.driveGuard.dataProducer.entity.Trip;
import com.driveGuard.dataProducer.exception.TripNotFoundException;
import com.driveGuard.dataProducer.repository.TripRepository;
import com.driveGuard.dataProducer.utility.Mapper;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@Transactional
public class TripService
{
    private final TripRepository tripRepository;

    private final Mapper mapper;

    TripService(TripRepository tripRepository,Mapper mapper){
        this.tripRepository = tripRepository;
        this.mapper = mapper;
    }

    public TripDTO addTrip(Trip trip){
        return mapper.tripTOTripDTO(tripRepository.save(trip));
    }

    public TripDTO getTripByTripNumber(String tripNumber){
        Optional<Trip> tripOptional = tripRepository.getTripByTripNumber(tripNumber);
        if(tripOptional.isEmpty()){
            throw new TripNotFoundException("TRIP WITH ID : " + tripNumber + " NOT FOUND.");
        }
        return mapper.tripTOTripDTO(tripOptional.get());
    }

    public TripDTO updateTripEndTime(String tripNumber){
        Optional<Trip> tripOptional = tripRepository.getTripByTripNumber(tripNumber);
        if(tripOptional.isEmpty()){
            throw new TripNotFoundException("TRIP WITH ID : " + tripNumber + " NOT FOUND.");
        }
        Trip trip = tripOptional.get();
        trip.setEndTime(Instant.now().toString());
        return mapper.tripTOTripDTO(tripRepository.save(trip));
    }
}
