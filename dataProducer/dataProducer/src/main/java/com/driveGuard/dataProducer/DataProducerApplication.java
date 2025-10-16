package com.driveGuard.dataProducer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DataProducerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DataProducerApplication.class, args);
	}

}
