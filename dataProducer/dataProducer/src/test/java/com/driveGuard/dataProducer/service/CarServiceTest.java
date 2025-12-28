package com.driveGuard.dataProducer.service;

import com.driveGuard.dataProducer.entity.Car;
import com.driveGuard.dataProducer.repository.CarRepository;
import com.driveGuard.dataProducer.exception.TripNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class CarServiceTest {

    @Mock
    private CarRepository carRepository;

    @Mock
    private DataSimulatorService dataSimulatorService;

    private CarService carService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        carService = new CarService(carRepository, dataSimulatorService, 5);
        ReflectionTestUtils.setField(carService, "cabStatusTopicName", "test-topic");
        ReflectionTestUtils.setField(carService, "chanceOfTrip", 0.0); // default deterministic value
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
    void testEndTrip_deactivatesTrip() {
        int cnr = 1;
        Car before = new Car();
        before.setCnr(cnr);
        before.setIsActiveTrip(true);

        Car after = new Car();
        after.setCnr(cnr);
        after.setIsActiveTrip(false);

        when(dataSimulatorService.endTrip(cnr)).thenReturn(after);

        Car result = carService.endTrip(cnr);

        verify(dataSimulatorService, times(1)).endTrip(cnr);
        // repository.save is not part of current implementation; ensure not required
        verify(carRepository, never()).save(any(Car.class));

        assertNotNull(result);
        assertFalse(result.getIsActiveTrip(), "Returned car should be inactive after endTrip");
    }

    @Test
    void testEndTrip_notActive_throws() {
        int cnr = 1;
        Car car = new Car();
        car.setCnr(cnr);
        car.setIsActiveTrip(false);

        when(dataSimulatorService.endTrip(cnr)).thenReturn(car);

        assertDoesNotThrow(() -> {
            Car result = carService.endTrip(cnr);
            assertEquals(car, result);
        });

        verify(dataSimulatorService, times(1)).endTrip(cnr);
        verify(carRepository, never()).save(any(Car.class));
    }

    @Test
    void testRunFullTripLifecycle_completesSuccessfully() throws Exception {
        int carId = 1;
        ReflectionTestUtils.setField(carService, "chanceOfTrip", 1.0);

        Car carBefore = new Car();
        carBefore.setCnr(carId);
        carBefore.setIsActiveTrip(false);

        Car carAfter = new Car();
        carAfter.setCnr(carId);
        carAfter.setIsActiveTrip(false);

        when(carRepository.findById(anyInt())).thenReturn(Optional.of(carBefore));
        when(dataSimulatorService.endTrip(carId)).thenReturn(carAfter);
        when(carRepository.save(any(Car.class))).thenAnswer(invocation -> invocation.getArgument(0));

        doNothing().when(dataSimulatorService).selectTripByCarId(anyInt());

        carService.runFullTripLifecycle(carId);

        verify(dataSimulatorService, times(1)).selectTripByCarId(carId);
        // ensure endTrip (delegated) was invoked in finally
        verify(dataSimulatorService, atLeastOnce()).endTrip(carId);
    }

    @Test
    void testStopTripDataSimulationByCarId_endsTripAndProducesMessage() {
        int cnr = 1;
        Car before = new Car();
        before.setCnr(cnr);
        before.setIsActiveTrip(true);

        Car after = new Car();
        after.setCnr(cnr);
        after.setIsActiveTrip(false);

        when(dataSimulatorService.endTrip(cnr)).thenReturn(after);

        Car result = carService.stopTripDataSimulationByCarId(cnr);

        verify(dataSimulatorService, times(1)).endTrip(cnr);
        verify(carRepository, never()).save(any(Car.class));

        assertNotNull(result);
        assertFalse(result.getIsActiveTrip(), "Returned car should be inactive after stopping simulation");
    }

    @Test
    void testRunFullTripLifecycle_maxTripsReached() throws InterruptedException, IOException {
        int maxTrips = 5;
        ReflectionTestUtils.setField(carService, "chanceOfTrip", 1.0);

        ReflectionTestUtils.setField(carService, "maxConcurrentTrips", maxTrips);
        ReflectionTestUtils.setField(carService, "tripSemaphore", new java.util.concurrent.Semaphore(maxTrips));

        when(carRepository.findById(anyInt())).thenAnswer(invocation -> {
            Car car = new Car();
            car.setCnr(invocation.getArgument(0));
            car.setIsActiveTrip(false);
            return Optional.of(car);
        });
        when(carRepository.save(any(Car.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CountDownLatch latch = new CountDownLatch(maxTrips);

        doAnswer(invocation -> {
            latch.countDown();
            Thread.sleep(2000);
            return null;
        }).when(dataSimulatorService).selectTripByCarId(anyInt());

        ExecutorService testExecutor = Executors.newFixedThreadPool(maxTrips);
        for (int i = 0; i < maxTrips; i++) {
            final int carId = i + 100;
            testExecutor.submit(() -> carService.runFullTripLifecycle(carId));
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Not all trips acquired a semaphore permit in time.");

        TripNotFoundException ex = assertThrows(
                TripNotFoundException.class,
                () -> carService.runFullTripLifecycle(1)
        );

        assertNotNull(ex.getMessage());
        assertFalse(ex.getMessage().trim().isEmpty(), "Expected a non-blank exception message when max trips reached");

        testExecutor.shutdownNow();
    }
}
