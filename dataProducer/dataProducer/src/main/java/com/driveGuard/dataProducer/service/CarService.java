package com.driveGuard.dataProducer.service;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import com.driveGuard.dataProducer.dto.CarDTO;
import com.driveGuard.dataProducer.utility.AppLogger;
import com.driveGuard.dataProducer.utility.Mapper;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.driveGuard.dataProducer.entity.Car;
import com.driveGuard.dataProducer.exception.TripNotFoundException;
import com.driveGuard.dataProducer.repository.CarRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class CarService {

    private static final AppLogger log = AppLogger.getLogger(CarService.class);

    private final CarRepository carRepository;
    private final DataSimulatorService dataSimulatorService;
    private final ExecutorService executorService;
    private final Mapper mapper;

    @Value("${data.cab.status.topic.name}")
    private String cabStatusTopicName;

    @Value("${data.simulation.max.concurrent.trips}")
    private Integer maxConcurrentTrips;

    @Value("${data.simulation.chance.of.trip}")
    private double chanceOfTrip;

    private final Semaphore tripSemaphore;

    public CarService(CarRepository carRepository, DataSimulatorService dataSimulatorService, Mapper mapper,@Value("${data.simulation.max.concurrent.trips}") Integer maxConcurrentTrips) {
        this.carRepository = carRepository;
        this.dataSimulatorService = dataSimulatorService;
        this.mapper = mapper;

        // CREATE THREAD POOL AND SEMAPHORE ONLY ONCE
        this.executorService = Executors.newCachedThreadPool();
        this.tripSemaphore = new Semaphore(maxConcurrentTrips);
    }

    // Get all cars
    public List<CarDTO> getAllCars() {
        return mapper.carListToCarDTOList(carRepository.findAll());
    }

    // Get car by ID
    public Optional<CarDTO> getCarById(Integer cnr) {
        Car car = carRepository.findById(cnr).orElse(null);
        return Optional.ofNullable(mapper.carToCarDTO(car));
    }

    // Save a new car
    @Transactional
    public CarDTO saveCar(Car car) {
        Car savedCar =  carRepository.save(car);
        return mapper.carToCarDTO(savedCar);
    }

    @Transactional
    public Car endTrip(Integer cnr) {
        return dataSimulatorService.endTrip(cnr);
    }

    @Scheduled(cron = "0 * * * * *")
    public void startTripDataSimulationCron() {
        // This method can be scheduled to run at fixed intervals using @Scheduled annotation
        log.info(String.format("%s :: CRON JOB: Looking for available cars to start trips... ", Instant.now().toString()));
        List<Car> cars = carRepository.findByIsActiveTrip(false); // Find only inactive cars

        for (Car car : cars) {
            double chance = Math.random();
            if (chance >= chanceOfTrip) {
                // Submit a task to run the full trip lifecycle
                executorService.submit(() -> runFullTripLifecycle(car.getCnr()));
            }
        }
    }

    public void runFullTripLifecycle(Integer carId) {
        try {
            // 2. ACQUIRE SEMAPHORE before starting anything
            if (!tripSemaphore.tryAcquire()) {
                throw new TripNotFoundException("Max Trip Limit Reached. Please try again later.");
            }

            // This is the actual long-running simulation
            dataSimulatorService.selectTripByCarId(carId);


        } catch (TripNotFoundException e) {
            log.error(String.format("TripNotFoundException for Car ID: %s", carId), e);
            throw e;
        } catch (Exception e) {
            // Ensure you handle potential exceptions from the simulation
            log.error(String.format("An unexpected error occurred during simulation for Car ID: %s", carId), (Path) e);
            // Attempt to clean up and end the trip if an error occurred
            try {
                endTrip(carId);
            } catch (Exception cleanupEx) {
                log.error(String.format("Failed to cleanup and end trip for Car ID: %s", carId), (Path) cleanupEx);
                throw cleanupEx;
            }
        } finally {
            tripSemaphore.release();
            stopTripDataSimulationByCarId(carId);
            log.info(String.format("Semaphore released for Car ID: %s", carId));
        }
    }


    @PreDestroy
    public void stopTripDataSimulation() {
        // This method will be called when the application is shutting down
        // STOP all active trips
        log.info("Shutting down CarService executor service.");
        List<Car> activeCars = carRepository.findByIsActiveTrip(true);

        for (Car car : activeCars) {
            try {
                // Use your existing method to end the trip and send the Kafka message.
                this.forceStopTripDataSimulationByCarId(car);
                log.info(String.format("Successfully ended trip for Car ID: %s", car.getCnr()));
            } catch (Exception e) {
                // Log an error but continue the shutdown process.
                log.error(String.format("Error while ending trip for Car ID: %s", car.getCnr()), e);
            }
        }

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Transactional
    public void forceStopTripDataSimulationByCarId(Car car) {
        // Implementation for stopping trip data simulation for a specific car
        endTrip(car.getCnr());
    }

    @Transactional
    public CarDTO stopTripDataSimulationByCarId(Integer carId) {
        // Implementation for stopping trip data simulation for a specific car
        return mapper.carToCarDTO(endTrip(carId));
    }

    // Change the parameters from (int page, int size) to (Pageable pageable)
    public Page<CarDTO> getAllActiveCars(Pageable pageable) {
        // Pass the 'pageable' object directly to the repository
        // This preserves the 'sort' information (e.g., activeTripNumber,desc)
        Page<Car> activeCarsPage = carRepository.findByIsActiveTrip(true, pageable);

        if(activeCarsPage.isEmpty()){
            throw new TripNotFoundException("No active cars found at the moment.");
        }

        return activeCarsPage.map(mapper::carToCarDTO);
    }
}
