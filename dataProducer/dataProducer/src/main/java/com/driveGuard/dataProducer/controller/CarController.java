package com.driveGuard.dataProducer.controller;

import java.io.IOException;
import java.util.List;

import com.driveGuard.dataProducer.dto.CarDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.driveGuard.dataProducer.entity.Car;
import com.driveGuard.dataProducer.exception.TripNotFoundException;
import com.driveGuard.dataProducer.service.CarService;


@RestController
@RequestMapping("/cars")
public class CarController {

    private final CarService carService;

    @Autowired
    public CarController(CarService carService) {
        this.carService = carService;
    }

    // Get all cars
    @GetMapping
    public List<CarDTO> getAllCars() {
        return carService.getAllCars();
    }

    // Get car by ID
    @GetMapping("/{id}")
    public CarDTO getCarById(@PathVariable Integer id) {
        return carService.getCarById(id)
                .orElseThrow(() -> new TripNotFoundException("Car not found with ID: " + id));
    }

    // Create a new car
    @PostMapping
    public CarDTO addCar(@RequestBody Car car) {
        return carService.saveCar(car);
    }


    @PutMapping("stopTrip/{id}")
    public ResponseEntity<CarDTO> putMethodName(@PathVariable("id") Integer carId) throws IOException {
        return ResponseEntity.ok(carService.stopTripDataSimulationByCarId(carId));
    }
}