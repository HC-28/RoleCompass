# RoleCompass — 31-Feature Schema v1
# STATUS: FROZEN for implementation. Role-vector values in separate file require approval.
# This file defines the exact 31-feature vector used by:
#   - FeatureAggregationService (Java)
#   - Synthetic data generator (Python — future)
#   - FastAPI inference endpoint (Python — future)
# NEVER reorder features without updating all three locations.

---

## Part A: Psychological Dimensions (Features 0–10)

These 11 features are derived from RIASEC, Prediger axes, and project-specific axes.
Aggregation: mean of all Likert responses mapped to this dimension, normalized to [0.0, 1.0].
Normalization formula: (mean_likert - 1) / 4  (maps 1→0.0, 5→1.0)

| Index | Feature ID       | Dimension Name        | Axis Type | High score means... |
|-------|------------------|-----------------------|-----------|---------------------|
| 0     | DIM_REALISTIC    | Realistic             | RIASEC    | Prefers hands-on, tangible, mechanical work |
| 1     | DIM_INVESTIGATIVE| Investigative         | RIASEC    | Prefers research, analysis, intellectual problem-solving |
| 2     | DIM_ARTISTIC     | Artistic              | RIASEC    | Prefers creative, expressive, open-ended work |
| 3     | DIM_SOCIAL       | Social                | RIASEC    | Prefers helping, teaching, collaborating with people |
| 4     | DIM_ENTERPRISING | Enterprising          | RIASEC    | Prefers leading, persuading, managing outcomes |
| 5     | DIM_CONVENTIONAL | Conventional          | RIASEC    | Prefers structured, ordered, detail-oriented work |
| 6     | DIM_DATA_IDEAS   | Data-Ideas Axis       | Prediger  | High=Data-oriented; Low=Ideas-oriented |
| 7     | DIM_THINGS_PEOPLE| Things-People Axis    | Prediger  | High=Things-oriented; Low=People-oriented |
| 8     | DIM_BREADTH_DEPTH| Breadth-Depth Axis    | Project   | High=Prefers specialisation; Low=Prefers breadth |
| 9     | DIM_STRUCT_AMBIG | Structure-Ambiguity   | Project   | High=Prefers structure; Low=Tolerates ambiguity |
| 10    | DIM_OFFENSE_DEF  | Offense-Defense Axis  | Project   | High=Prefers building/creating; Low=Prefers defending/protecting |

NOTE: Project-specific axes (indices 8, 9, 10) are unvalidated project-defined dimensions.
They are NOT independently validated psychological instruments.

---

## Part B: Technical Aggregate Features (Features 11–30)

These 20 features capture self-reported aptitude/interest across technical skill clusters.
They do NOT name specific technologies (exposure-independence rule).

Aggregation: mean of all questions tagged to this feature, normalized to [0.0, 1.0].
Formula: (mean_likert - 1) / 4

