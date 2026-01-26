# DriveGuard - Real-time Driver Safety Monitoring System

A sophisticated microservices-based platform for monitoring vehicle telemetry data, detecting driving anomalies, and generating real-time alerts. The system uses Apache Kafka for event streaming, WebSocket for real-time data broadcasting, and a rule-based engine for anomaly detection.

## 📋 Table of Contents

- [System Architecture](#system-architecture)
- [Microservices Overview](#microservices-overview)
  - [Data Producer Microservice](#data-producer-microservice)
  - [Rule Engine Microservice](#rule-engine-microservice)
- [Data Flow](#data-flow)
- [Prerequisites](#prerequisites)
- [Installation & Setup](#installation--setup)
- [Configuration](#configuration)
- [API Endpoints](#api-endpoints)
- [WebSocket Endpoints](#websocket-endpoints)
- [Kafka Topics](#kafka-topics)
- [Database Schema](#database-schema)
- [Running the Application](#running-the-application)
- [Project Structure](#project-structure)
- [Key Components](#key-components)
- [Logging](#logging)
- [License](#license)
- [Support & Contributing](#support--contributing)

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         DriveGuard System                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────────────┐    ┌──────────────────────────┐   │
│  │   Data Producer MS       │    │   Rule Engine MS         │   │
│  │   (Port: 8080)           │    │   (Port: 8082)           │   │
│  │                          │    │                          │   │
│  │  • Data Simulation       │    │  • Kafka Consumers       │   │
│  │  • CSV Data Loading      │    │  • Rule Evaluation       │   │
│  │  • Trip Management       │    │  • Alert Generation      │   │
│  │  • Kafka Publishing      │    │  • WebSocket Broadcasting│   │
│  └────────┬─────────────────┘    └────────┬─────────────────┘   │
│           │                               │                     │
│           ├──────────────────┬────────────┘                     │
│           │                  │                                  │
│      ┌────▼─────┐      ┌─────▼────────┐                         │
│      │  Kafka   │      │   MySQL      │                         │
│      │  Broker  │      │   Database   │                         │
│      └────┬─────┘      └──────────────┘                         │
│           │                  ▲                                  │
│           └──────────────────┘                                  │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Frontend / Client Applications                          │   │
│  │  • WebSocket connections for real-time updates           │   │
│  │  • REST API calls for data retrieval                     │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Microservices Overview

### Data Producer Microservice

**Purpose:** Simulates vehicle telemetry data and publishes events to Kafka topics.

**Key Responsibilities:**
- Loads trip data from CSV files
- Simulates real-time vehicle movements with telemetry information
- Publishes location events to the `trip-locations` Kafka topic
- Publishes trip status changes to the `trip-status` Kafka topic
- Manages car and trip information in the database

**Technology Stack:**
- Spring Boot 3.5.4
- Apache Kafka
- MySQL
- Spring Data JPA/Hibernate
- Spring Validation

**Key Services:**
1. **DataSimulatorService** - Simulates vehicle trips and publishes location/status events
2. **TripService** - Manages trip lifecycle and persistence
3. **CarService** - Manages vehicle information
4. **ProduceMessages** - Handles Kafka message publishing

---

### Rule Engine Microservice

**Purpose:** Consumes vehicle telemetry, evaluates safety rules, and generates real-time alerts.

**Key Responsibilities:**
- Consumes location data from the `trip-locations` Kafka topic
- Consumes trip status updates from the `trip-status` Kafka topic
- Evaluates driving rules against telemetry data
- Generates and persists safety alerts
- Broadcasts real-time location updates via WebSocket
- Manages active trip cache and cleanup of stale data

**Technology Stack:**
- Spring Boot 3.5.6
- Apache Kafka
- MySQL
- Spring Data JPA/Hibernate
- WebSocket for real-time communication
- Redis (for caching active trips)
- Spring Scheduling

**Key Services:**
1. **LocationStreamService** - Consumes Kafka events and orchestrates data flow
2. **RuleEngineService** - Evaluates driving rules and generates alerts
3. **AlertCacheService** - Manages active trips cache and cleanup
4. **AlertPersistenceService** - Persists alerts to database
5. **LocationWebSocketHandler** - Handles WebSocket connections and message broadcasting
6. **RuleStrategy implementations** - SpeedingRule, DangerousTurningRule

---

## Data Flow

### Typical Event Lifecycle

```
1. Data Producer Starts Simulation
   ├─ Loads CSV data for a vehicle
   ├─ Creates Trip record
   └─ Emits TripStatusMessage (START)
      └─ Published to: trip-status topic

2. Location Event Published
   ├─ DataSimulatorService publishes TripRow
   └─ Published to: trip-locations topic

3. Rule Engine Receives Location Event
   ├─ LocationStreamService consumes from trip-locations
   ├─ Updates active trip heartbeat in cache
   ├─ Broadcasts to WebSocket clients
   ├─ RuleEngineService evaluates rules
   ├─ If rule violated → Generate Alert
   └─ Alert persisted to database

4. Trip Ends
   ├─ DataSimulatorService publishes TripStatusMessage (END)
   ├─ Published to: trip-status topic
   ├─ LocationStreamService consumes status message
   ├─ Closes WebSocket connections for the carId
   └─ Removes trip from active cache
```

### Data Structure Examples

**TripRow (Location Event)**
```json
{
  "tripNumber": "TRIP_20210315_001",
  "carId": "001",
  "latitude": "28.6139",
  "longitude": "77.2090",
  "targetSpeed": "45.5",
  "wayMaxspeed": "50",
  "speedOsrm": "45.2",
  "elevation": "85.5",
  "fwdAzimuth": "120.5",
  "wayType": "residential",
  "waySurface": "asphalt",
  "timestamp": "2021-03-15T10:30:45Z"
}
```

**TripStatusMessage**
```json
{
  "tripNumber": "TRIP_20210315_001",
  "cnr": 1,
  "tripStatus": false,  // false = TRIP_ENDED
  "timestamp": "2021-03-15T11:30:00Z"
}
```

**Alert**
```json
{
  "id": 1,
  "tripNumber": "TRIP_20210315_001",
  "carId": "001",
  "alertType": "SPEEDING",
  "severity": "HIGH",
  "description": "Speed exceeded maximum limit",
  "location": {
    "latitude": "28.6139",
    "longitude": "77.2090"
  },
  "currentSpeed": "75.5",
  "speedLimit": "50",
  "timestamp": "2021-03-15T10:30:45Z"
}
```

---

## Prerequisites

- **Java 17+** (for Data Producer: Java 21 recommended, for Rule Engine: Java 17)
- **Apache Kafka 3.x+** with running Kafka broker
- **MySQL 8.0+** database
- **Maven 3.8+** for building the project
- **Optional:** Redis for advanced caching features
- **Git** for cloning the repository

---

## Installation & Setup

### 1. Clone the Repository

```bash
git clone https://github.com/ShubhamS-30/DriveGuard.git
cd DriveGuard
```

### 2. Database Setup

Create the MySQL database and tables:

```sql
CREATE DATABASE IF NOT EXISTS driveguard;
USE driveguard;

-- Tables will be automatically created by Hibernate DDL (ddl-auto: update)
```

### 3. Start Kafka

Ensure Kafka is running:

```bash
# Start Zookeeper
bin/zookeeper-server-start.sh config/zookeeper.properties

# In another terminal, start Kafka broker
bin/kafka-server-start.sh config/server.properties

# Or for Windows
.\bin\windows\kafka-server-start.bat  .\config\server.properties
```

Create the required Kafka topics:

```bash
kafka-topics.sh --create --topic trip-locations --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics.sh --create --topic trip-status --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
kafka-topics.sh --create --topic anomaly-alerts --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1

# For Windows use kafka-topics.bat
.\bin\windows\kafka-topics.bat --create --topic my-first-topic --partitions 1 --replication-factor 1 --bootstrap-server localhost:9092
```

### 4. Build the Microservices

**Data Producer:**
```bash
cd dataProducer/dataProducer
mvn clean install
```

**Rule Engine:**
```bash
cd rule-engine/rule-engine
mvn clean install
```

---

## Configuration

### Data Producer Configuration

**File:** `dataProducer/dataProducer/src/main/resources/application.properties`

```properties
# Application
spring.application.name=dataProducer

# Data Simulator Settings
data.simulator.directory=C:/path/to/data/directory
data.simulation.year=2021
data.simulation.max.concurrent.trips=5
data.simulation.chance.of.trip=0.40

# Kafka Topics
data.cab.status.topic.name=trip-status
data.cab.location.topic.name=trip-locations

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/driveguard
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect

# Kafka
spring.kafka.bootstrap-servers=localhost:9092
```

### Rule Engine Configuration

**File:** `rule-engine/rule-engine/src/main/resources/application.properties`

```properties
# Application
spring.application.name=rule-engine

# Kafka Topics
data.cab.status.topic.name=trip-status
data.cab.location.topic.name=trip-locations
data.cab.alert.topic.name=anomaly-alerts

# Alert Batch Configuration
data.alerts.batch.size=5

# Server
server.port=8082

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/driveguard
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
spring.jpa.properties.hibernate.jdbc.batch_size=5
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true

# Kafka
spring.kafka.bootstrap-servers=localhost:9092
```

---

## API Endpoints

### Data Producer Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/data/car/{carNumber}/files` | Get Excel files for a specific car |
| GET | `/data/trip/{carNumber}/{year}/{month}/{tripId}` | Get trip details (first 20 rows) |
| POST | `/simulation/start/{carNumber}/{year}/{month}/{tripId}` | Start trip simulation |
| POST | `/simulation/stop/{tripNumber}` | Stop trip simulation |
| GET | `/cars` | Get all registered cars |

### Rule Engine Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/location-stream/status` | Get location streaming service status |

**Location Stream Status Response:**
```json
{
  "service": "location-stream",
  "status": "running",
  "totalActiveConnections": 5,
  "connectionsByCarId": {
    "001": 2,
    "002": 3
  },
  "websocketEndpointsAll": "ws://localhost:8082/ws/locations",
  "websocketEndpointsByCarId": "ws://localhost:8082/ws/locations/{carId}",
  "kafkaTopic": "trip-locations"
}
```

---

## WebSocket Endpoints

The Rule Engine provides real-time location streaming via WebSocket.

### Available WebSocket URLs

**1. Stream all vehicle locations:**
```
ws://localhost:8082/ws/locations
```

**2. Stream locations for a specific car:**
```
ws://localhost:8082/ws/locations/{carId}
```
### Message Format

Messages sent via WebSocket contain the complete TripRow object as JSON:

```json
{
  "tripNumber": "TRIP_20210315_001",
  "carId": "001",
  "latitude": "28.6139",
  "longitude": "77.2090",
  "targetSpeed": "45.5",
  "wayMaxspeed": "50",
  "speedOsrm": "45.2",
  "elevation": "85.5",
  "timestamp": "2021-03-15T10:30:45Z"
}
```

### Connection Lifecycle

- **Connection Established:** Client connects to WebSocket endpoint
- **Streaming:** Real-time location updates received
- **Auto-Close:** When a trip ends (trip-status = false), connections for that carId are automatically closed
- **Manual Close:** Client can close connection at any time

---

## Kafka Topics

### 1. trip-locations

**Purpose:** Real-time vehicle location and telemetry data

**Message Format:** TripRow (JSON)

**Partitions:** 3 (for parallelism)

**Consumers:**
- LocationStreamService (Rule Engine) - Broadcasts to WebSocket clients
- RuleEngineService (Rule Engine) - Evaluates driving rules

**Frequency:** High frequency (typically 10+ messages per second per vehicle)

### 2. trip-status

**Purpose:** Trip lifecycle events (start/end)

**Message Format:** TripStatusMessage (JSON)

**Partitions:** 1 (ordering matters)

**Consumers:**
- LocationStreamService (Rule Engine) - Manages connections and caches

**Events:**
- `tripStatus = true` - Trip started
- `tripStatus = false` - Trip ended

### 3. anomaly-alerts

**Purpose:** Generated safety alerts

**Message Format:** Alert (JSON)

**Partitions:** 1 (ordering matters)

**Producers:**
- RuleEngineService (Rule Engine)

**Consumers:**
- External alert systems, dashboards, etc.

---

## Database Schema

### Key Tables

#### rule_config
Stores configuration for safety rules:

```sql
CREATE TABLE driveguard.rule_config (
	id bigint unsigned NOT NULL,
	rule_name varchar(255) NULL,
	display_name varchar(255) NULL,
	is_enabled tinyint(1) NULL,
	description varchar(255) NULL
);

INSERT INTO driveguard.rule_config
(id, rule_name, display_name, is_enabled, description)
VALUES(1, 'SpeedingRule', 'SpeedingRule', 1, 'Check for overspeeding');
INSERT INTO driveguard.rule_config
(id, rule_name, display_name, is_enabled, description)
VALUES(2, 'DangerousTurningRule', 'DangerousTurningRule', 1, 'Detect aggressive swerving or sharp turns');
```

#### trip_alerts
Stores detected safety alerts:

```sql
CREATE TABLE trip_alerts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  trip_number VARCHAR(255),
  car_id VARCHAR(10),
  alert_type VARCHAR(50),
  severity VARCHAR(20),
  description TEXT,
  latitude DECIMAL(10,6),
  longitude DECIMAL(10,6),
  current_speed DECIMAL(5,2),
  speed_limit DECIMAL(5,2),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### cars
Vehicle information:

```sql
CREATE TABLE driveguard.car (
	cnr int NOT NULL,
	fuel varchar(255) NULL,
	manufacturer varchar(255) NULL,
	model varchar(255) NULL,
	power_kw int NULL,
	transmission varchar(255) NULL,
	weight_kg int NULL,
	active_trip bit(1) NULL,
	active_trip_number varchar(255) NULL
);

INSERT INTO driveguard.car
(cnr, fuel, manufacturer, model, power_kw, transmission, weight_kg, active_trip, active_trip_number)
VALUES(2, 'gasoline', 'Skoda', 'Fabia', 81, 'Manual', 1312, 0, NULL);
INSERT INTO driveguard.car
(cnr, fuel, manufacturer, model, power_kw, transmission, weight_kg, active_trip, active_trip_number)
VALUES(3, 'gasoline', 'Seat', 'Ibiza', 110, 'Manual', 1376, 0, NULL);
INSERT INTO driveguard.car
(cnr, fuel, manufacturer, model, power_kw, transmission, weight_kg, active_trip, active_trip_number)
VALUES(4, 'gasoline', 'Skoda', 'Octavia', 140, 'Automatic', 1728, 0, NULL);
INSERT INTO driveguard.car
(cnr, fuel, manufacturer, model, power_kw, transmission, weight_kg, active_trip, active_trip_number)
VALUES(5, 'gasoline', 'Skoda', 'Octavia', 110, 'Manual', 1376, 0, NULL);
INSERT INTO driveguard.car
(cnr, fuel, manufacturer, model, power_kw, transmission, weight_kg, active_trip, active_trip_number)
VALUES(6, 'gasoline', 'Skoda', 'Karoq', 110, 'Manual', 1632, 0, NULL);
```

#### trips
Trip records:

```sql
CREATE TABLE trips (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  trip_number VARCHAR(255) UNIQUE NOT NULL,
  car_id BIGINT,
  start_time TIMESTAMP,
  end_time TIMESTAMP,
  status VARCHAR(20),
  FOREIGN KEY (car_id) REFERENCES cars(id)
);
```

---

## Running the Application

### 1. Start Data Producer

```bash
cd dataProducer/dataProducer
mvn spring-boot:run
```

Expected output:
```
Started DataProducerApplication in X seconds
```

### 2. Start Rule Engine

```bash
cd rule-engine/rule-engine
mvn spring-boot:run
```

Expected output:
```
Started RuleEngineApplication in X seconds
Tomcat started on port 8082
```

### 3. Monitor Services

Check service status:

```bash
# Data Producer health
curl http://localhost:8080/actuator/health

# Rule Engine health
curl http://localhost:8082/api/location-stream/status
```

### 4. Start a Trip Simulation

```bash
curl -X POST "http://localhost:8080/simulation/start/001/2021/03/1"
```

### 5. Monitor Locations via WebSocket

```bash
# Using wscat
npm install -g wscat

# Connect to all locations
wscat -c ws://localhost:8082/ws/locations

# Or connect to specific car
wscat -c ws://localhost:8082/ws/locations/001
```

---

## Project Structure

```
DriveGuard/
│
├── dataProducer/
│   └── dataProducer/
│       ├── src/
│       │   ├── main/
│       │   │   ├── java/com/driveGuard/dataProducer/
│       │   │   │   ├── DataProducerApplication.java
│       │   │   │   ├── config/          # Configuration classes
│       │   │   │   ├── controller/      # REST controllers
│       │   │   │   ├── dto/             # Data transfer objects
│       │   │   │   ├── entity/          # JPA entities (Car, Trip, TripRow)
│       │   │   │   ├── exception/       # Custom exceptions
│       │   │   │   ├── repository/      # Spring Data repositories
│       │   │   │   ├── service/         # Business logic
│       │   │   │   │   ├── DataSimulatorService.java
│       │   │   │   │   ├── TripService.java
│       │   │   │   │   ├── CarService.java
│       │   │   │   │   └── ProduceMessages.java
│       │   │   │   └── utility/         # Utility classes (AppLogger, Mapper)
│       │   │   └── resources/
│       │   │       └── application.properties
│       │   └── test/
│       └── pom.xml
│
├── rule-engine/
│   └── rule-engine/
│       ├── src/
│       │   ├── main/
│       │   │   ├── java/com/driveguard/rule_engine/
│       │   │   │   ├── RuleEngineApplication.java
│       │   │   │   ├── AppLogger.java            # Custom logging
│       │   │   │   ├── Mapper.java               # Object mapping
│       │   │   │   ├── config/                   # Configuration classes
│       │   │   │   ├── controller/               # REST controllers
│       │   │   │   │   └── LocationStreamController.java
│       │   │   │   ├── dto/                      # DTOs (Alert, TripRow, etc.)
│       │   │   │   ├── entity/                   # JPA entities (RuleConfig, TripAlerts)
│       │   │   │   ├── exception/                # Custom exceptions
│       │   │   │   ├── repository/               # Spring Data repositories
│       │   │   │   ├── rules/                    # Rule implementations
│       │   │   │   │   ├── RuleStrategy.java     # Interface
│       │   │   │   │   ├── SpeedingRule.java
│       │   │   │   │   └── DangerousTurningRule.java
│       │   │   │   ├── service/                  # Business logic
│       │   │   │   │   ├── LocationStreamService.java
│       │   │   │   │   ├── RuleEngineService.java
│       │   │   │   │   ├── AlertCacheService.java
│       │   │   │   │   ├── AlertPersistenceService.java
│       │   │   │   │   └── TripAlertsStorageService.java
│       │   │   │   └── websocket/
│       │   │   │       └── LocationWebSocketHandler.java
│       │   │   └── resources/
│       │   │       └── application.properties
│       │   └── test/
│       └── pom.xml
│
└── README.md (this file)
```

---

## Key Components

### Data Producer

**DataSimulatorService**
- Loads CSV data from filesystem
- Simulates vehicle movements at configurable speeds
- Publishes location events to Kafka in real-time
- Manages trip lifecycle
- Publishes trip status changes

**ProduceMessages**
- Kafka producer for trip-locations and trip-status topics
- Handles serialization of domain objects

**TripService & CarService**
- Manage persistence of trips and cars
- Provide repository operations

### Rule Engine

**LocationStreamService**
- Kafka consumer for trip-locations and trip-status topics
- Orchestrates data flow between Kafka and WebSocket
- Manages active trip lifecycle
- Triggers alert generation

**RuleEngineService**
- Evaluates safety rules against vehicle telemetry
- Uses strategy pattern for extensible rule implementation
- Returns Alert objects when rules are violated

**LocationWebSocketHandler**
- Handles WebSocket connection/disconnection
- Segregates connections by carId
- Broadcasts location updates in real-time
- Closes connections when trips end

**AlertCacheService**
- Maintains cache of active trips in Redis
- Implements automatic cleanup of stale trips
- Optimizes database queries by tracking active trips

**AlertPersistenceService**
- Batches alerts for efficient database writes
- Implements transaction management

### Rules

**SpeedingRule**
- Detects when vehicle exceeds maximum speed limit
- Compares actual speed against wayMaxspeed

**DangerousTurningRule**
- Detects sharp turns or dangerous angular changes
- Analyzes fwdAzimuth changes between consecutive points

---

## Logging

The application uses a custom **AppLogger** utility instead of SLF4J for consistency.

**Usage:**
```java
private static final AppLogger log = AppLogger.getLogger(MyClass.class);

// Logging methods
log.info(String.format("Trip %s started", tripNumber));
log.warn(String.format("High speed detected: %s km/h", speed));
log.error(String.format("Error in processing: %s", message), exception);
log.debug(String.format("Debug info: %s", details));
```

**Note:** All string formatting should use `String.format()` for consistency.

---

## Performance Optimization

### Data Producer- **Batch Publishing:** Location data is published in batches to reduce overhead
- **Concurrent Trips:** Configurable max concurrent trips to prevent resource exhaustion
- **In-Memory Caching:** Last known positions cached to avoid redundant lookups

### Rule Engine
- **Kafka Partitioning:** trip-locations topic partitioned for parallel consumption
- **Batch Alert Persistence:** Alerts batched (size = 5) before database write
- **Redis Caching:** Active trips cached to avoid database lookups for every event
- **Scheduled Cleanup:** Ghost trip cleanup runs every 30 minutes
- **WebSocket Segregation:** Connections organized by carId for efficient broadcasting

---

## Monitoring & Debugging

### Check Kafka Messages

```bash
# Consume from trip-locations topic
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic trip-locations 

# Consume from trip-status topic
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic trip-status 

# Consume from anomaly-alerts topic
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic anomaly-alerts 

# for windows use kafka-console-consumer.bat
.\bin\windows\kafka-console-consumer.bat --bootstrap-server localhost:9092 --topic test-topic
```

### Check Database

```sql
-- View all generated alerts
SELECT * FROM trip_alerts ORDER BY created_at DESC LIMIT 20;

-- Count alerts by type
SELECT alert_type, COUNT(*) as count FROM trip_alerts GROUP BY alert_type;
```

### Check WebSocket Connections

Use the location stream status endpoint:
```bash
curl http://localhost:8082/api/location-stream/status
```

---

## Future Enhancements

- [ ] Add more rule types (Lane Departure, Harsh Acceleration, etc.)
- [ ] Implement user authentication and authorization
- [ ] Add historical analytics and reporting
- [ ] Implement geofencing features
- [ ] Implement machine learning-based anomaly detection
- [ ] Add real-time visualization dashboard
- [ ] Implement alert notification system (SMS, Email)

---

## License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

### MIT License Summary

You are free to:
- ✅ Use this software for any purpose (commercial or private)
- ✅ Modify and create derivatives
- ✅ Distribute the software
- ✅ Use the software privately

Under the conditions:
- ℹ️ Include a copy of the license and copyright notice with any distribution
- ℹ️ Provide a copy of the license to anyone who receives the software

The software is provided "as is" without warranty or liability.

### Full License Text

```
MIT License

Copyright (c) 2026 DriveGuard Contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## Support & Contributing

For issues, questions, or contributions, please:

1. **Report Issues:** Open an issue with detailed description and reproduction steps
2. **Submit Features:** Create a pull request with proposed enhancements
3. **Questions:** Check existing documentation or open a discussion

### Contributing Guidelines

When contributing to this project:

1. Follow the established code structure and naming conventions
2. Use **AppLogger** for all logging instead of SLF4J
3. Use **String.format()** for string formatting
4. Add appropriate comments for complex logic
5. Ensure Kafka messages follow the defined DTOs
6. Test changes with real Kafka/MySQL setup
7. Include unit tests for new features
8. Update documentation as needed

---

## Glossary

- **TripRow:** A single location record with vehicle telemetry data
- **Trip:** A complete journey with start and end points
- **Anomaly:** A detected unsafe driving condition (e.g., speeding)
- **Alert:** A generated warning when a rule is violated
- **carId:** Unique identifier for a vehicle (formatted as 3-digit number)
- **tripNumber:** Unique identifier for a trip instance
- **Rule:** A safety criterion evaluated against vehicle telemetry
- **Strategy Pattern:** Design pattern used for pluggable rule implementations

---

**Version:** 1.0.0  
**Last Updated:** January 2026  
**Status:** Active Development
