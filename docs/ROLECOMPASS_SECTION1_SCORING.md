# RoleCompass — Section 1 Scoring & Aggregation Specification

**Document Version:** 1.0  
**Status:** Canonical Reference for Section 1 (Hobbies & Interests)  

---

## 1. Section 1 Structure & Dimension Mapping

Section 1 consists of exactly **16 universal questions** measuring 8 psychological and behavioural dimensions (2 questions per dimension).

| Dimension Name | Internal Tag | Axis Family | Feature Vector Index | Question Count |
|---|---|---|---|---|
| Realistic | `DIM_REALISTIC` | RIASEC | Index 0 | 2 |
| Investigative | `DIM_INVESTIGATIVE` | RIASEC | Index 1 | 2 |
| Artistic | `DIM_ARTISTIC` | RIASEC | Index 2 | 2 |
| Social | `DIM_SOCIAL` | RIASEC | Index 3 | 2 |
| Enterprising | `DIM_ENTERPRISING` | RIASEC | Index 4 | 2 |
| Conventional | `DIM_CONVENTIONAL` | RIASEC | Index 5 | 2 |
| Data ↔ Ideas | `DIM_DATA_IDEAS` | Prediger | Index 6 | 2 |
| Things ↔ People | `DIM_THINGS_PEOPLE` | Prediger | Index 7 | 2 |

---

## 2. Aggregation Formula

For each dimension $d \in \{0, \dots, 7\}$:

Let $A_d = \{v_1, v_2, \dots, v_k\}$ be the set of valid Likert values $v_i \in \{1, 2, 3, 4, 5\}$ recorded for questions mapped to dimension $d$.

1. **Arithmetic Mean:**
   $$\mu_d = \frac{1}{|A_d|} \sum_{v \in A_d} v$$

2. **Min-Max Normalization to $[0.0, 1.0]$:**
   $$\text{score}_d = \frac{\mu_d - 1.0}{4.0}$$
   - Likert $1 \to 0.0$
   - Likert $2 \to 0.25$
   - Likert $3 \to 0.50$
   - Likert $4 \to 0.75$
   - Likert $5 \to 1.00$

3. **Clamping:**
   $$\text{score}_d = \min(1.0, \max(0.0, \text{score}_d))$$

---

## 3. Reverse Scoring

- **Current Status:** *No reverse-scored questions are currently defined in Section 1.*
- All 16 questions in the initial bank are positively keyed to their respective dimensions.
- If reverse-scored items are introduced in future revisions, the transformation $v' = 6 - v$ must be explicitly declared in this specification and implemented identically across Java backend and Python inference pipelines.

---

## 4. Edge Cases & Missing Answer Handling

### 4.1 Unanswered Dimensions
- If a dimension has $|A_d| = 0$ (no questions answered yet for this dimension):
  $$\text{score}_d = 0.50 \quad (\text{Neutral Midpoint})$$
- The adaptive routing engine tracks `coveredFeatures[d]` to differentiate between an explicitly neutral score (e.g. Likert 3) and an un-answered dimension default.

### 4.2 Duplicate Answers
- Answers are identified by composite key `(session_id, question_id)`.
- If a student re-submits an answer for the same `question_id` within the same session, JPA/database updates the existing record with the new `likert_value`, ensuring no double-counting occurs in the arithmetic mean.

### 4.3 Technical Dimensions (Indices 11–30) during Section 1
- During Section 1 evaluation, technical aggregate features (indices 11–30) default to neutral $0.50$ until technical assessment sections are encountered.

---

## 5. Summary of Pending Specifications

- **Differential Weighting:** Currently all questions in a dimension have equal weight (1.0). Non-linear / Bayesian weighting is *pending future empirical validation*.
- **Cross-Dimensional Item Loadings:** Currently each Section 1 question maps primarily to one dimension tag for clear orthogonal measurement. Multi-loading item weights are *pending future psychometric calibration*.
