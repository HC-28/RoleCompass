# RoleCompass — Role-Vector Specification Review

**Audited:** 2026-08-22  
**Status:** SPECIFICATION REVIEW ONLY — REQUIRES HUMAN/DOMAIN REVIEW  
**Reference Document:** `docs/model/role-vector-placeholder-v1.md`  

---

## 1. Overview & Objectives

This document conducts a systematic review of the proposed 10 × 11 role-target-vector matrix prior to implementation and ML dataset generation.

The purpose of this review is to:
1. Explicitly define the polarity ($0.0 \leftrightarrow 1.0$) for all 11 psychological and behavioral dimensions.
2. Identify conceptual contradictions, inverted polarities, and ambiguous definitions across the 10 target IT roles.
3. Provide a defensible domain rationale for each role's target vector rather than fitting values artificially for classifier separability.

---

## 2. Dimensional Definitions & Direction Review

| Dimension | Feature Tag | Current Conceptual Definition | Identified Problem | Affected Roles | Recommended Clarification | Review Status |
|---|---|---|---|---|---|---|
| **0. Realistic** | `DIM_REALISTIC` | RIASEC Realistic: Hands-on, tangible, mechanical, tool/machine interaction. | In software/IT, physical tool work does not directly apply. DevOps (0.75) and Cybersecurity (0.70) score high without clear definition of virtual vs physical systems. | DevOps, Cloud, Backend, Cyber | Clarify: Measures preference for interacting with low-level systems, runtime infrastructure, and direct OS/machine mechanics rather than abstract human or business domains. | **REQUIRES DOMAIN REVIEW** |
| **1. Investigative** | `DIM_INVESTIGATIVE` | RIASEC Investigative: Research, analysis, intellectual problem solving. | Range compression (0.60–0.90). Almost all IT roles score high, which diminishes discriminatory power in routing. | All 10 roles (especially Data Science, Cyber, Backend) | Clarify: Distinguish scientific inquiry, hypothesis testing, and algorithmic research (Data Science = 0.90) from routine application development and feature building. | **REQUIRES DOMAIN REVIEW** |
| **2. Artistic** | `DIM_ARTISTIC` | RIASEC Artistic: Creative, expressive, visual design, open-ended layout. | Clear for Frontend (0.70) and Android (0.60), but all non-UI roles are compressed to 0.25–0.30. | Frontend, Android, Full Stack, Data Science | Validated: Measures aesthetic sensibility, client-side visual expression, and open-ended design preferences. | **CLEAR — MINOR CALIBRATION** |
| **3. Social** | `DIM_SOCIAL` | RIASEC Social: Helping, teaching, collaborating, interpersonal empathy. | All tech roles cluster between 0.25 and 0.40. Axis is virtually dormant for candidate role differentiation. | All 10 roles | Clarify: Determine whether Social should differentiate user-advocacy and cross-functional roles (QA, Frontend, Full Stack) or remain purely an exclusionary filter against non-tech paths. | **REQUIRES DOMAIN REVIEW** |
| **4. Enterprising** | `DIM_ENTERPRISING` | RIASEC Enterprising: Leadership, persuasion, initiative, project direction. | Narrow range (0.35–0.50). Cloud (0.50) and Full Stack (0.50) slightly elevated without rigorous rationale. | Cloud, Full Stack, DevOps | Clarify: Measures organizational initiative, project leadership, and desire to drive business outcomes through technical architecture. | **REQUIRES DOMAIN REVIEW** |
| **5. Conventional** | `DIM_CONVENTIONAL` | RIASEC Conventional: Structured, rule-bound, detail-oriented, standardized. | High values for QA (0.85), Cyber (0.80), DevOps (0.80), Data Eng (0.75). Overlaps heavily with Structure ↔ Ambiguity. | QA, Cyber, DevOps, Data Eng, Backend | Clarify: Delineate Conventional (procedural compliance, standards adherence) from Structure-Ambiguity (cognitive need for certainty). | **REQUIRES DOMAIN REVIEW** |
| **6. Data ↔ Ideas** | `DIM_DATA_IDEAS` | Prediger Bipolar Axis: Facts & numbers vs abstract concepts & possibilities. | Polarity was implicit in initial draft, risking inversion during scoring and model training. | Data Scientist, Data Engineer, Backend, Frontend | **Explicit Polarity:**<br>**0.0 = Ideas** (Abstract theories, possibilities, novel concepts)<br>**1.0 = Data** (Concrete facts, numbers, structured empirical records) | **REQUIRES HUMAN/DOMAIN APPROVAL** |
| **7. Things ↔ People** | `DIM_THINGS_PEOPLE` | Prediger Bipolar Axis: Systems/tools vs human interactions. | Polarity was implicit. All IT roles cluster toward "Things" (0.55–0.80). | DevOps, Cloud, Cyber, Frontend | **Explicit Polarity:**<br>**0.0 = People** (Interpersonal interactions, human collaboration)<br>**1.0 = Things** (Machines, systems, technical mechanisms, tools) | **REQUIRES HUMAN/DOMAIN APPROVAL** |
| **8. Breadth ↔ Depth** | `DIM_BREADTH_DEPTH` | Project Axis: Generalist vs deep specialist orientation. | Polarity was ambiguous. Full Stack (0.35) was low while Data Science was high (0.70), but polarity was not formally declared. | Full Stack, Data Science, Specialist Roles | **Explicit Polarity:**<br>**0.0 = Breadth** (Generalist across multiple layers)<br>**1.0 = Depth** (Deep specialist in a dedicated domain) | **REQUIRES HUMAN/DOMAIN APPROVAL** |
| **9. Structure ↔ Ambiguity** | `DIM_STRUCT_AMBIG` | Project Axis: Tolerance for fluid/unclear conditions vs need for strict structure. | Polarity was ambiguous. Data Science (0.50) vs QA (0.85). | Data Science, QA, Cyber, DevOps, Backend | **Explicit Polarity:**<br>**0.0 = Ambiguity Tolerance** (Comfortable with fluid, exploratory, changing tasks)<br>**1.0 = Structure Need** (Requires clear guidelines, deterministic rules, strict standards) | **REQUIRES HUMAN/DOMAIN APPROVAL** |
| **10. Offense ↔ Defense** | `DIM_OFFENSE_DEF` | Project Axis: Security-adjacent risk orientation (Attacker vs Safeguard). | **CRITICAL FLAW:** Initial draft conflated "building software" with "Offense" (Backend 0.75, Frontend 0.80) and "protecting" with "Defense" (Cybersecurity 0.25!). This directly contradicts the RoleCompass security specification. | **ALL 10 ROLES** (Cybersecurity, Backend, Frontend, QA, Android, DevOps) | **Explicit Polarity:**<br>**0.0 = Defensive** (Building safeguards, access controls, compliance, hardening)<br>**1.0 = Offensive** (Adversarial mindset, attack vector discovery, penetration exploitation)<br>**0.5 = Neutral** (Non-security general application engineering) | **CRITICAL — REQUIRES IMMEDIATE CORRECTION** |

