package com.driveGuard.dataProducer.service;

import com.driveGuard.dataProducer.entity.Car;
import com.driveGuard.dataProducer.repository.CarRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

class CarServiceTest {

    @Mock
    private CarRepository carRepository;

    @InjectMocks
    private CarService carService;

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
}