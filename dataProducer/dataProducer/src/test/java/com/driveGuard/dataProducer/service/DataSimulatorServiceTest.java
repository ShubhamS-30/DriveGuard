package com.driveGuard.dataProducer.service;

import com.driveGuard.dataProducer.dto.message.TripStatusMessage;
import com.driveGuard.dataProducer.entity.Car;
import com.driveGuard.dataProducer.entity.TripRow;
import com.driveGuard.dataProducer.exception.TripNotFoundException;
import com.driveGuard.dataProducer.repository.CarRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DataSimulatorServiceTest {

    @Mock
    private ProduceMessages produceMessages;

    private DataSimulatorService dataSimulatorService;

    @Mock
    private CarRepository carRepository;

    // JUnit will create and clean up this temporary directory for us
    @TempDir
    Path tempDir;

    // We'll define topic names here to use for setup and verification
    private final String LOCATION_TOPIC = "test-locations";
    private final String STATUS_TOPIC = "test-status";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        dataSimulatorService = new DataSimulatorService(produceMessages, carRepository);

        // Inject all @Value fields
        ReflectionTestUtils.setField(dataSimulatorService, "dataSimulatorDirectory", tempDir.toString());
        ReflectionTestUtils.setField(dataSimulatorService, "simulationYear", 2021);
        ReflectionTestUtils.setField(dataSimulatorService, "cabLocationTopicName", LOCATION_TOPIC);
        ReflectionTestUtils.setField(dataSimulatorService, "cabStatusTopicName", STATUS_TOPIC);
    }

    @Test
    void getTripIdsForMonth_whenFolderExists_returnsTripIds() throws IOException {
        // --- Given (Arrange) ---
        // Create a fake directory and trip files inside the temporary directory
        String carNumber = "007";
        String year = "2021";
        String month = "05";
        Path tripFolder = tempDir.resolve(carNumber + "_" + year + "_" + month);
        Files.createDirectories(tripFolder);
        Files.createFile(tripFolder.resolve("0_00001.csv"));
        Files.createFile(tripFolder.resolve("0_00002.csv"));

        // --- When (Act) ---
        List<String> tripIds = dataSimulatorService.getTripIdsForMonth(carNumber, year, month);

        // --- Then (Assert) ---
        assertNotNull(tripIds);
        assertEquals(2, tripIds.size());
        assertTrue(tripIds.contains("0_00001"));
        assertTrue(tripIds.contains("0_00002"));
    }

    @Test
    void getTripIdsForMonth_whenFolderDoesNotExist_throwsException() {
        // --- Given ---
        String carNumber = "999"; // A car with no data
        String year = "2021";
        String month = "01";

        // --- When & Then ---
        TripNotFoundException ex = assertThrows(
                TripNotFoundException.class,
                () -> dataSimulatorService.getTripIdsForMonth(carNumber, year, month)
        );
        assertTrue(ex.getMessage().contains("Trip/vehicle folder not found"));
    }

    @Test
    void publishTripData_whenFileExists_producesMessages() throws IOException {
        // --- Given ---
        String carNumber = "001";
        String tripId = "0_00001";
        String tripMonth = "01";
        String folderName = carNumber + "_2021_" + tripMonth;

        // Create a fake CSV file
        Path tripFolder = tempDir.resolve(folderName);
        Files.createDirectories(tripFolder);
        Path tripFile = tripFolder.resolve(tripId + ".csv");
        // Use the correct header name from your TripRow class
        String csvContent = "target_speed,latitude,longitude\n" +
                "50.5,12.34,56.78\n" +
                "60.0,12.35,56.79\n";
        Files.writeString(tripFile, csvContent);

        // --- Mock the Repository ---
        Car mockCar = new Car();
        mockCar.setCnr(1);
        mockCar.setIsActiveTrip(false); // Start as inactive
        when(carRepository.findById(1)).thenReturn(Optional.of(mockCar));
        when(carRepository.save(any(Car.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // --- When ---
        dataSimulatorService.publishTripData(carNumber, tripId, tripMonth);

        // --- Then ---
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<TripRow> messageCaptor = ArgumentCaptor.forClass(TripRow.class);

        // Verify the location topic calls (2 times)
        verify(produceMessages, times(2))
                .produceMessageByTopicAndKey(eq(LOCATION_TOPIC), keyCaptor.capture(), messageCaptor.capture());

        // Verify the status topic calls (also 2 times, for start and end)
        verify(produceMessages, times(2))
                .produceMessageByTopic(eq(STATUS_TOPIC), any(TripStatusMessage.class));

        // We captured two TripRow messages, let's get the last one.
        TripRow lastMessage = messageCaptor.getValue();
        assertEquals("60.0", lastMessage.getTargetSpeed());
        assertEquals(carNumber, lastMessage.getCarId());
        assertEquals(100.0, lastMessage.getTripCompletion());
    }

    @Test
    void publishTripData_whenFileDoesNotExist_throwsException() {
        // --- Given ---
        String carNumber = "001";
        String tripId = "non_existent_trip";
        String tripMonth = "01";

        // --- When & Then ---
        TripNotFoundException ex = assertThrows(
                TripNotFoundException.class,
                () -> dataSimulatorService.publishTripData(carNumber, tripId, tripMonth)
        );
        assertTrue(ex.getMessage().contains("Trip file not found"));
    }
}