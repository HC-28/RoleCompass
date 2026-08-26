# RoleCompass — Antigravity Audit

**Audited:** 2026-08-22
**Auditor:** Antigravity
**Scope:** Full repository snapshot before ML foundation build

---

## 1. Repository Layout

No `src/test/` directory exists — zero automated tests.
No `docs/` directory existed before this audit.

---

## 2. What Already Works

| Area | Status | Notes |
|------|--------|-------|
| User registration (POST /api/auth/register) | Works | email + password only, correct contract |
| User login (POST /api/auth/login) | Works | returns JWT |
| JWT filter + Spring Security stateless | Works | JJWT 0.12.x, BCrypt |
| Session start (POST /api/session/start) | Works (demo) | returns 4 seeded questions |
| Answer submission (POST /api/session/{id}/answers) | Works (demo) | saves to DB, immediately sets ready_to_predict |
| Mock prediction (POST /api/session/{id}/predict) | Works (mock) | always returns Backend Developer / 0.88 |
| CORS configuration | Works | allows localhost:5173 |
| GlobalExceptionHandler | Works | handles validation, auth, API, generic errors |
| Question schema (id, section_id, text, dimension_tags, trigger_predicate) | Correct structure | matches AGENTS.md spec |
| React dumb client | Correct | consumes {id, text, options}, sends Likert 1-5, no routing/scoring logic |
| PostgreSQL + Hibernate ddl-auto: update | Works | no migration tool |

---

## 3. What Is Missing / Broken

### 3.1 Critical Missing Components

| Missing | Impact |
|---------|--------|
| AdaptiveRoutingEngine | No routing logic exists |
| FeatureAggregationService | No 31-feature vector is ever computed |
| Role-vector matrix (10x11) | Cannot do routing, elimination, or training |
| Frozen 31-feature schema | Feature ordering undefined |
| Question -> feature mapping | 4 seeded questions have informal tags, not mapped to 31 feature indices |
| Any automated tests | Zero tests exist |

### 3.2 Demo Bypass Problems

SessionService.java lines 66-68: bypass is not isolated — it is the ONLY code path.
SessionService.startSession() line 34: candidateRoleIds = [1L,2L,3L,4L] — only 4 of 10 roles.
answeredVector and answeredDimsMask are hardcoded to length 4, not 31.

### 3.3 Exposure-Independence Violations in Seed Questions

All 4 seeded questions violate AGENTS.md section 3.2:
- Q1: names "REST APIs", "database schemas"
- Q2: names "CI/CD pipelines", "Docker/K8s", "cloud infrastructure"
- Q3: borderline acceptable
- Q4: names "machine learning models", "Python"

### 3.4 Schema Risks

- ddl-auto: update — no migration audit trail
- candidateRoleIds bigint[], answeredVector float8[], answeredDimsMask boolean[] all wrong length (4, not 31)
- No roles table — role IDs 1-10 are implicit integers
- No sections table — section IDs unvalidated

### 3.5 API Contract — Current State (All Match AGENTS.md)

POST /api/auth/register, POST /api/auth/login, POST /api/session/start,
POST /api/session/{id}/answers, POST /api/session/{id}/predict — all intact.

---

## 4. Ambiguities That Block Safe Implementation

### BLOCKER-1: Role-Vector Matrix Undefined
The 10x11 role-target-vector matrix does not exist anywhere in the repository.
Routing engine cannot eliminate candidates. Training cannot begin.
A placeholder is provided in docs/model/role-vector-placeholder-v1.md.

### BLOCKER-2: Question-to-Feature Mapping Incomplete
dimension_tags are informal strings, not mapped to feature indices 0-30.

### BLOCKER-3: Technical Aggregate Feature Definitions Undefined
20 technical features undefined — no aggregation rules, normalization scheme, or reverse scoring defined.

---

## 5. What Will Be Built in This Task

1. Audit (this document)
2. docs/model/feature-schema-v1.md — frozen 31-feature schema
3. docs/model/role-vector-placeholder-v1.md — placeholder, requires approval
4. Updated DataSeeder — exposure-independent questions
5. FeatureSchemaConstants — Java constants for feature indices
6. AdaptiveRoutingEngine — dedicated Spring service
7. FeatureAggregationService — dedicated Spring service
8. Unit tests for both services
9. Updated SessionService — demo bypass clearly isolated

NOT in scope: synthetic data, model training, FastAPI, replacing mock prediction.
