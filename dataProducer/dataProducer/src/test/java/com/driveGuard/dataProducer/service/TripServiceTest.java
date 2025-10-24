package com.driveGuard.dataProducer.service;

import com.driveGuard.dataProducer.dto.TripDTO;
import com.driveGuard.dataProducer.entity.Trip;
import com.driveGuard.dataProducer.exception.TripNotFoundException;
import com.driveGuard.dataProducer.repository.TripRepository;
import com.driveGuard.dataProducer.utility.Mapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {
    // Creates a mock instance of the TripRepository
    @Mock
    private TripRepository tripRepository;

    // Creates a mock instance of the Mapper
    @Mock
    private Mapper mapper;

    // Injects the mocks (@Mock) into the TripService
    @InjectMocks
    private TripService tripService;

    // Reusable test data
    private Trip testTrip;
    private TripDTO testTripDTO;

    @BeforeEach
    void setUp() {
        // Initialize our test objects before each test
        testTrip = new Trip();
        testTrip.setTripId(1);
        testTrip.setTripNumber("TRIP-123");
        testTrip.setStartTime(Instant.now().toString());

        testTripDTO = new TripDTO(); // Assuming TripDTO has similar fields
        testTripDTO.setTripNumber("TRIP-123");
        testTripDTO.setStartTime(testTrip.getStartTime());
    }

// --- Test Cases for addTrip ---

    @Test
    void testAddTrip_shouldSaveAndReturnDTO() {
        // --- Given (Arrange) ---
        // When tripRepository.save() is called with any Trip, return our testTrip
        when(tripRepository.save(any(Trip.class))).thenReturn(testTrip);
        // When mapper.tripTOTripDTO() is called with testTrip, return our testTripDTO
        when(mapper.tripTOTripDTO(testTrip)).thenReturn(testTripDTO);

        // --- When (Act) ---
        TripDTO result = tripService.addTrip(new Trip());

        // --- Then (Assert) ---
        assertNotNull(result); // The result should not be null
        assertEquals("TRIP-123", result.getTripNumber()); // The DTO should match
        verify(tripRepository, times(1)).save(any(Trip.class)); // Verify save was called once
        verify(mapper, times(1)).tripTOTripDTO(testTrip); // Verify mapper was called once
    }

// --- Test Cases for getTripByTripNumber ---

    @Test
    void testGetTripByTripNumber_whenTripExists_shouldReturnDTO() {
        // --- Given ---
        String tripNumber = "TRIP-123";
        // Mock the repository to return our testTrip
        when(tripRepository.getTripByTripNumber(tripNumber)).thenReturn(Optional.of(testTrip));
        when(mapper.tripTOTripDTO(testTrip)).thenReturn(testTripDTO);

        // --- When ---
        TripDTO result = tripService.getTripByTripNumber(tripNumber);

        // --- Then ---
        assertNotNull(result);
        assertEquals(tripNumber, result.getTripNumber());
        verify(tripRepository, times(1)).getTripByTripNumber(tripNumber);
    }

    @Test
    void testGetTripByTripNumber_whenTripNotFound_shouldThrowException() {
        // --- Given ---
        String tripNumber = "NOT-FOUND";
        // Mock the repository to return an empty Optional
        when(tripRepository.getTripByTripNumber(tripNumber)).thenReturn(Optional.empty());

        // --- When & Then ---
        // Assert that the specified exception is thrown
        TripNotFoundException exception = assertThrows(
                TripNotFoundException.class,
                () -> tripService.getTripByTripNumber(tripNumber)
        );

        // Verify the exception message is correct
        assertEquals("TRIP WITH ID : NOT-FOUND NOT FOUND.", exception.getMessage());
        // Verify the mapper was never called
        verify(mapper, never()).tripTOTripDTO(any());
    }

// --- Test Cases for updateTripEndTime ---

    @Test
    void testUpdateTripEndTime_whenTripExists_shouldUpdateAndReturnDTO() {
        // --- Given ---
        String tripNumber = "TRIP-123";
        // The trip initially has no end time
        testTrip.setEndTime(null);

        // Mock the repository
        when(tripRepository.getTripByTripNumber(tripNumber)).thenReturn(Optional.of(testTrip));
        when(tripRepository.save(any(Trip.class))).thenReturn(testTrip);
        when(mapper.tripTOTripDTO(testTrip)).thenReturn(testTripDTO);

        // --- When ---
        TripDTO result = tripService.updateTripEndTime(tripNumber);

        // --- Then ---
        assertNotNull(result);

        // Use an ArgumentCaptor to capture the object that was passed to .save()
        ArgumentCaptor<Trip> tripCaptor = ArgumentCaptor.forClass(Trip.class);
        verify(tripRepository, times(1)).save(tripCaptor.capture());

        // Get the captured trip and verify its endTime was set
        Trip savedTrip = tripCaptor.getValue();
        assertNotNull(savedTrip.getEndTime()); // Verify the end time is no longer null
    }

    @Test
    void testUpdateTripEndTime_whenTripNotFound_shouldThrowException() {
        // --- Given ---
        String tripNumber = "NOT-FOUND";
        when(tripRepository.getTripByTripNumber(tripNumber)).thenReturn(Optional.empty());

        // --- When & Then ---
        TripNotFoundException exception = assertThrows(
                TripNotFoundException.class,
                () -> tripService.updateTripEndTime(tripNumber)
        );

        assertEquals("TRIP WITH ID : NOT-FOUND NOT FOUND.", exception.getMessage());
        // Verify that save and map were never called
        verify(tripRepository, never()).save(any());
        verify(mapper, never()).tripTOTripDTO(any());
    }
}