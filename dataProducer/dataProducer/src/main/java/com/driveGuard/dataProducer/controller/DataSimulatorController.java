package com.driveGuard.dataProducer.controller;

import java.io.IOException;
import java.util.List;

import com.driveGuard.dataProducer.dto.TripRowDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.driveGuard.dataProducer.exception.TripNotFoundException;
import com.driveGuard.dataProducer.service.DataSimulatorService;

@RestController
@RequestMapping("/data")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT})
@Tag(name = "Data Simulation", description = "Endpoints for simulating and retrieving car trip data")
public class DataSimulatorController {

    private final DataSimulatorService dataSimulatorService;

    @Autowired
    public DataSimulatorController(DataSimulatorService dataSimulatorService) {
        this.dataSimulatorService = dataSimulatorService;
    }

    @Operation(
            summary = "Get Excel Files for Car",
            description = "Retrieve a list of Excel files for a specific car number"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Excel files retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Car or files not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/car/{carNumber}/files")
    public List<String> getExcelFiles(@PathVariable String carNumber) throws TripNotFoundException {
        return dataSimulatorService.getCarMonthFoldersWithExcelFiles(carNumber);
    }

    @Operation(
            summary = "Get Trip Details",
            description = "Retrieve the first 20 rows of trip details for a specific car, year, month, and trip ID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Trip details retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Trip not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/car/{carNumber}/trip/{year}/{month}/{tripId}")
    public List<TripRowDTO> getTripDetails(
            @PathVariable String carNumber,
            @PathVariable String year,
            @PathVariable String month,
            @PathVariable String tripId) throws IOException, TripNotFoundException {

        // Compose folder and file names
        String folderName = carNumber + "_" + year + "_" + String.format("%02d", Integer.parseInt(month));
        String fileName = "0_" + String.format("%05d", Integer.parseInt(tripId)) + ".csv";

        return dataSimulatorService.getTripDetailsFirst20Rows(folderName, fileName);
    }

    @Operation(
            summary = "Get Trip IDs for Month",
            description = "Retrieve a list of trip IDs for a specific car number, year, and month"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Trip IDs retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Car or trips not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/car/{carNumber}/trips/{year}/{month}")
    public List<String> getTripIdsForMonth(
            @PathVariable String carNumber,
            @PathVariable String year,
            @PathVariable String month) throws IOException, TripNotFoundException {
        return dataSimulatorService.getTripIdsForMonth(carNumber, year, month);
    }
}
