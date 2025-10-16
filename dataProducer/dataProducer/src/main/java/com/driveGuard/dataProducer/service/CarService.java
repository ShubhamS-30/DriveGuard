package com.driveGuard.dataProducer.service;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.driveGuard.dataProducer.AppLogger;
import com.driveGuard.dataProducer.entity.Car;
import com.driveGuard.dataProducer.exception.TripNotFoundException;
import com.driveGuard.dataProducer.repository.CarRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class CarService {

    private static final AppLogger log = AppLogger.getLogger(CarService.class);

    private final CarRepository carRepository;
    private final ProduceMessages produceMessages;
    private final DataSimulatorService dataSimulatorService;
    private ExecutorService executorService;

    @Value("${data.cab.status.topic.name}")
    private String cabStatusTopicName;

    @Value("${data.simulation.max.concurrent.trips}")
    private Integer maxConcurrentTrips;

    @Value("${data.simulation.chance.of.trip}")
    private double chanceOfTrip;

    private Semaphore tripSemaphore;

    public CarService(CarRepository carRepository, ProduceMessages produceMessages,DataSimulatorService dataSimulatorService,@Value("${data.simulation.max.concurrent.trips}") Integer maxConcurrentTrips) {
        this.carRepository = carRepository;
        this.produceMessages = produceMessages;
        this.dataSimulatorService = dataSimulatorService;

        // 1. CREATE THREAD POOL AND SEMAPHORE ONLY ONCE
        this.executorService = Executors.newCachedThreadPool(); // Or a fixed pool
        this.tripSemaphore = new Semaphore(maxConcurrentTrips);
    }

    // Get all cars
    public List<Car> getAllCars() {
        return carRepository.findAll();
    }

    // Get car by ID
    public Optional<Car> getCarById(Integer cnr) {
        return carRepository.findById(cnr);
    }

    // Save a new car
    public Car saveCar(Car car) {
        return carRepository.save(car);
    }

    // Start a trip
    public Car startTrip(Integer cnr) {
        Optional<Car> optionalCar = carRepository.findById(cnr);
        if (optionalCar.isPresent()) {
            Car car = optionalCar.get();
            if(Boolean.TRUE.equals(car.getIsActiveTrip())){
                throw new TripNotFoundException("Trip is already active for Car ID: " + cnr);
            }
            car.setIsActiveTrip(true);
            return carRepository.save(car);
        }
        throw new TripNotFoundException("Car not found with ID: " + cnr);
    }

    public Car endTrip(Integer cnr) {
        Optional<Car> optionalCar = carRepository.findById(cnr);
        if (optionalCar.isPresent()) {
            Car car = optionalCar.get();
            if(Boolean.FALSE.equals(car.getIsActiveTrip())){
                throw new TripNotFoundException("Trip is not active for Car ID: " + cnr);
            }
            car.setIsActiveTrip(false);
            return carRepository.save(car);
        }
        throw new TripNotFoundException("Car not found with ID: " + cnr);
    }

    @Scheduled(cron = "0 * * * * *")
    public void startTripDataSimulationCron() {
        // This method can be scheduled to run at fixed intervals using @Scheduled annotation
        log.info(Instant.now().toString() + " :: CRON JOB: Looking for available cars to start trips... ");
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
        Car carDetails = startTrip(carId);
        try {
            // 2. ACQUIRE SEMAPHORE before starting anything
            if (!tripSemaphore.tryAcquire()) {
                throw new TripNotFoundException("Max Trip Limit Reached. Please try again later.");
            }

            log.info("STARTING trip data simulation for Car ID: {}" + carId);

            // The startTrip method now handles the race condition better with @Transactional
            produceMessages.produceMessageByTopic(cabStatusTopicName, "STARTING = " + carDetails.toString());

            // This is the actual long-running simulation
            dataSimulatorService.selectTripByCarId(carDetails.getCnr());

            log.info("ENDING trip data simulation for Car ID: {}" + carId);
            Car endedCarDetails = endTrip(carId);
            produceMessages.produceMessageByTopic(cabStatusTopicName, "ENDING = " + endedCarDetails.toString());

        } catch (TripNotFoundException e) {
            log.error("TripNotFoundException for Car ID: {}" + carId, e);
            throw e;
        } catch (Exception e) {
            // Ensure you handle potential exceptions from the simulation
            log.error("An unexpected error occurred during simulation for Car ID: " + carId, (Path) e);
            // Attempt to clean up and end the trip if an error occurred
            try {
                endTrip(carId);
            } catch (Exception cleanupEx) {
                log.error("Failed to cleanup and end trip for Car ID: " + carId, (Path) cleanupEx);
                throw cleanupEx;
            }
        } finally {
            tripSemaphore.release();
            stopTripDataSimulationByCarId(carId);
            log.info("Semaphore released for Car ID: " + carId);
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
                this.stopTripDataSimulationByCarId(car.getCnr());
                log.info("Successfully ended trip for Car ID: " + car.getCnr());
            } catch (Exception e) {
                // Log an error but continue the shutdown process.
                log.error("Error while ending trip for Car ID: " + car.getCnr(), e);
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

    public Car stopTripDataSimulationByCarId(Integer carId)  {
       // Implementation for stopping trip data simulation for a specific car
       Car details = endTrip(carId);
       produceMessages.produceMessageByTopic(cabStatusTopicName, "ENDING = " + details.toString());
       return details;
    }
}
