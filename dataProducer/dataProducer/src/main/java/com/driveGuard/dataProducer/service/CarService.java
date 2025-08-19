package com.driveGuard.dataProducer.service;

import com.driveGuard.dataProducer.entity.Car;
import com.driveGuard.dataProducer.repository.CarRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CarService {

    private final CarRepository carRepository;

    public CarService(CarRepository carRepository) {
        this.carRepository = carRepository;
    }

    // Get all cars
    public List<Car> getAllCars() {
        return (List<Car>) carRepository.findAll();
    }

    // Get car by ID
    public Optional<Car> getCarById(Integer cnr) {
        return carRepository.findById(cnr);
    }

    // Save a new car
    public Car saveCar(Car car) {
        return carRepository.save(car);
    }
}
