# 📦 Order Management System

[![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=java)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.2-brightgreen?style=for-the-badge&logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-latest-blue?style=for-the-badge&logo=postgresql)](https://www.postgresql.org/)

A high-performance, scalable **Order Management System (OMS)** that orchestrates the entire order lifecycle—from secure placement and dynamic pricing to real-time inventory synchronization and automated administrative analytics.

---

## 💻 Core Capabilities

### 🆕 What’s New
* **Multi-tenant onboarding:** Platform owners can create organizations and delegate organization administrators; public sign-up is limited to customers by subdomain.
* **Token lifecycle hardening:** Refresh-token rotation with blacklist on logout/refresh, plus rate-limited registration and password-recovery flows.
* **Self-service recovery:** Forgot/Reset password with time-bound temporary credentials and tenant-aware validation.
* **Analytics upgrades:** Revenue and order analytics with item/status filters, pagination, and optional email delivery.
* **Bulk operations:** Bulk order status updates, price set/update, inventory create/update, and stock additions for high-volume workflows.

### 🛒 Precision Order Orchestration
*   **Real-time Validation:** Instant verification of customer profile completeness and item availability.
*   **State Machine Architecture:** Robust order lifecycle management with strictly defined transitions:
    `PENDING` ➔ `CONFIRMED` ➔ `PROCESSING` ➔ `SHIPPED` ➔ `DELIVERED`
*   **Safety Protocols:** Integrated cancellation logic for `PENDING` orders with automatic stock reversal.

### 📦 Dynamic Inventory & Pricing
*   **Stock Synchronization:** Seamless coordination between order status and inventory (Reservations vs. Deductions).
*   **Live Pricing Catalog:** Intelligent fetching of the most recent pricing data during order initialization.

### 📊 Advanced Analytics & Reporting
*   **Revenue & Order Analytics:** Item/status filters with pagination for precise drill-downs.
*   **Actionable Emails:** Optional email dispatch for analytics reports, receipts, and status updates.

### 🔐 Enterprise Security
*   **JWT Implementation:** Stateless authentication utilizing JSON Web Tokens for secure API consumption.
*   **Role-Based Access:** Tiered permissions spanning platform, organization, administrative, and customer personas.

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
Configure SMTP credentials in your environment to enable reporting and notification emails.

### 3. Build & Launch
```bash
# Install dependencies and build
mvn clean install

# Launch application
mvn spring-boot:run
```

---

## 📖 API Reference


🔗 **API Reference — User Grouped:** [doc/API_BY_USER.md](doc/API_BY_USER.md)


Explore and interact with the APIs using the integrated Swagger UI:

🔗 **[Swagger Documentation](http://localhost:8081/swagger-ui/index.html)**
*(Ensure the application is running on port same port as in application.properties)*
---

*   **Hardik I Joshi** - Mentor
*   **Jainish Mistry** - Mentor
*   **Anand Chaniyara** - Backend Developer


© 2026 Order Management System. All rights reserved.
