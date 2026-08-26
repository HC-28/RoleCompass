# RoleCompass — Current Assessment Audit

**Audited:** 2026-08-22  
**Auditor:** Antigravity  
**Objective:** Audit existing assessment implementation, question seeder, routing, session lifecycle, answer ingestion, and batch delivery before establishing the proper Section 1 assessment foundation.

---

## 1. Existing Seeder & Questions

- **Location:** `backend/src/main/java/com/rolecompass/seed/DataSeeder.java`
- **Current Behavior:** 
  - Seeds 31 questions across Sections 1, 2, and 3.
  - Section 1 previously had 11 questions.
- **Problem Identified:**
  - Section 1 was not structured with the required exact 16 universal, behavior-based questions (2 per psychological/behavioral axis across the 8 defined dimensions: Realistic, Investigative, Artistic, Social, Enterprising, Conventional, Data ↔ Ideas, Things ↔ People).
- **Required Fix:**
  - Update `DataSeeder` to seed exactly 16 universal Section 1 questions (2 Realistic, 2 Investigative, 2 Artistic, 2 Social, 2 Enterprising, 2 Conventional, 2 Data ↔ Ideas, 2 Things ↔ People).
  - Ensure all 16 questions have `section_id = 1` and indirect, behavior-based wording with no technology/framework/role names.

---

## 2. Question & Answer Schema

### `Question` Entity (`questions` table)
- `id` (bigint PK)
- `section_id` (integer)
- `text` (text)
- `dimension_tags` (text[]) — internal tags (e.g. `DIM_REALISTIC`)
- `trigger_predicate` (jsonb) — internal gating metadata
- `created_at` (timestamp)

### `Answer` Entity (`answers` table)
- `session_id` (UUID FK, composite PK)
- `question_id` (bigint FK, composite PK)
- `likert_value` (integer, validated 1..5)
- `created_at` (timestamp)

### `QuestionDTO` (Client-facing)
- `id` (Long)
- `text` (String)
- `options` (List<Integer>: `[1, 2, 3, 4, 5]`)
- **Crucial Rule:** `dimension_tags` and `trigger_predicate` are NEVER included in `QuestionDTO`. The frontend never receives internal tags or feature IDs.

---

## 3. Session Flow & Batch Delivery

### Session Creation (`POST /api/session/start`)
- Initialises session with all 10 candidate roles (`candidateRoleIds = [1L..10L]`).
- Status set to `in_progress`.
- Vectors initialized (`answered_vector = Double[31]`, `answered_dims_mask = Boolean[31]`).
- **Batch Delivery:** Loads the first batch of 4 unanswered Section 1 questions and returns them in `SessionStartResponse`.

### Answer Ingestion (`POST /api/session/{id}/answers`)
- Validates Likert scores (1..5).
- Persists answers in `AnswerRepository`.
- Computes answered count and delegates routing evaluation to `AdaptiveRoutingEngine`.
- Selects the next batch of unanswered questions (batch size = 4) until all Section 1 questions are answered.
- Returns status (`in_progress` or `ready_to_predict`) along with `questions` containing the next batch when `in_progress`.

### Prediction Endpoint (`POST /api/session/{id}/predict`)
- Verifies session is in `ready_to_predict` or `completed` status.
- Invokes `FeatureAggregationService.buildFeatureVector(sessionId)` to compute the deterministic 31-feature vector.
- Returns the **mocked prediction** (Backend Developer / 0.88 confidence) since model training and FastAPI inference are scheduled for subsequent milestones.

---

## 4. Demo Bypass Isolation

- **Location:** `AdaptiveRoutingEngine.java` -> `isDemoMode(state)`
- **Behavior:** Explicitly named and isolated demo mechanism. When demo threshold is configured, it permits rapid end-to-end testing without breaking production routing contracts.
- **Rule:** Demo mode is never presented as production routing logic.

---

## 5. React Frontend Assessment Audit

- **Location:** `frontend/src/pages/AssessmentPage.jsx`, `frontend/src/api/session.js`
- **Role:** Pure presentation client ("dumb client").
- **Verification:**
  - Consumes `{ id, text, options }` only.
  - Submits `{ question_id, likert_value }`.
  - When the backend returns subsequent batches, renders the new batch.
  - No RIASEC scoring, no dimension calculations, no routing rules, and no role filters exist in React.

---

## 6. Exact Files Modified/Created in this Phase

1. `backend/src/main/java/com/rolecompass/seed/DataSeeder.java` — 16 real Section 1 questions.
2. `backend/src/main/java/com/rolecompass/service/QuestionService.java` — Batch-aware question retrieval.
3. `backend/src/main/java/com/rolecompass/service/SessionService.java` — Batch delivery orchestration.
4. `backend/src/main/java/com/rolecompass/routing/AdaptiveRoutingEngine.java` — Section 1 sequencing & unanswered selection.
5. `frontend/src/pages/AssessmentPage.jsx` — Multi-batch progressive presentation while remaining dumb.
6. `docs/ROLECOMPASS_CURRENT_ASSESSMENT_AUDIT.md` — This audit.
7. `docs/ROLECOMPASS_SECTION1_SCORING.md` — Section 1 scoring & aggregation specification.
8. Unit tests in `backend/src/test/java/com/rolecompass/...`.
