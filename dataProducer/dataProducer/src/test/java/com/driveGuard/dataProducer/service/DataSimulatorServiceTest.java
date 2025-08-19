package com.driveGuard.dataProducer.service;

import com.driveGuard.dataProducer.exception.TripNotFoundException;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DataSimulatorServiceTest {

    private DataSimulatorService service;
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        service = new DataSimulatorService();
        tempDir = Files.createTempDirectory("dataSimulatorTest");
        ReflectionTestUtils.setField(service, "dataSimulatorDirectory", tempDir.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.walk(tempDir)
                .map(Path::toFile)
                .forEach(java.io.File::delete);
    }

    @Test
    void testGetCarMonthFoldersWithExcelFiles_found() throws Exception {
        String carNumber = "CAR123";
        String folderName = carNumber + "_2021_01";
        Files.createDirectory(tempDir.resolve(folderName));
        List<String> folders = service.getCarMonthFoldersWithExcelFiles(carNumber);
        assertTrue(folders.contains(folderName));
    }

    @Test
    void testGetCarMonthFoldersWithExcelFiles_notFound() {
        String carNumber = "CAR999";
        assertThrows(TripNotFoundException.class, () -> service.getCarMonthFoldersWithExcelFiles(carNumber));
    }

    @Test
    void testGetTripIdsForMonth_found() throws Exception {
        String carNumber = "CAR123";
        String year = "2021";
        String month = "01";
        String folderName = carNumber + "_" + year + "_" + month;
        Path monthDir = tempDir.resolve(folderName);
        Files.createDirectory(monthDir);
        Files.createFile(monthDir.resolve("0_00001.csv"));
        Files.createFile(monthDir.resolve("0_00002.csv"));

        List<String> tripIds = service.getTripIdsForMonth(carNumber, year, month);
        assertTrue(tripIds.contains("0_00001"));
        assertTrue(tripIds.contains("0_00002"));
    }

    @Test
    void testGetTripIdsForMonth_notFound() {
        assertThrows(TripNotFoundException.class, () -> service.getTripIdsForMonth("CAR123", "2021", "01"));
    }

    @Test
    void testGetTripDetailsFirst20Rows_notFound() {
        assertThrows(TripNotFoundException.class, () -> service.getTripDetailsFirst20Rows("folder", "file.csv"));
    }
}