---

## 3. In-Depth Analysis of Critical Contradictions

### 3.1 The Offense ↔ Defense Conceptual Inversion
In the initial proposed vector matrix:
- Frontend Developer was assigned `O-D = 0.80`
- Backend Developer was assigned `O-D = 0.75`
- Android Developer was assigned `O-D = 0.80`
- DevOps Engineer was assigned `O-D = 0.70`
- **Cybersecurity Engineer was assigned `O-D = 0.25`**

**Why this is contradictory:**
The author of the initial draft confused *“writing new features”* with *“Offense”* and *“maintaining code”* with *“Defense”*. In the RoleCompass specification, this axis explicitly measures **security-adjacent risk orientation**:
- **Offensive Security (1.0):** Penetration testing, ethical hacking, vulnerability exploitation, adversarial thinking ("how can I break into this?").
- **Defensive Security (0.0):** Hardening systems, implementing firewalls, access control policies, encryption standards, SOC monitoring ("how can I protect this?").
- **Non-Security Software Roles (0.50):** Frontend, Android, Full Stack, and Data Science do not operate with a primary security offense/defense posture; their natural profile on this axis is neutral ($0.50$).

### 3.2 Data ↔ Ideas Polarity
- **Polarity:** $0.0 = \text{Ideas (Abstract)}$ $\longleftrightarrow$ $1.0 = \text{Data (Concrete Facts \& Numbers)}$.
- **Data Scientist:** Highest investigative combined with data focus, but balances exploratory hypothesis generation (Ideas) with empirical modeling (Data). Expected: $0.85 - 0.90$.
- **Data Engineer:** Heavily data/storage focused ($0.85$).
- **Frontend Developer:** Balances layout concept creation (Ideas $\approx 0.40 - 0.50$) with deterministic rendering.

### 3.3 Breadth ↔ Depth Polarity
- **Polarity:** $0.0 = \text{Breadth (Generalist)}$ $\longleftrightarrow$ $1.0 = \text{Depth (Specialist)}$.
- **Full Stack Developer:** Must be low ($0.25 - 0.35$) because the defining psychological characteristic of Full Stack is comfort spanning multiple architectural tiers rather than deep specialization in one.
- **Data Scientist / Cybersecurity Engineer:** High depth ($0.70 - 0.80$).

### 3.4 Structure ↔ Ambiguity Polarity
- **Polarity:** $0.0 = \text{Ambiguity Tolerant / Exploratory}$ $\longleftrightarrow$ $1.0 = \text{Structure Dependent / Strict Rules}$.
- **QA / Test Automation:** Highest structure need ($0.85 - 0.90$) due to deterministic verification, test assertions, and strict specification matching.
- **Data Scientist:** Low structure / high ambiguity tolerance ($0.40 - 0.50$) due to empirical experimentation, unlabelled data exploration, and probabilistic modeling.
- **Cybersecurity:** High structure for policy and compliance, but requires high adaptability for emerging threat vectors.

---

## 4. Summary of Required Actions Before Approval

1. **Owner Sign-Off:** Faculty/owner must formally review and approve the explicit polarities for dimensions 6, 7, 8, 9, and 10.
2. **Correct Offense-Defense Axis in Code & Specs:** Re-align all 10 role vectors so that general software roles sit at neutral $0.50$ and Cybersecurity/QA accurately reflect security postures.
3. **No Synthetic Generation Until Approved:** Freeze dataset generation scripts until the revised matrix in `ROLE_VECTOR_APPROVAL_V1.md` receives formal approval.
