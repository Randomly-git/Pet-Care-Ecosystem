### 1. Input 

**System Overview:** This system is a hot-cold data separation system based on distributed storage (MySQL + HBase). It is designed to archive infrequently accessed “cold posts” and their associated data (comments, likes) to HBase via scheduled tasks, thereby reducing storage pressure and query load on the MySQL database. The system incorporates automatic migration logic, asynchronous recovery logic based on a message queue (MQ), and complex concurrency protection mechanisms. To ensure the system’s functional integrity, a moderation mechanism for posts has been designed.

---

**Functional Requirements:**

- **cold data migration（M-1, M-1S, M-2, M-6）**：

  The scheduled task migrates eligible records to HBase based on their dynamic review status (APPROVED / REJECTED) and the last access time threshold (7 days / 3 days). During migration, the status changes to `MIGRATING`, and the records are deleted from MySQL after they are successfully written to HBase. If the number of comments exceeds 2,000, the migration is skipped and the status is rolled back. The system supports idempotent retries and self-healing mechanisms in the event of migration failure.
- **cold data recovery（M-4, M-3B, M-7）**：  
  When a user actively accesses cold data or when there is insufficient hot data in a list query, the system uses MQ to asynchronously write archived data from HBase back to MySQL and deletes the HBase records. After the restoration, the migration status is reset to `NONE`, and the original audit status is retained.
- **Concurrency Control and State Preservation（M-5, M-8）**：

  When the status is `MIGRATING`, any actions such as commenting, liking, editing content, or changing the review status are prohibited to ensure data consistency and state machine integrity during the migration.
- **Review Workflow and Content Edits（M-10）**： 

  When a user edits the content of a post that has already been approved, the system automatically reverts the approval status to `PENDING` and hides the post, ensuring that non-compliant content cannot bypass the review process and be published.
- **delete of moments（M-9）**： 

  Supports the physical deletion of posts in any state (ACTIVE, MIGRATING, COLD), while simultaneously clearing the corresponding records in MySQL and HBase to prevent storage waste.
- **Visibility control（M-3C, M-3A）**：  
Only posts with a review status of `APPROVED` are visible to others; posts with a status of `PENDING` or `REJECTED` are visible only to the author, to prevent unauthorized access to content that has not passed review.

> **Project Time Zone Specifications:** This project is intended for Mainland China. All time-based calculations in business logic (such as determining 3-day/7-day cold migration thresholds and updating last access times) are based on **Beijing Time (UTC+8)**.

---
**Testing Base:**

Requirements Document: `Documents/HBase/Moment 冷热分离需求文档.md` (includes state transition diagrams and use case tables).

---
### 2. Tool Artifact 
**LLM Used:** Gemini Code Assist (Google)
**Refined Prompts (Core Prompt Sequence):** To meet the technical requirements for black-box testing (STT, DT, EP/BVA), I have used the following optimized prompts:

- **Prompt 1: Quality Risk Analysis**

  > “As a software testing expert, please analyze the *Moment Hot/Cold Separation Requirements Document* and related logic code to identify potential quality risks. Please generate a risk analysis table that includes technical risks, business risks, risk priority, and testing recommendations, prioritized as follows: data loss > data inconsistency > state lockup > system stability.”

- **Prompt 2: State Transition Modeling**

  > "Based on the state transition diagram in the requirements document, please generate a standard state transition table (STT). Identify all valid state transition paths (e.g., NONE -> MIGRATING -> COLD) as well as exception interruption paths (e.g., MIGRATING -> NONE rollback), and annotate the corresponding trigger events, guard conditions, and actions for each transition."

- **Prompt 3: Decision Table Modeling**

  > “For the core migration method `migrateSingleMoment`, analyze its internal multi-condition combination logic (e.g., whether data is cold, current state, comment threshold, HBase write status, MySQL delete status), and generate a decision table (DT). Focus on identifying exceptional boundary scenarios such as Rule 6 (HBase failure) and Rule 7 (MySQL delete failure) through rule combinations.”

- **Prompt 4: Data Boundary Analysis (EP & BVA Analysis)**

  > "For the key variables affecting decision-making in the system—total number of comments (threshold 2000) and last access time (threshold 7 days)—identify their valid equivalence classes, invalid equivalence classes, and boundary values (BVA). Please provide specific value suggestions for use in subsequent test case design. "

- **Prompt 5: Test Coverage Item Generation**

  > "Based on the STT, DT, and EP/BVA modeling results described above, generate a list of atomic-level test coverage items (TCIs). Each TCI should represent a minimal verifiable logical point (e.g., a specific input in a specific state, a rule in a decision table, or a boundary value). Please assign a unique ID to each TCI and annotate it with its associated quality risk ID, to serve as a direct basis for subsequent test case design."

