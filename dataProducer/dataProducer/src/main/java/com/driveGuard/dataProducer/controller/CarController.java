package com.driveGuard.dataProducer.controller;

import java.io.IOException;
import java.util.List;

import com.driveGuard.dataProducer.dto.CarDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Vehicle Management", description = "Endpoints for managing the fleet and tracking car status")
public class CarController {

    private final CarService carService;

    @Autowired
    public CarController(CarService carService) {
        this.carService = carService;
    }

    // Get all cars
    @Operation(
            summary = "Get All Cars",
            description = "Get All Cars in the Fleet"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cars retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Cars not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public List<CarDTO> getAllCars() {
        return carService.getAllCars();
    }

    // Get car by ID
    @Operation(
            summary = "Get Car by ID",
            description = "Finds Car by ID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Car retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Car not found"),
            @ApiResponse(responseCode = "400", description = "Invalid ID supplied"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}")
    public CarDTO getCarById(@PathVariable Integer id) {
        return carService.getCarById(id)
                .orElseThrow(() -> new TripNotFoundException("Car not found with ID: " + id));
    }

    // Create a new car
    @Operation(
            summary = "Add New Car",
            description = "Adds a new Car to the Fleet"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Car created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public CarDTO addCar(@RequestBody Car car) {
        return carService.saveCar(car);
    }

    // Start trip data simulation for a car
    @Operation(
            summary = "Start Trip Data Simulation",
            description = "Starts Trip Data Simulation for a specific Car by ID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Trip data simulation started successfully"),
            @ApiResponse(responseCode = "404", description = "Car not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("stopTrip/{id}")
    public ResponseEntity<CarDTO> putMethodName(@PathVariable("id") Integer carId) throws IOException {
        return ResponseEntity.ok(carService.stopTripDataSimulationByCarId(carId));
    }
}