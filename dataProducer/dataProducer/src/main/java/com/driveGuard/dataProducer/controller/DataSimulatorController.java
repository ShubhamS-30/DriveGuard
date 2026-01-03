package com.driveGuard.dataProducer.controller;

import java.io.IOException;
import java.util.List;

import com.driveGuard.dataProducer.dto.TripRowDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.driveGuard.dataProducer.entity.TripRow;
import com.driveGuard.dataProducer.exception.TripNotFoundException;
import com.driveGuard.dataProducer.service.DataSimulatorService;

@RestController
@RequestMapping("/data")
public class DataSimulatorController {

    private final DataSimulatorService dataSimulatorService;

    @Autowired
    public DataSimulatorController(DataSimulatorService dataSimulatorService) {
        this.dataSimulatorService = dataSimulatorService;
    }

    @GetMapping("/car/{carNumber}/files")
    public List<String> getExcelFiles(@PathVariable String carNumber) throws TripNotFoundException {
        return dataSimulatorService.getCarMonthFoldersWithExcelFiles(carNumber);
    }

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

    @GetMapping("/car/{carNumber}/trips/{year}/{month}")
    public List<String> getTripIdsForMonth(
            @PathVariable String carNumber,
            @PathVariable String year,
            @PathVariable String month) throws IOException, TripNotFoundException {
        return dataSimulatorService.getTripIdsForMonth(carNumber, year, month);
    }
}