- **Prompt 6: Test Case Generation**

  > “Please integrate the aforementioned STT, DT, and EP/BVA models to generate a set of detailed black-box test cases. The test cases should cover full lifecycle path coverage, fault injection testing, and negative concurrency protection testing. Each test case must include: test ID, scenario description, preconditions, test steps, and expected rollback/self-healing behavior.”
---
### 3. Generated Output & Artifacts 

#### 3.1 Description of the Delivery Documentation Structure
The test deliverables for this project are stored in the `Moment Hot/Cold Separation Testing/` directory. The documentation evolution follows this structure:
- **AI Initial Output (.md)**: Includes `RiskAnalysis_Draft.md`, `TestModeling_Draft.md`, `TCICoverage_Draft.md`, and `TestCases_Draft.md`, documenting the LLM’s initial analysis of requirements and the process of generating preliminary test cases.
- **Refactored Version (Test Cases_Refactored.md)**: The final version following prompt iteration, scenario consolidation, and manual proofreading, serving as the direct basis for automated script generation.
- **Excel Summary Sheet (.xlsx)**: A manually compiled summary of the results, providing a clearer traceability view of requirements, risks, and test cases. Among these, `Moment_Testing_Engineering - Refactored.xlsx` is the final version.

#### 3.2 Test Modeling Statistics

Based on black-box test modeling, the final test asset inventory is as follows:

| Type | Count | Description |
|------|------|------|
| **Quality Risks (Risks)** | **26** | Includes 25 business risks and stability risks derived from unexpected paths (RSK-1.4.007) |
| **State Transitions (STT)** | **55** | Includes 13 standard paths and 42 unexpected paths; supplemented by decision tables (DT), equivalence classes (EP), and boundary values (BVA) for modeling |
| **TCI (Test Coverage Items)** | **29** | Covers STT states, decision tables (DT), equivalence classes (EP), and boundary values (BVA) |
| **Test Cases (Black-Box Test Cases)** | **16** | Highly cohesive scenario-based test cases integrated through **Path Sensitizing** |
| **Automated Scripts** | **15** | Physical execution scripts, incorporating 3 boundary value tests and adding one integration test |

#### 3.3 List of Automated Test Scripts

The 15 physical test methods for `CommunityColdDataMigrationJob` provide complete coverage of the 16 scenario test cases after refactoring:

- `CommunityColdDataMigrationJobTest.java`: Contains 9 core methods covering TS-1 (Consistency) and TS-5 (Business Boundaries).
- `MomentServiceLifecycleTest.java`: Contains 6 test cases for the business lifecycle (verifying review workflow and state rollback after content modification).
#### 3.4 Latest Run

