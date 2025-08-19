package com.driveGuard.dataProducer.controller;

import com.driveGuard.dataProducer.entity.Car;
import com.driveGuard.dataProducer.exception.TripNotFoundException;
import com.driveGuard.dataProducer.service.CarService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cars")
public class CarController {

    private final CarService carService;

    public CarController(CarService carService) {
        this.carService = carService;
    }

    // Get all cars
    @GetMapping
    public List<Car> getAllCars() {
        return carService.getAllCars();
    }

    // Get car by ID
    @GetMapping("/{id}")
    public Car getCarById(@PathVariable Integer id) {
        return carService.getCarById(id)
                .orElseThrow(() -> new TripNotFoundException("Car not found with ID: " + id));
    }

    // Create a new car
    @PostMapping
    public Car addCar(@RequestBody Car car) {
        return carService.saveCar(car);
    }
}