| Index | Feature ID            | Cluster Description |
|-------|-----------------------|---------------------|
| 11    | TECH_SERVER_LOGIC     | Interest in server-side processing, request handling, and application logic |
| 12    | TECH_DATA_STORAGE     | Interest in structured data storage, querying, and schema design |
| 13    | TECH_API_DESIGN       | Interest in designing and consuming inter-service communication contracts |
| 14    | TECH_UI_RENDERING     | Interest in visual layout, client-side rendering, and user interaction patterns |
| 15    | TECH_STATE_MGMT       | Interest in managing client-side or application-level state across sessions |
| 16    | TECH_BUILD_PIPELINE   | Interest in automating build, test, and release processes |
| 17    | TECH_INFRA_PROVISION  | Interest in provisioning, configuring, and maintaining infrastructure |
| 18    | TECH_CONTAINER_ORCH   | Interest in running workloads in isolated, reproducible runtime environments |
| 19    | TECH_CLOUD_SERVICES   | Interest in managed remote computing services and distributed resource allocation |
| 20    | TECH_STAT_ANALYSIS    | Interest in numerical analysis, statistics, and pattern discovery in data |
| 21    | TECH_MODEL_BUILDING   | Interest in building predictive systems from historical data |
| 22    | TECH_DATA_PIPELINE    | Interest in moving, transforming, and orchestrating large datasets |
| 23    | TECH_MOBILE_CLIENT    | Interest in building applications that run natively on handheld devices |
| 24    | TECH_THREAT_ANALYSIS  | Interest in identifying and reasoning about adversarial attack surfaces |
| 25    | TECH_SYSTEM_HARDENING | Interest in reducing attack surface, access controls, and defensive configuration |
| 26    | TECH_TEST_DESIGN      | Interest in designing systematic verification strategies for software behaviour |
| 27    | TECH_TEST_AUTOMATION  | Interest in automating test execution and result reporting |
| 28    | TECH_OBSERVABILITY    | Interest in monitoring, logging, and diagnosing system behaviour in production |
| 29    | TECH_PERF_OPTIM       | Interest in measuring and improving speed, throughput, and resource efficiency |
| 30    | TECH_FULL_SPECTRUM    | Interest in contributing across the full stack from UI to database to deployment |

---

## Part C: Aggregation Rules

### Standard aggregation (applies to all 31 features unless overridden):
1. Collect all Answer records for the session where question.dimension_tags contains this feature's tag.
2. Compute the arithmetic mean of likert_value across those answers.
3. If no answers exist for this feature: use 0.5 (neutral midpoint on normalized scale).
4. Normalize: (mean - 1.0) / 4.0
5. Clamp result to [0.0, 1.0].

### Reverse scoring:
- No reverse scoring is currently defined.
- If added in future, it must be noted here and implemented in both Java and Python.

### Missing answer handling:
- A session that has answered zero questions for a feature defaults to 0.5 (neutral).
- The routing engine is responsible for ensuring sufficient coverage before predict is called.

---

## Part D: Exact Feature Vector Layout

```
index | feature_id
------|-----------------
0     | DIM_REALISTIC
1     | DIM_INVESTIGATIVE
2     | DIM_ARTISTIC
3     | DIM_SOCIAL
4     | DIM_ENTERPRISING
5     | DIM_CONVENTIONAL
6     | DIM_DATA_IDEAS
7     | DIM_THINGS_PEOPLE
8     | DIM_BREADTH_DEPTH
9     | DIM_STRUCT_AMBIG
10    | DIM_OFFENSE_DEF
11    | TECH_SERVER_LOGIC
12    | TECH_DATA_STORAGE
13    | TECH_API_DESIGN
14    | TECH_UI_RENDERING
15    | TECH_STATE_MGMT
16    | TECH_BUILD_PIPELINE
17    | TECH_INFRA_PROVISION
18    | TECH_CONTAINER_ORCH
19    | TECH_CLOUD_SERVICES
20    | TECH_STAT_ANALYSIS
21    | TECH_MODEL_BUILDING
22    | TECH_DATA_PIPELINE
23    | TECH_MOBILE_CLIENT
24    | TECH_THREAT_ANALYSIS
25    | TECH_SYSTEM_HARDENING
26    | TECH_TEST_DESIGN
27    | TECH_TEST_AUTOMATION
28    | TECH_OBSERVABILITY
29    | TECH_PERF_OPTIM
30    | TECH_FULL_SPECTRUM
```

Total features: 31. This count is frozen.

---

## Part E: Question Dimension Tag Convention

Each Question entity has `dimension_tags: String[]`.
Each tag must be one of the 31 feature IDs listed above (exact string match).
A question may map to multiple features.

Example:
  dimension_tags = ["DIM_INVESTIGATIVE", "TECH_STAT_ANALYSIS", "TECH_MODEL_BUILDING"]

Internal tags. MUST NOT be sent to the React client.
