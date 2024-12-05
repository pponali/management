# Price Management System

A robust and scalable price management system built with Spring Boot, designed to handle complex pricing scenarios for e-commerce platforms. This system provides comprehensive price management capabilities with support for dynamic pricing rules, bulk operations, and multi-tenant architecture.

## 📚 Table of Contents
- [Features](#-features)
- [System Architecture](#-system-architecture)
- [Tech Stack](#-tech-stack)
- [Prerequisites](#-prerequisites)
- [Installation & Setup](#-installation--setup)
- [API Documentation](#-api-documentation)
- [Configuration](#-configuration)
- [Contributing](#-contributing)
- [License](#-license)

## 🚀 Features

### Core Price Management
- **Dynamic Price Management**
  - Schedule price changes with effective start and end dates
  - Support for multiple price types (regular, sale, promotional)
  - Historical price tracking and audit logs
  - Real-time price updates
  
- **Bulk Price Operations**
  - Asynchronous processing using Kafka
  - Excel/CSV file upload support
  - Batch processing with configurable sizes
  - Progress tracking and status reporting
  - Error handling and retry mechanisms
  
- **Multi-tenant Architecture**
  - Isolated price management per seller
  - Site-specific pricing configurations
  - Customizable pricing rules per tenant
  - Cross-site price synchronization

### Price Rules Engine
- **Rule Management**
  - Time-based rules
  - Category-based rules
  - Customer segment rules
  - Geographical pricing rules
  - Channel-specific rules
  
- **Constraint Management**
  - Price range constraints
  - Margin constraints
  - Inventory-based constraints
  - Competition-based constraints
  - Discount stacking rules

### Bundle Management
- **Product Bundling**
  - Dynamic bundle creation
  - Bundle-specific pricing
  - Cross-product bundling
  - Time-limited bundles

### Audit & Compliance
- **Audit Trail**
  - Complete price change history
  - User action tracking
  - Rule application logging
  - Compliance reporting

### Technical Features
- **Performance Optimizations**
  - Caching support
  - Database indexing
  - Connection pooling
  
- **Monitoring & Logging**
  - Actuator endpoints
  - Detailed logging
  - Performance metrics
  - Health checks

## 🏗 System Architecture

### Core Modules

1. **Price Core (com.scaler.price.core)**
   - Price management fundamentals
   - CRUD operations
   - Price validation
   - Data access layer

2. **Rule Engine (com.scaler.price.rule)**
   - Rule definition and management
   - Rule evaluation
   - Constraint management
   - Action execution

3. **Audit System (com.scaler.price.audit)**
   - Price change tracking
   - User action logging
   - Audit reporting
   - Compliance monitoring

4. **Bulk Operations (com.scaler.price.bulk)**
   - File processing
   - Batch operations
   - Async processing
   - Status tracking

## 🛠 Tech Stack

- **Java 21**
- **Spring Boot 3.2.0**
- **Spring Data JPA**
- **PostgreSQL**
- **Apache Kafka**
- **Maven**
- **JUnit 5 & MockMvc**
- **Lombok**
- **MapStruct**

## 📋 Prerequisites

### Software Requirements
- JDK 21 or later
- Maven 3.8+
- PostgreSQL 14+
- Apache Kafka 3.x
- Docker (optional)

### Development Tools
- IDE (IntelliJ IDEA, Eclipse, or VS Code)
- Git
- Postman or similar API testing tool

## 🏗 Installation & Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/pponali/management.git
   cd management
   ```

2. **Database Setup**
   ```bash
   # Create PostgreSQL database
   createdb price_management
   
   # The application will automatically create the schema using Flyway migrations
   ```

3. **Kafka Setup**
   ```bash
   # Start Zookeeper
   bin/zookeeper-server-start.sh config/zookeeper.properties
   
   # Start Kafka Server
   bin/kafka-server-start.sh config/server.properties
   ```

4. **Application Configuration**
   - Copy `application.properties.template` to `application.properties`
   - Update the following properties:
     ```properties
     spring.datasource.url=jdbc:postgresql://localhost:5432/price_management
     spring.datasource.username=your_username
     spring.datasource.password=your_password
     
     # Kafka configuration
     spring.kafka.bootstrap-servers=localhost:9092
     ```

5. **Build the Application**
   ```bash
   mvn clean install
   ```

6. **Run the Application**
   ```bash
   mvn spring-boot:run
   ```

## 📚 API Documentation

### Price Management APIs

#### Core Price Operations
```
GET    /api/v1/prices/{id}                    # Get price by ID
POST   /api/v1/prices                         # Create new price
PUT    /api/v1/prices/{id}                    # Update price
DELETE /api/v1/prices/{id}                    # Delete price
GET    /api/v1/prices/product/{productId}     # Get prices by product
GET    /api/v1/prices/site/{siteId}/seller/{sellerId}/product/{productId}  # Get price by site and seller
```

#### Bulk Operations
```
POST   /api/v1/prices/bulk/upload             # Upload bulk prices (Excel/CSV)
GET    /api/v1/prices/bulk/status/{jobId}     # Get bulk upload status
```

### Price Rules APIs

#### Rule Management
```
POST   /api/v1/rules                          # Create pricing rule
GET    /api/v1/rules/{ruleId}                 # Get rule by ID
PUT    /api/v1/rules/{ruleId}                 # Update rule
DELETE /api/v1/rules/{ruleId}                 # Delete rule
POST   /api/v1/rules/{ruleId}/activate        # Activate rule
POST   /api/v1/rules/{ruleId}/deactivate      # Deactivate rule
GET    /api/v1/rules/{ruleId}/history         # Get rule history
GET    /api/v1/rules/site/{siteId}            # Get site rules
```

#### Rule Evaluation
```
POST   /api/v1/evaluation/evaluate            # Evaluate single price
POST   /api/v1/evaluation/batch-evaluate      # Evaluate batch of prices
POST   /api/v1/evaluation/preview             # Preview rule application
```

### Bundle Management APIs
```
POST   /api/v1/bundles                        # Create bundle
GET    /api/v1/bundles/{id}                   # Get bundle by ID
PUT    /api/v1/bundles/{id}                   # Update bundle
DELETE /api/v1/bundles/{id}                   # Delete bundle
GET    /api/v1/bundles/product/{productId}    # Get bundles for product
```

### Audit APIs
```
GET    /api/v1/audit/events                   # Get audit events
GET    /api/v1/audit/events/type/{eventType}  # Get audit events by type
GET    /api/v1/audit/events/user/{userId}     # Get audit events by user
GET    /api/v1/audit/statistics               # Get audit statistics
```

### Constraint Management APIs
```
POST   /api/v1/constraints/validate/price     # Validate price constraints
POST   /api/v1/constraints/validate/margin    # Validate margin constraints
POST   /api/v1/constraints/validate/time      # Validate time constraints
POST   /api/v1/constraints/validate/category  # Validate category constraints
POST   /api/v1/constraints/validate/channel   # Validate channel constraints
```

## 🔧 Configuration

### Application Properties
```properties
# Server Configuration
server.port=8080
server.servlet.context-path=/api/v1

# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/price_management
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=validate

# Kafka Configuration
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=price-management
spring.kafka.consumer.auto-offset-reset=earliest

# Cache Configuration
spring.cache.type=caffeine
spring.cache.cache-names=prices,rules
spring.cache.caffeine.spec=maximumSize=500,expireAfterWrite=30m

# Actuator Configuration
management.endpoints.web.exposure.include=health,metrics,info
management.endpoint.health.show-details=always
```

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.