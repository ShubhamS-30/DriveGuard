package com.driveGuard.dataProducer.repository;

import com.driveGuard.dataProducer.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TripRepository extends JpaRepository<Trip,Integer> {
    Optional<Trip> getTripByTripNumber(String tripNumber);
}
