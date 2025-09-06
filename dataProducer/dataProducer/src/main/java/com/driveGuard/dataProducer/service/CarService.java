package com.driveGuard.dataProducer.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
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

    @Value("${data.cab.status.topic.name}")
    private String cabStatusTopicName;

    public CarService(CarRepository carRepository, ProduceMessages produceMessages) {
        this.carRepository = carRepository;
        this.produceMessages = produceMessages;
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
            if(car.getIsActiveTrip()==true){
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
            if(car.getIsActiveTrip()==false){
                throw new TripNotFoundException("Trip is not active for Car ID: " + cnr);
            }
            car.setIsActiveTrip(false);
            return carRepository.save(car);
        }
        throw new TripNotFoundException("Car not found with ID: " + cnr);
    }

    public void startTripDataSimulation() {
        // Implementation for starting trip data simulation
        List<Car> cars = getAllCars();
        for (Car car : cars) {
            log.info("Car details: " + car.toString());
            if(!car.getIsActiveTrip()){
                this.startTripDataSimulationByCarId(car.getCnr());
            }
        }

    }

    public Car startTripDataSimulationByCarId(Integer carId) {
        
        Car details = startTrip(carId);
        produceMessages.produceMessageByTopic(cabStatusTopicName, details.toString());
        return details;
    }
    

    public void stopTripDataSimulation() {
        // Implementation for stopping trip data simulation
        List<Car> cars = getAllCars();
        for (Car car : cars) {
            if(car.getIsActiveTrip()){
                this.stopTripDataSimulationByCarId(car.getCnr());
            }
        }
    }

    public Car stopTripDataSimulationByCarId(Integer carId) {
       // Implementation for stopping trip data simulation for a specific car
       Car details = endTrip(carId);
       produceMessages.produceMessageByTopic(cabStatusTopicName, details.toString());
       return details;
    }
}
