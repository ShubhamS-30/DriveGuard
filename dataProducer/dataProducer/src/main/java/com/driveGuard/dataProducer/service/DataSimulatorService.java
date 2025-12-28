package com.driveGuard.dataProducer.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import com.driveGuard.dataProducer.dto.message.TripStatusMessage;
import com.driveGuard.dataProducer.entity.Car;
import com.driveGuard.dataProducer.entity.Trip;
import com.driveGuard.dataProducer.repository.CarRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.driveGuard.dataProducer.utility.AppLogger;
import com.driveGuard.dataProducer.entity.TripRow;
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

    @Value("${data.simulation.year}")
    private Integer simulationYear;

    @Value("${data.cab.location.topic.name}")
    private String cabLocationTopicName;

    @Value("${data.cab.status.topic.name}")
    private String cabStatusTopicName;

    private final ProduceMessages produceMessages;

    private final CarRepository carRepository;

    private final TripService tripService;

    Random r;

    // Inside DataSimulatorService
    private final Map<String, TripRow> lastKnownPositions = new ConcurrentHashMap<>();
    private final Map<String, Double> currentTripDistances = new ConcurrentHashMap<>();

    public DataSimulatorService(ProduceMessages produceMessages, CarRepository carRepository, TripService tripService) {
        this.produceMessages = produceMessages;
        this.carRepository = carRepository;
        this.tripService = tripService;
        r = new Random();
    }

    public List<String> getCarMonthFoldersWithExcelFiles(String carNumber) throws TripNotFoundException {
        List<String> folderNames = new ArrayList<>();
        Path dataDir = Paths.get(dataSimulatorDirectory);
        if (!Files.exists(dataDir) || !Files.isDirectory(dataDir)) {
            throw new TripNotFoundException(String.format("Data simulator directory not found: %s", dataSimulatorDirectory));
        }
        boolean found = false;
        for (int month = 1; month <= 12; month++) {
            String monthStr = String.format("%02d", month);
            String folderName = String.format("%s_%s_%s", carNumber, simulationYear, monthStr);
            Path monthDir = dataDir.resolve(folderName);
            if (Files.exists(monthDir) && Files.isDirectory(monthDir)) {
                folderNames.add(folderName);
                found = true;
            }
        }
        if (!found) {
            throw new TripNotFoundException(String.format("No folders found for car: %s", carNumber));
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
        String folderName = String.format("%s_%s_%s", carNumber, year, paddedMonth);
        log.info(String.format("Looking for trip IDs in folder: %s", folderName));

        Path monthDir = Paths.get(dataSimulatorDirectory, folderName);

        log.info(String.format("Checking directory: %s", monthDir));

        if (!Files.exists(monthDir) || !Files.isDirectory(monthDir)) {
            throw new TripNotFoundException(String.format("Trip/vehicle folder not found: %s", folderName));
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

    public void selectTripByCarId(Integer carId) throws IOException, TripNotFoundException {
        int tripMonth = r.nextInt(12) + 1;
        String carNumber = String.format("%03d", carId); // Safer way to format car ID

        // This will prevent a trip from being marked "ended" if no data could be found.
        List<String> trips = getTripIdsForMonth(carNumber, simulationYear.toString(), Integer.toString(tripMonth));
        if (trips.isEmpty()) {
            throw new TripNotFoundException(String.format("No trips found for car %s in month %d", carNumber, tripMonth));
        }

        int tripIndex = r.nextInt(trips.size());
        String tripId = trips.get(tripIndex);
        publishTripData(carNumber, tripId, Integer.toString(tripMonth));
    }

    // Start a trip
    public void startTrip(Integer cnr, String tripNumber) {
        Optional<Car> optionalCar = carRepository.findById(cnr);
        if (optionalCar.isPresent()) {
            Car car = optionalCar.get();
            if (Boolean.TRUE.equals(car.getIsActiveTrip())) {
                throw new TripNotFoundException(String.format("Trip is already active for Car ID: %d", cnr));
            }
            car.setIsActiveTrip(true);
            car.setActiveTripNumber(tripNumber);


            // ADDING NEW TRIP TO TRIP TABLE
            Trip trip = new Trip();
            trip.setCar(car);
            trip.setTripNumber(tripNumber);
            trip.setStartTime(Instant.now().toString());
            tripService.addTrip(trip);

            carRepository.save(car);
        } else {
            throw new TripNotFoundException(String.format("Car not found with ID: %d", cnr));
        }
    }

    @Transactional
    public Car endTrip(Integer cnr) {
        Optional<Car> optionalCar = carRepository.findById(cnr);
        if (optionalCar.isPresent()) {
            Car car = optionalCar.get();
            String tripNumber = car.getActiveTripNumber();
            TripRow lastPosition = lastKnownPositions.get(tripNumber);
            Double distance = currentTripDistances.get(tripNumber);

            if (Boolean.FALSE.equals(car.getIsActiveTrip())) {
                throw new TripNotFoundException(String.format("Trip is not active for Car ID: %d", cnr));
            }

            TripStatusMessage tripStatusMessage = new TripStatusMessage();
            tripStatusMessage.setCnr(car.getCnr());
            tripStatusMessage.setTripNumber(tripNumber);
            tripStatusMessage.setTripStatus(false);
            // ENDING THE TRIP
            tripService.updateTripEndLocationAndDistance(tripNumber, lastPosition.getLatitude(), lastPosition.getLongitude(), distance);
            car.setIsActiveTrip(false);
            car.setActiveTripNumber(null);

            tripStatusMessage.setTimestamp(Instant.now().toString());
            produceMessages.produceMessageByTopic(cabStatusTopicName, tripStatusMessage);

            return carRepository.save(car);
        }
        throw new TripNotFoundException(String.format("Car not found with ID: %d", cnr));
    }

    /**
     * Reads a specific trip file and publishes its data to Kafka, simulating real-time speed.
     *
     * @param carNumber The ID of the car (e.g., "005").
     * @param tripId    The ID of the trip (e.g., "0_00001").
     * @param tripMonth The month of the trip (e.g., "2").
     * @throws IOException           If the file cannot be read.
     * @throws TripNotFoundException If the specified trip file does not exist.
     */
    @Transactional
    public void publishTripData(String carNumber, String tripId, String tripMonth) throws IOException, TripNotFoundException {
        String tripMonthFormatted = String.format("%02d", Integer.parseInt(tripMonth));
        String folderName = String.format("%s_%d_%s", carNumber, simulationYear, tripMonthFormatted);
        String fileName = tripId + ".csv";
        Path tripFile = Paths.get(dataSimulatorDirectory, folderName, fileName);
        if (!Files.exists(tripFile)) {
            log.error(String.format("Trip file not found at path: %s", tripFile), tripFile);
            throw new TripNotFoundException(String.format("Trip file not found: %s", tripFile));
        }

        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        String tripNumber = "";

        // Process the file line-by-line to avoid high memory usage.
        try (FileInputStream fis = new FileInputStream(tripFile.toFile())) {
            MappingIterator<TripRow> it = mapper.readerFor(TripRow.class).with(schema).readValues(fis);
            long totalRows = 0;
            try (Stream<String> lines = Files.lines(tripFile)) {
                totalRows = lines.count() - 1;
            }

            if (totalRows == 0) {
                throw new TripNotFoundException(String.format("Trip file is empty: %s", tripFile));
            }

            long currentRow = 0;

            tripNumber = String.format("%s_%s", LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS")
            ), tripId);

            log.info(String.format("Starting simulation for Vehicle: %s Trip: %s", carNumber, tripNumber));
            startTrip(Integer.valueOf(carNumber), tripNumber);
            TripStatusMessage tripStatusMessage = new TripStatusMessage();
            tripStatusMessage.setCnr(Integer.valueOf(carNumber));
            tripStatusMessage.setTripNumber(tripNumber);
            tripStatusMessage.setTripStatus(true);
            tripStatusMessage.setTimestamp(Instant.now().toString());
            produceMessages.produceMessageByTopic(cabStatusTopicName, tripStatusMessage);
            TripRow firstRow = null;
            TripRow lastRow = null;
            double runningDistance = 0.0;
            while (it.hasNext()) {
                TripRow row = it.next();
                currentRow++;

                if (firstRow == null) {
                    firstRow = row;
                    tripService.updateTripStartLocation(tripNumber, firstRow.getLatitude(), firstRow.getLongitude());
                }
                if (lastRow != null) {
                    runningDistance += calculateDistance(
                            Double.parseDouble(lastRow.getLatitude()), Double.parseDouble(lastRow.getLongitude()),
                            Double.parseDouble(row.getLatitude()), Double.parseDouble(row.getLongitude())
                    );
                    // Keep the maps updated for the shutdown hook
                    currentTripDistances.put(tripNumber, runningDistance);
                }
                lastRow = row;
                lastKnownPositions.put(tripNumber, lastRow);

                // Enrich the row data
                row.setCarId(carNumber);
                row.setTimestamp(Instant.now().toString());
                row.setTripNumber(tripNumber);
                double completionPercent = ((double) currentRow / totalRows) * 100;
                row.setTripCompletion(completionPercent);

                // Send the message
                produceMessages.produceMessageByTopicAndKey(cabLocationTopicName, carNumber, row);

                // Simulate the time delay
                simulateTimeDelay(Double.parseDouble(row.getTargetSpeed()));
            }
        } catch (InterruptedException e) {
            log.warn(String.format("Simulation for trip %s was interrupted.", tripId), tripId);
            Thread.currentThread().interrupt();
        } catch (NumberFormatException e) {
            log.error(String.format("Could not parse target_speed for a row in trip %s", tripId), (Path) e);
        } finally {
            endTrip(Integer.valueOf(carNumber));
            log.info(String.format("Finished simulation for Vehicle: %s Trip: %s", carNumber, tripId));
        }

    }

    /**
     * Pauses the current thread to simulate the time it would take to travel 1 meter.
     *
     * @param speedKmh The vehicle's speed in kilometers per hour.
     * @throws InterruptedException if the thread is interrupted while sleeping.
     */
    private void simulateTimeDelay(double speedKmh) throws InterruptedException {
        if (speedKmh > 0) {
            // Convert km/h to meters/second
            double speedMps = speedKmh / 3.6;
            // Calculate time (in seconds) to travel 1 meter
            double timeSeconds = 1.0 / speedMps;
            // Convert to milliseconds for Thread.sleep()
            long sleepDurationMs = (long) (timeSeconds * 1000);

            Thread.sleep(sleepDurationMs);
        } else {
            // If the car is stationary, pause for a default interval (e.g., 1 second)
            Thread.sleep(1000);
        }
    }

    // Haversine formula to calculate distance between two lat/lon points
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

}
