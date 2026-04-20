# Community Backend Testing Guide

This document provides instructions on how to run and understand the automated tests for the `community-backend` module, specifically focusing on the **Hot-Cold Data Separation** and **Moment Lifecycle** logic.

## Prerequisites

- **Java 17** or higher.
- **Maven 3.8+** (or use the provided `./mvnw`).
- No external infrastructure (MySQL, HBase, RabbitMQ) is required to be running, as these tests utilize **Mockito** for dependency mocking.

## Test Suites Overview

The testing suite is divided into two primary categories:

### 1. Cold Data Migration Logic (`CommunityColdDataMigrationJobTest`)
Focuses on the background scheduled task that moves data from MySQL to HBase.
- **Key Scenarios**: Standard migration path, comment threshold interception (2000+ comments), HBase write failure rollbacks, and **Idempotent Self-healing** (recovering from partially failed migrations).

### 2. Service Lifecycle & Concurrency (`MomentServiceLifecycleTest`)
Focuses on user-facing operations and state machine integrity.
- **Key Scenarios**: Audit workflow (Pending/Approved/Rejected), concurrency protection (preventing edits during migration), and cold data restoration triggers.

## Running the Tests

### Run All Tests
Execute the following command in the terminal from the `community-backend` directory:
```bash
./mvnw test
```

### Run Specific Test Classes
To run only the migration job tests:
```bash
./mvnw test -Dtest=CommunityColdDataMigrationJobTest
```

To run only the service lifecycle tests:
```bash
./mvnw test -Dtest=MomentServiceLifecycleTest
```

## Testing Methodology

These tests were designed using a combination of traditional black-box techniques and AI-assisted modeling:
- **State Transition Testing (STT)**: Validating the `NONE -> MIGRATING -> COLD` transitions.
- **Decision Table (DT)**: Testing complex business rule combinations (e.g., audit status + last access time).
- **Boundary Value Analysis (BVA)**: Testing limits like exactly 2000 comments or exactly 7 days of inactivity.

## Troubleshooting

If you encounter `InaccessibleObjectException` related to HBase/Hadoop on newer JDKs, the test suite is configured to handle these via Mockito inline mocking. Ensure your environment allows dynamic agent loading (standard in most modern IDEs and Maven environments).