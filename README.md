# 📦 Order Management System (FinTech)

[![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=java)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.2-brightgreen?style=for-the-badge&logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-latest-blue?style=for-the-badge&logo=postgresql)](https://www.postgresql.org/)

A high-performance, scalable **Order Management System (OMS)** architected for the FinTech sector. This module orchestrates the entire order lifecycle—from secure placement and dynamic pricing to real-time inventory synchronization and automated administrative analytics.

---

## 💻 Core Capabilities

### 🛒 Precision Order Orchestration
*   **Real-time Validation:** Instant verification of customer profile completeness and item availability.
*   **State Machine Architecture:** Robust order lifecycle management with strictly defined transitions:
    `PENDING` ➔ `CONFIRMED` ➔ `PROCESSING` ➔ `SHIPPED` ➔ `DELIVERED`
*   **Safety Protocols:** Integrated cancellation logic for `PENDING` orders with automatic stock reversal.

### 📦 Dynamic Inventory & Pricing
*   **Stock Synchronization:** Seamless coordination between order status and inventory (Reservations vs. Deductions).
*   **Live Pricing Catalog:** Intelligent fetching of the most recent pricing data during order initialization.

### 📊 Advanced Analytics & Reporting
*   **Monthly Performance Logs:** Automated collation of revenue data, volume metrics, and item-wise performance metrics.
*   **Automated Emailers:** Scheduled SMTP integration to deliver critical sales summaries to stakeholders.

### 🔐 Enterprise Security
*   **JWT Implementation:** Stateless authentication utilizing JSON Web Tokens for secure API consumption.
*   **Role-Based Access:** Granular control over Admin and Customer endpoints.

---

## 🛠️ Technology Stack

| Component | Technology |
| :--- | :--- |
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.4.2 |
| **Security** | Spring Security + JWT (jjwt) |
| **Database** | PostgreSQL / MySQL |
| **ORM** | Spring Data JPA (Hibernate) |
| **Documentation** | SpringDoc OpenAPI 2.8.4 (Swagger) |
| **Build Tool** | Maven |

---

## 📂 Architecture Overview

```bash
src/main/java/com/example/ordermgmt/
├── 🛡️ config/         # Security, OpenAPI, and Global configurations
├── 🎮 controller/     # REST Endpoints (Admin & Customer interfaces)
├── 📦 dto/            # Data Transfer Objects & Analytics payloads
├── 🏛️ entity/         # JPA Persistence Models
├── 💾 repository/     # Data Access Layer (JPQL & Query Methods)
├── 🔐 security/       # JWT Filters & Auth Providers
└── ⚙️ service/        # Core Business Logic & Implementations
```

---

## ⚙️ Execution Guide

### 1. Database Provisioning
Configure your instance in `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/ordermgmt
spring.datasource.username=your_credentials
spring.datasource.password=your_secure_password
```

### 2. Environment Variables (Email)
To enable the reporting module, set the following environment variables:
*   `MAIL_USERNAME`: Your SMTP email
*   `MAIL_PASSWORD`: Your App-specific password

### 3. Build & Launch
```bash
# Install dependencies and build
mvn clean install

# Launch application
mvn spring-boot:run
```

---

## 📖 API Reference

Explore and interact with the APIs using the integrated Swagger UI:

🔗 **[Swagger Documentation](http://localhost:8081/swagger-ui/index.html)**
*(Ensure the application is running on port same port as in application.properties)*

---

## 🤝 Project Leadership

*   **Hardik I Joshi** - Mentor
*   **Anand Chaniyara** - Backend Developer

---

© 2026 Order Management System. All rights reserved.
