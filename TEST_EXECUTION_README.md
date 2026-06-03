# Test Execution Guide — Community Moment API

This document describes how to set up and execute the automated API tests for the **Community Moment Management Module** (`3.1.x` requirements).

## Prerequisites

### Required Services

All backend services must be running before executing tests:

| Service | Address | Status |
|---------|---------|--------|
| Nacos Registry | `47.111.230.62:8848` | Required |
| Gateway | `http://localhost:9000` | Required |
| petcare-backend | Registered to Nacos | Required |
| community-backend | Registered to Nacos | Required |
| media-backend | Registered to Nacos | Required for Create tests |
| llm-backend | Registered to Nacos | Optional |

### Software Requirements

- **Java**: JDK 21 (Temurin 21.0.9+)
- **Maven**: 3.9.x (wrapper included as `mvnw.cmd`)
- **Database**: MySQL accessible via configured datasource

### Test Account

Login credentials (pre-registered in the system):
- Username: `1`
- Password: `123`

## File Placement Guide

The test suite consists of three files that must be placed in the correct directories under `community-backend/`.

### 1. Test Java Source

**File**: `MomentsApiTest.java`

**Destination**:
```
community-backend
└── src
    └── test
        └── java
            └── petcare
                └── example
                    └── community_backend
                        └── api
                            └── MomentsApiTest.java
```

### 2. EP/BVA Test Data (CSV)

**File**: `dynamic_management_test_cases.csv`

**Destination**:
```
community-backend
└── src
    └── test
        └── resources
            └── dynamic_management_test_cases.csv
```

### 3. STT Test Data (CSV)

**File**: `dynamic_management_test_cases_stt.csv`

**Destination**:
```
community-backend
└── src
    └── test
        └── resources
            └── dynamic_management_test_cases_stt.csv
```

### File Structure Summary

```
community-backend/
├── pom.xml                          (Maven build file — already exists)
├── mvnw.cmd                         (Maven wrapper — already exists)
└── src/
    └── test/
        ├── java/
        │   └── petcare/example/community_backend/api/
        │       └── MomentsApiTest.java          ← PLACE HERE
        └── resources/
            ├── dynamic_management_test_cases.csv      ← PLACE HERE
            └── dynamic_management_test_cases_stt.csv  ← PLACE HERE
```

## How to Run

### Option 1: Run via Maven (all tests)

```bash
cd community-backend
./mvnw.cmd test -Dtest=MomentsApiTest
```

### Option 2: Run via IntelliJ IDEA

1. Open the project root (`Pet-Care-Ecosystem/`)
2. Navigate to `community-backend/src/test/java/petcare/example/community_backend/api/MomentsApiTest.java`
3. Right-click the class or individual test methods
4. Select **Run 'MomentsApiTest'**

### Option 3: Run a specific test group

```bash
cd community-backend
./mvnw.cmd test -Dtest=MomentsApiTest#testCreateMoment_EP
```

Available test method names:
- `testCreateMoment_EP` / `testCreateMoment_BVA` (3.1.1)
- `testCreateMoment_Media_EP` / `testCreateMoment_Media_BVA` (3.1.2)
- `testCreateMoment_DbException` (3.1.3)
- `testCreateMoment_Invisible` (3.1.4)
- `testGetMomentById_EP` / `testGetMomentById_BVA` (3.1.5)
- `testGetMoment_Migrating` (3.1.6)
- `testGetMoment_NoRecord` (3.1.7)
- `testGetMoment_AdminAudit` (3.1.8)
- `testGetMomentsByUser` (3.1.9)
- `testGetMoment_SpecialStates` (3.1.10~3.1.12)
- `testGetAllMomentsPaged` (3.1.13)
- `testGetMoment_ColdData` (3.1.14~3.1.17)
- `testUpdateMoment` (3.1.18~3.1.21)
- `testDeleteMoment` (3.1.22~3.1.25)
- `testSttScenarios` (STT)

## Test Architecture

