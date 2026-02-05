package com.driveGuard.dataProducer.repository;

import com.driveGuard.dataProducer.entity.Car;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarRepository extends JpaRepository<Car, Integer> {

    List<Car> findByIsActiveTrip(boolean isActiveTrip);

    Page<Car> findByIsActiveTrip(boolean isActiveTrip, Pageable pageable);
}
