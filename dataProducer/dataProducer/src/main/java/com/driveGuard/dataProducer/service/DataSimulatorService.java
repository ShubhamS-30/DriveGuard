package com.driveGuard.dataProducer.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.driveGuard.dataProducer.AppLogger;
import com.driveGuard.dataProducer.dto.TripRow;
import com.driveGuard.dataProducer.entity.Car;
import com.driveGuard.dataProducer.exception.TripNotFoundException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

@Service
public class DataSimulatorService {

    private static final AppLogger log = AppLogger.getLogger(DataSimulatorService.class
    );

    @Value("${data.simulator.directory}")
    private String dataSimulatorDirectory;

    public List<String> getCarMonthFoldersWithExcelFiles(String carNumber) throws TripNotFoundException {
        List<String> folderNames = new ArrayList<>();
        Path dataDir = Paths.get(dataSimulatorDirectory);
        if (!Files.exists(dataDir) || !Files.isDirectory(dataDir)) {
            throw new TripNotFoundException("Data simulator directory not found: " + dataSimulatorDirectory);
        }
        boolean found = false;
        for (int month = 1; month <= 12; month++) {
            String monthStr = String.format("%02d", month);
            String folderName = carNumber + "_2021_" + monthStr;
            Path monthDir = dataDir.resolve(folderName);
            if (Files.exists(monthDir) && Files.isDirectory(monthDir)) {
                folderNames.add(folderName);
                found = true;
            }
        }
        if (!found) {
            throw new TripNotFoundException("No folders found for car: " + carNumber);
        }
        return folderNames;
    }

    public List<TripRow> getTripDetailsFirst20Rows(String folderName, String tripFileName) throws IOException, TripNotFoundException {
        List<TripRow> rowsData = new ArrayList<>();
        Path tripFile = Paths.get(dataSimulatorDirectory, folderName, tripFileName);
        if (!Files.exists(tripFile)) {
            throw new TripNotFoundException("Trip/vehicle not found");
        }
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        int rowCount = 0;
        try (FileInputStream fis = new FileInputStream(tripFile.toFile())) {
            MappingIterator<TripRow> it = mapper.readerFor(TripRow.class).with(schema).readValues(fis);
            while (it.hasNext()) {
                TripRow row = it.next();
                rowsData.add(row);
                rowCount++;
                if (rowCount >= 20) {
                    break;
                }
            }
        }
        return rowsData;
    }

    /**
     * Returns all unique trip IDs for a given car and month. Trip files are
     * named like 0_00001.csv, 0_00002.csv, etc.
     */
    public List<String> getTripIdsForMonth(String carNumber, String year, String month)
            throws IOException, TripNotFoundException {
        List<String> tripIds = new ArrayList<>();
        String paddedMonth = month.length() == 2 ? month : String.format("%02d", Integer.valueOf(month));
        String folderName = carNumber + "_" + year + "_" + paddedMonth;
        log.info("Looking for trip IDs in folder: " + folderName);

        Path monthDir = Paths.get(dataSimulatorDirectory, folderName);

        log.info("Checking directory: " + monthDir.toString());

        if (!Files.exists(monthDir) || !Files.isDirectory(monthDir)) {
            throw new TripNotFoundException("Trip/vehicle folder not found: " + folderName);
        }
        try (var stream = Files.newDirectoryStream(monthDir, "*.csv")) {
            for (Path file : stream) {
                String fileName = file.getFileName().toString();
                if (fileName.endsWith(".csv")) {
                    String tripId = fileName.substring(0, fileName.length() - 4); // Remove ".csv"
                    tripIds.add(tripId);
                }
            }
        }
        return tripIds;
    }

    public List<Car> getActiveSimulatedCars() {
        // Implementation to return list of cars currently being simulated
        return new ArrayList<>();
    }

}
