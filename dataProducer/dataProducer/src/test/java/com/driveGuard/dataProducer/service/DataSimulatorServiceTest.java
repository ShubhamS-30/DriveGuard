package com.driveGuard.dataProducer.service;

import com.driveGuard.dataProducer.exception.TripNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DataSimulatorServiceTest {

    @Mock
    private ProduceMessages produceMessages;

    private DataSimulatorService dataSimulatorService;

    // JUnit will create and clean up this temporary directory for us
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // This line activates all @Mock annotations in this class
        MockitoAnnotations.openMocks(this);

        // Now, 'produceMessages' is a fully initialized mock object, not null
        dataSimulatorService = new DataSimulatorService(produceMessages);

        // Set the @Value fields for the test
        ReflectionTestUtils.setField(dataSimulatorService, "dataSimulatorDirectory", tempDir.toString());
        ReflectionTestUtils.setField(dataSimulatorService, "simulationYear", 2021);
        ReflectionTestUtils.setField(dataSimulatorService, "cabLocationTopicName", "test-locations");
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

        // Create a fake CSV file with a header and two data rows
        Path tripFolder = tempDir.resolve(folderName);
        Files.createDirectories(tripFolder);
        Path tripFile = tripFolder.resolve(tripId + ".csv");
        String csvContent = "target_speed,latitude,longitude\n" +
                "50.5,12.34,56.78\n" +
                "60.0,12.35,56.79\n";
        Files.writeString(tripFile, csvContent);

        // --- When ---
        dataSimulatorService.publishTripData(carNumber, tripId, tripMonth);

        // --- Then ---
        // Verify that our message producer was called exactly 2 times (once for each data row)
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        verify(produceMessages, times(2))
                .produceMessageByTopicAndKey(anyString(), keyCaptor.capture(), messageCaptor.capture());

        // Assert the key is correct
        assertEquals(carNumber, keyCaptor.getValue());

        // Assert the content of the last message
        String lastMessage = messageCaptor.getValue();
        assertTrue(lastMessage.contains("targetSpeed=60.0"));
        assertTrue(lastMessage.contains("carId=001"));
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