```log
[INFO] Running petcare.example.community_backend.service.CommunityColdDataMigrationJobTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running petcare.example.community_backend.service.MomentServiceLifecycleTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---
### 4. Experimental Analysis

#### 4.1 Limitations of AI
In practice, we have observed the following five core limitations in AI-generated test cases:

1. **Lack of risk assessment criteria**: By default, AI cannot discern the priority of business importance. For example, it initially treated “storage space waste” and “data loss” as risks of equal severity. To address this issue, we must manually enforce evaluation rules in the prompts (e.g., **data loss > data incompleteness > weak data consistency > storage space waste**).
2. **Duplicate test cases and poor traceability caused by long context**: Due to LLM limitations on context reading length and memory decay, when requirement documents are too long, AI is prone to generating duplicate TCI IDs or logical gaps in the latter parts of the table. This requires us to perform manual secondary alignment, remove redundant items, and organize them into clearly structured Excel/Markdown checklists to ensure the rigor of the traceability chain.
3. **Misunderstanding of Software Testing Concepts**: AI tends to generate fragmented, flat lists of test cases rather than structured quality risk domains or test suites. It implicitly ignores the classification of quality attributes (such as reliability and efficiency), leading to disorganized test suite structures. To address this, we manually defined a classification framework to force the output into clearly categorized “quality risk tables” and “scenario-based test cases.”
4. **Lack of Awareness Regarding Scenario Modeling and Integration**: When designing test cases, AI tends to mechanically map one TCI to one TC, failing to automatically understand the engineering optimization logic that “a single end-to-end path can cover multiple coverage items.” Without human intervention, AI generates a large number of redundant and inefficient atomic test cases.
5. **Omission of Unexpected Transition Paths (Negative State Transitions)**: AI typically focuses only on explicitly marked “forward” paths in the state transition diagram, automatically ignoring undefined paths that should theoretically be prohibited (e.g., a dynamic under review being unexpectedly triggered). This “what isn’t drawn on the diagram doesn’t exist” mindset must be corrected through dedicated negative testing prompts.
#### 4.2 Refining Prompts for Accuracy 
To address the limitations mentioned above, we iterated on the initial prompts:

- **For Risk Assessment (Refined Prompt A)**:
  > “Please reassess the risk levels. When assessing risks related to data, strictly adhere to the following risk evaluation criteria: data loss > data incompleteness > weak data consistency > wasted storage space”

  Improvement: With this prompt, the AI is better able to score and rank risks, though human adjustment is still required. The **Risk Analysis Table_Draft.md** in the project documentation illustrates the process of the AI’s iterative risk assessments.

- **Regarding Conceptual Understanding and Classification (Refined Prompt B)**:

  > “Do not directly output test cases. First, generate the corresponding Test Suite structure based on a quality risk matrix comprising the five categories: ‘Data Integrity,’ 'Data Consistency,‘ 'Concurrency Control,’ 'System Reliability,‘ and 'Functional Reliability.’ Then, design specific test cases.”

  Improvement: The AI is now capable of designing multiple Test Suites based on quality risk categories as part of high-level test design, followed by the design of specific test cases, resulting in a more targeted and traceable approach. Both the final version of the Excel spreadsheet and **Test Cases_Rework.md** reference the following quality risk category-test suite mapping diagram, which is based on PUML code. This diagram broadly illustrates the design relationship between the two, but the specific risk-test case mappings may vary.
- **Path Risk (Refined Prompt C)**:

  > “Analyze the state transition diagram. Please identify all unlabeled unexpected transitions in the diagram. For example, if the current state of the dynamic is ACTIVE and the ‘Restore Cold Data’ event is received, the expected behavior should be ‘silently skip.’ Based on these ‘prohibited transition’ scenarios, please define a new category of quality risks, update the coverage items and test modeling, and ultimately generate a set of negative test cases.”

  Improvement Results: Based on this prompt, the AI began identifying unexpected transition paths, and the identified paths were subsequently organized into an Excel file by a human. The **STT.py** file in the project documentation demonstrates the AI-generated automation script, which facilitates the identification of unexpected paths. Below is the core code.
```
  # 提取所有唯一的状态和输入事件
  all_states = sorted(original_stt["当前状态"].unique())
  all_events = sorted(original_stt["输入事件"].unique())
  
  # 生成所有组合
  all_combinations = list(product(all_states, all_events))
  
  # 构建查找字典
  stt_dict = {}
  for _, row in original_stt.iterrows():
      key = (row["当前状态"], row["输入事件"])
      stt_dict[key] = {
          "约束条件(Guard)": row["约束条件(Guard)"],
          "目标状态": row["目标状态"],
          "动作/输出": row["动作/输出"]
      }
  
  # 生成完整 STT
  full_stt_rows = []
  for state, event in all_combinations:
      key = (state, event)
      if key in stt_dict:
          guard = stt_dict[key]["约束条件(Guard)"]
          target = stt_dict[key]["目标状态"]
          action = stt_dict[key]["动作/输出"]
      else:
          guard = "Undefined"
          target = "Undefined"
          action = "Undefined"
      full_stt_rows.append([state, event, guard, target, action])
  
  full_stt_df = pd.DataFrame(full_stt_rows, columns=["当前状态", "输入事件", "约束条件(Guard)", "目标状态", "动作/输出"])
  ```
-  **Refined Prompt D**:

> "Please do not perform a 1:1 mechanical mapping. Please apply **Path Sensitizing** and **Scenario Consolidation** techniques to identify **Basis Paths** that cover the most nodes by analyzing state transition diagrams. Merge related state transitions, boundary checks, and self-healing logic into a single end-to-end business scenario to generate scenario-based test cases with higher testing efficiency."

Improvement Results: Comparing the final version of the Excel document with the previous two versions reveals that the number of test cases redesigned through path sensitization has decreased from 29 (corresponding one-to-one with TCI) to 15, and the correspondence with TCI is now more flexible.
#### 4.3 Bug Discovery
While executing the refactored scenario test case **TC-1-01 (Standard Migration Full Lifecycle and Self-Healing)**, the automated test script detected and confirmed a high-risk logic defect:

**[BUG-COLD-001] Risk of “orphaned data” due to the state machine's self-healing priority being lower than that of business rules**
- **Defect Description**: In the `migrateSingleMoment` method, the system prioritizes checking the business admission rule (`last_access_time`) at the beginning of the transaction, rather than first determining the state machine’s lock status.
- **Triggering Scenario**: If the migration task crashes during the MySQL deletion phase, the record remains in the `MIGRATING` state. If, at this point, the record’s access time is updated due to operations, a system-wide full scan, or a logic vulnerability, the next migration task will skip self-healing because the record is marked as “recently accessed.”

**Test Failure Log (Defect Evidence - Pre-Fix):**
```log
19:48:25.312 [main] INFO ... - 【冷迁移】查询到动态: momentId=6, userId=600, status=MIGRATING, lastAccessTime=2026-04-13...
19:48:25.312 [main] INFO ... - 【冷迁移】动态最近被访问过，跳过迁移: momentId=6, lastAccessTime=2026-04-13...
...
[ERROR] petcare.example.community_backend.service.CommunityColdDataMigrationJobTest.testTC_1_01_StandardMigrationAndIdempotentRecovery -- Time elapsed: 0.013 s <<< FAILURE!
org.opentest4j.AssertionFailedError: 自愈迁移应当返回 true ==> expected: <true> but was: <false>
```

- **Impact Assessment**: This record will be invisible to users due to its `MIGRATING` status, and because it has “warmed up,” it cannot be processed by the background self-healing logic. Ultimately, it will become “zombie data” that is permanently inaccessible and cannot be archived.
- **Fix**: Adjust the logic sequence of the `migrateSingleMoment` method to prioritize the `MIGRATING` status check (self-healing path) before the business rule determination (hot/cold check).

**Verification Results**:
After the development team applied the fix, they reran the automated test scripts. The results showed that `testTC_DT_04_IdempotentRecovery` successfully ignored the interference from the “hot timestamp” and correctly triggered the idempotent cleanup logic. All 9 core unit tests passed.
#### 4.4 Coverage Analysis
Based on quantitative statistics from the `Risk-Test Case Coverage Matrix` and the `Risk Traceability Matrix (RTM)`, the following analysis of risk coverage and test case effectiveness for this round of testing is provided.

| Risk Category | Total Risks | Covered Risks | Coverage Rate |
|---------|---------|---------|--------|
| Data Consistency (1.1.x) | 7 | 7 | 100% |
| Data Integrity (1.2.x) | 6 | 6 | 100% |
| Concurrency Control (1.3.x) | 3 | 3 | 100% |
| System Stability (1.4.x) | 7 | 7 | 100% |
| Functional Availability (1.5.x) | 4 | 4 | 100% |
| **Total** | **26** | **26** | **100%** |

> ✅ **Conclusion**: All identified quality risks are covered by at least one test case; there are no “uncovered risks.”


Based on the statistics of the number of risks associated with each test case in the `Coverage Matrix`:

- **TC-2-01 (End-to-End Cold Data Recovery)** covers 10 risks; it is a high-value scenario test case that meets the expectations of “path-sensitive” design.
- All **16 test cases** are associated with at least 1 risk; there are no “zero-coverage” cases, indicating that the test case design is effective.
---


### 5. Project Report

#### 5.1 Comparison to Traditional Techniques

---
*   **Traditional Manual Testing Approach**: This approach relies heavily on testers’ in-depth understanding of project requirements and source code logic. When dealing with systems featuring complex logic (such as hot/cold separation), testers must spend significant time manually exhaustively testing all logical possibilities and modeling quality risks, and they are prone to overlooking issues due to cognitive biases.
*   **AI-Empowered Testing Approach (Gemini Code Assist)**:
    *   **Cognitive Acceleration**: AI can parse extensive requirements documents and project source code within minutes, rapidly establishing a clear understanding of the system’s full scope.
    *   **Deep Exhaustive Analysis**: Leveraging the generalization capabilities of Large Language Models (LLMs), AI can identify potential logical risk points at a speed far exceeding that of humans, and automatically generate corresponding Test Coverage Items (TCIs) and black-box test cases.
*   **A New Paradigm of Collaboration**: While AI significantly boosts output efficiency, **human intervention remains indispensable**. The role of testers shifts from “executor” to “supervisor and architect”:
    *   **Value Assessment**: Assign “priority/importance” ratings to risk points identified by AI based on real-world business scenarios.
    *   **End-to-End Control**: Oversee the entire process from quality risk analysis to High-Level Design (HLD) and Low-Level Design (LLD).
    *   **Traceability Management**: Manually proofread and reorganize the AI’s original output (e.g., maintaining Excel matrices) to ensure the traceability and engineering quality of test assets.
#### 5.2 Summary
Through a closed-loop process of “manually defined frameworks + AI-driven logic filling + human feedback for correction,” this project not only achieved over 85% business code coverage in a very short time but also successfully identified deep-seated risks, such as state machine self-healing logic, thereby validating the robustness of the hot-cold separation system.

---
**Team Members:** 2351887-孟炜程  2352488 丁桢垚 2353579 孙修明 2353596 吴瑞泽 2353726 付煜超
**Date:** 2026-04-18