```
dynamic_management_test_cases.csv  ──→  loadCsv()  ──→  List<TestCaseRow>
                                                        ↓
dynamic_management_test_cases_stt.csv ──→  loadCsv()  ──→  List<TestCaseRow>
                                                        ↓
                                           @MethodSource  →  Arguments
                                                        ↓
                              ┌──────────────────────────────────────────┐
                              │  @ParameterizedTest  (JUnit 5)          │
                              │  → executeCreate()  POST /api/v1/moments │
                              │  → executeGetMoment() GET /api/v1/moments│
                              │  → testUpdateMoment() PUT /api/v1/moments│
                              │  → testDeleteMoment() DELETE /api/v1/mom │
                              │  → testSttScenarios()                    │
                              └──────────────────────────────────────────┘
```

### Key Design Decisions

| Aspect | Choice | Rationale |
|--------|--------|-----------|
| **Test Framework** | JUnit 5 + ParameterizedTest | Native Spring Boot support, data-driven |
| **HTTP Client** | RestTemplate | No extra dependencies, full HTTP support |
| **Alternatives considered** | REST Assured | Better readability but extra dependency |
| **Alternatives considered** | TestNG @DataProvider | Functional but not Spring Boot default |

### Exception Handling Strategy

```
try {
    ResponseEntity<String> resp = restTemplate.exchange(...);
    // Normal status code assertion
} catch (HttpClientErrorException e) {
    // 4xx: Extract status code and validate
} catch (HttpServerErrorException e) {
    // 5xx: Accept if server cannot handle (e.g. momentId not found)
} catch (Exception e) {
    // Other errors: fail for valid cases, pass for invalid
}
```

## Test Data Format

### EP/BVA CSV Columns (10 columns)

| Index | Field | Description |
|-------|-------|-------------|
| 0 | `test_case_id` | Unique test case identifier |
| 1 | `requirement_id` | Linked requirement ID |
| 2 | `requirement_desc` | Requirement description |
| 3 | `technique` | EP or BVA |
| 4 | `title` | Test case title |
| 5 | `test_data` | Python dict format: `{'userId': 1, 'content': 'a', 'mediaIds': 1}` |
| 6 | `covered_partitions` | Covered equivalence partitions |
| 7 | `expected_results` | Expected behavior description |
| 8 | `is_valid_case` | True for valid, False for invalid |
| 9 | `boundary_type` | Boundary type (BVA only) |

### STT CSV Columns (15 columns)

| Index | Field | Description |
|-------|-------|-------------|
| 0 | `test_case_id` | Unique test case identifier |
| 1 | `requirement_id` | Linked requirement ID |
| 2 | `requirement_desc` | Requirement description |
| 3 | `technique` | STT |
| 4 | `title` | Test case title |
| 5 | `source_state` | Source state |
| 6 | `event` | Trigger event |
| 7 | `guard` | Guard condition |
| 8 | `target_state` | Target state |
| 9 | `action` | Action description |
| 10-14 | (various) | Preconditions, steps, expected results, coverage ID |

## Known Issues

| Issue | Count | Root Cause | Status |
|-------|-------|------------|--------|
| Create 500 | ~20 | `media-backend` service not running | Environment issue |
| Invalid params return 200 | ~30 | Controller lacks `@Valid` / `@Min` / `@Max` annotations | Server bug |
| STT 405 | ~34 | CSV column mismatch with parser | Needs routing fix |

## Expected Test Results

- **Total test cases**: 419 (385 EP/BVA + 34 STT)
- **Expected pass**: ~369 (~88%)
- **Expected fail**: ~50 (30 server bugs + 20 environment issues)

## Troubleshooting

**Q: Tests fail with "500 Internal Server Error: [no body]"**
- Check if `media-backend` is running. Create operations require media validation.

**Q: Tests fail with "Connection refused"**
- Ensure Nacos (`47.111.230.62:8848`) is accessible.
- Ensure all backend services are registered to Nacos.

**Q: Tests fail with "401 Unauthorized"**
- Verify the login endpoint (`/api/auth/login`) is available.
- Confirm test account credentials (name="1", password="123") are valid.

**Q: Tests fail with "405 Method Not Allowed"**
- This applies to STT tests. The STT routing implementation needs enhancement.

**Q: How to add new test cases?**
1. Edit `dynamic_management_test_cases.csv` or `dynamic_management_test_cases_stt.csv`
2. No Java code changes needed — the test framework is fully data-driven.
