package com.driveGuard.dataProducer.service;

import com.driveGuard.dataProducer.entity.Car;
import com.driveGuard.dataProducer.repository.CarRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

import com.driveGuard.dataProducer.exception.TripNotFoundException;

class CarServiceTest {

    @Mock
    private CarRepository carRepository;

    @InjectMocks
    private CarService carService;

    @Mock
    private ProduceMessages produceMessages;
    

    @BeforeEach
    void setUp() {
        openMocks(this);
    }

    @Test
    void testGetAllCars() {
        List<Car> cars = Arrays.asList(new Car(), new Car());
        when(carRepository.findAll()).thenReturn(cars);

        List<Car> result = carService.getAllCars();
        assertEquals(2, result.size());
        verify(carRepository).findAll();
    }

    @Test
    void testGetCarById_found() {
        Car car = new Car();
        when(carRepository.findById(1)).thenReturn(Optional.of(car));

        Optional<Car> result = carService.getCarById(1);
        assertTrue(result.isPresent());
        assertEquals(car, result.get());
        verify(carRepository).findById(1);
    }

    @Test
    void testGetCarById_notFound() {
        when(carRepository.findById(2)).thenReturn(Optional.empty());

        Optional<Car> result = carService.getCarById(2);
        assertFalse(result.isPresent());
        verify(carRepository).findById(2);
    }

    @Test
    void testSaveCar() {
        Car car = new Car();
        when(carRepository.save(car)).thenReturn(car);

        Car result = carService.saveCar(car);
        assertEquals(car, result);
        verify(carRepository).save(car);
    }

    @Test
    void testStartTrip_activatesTrip() {
        Car car = new Car();
        car.setCnr(1);
        car.setIsActiveTrip(false);
        when(carRepository.findById(1)).thenReturn(Optional.of(car));
        when(carRepository.save(any(Car.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Car result = carService.startTrip(1);
        assertTrue(result.getIsActiveTrip());
        verify(carRepository).save(car);
    }

    @Test
    void testStartTrip_alreadyActive_throws() {
        Car car = new Car();
        car.setCnr(1);
        car.setIsActiveTrip(true);
        when(carRepository.findById(1)).thenReturn(Optional.of(car));

        assertThrows(TripNotFoundException.class, () -> carService.startTrip(1));
    }

    @Test
    void testEndTrip_deactivatesTrip() {
        Car car = new Car();
        car.setCnr(1);
        car.setIsActiveTrip(true);
        when(carRepository.findById(1)).thenReturn(Optional.of(car));
        when(carRepository.save(any(Car.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Car result = carService.endTrip(1);
        assertFalse(result.getIsActiveTrip());
        verify(carRepository).save(car);
    }

    @Test
    void testEndTrip_notActive_throws() {
        Car car = new Car();
        car.setCnr(1);
        car.setIsActiveTrip(false);
        when(carRepository.findById(1)).thenReturn(Optional.of(car));

        assertThrows(TripNotFoundException.class, () -> carService.endTrip(1));
    }

    @Test
    void testStartTripDataSimulationByCarId_startsTripAndProducesMessage() {
        Car car = new Car();
        car.setCnr(1);
        car.setIsActiveTrip(false);
        when(carRepository.findById(1)).thenReturn(Optional.of(car));
        when(carRepository.save(any(Car.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Set the topic name
        ReflectionTestUtils.setField(carService, "cabStatusTopicName", "test-topic");

        Car result = carService.startTripDataSimulationByCarId(1);
        assertTrue(result.getIsActiveTrip());
        verify(produceMessages).produceMessageByTopic(anyString(), anyString());
    }

    @Test
    void testStopTripDataSimulationByCarId_endsTripAndProducesMessage() {
        Car car = new Car();
        car.setCnr(1);
        car.setIsActiveTrip(true);
        when(carRepository.findById(1)).thenReturn(Optional.of(car));
        when(carRepository.save(any(Car.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Set the topic name
        ReflectionTestUtils.setField(carService, "cabStatusTopicName", "test-topic");

        Car result = carService.stopTripDataSimulationByCarId(1);
        assertFalse(result.getIsActiveTrip());
        verify(produceMessages).produceMessageByTopic(anyString(), anyString());
    }
}