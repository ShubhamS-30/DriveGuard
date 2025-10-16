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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CarServiceTest {

    @Mock
    private CarRepository carRepository;

    @Mock
    private ProduceMessages produceMessages;

    @Mock
    private DataSimulatorService dataSimulatorService;

    private CarService carService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        carService = new CarService(carRepository, produceMessages, dataSimulatorService, 5);
        ReflectionTestUtils.setField(carService, "cabStatusTopicName", "test-topic");
        ReflectionTestUtils.setField(carService, "chanceOfTrip", 0.0); // For deterministic tests
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
    void testRunFullTripLifecycle_completesSuccessfully() throws Exception {
        // --- 1. Given (Arrange) ---
        int carId = 1;
        Car car = new Car();
        car.setCnr(carId);
        car.setIsActiveTrip(false);

        // Mock the database interactions
        when(carRepository.findById(carId)).thenReturn(Optional.of(car));
        when(carRepository.save(any(Car.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Mock the long-running simulation
        doNothing().when(dataSimulatorService).selectTripByCarId(anyInt());

        // --- 2. When (Act) ---
        // The method is now void, so there's no return value to capture
        carService.runFullTripLifecycle(carId);

        // --- 3. Then (Assert) ---
        // Verify that the simulation was called exactly once
        verify(dataSimulatorService, times(1)).selectTripByCarId(carId);

        // Capture the Car objects passed to the save method
        ArgumentCaptor<Car> carCaptor = ArgumentCaptor.forClass(Car.class);
        verify(carRepository, times(2)).save(carCaptor.capture());

        // Get the car object from the SECOND save call (which is from endTrip)
        Car finalCarState = carCaptor.getAllValues().get(1);

        // Assert that the car is INACTIVE in its final saved state
        assertFalse(finalCarState.getIsActiveTrip(), "Car should be inactive in its final state.");

        // You can still capture and verify the Kafka messages as before
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(produceMessages, times(2)).produceMessageByTopic(anyString(), messageCaptor.capture());

        List<String> capturedMessages = messageCaptor.getAllValues();
        assertTrue(capturedMessages.get(0).contains("STARTING"), "First message should be a STARTING event.");
        assertTrue(capturedMessages.get(1).contains("ENDING"), "Second message should be an ENDING event.");
    }

    @Test
    void testStopTripDataSimulationByCarId_endsTripAndProducesMessage() throws IOException {
        Car car = new Car();
        car.setCnr(1);
        car.setIsActiveTrip(true);
        when(carRepository.findById(1)).thenReturn(Optional.of(car));
        when(carRepository.save(any(Car.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Car result = carService.stopTripDataSimulationByCarId(1);
        assertFalse(result.getIsActiveTrip());
        verify(produceMessages).produceMessageByTopic(anyString(), anyString());
    }

    @Test
    void testRunFullTripLifecycle_maxTripsReached() throws InterruptedException, IOException {
        // --- Setup Mocks and Configuration ---
        int maxTrips = 5;
        // Manually set the @Value field for the test
        ReflectionTestUtils.setField(carService, "maxConcurrentTrips", maxTrips);
        // Re-initialize the semaphore with the correct value
        ReflectionTestUtils.setField(carService, "tripSemaphore", new java.util.concurrent.Semaphore(maxTrips));

        when(carRepository.findById(anyInt())).thenAnswer(invocation -> {
            Car car = new Car();
            car.setCnr(invocation.getArgument(0));
            car.setIsActiveTrip(false);
            return Optional.of(car);
        });
        when(carRepository.save(any(Car.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Use a latch to wait for background tasks to start
        CountDownLatch latch = new CountDownLatch(maxTrips);

        // **CRITICAL:** Mock the long-running task to "hang" and hold the semaphore permit
        doAnswer(invocation -> {
            latch.countDown(); // Signal that this task has started
            Thread.sleep(2000); // Block to simulate a long trip
            return null;
        }).when(dataSimulatorService).selectTripByCarId(anyInt());

        // --- Execute Test ---
        ExecutorService testExecutor = Executors.newFixedThreadPool(maxTrips);
        // Start 'maxTrips' trips in the background to use up all permits
        for (int i = 0; i < maxTrips; i++) {
            final int carId = i + 100;
            testExecutor.submit(() -> carService.runFullTripLifecycle(carId));
        }

        // Wait for all trips to start and acquire their permits
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Not all trips acquired a semaphore permit in time.");

        // --- Assert ---
        // Now, this 6th call on the main thread should fail
        TripNotFoundException ex = assertThrows(
                TripNotFoundException.class,
                () -> carService.runFullTripLifecycle(1)
        );

        assertTrue(ex.getMessage().contains("Max Trip Limit Reached"));

        // --- Cleanup ---
        testExecutor.shutdownNow();
    }
}
