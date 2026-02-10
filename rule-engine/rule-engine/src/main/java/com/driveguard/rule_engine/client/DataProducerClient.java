package com.driveguard.rule_engine.client;

import com.driveguard.rule_engine.AppLogger;
import com.driveguard.rule_engine.dto.CarResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Client for making external API calls to the Data Producer Microservice.
 * Handles communication with the Data Producer controllers for car and trip information.
 */
@Component
public class DataProducerClient {

    private static final AppLogger logger = AppLogger.getLogger(DataProducerClient.class);

    private final RestTemplate restTemplate;
    private final String dataProducerBaseUrl;

    public DataProducerClient(RestTemplate restTemplate, @Value("${data.producer.base.url}") String dataProducerBaseUrl) {
        this.restTemplate = restTemplate;
        this.dataProducerBaseUrl = dataProducerBaseUrl;
    }

    /**
     * Get Car by Trip Number from the Data Producer API
     *
     * @param tripNumber The trip number to query
     * @return CarResponseDTO containing car information
     * @throws RestClientException if the API call fails
     */
    public CarResponseDTO getCarByTripNumber(String tripNumber) {
        try {
            String url = String.format("%s/cars/trip/%s/car", dataProducerBaseUrl, tripNumber);
            logger.info(String.format("Calling Data Producer API: GET %s", url));

            ResponseEntity<CarResponseDTO> response = restTemplate.getForEntity(url, CarResponseDTO.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                logger.info(String.format("Successfully retrieved car data for trip number: %s", tripNumber));
                return response.getBody();
            } else {
                logger.warn(String.format("Unexpected response status from Data Producer API for trip: %s", tripNumber));
                return null;
            }
        } catch (RestClientException e) {
            logger.error(String.format("Error calling Data Producer API for trip number: %s", tripNumber), e);
            throw e;
        }
    }

    /**
     * Get Car by Car ID from the Data Producer API
     *
     * @param carId The car ID to query
     * @return CarResponseDTO containing car information
     * @throws RestClientException if the API call fails
     */
    public CarResponseDTO getCarById(Integer carId) {
        try {
            String url = String.format("%s/cars/%d", dataProducerBaseUrl, carId);
            logger.info(String.format("Calling Data Producer API: GET %s", url));

            ResponseEntity<CarResponseDTO> response = restTemplate.getForEntity(url, CarResponseDTO.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                logger.info(String.format("Successfully retrieved car data for car ID: %d", carId));
                return response.getBody();
            } else {
                logger.warn(String.format("Unexpected response status from Data Producer API for car ID: %d", carId));
                return null;
            }
        } catch (RestClientException e) {
            logger.error(String.format("Error calling Data Producer API for car ID: %d", carId), e);
            throw e;
        }
    }
}
