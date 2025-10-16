package com.driveGuard.dataProducer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ExecutorConfig {

    @Bean
    public ExecutorService executorService() {
        // You can adjust the pool size or use a cached thread pool as needed
        return Executors.newCachedThreadPool();
    }
}
