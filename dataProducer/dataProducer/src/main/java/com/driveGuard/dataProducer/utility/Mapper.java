package com.driveGuard.dataProducer.utility;

import com.driveGuard.dataProducer.dto.CarDTO;
import com.driveGuard.dataProducer.dto.TripDTO;
import com.driveGuard.dataProducer.dto.TripRowDTO;
import com.driveGuard.dataProducer.entity.Car;
import com.driveGuard.dataProducer.entity.Trip;
import com.driveGuard.dataProducer.entity.TripRow;
import com.driveGuard.dataProducer.service.CarService;
import com.driveGuard.dataProducer.service.DataSimulatorService;
import org.springframework.stereotype.Service;

@Service
public class Mapper {


    Mapper() {
    }

    public TripRowDTO TripRowToTripRowDTO(TripRow tripRow){
        TripRowDTO tripRowDTO = new TripRowDTO();
        tripRowDTO.setLatitude(tripRow.getLatitude());
        tripRowDTO.setLongitude(tripRow.getLongitude());
        tripRowDTO.setTimestamp(tripRow.getTimestamp());
        tripRowDTO.setTargetSpeed(tripRow.getTargetSpeed());
        tripRowDTO.setWayMaxspeed(tripRow.getWayMaxspeed());
        tripRowDTO.setSpeedOsrm(tripRow.getSpeedOsrm());
        tripRowDTO.setElevation(tripRow.getElevation());
        tripRowDTO.setFwdAzimuth(tripRow.getFwdAzimuth());
        tripRowDTO.setWayType(tripRow.getWayType());
        tripRowDTO.setWaySurface(tripRow.getWaySurface());
        tripRowDTO.setNodeIntersection(tripRow.getNodeIntersection());
        tripRowDTO.setNodeRailway(tripRow.getNodeRailway());
        tripRowDTO.setNodeCrossing(tripRow.getNodeCrossing());
        tripRowDTO.setNodeHighway(tripRow.getNodeHighway());
        tripRowDTO.setNodeStop(tripRow.getNodeStop());
        tripRowDTO.setStartStop(tripRow.getStartStop());
        tripRowDTO.setAzimuthDiff(tripRow.getAzimuthDiff());
        tripRowDTO.setElevationDiff(tripRow.getElevationDiff());
        tripRowDTO.setCarId(tripRow.getCarId());
        tripRowDTO.setTripNumber(tripRow.getTripNumber());
        tripRowDTO.setTripCompletion(tripRow.getTripCompletion());
        return tripRowDTO;
    }

    public CarDTO carToCarDTO(Car car){
        CarDTO carDTO = new CarDTO();
        carDTO.setCnr(car.getCnr());
        carDTO.setManufacturer(car.getManufacturer());
        carDTO.setModel(car.getModel());
        carDTO.setFuel(car.getFuel());
        carDTO.setPowerKw(car.getPowerKw());
        carDTO.setTransmission(car.getTransmission());
        carDTO.setWeightKg(car.getWeightKg());
        carDTO.setIsActiveTrip(car.getIsActiveTrip());
        carDTO.setActiveTripNumber(carDTO.getActiveTripNumber());
        return carDTO;
    }

    public TripDTO tripTOTripDTO(Trip trip){
        TripDTO tripDTO = new TripDTO();
        tripDTO.setTripId(trip.getTripId());
        tripDTO.setTripNumber(trip.getTripNumber());
        tripDTO.setCar(carToCarDTO(trip.getCar()));
        tripDTO.setStartTime(trip.getStartTime());
        tripDTO.setEndTime(trip.getEndTime());
        return tripDTO;
    }
}
