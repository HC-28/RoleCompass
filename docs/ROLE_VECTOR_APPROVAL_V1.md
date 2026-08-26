# RoleCompass — Role-Vector Specification & Approval Document

**Document Version:** 1.0  
**Current Status:** `PROPOSED — NOT YET APPROVED`  
**Target Milestone:** ML Specification & Route Elimination Foundation  
**Auditor / Reviewer:** Antigravity  

---

## 1. Dimension Definitions & Polarities

Every dimension in the RoleCompass 11-feature psychological profile is normalized strictly to $[0.0, 1.0]$. The exact definitions and polarities are frozen below.

### 1.1 RIASEC Dimensions (Indices 0–5)
Unipolar scales measuring intensity of interest/aptitude ($0.0 = \text{Low Interest}$, $1.0 = \text{High Interest}$):

- **Index 0 — `DIM_REALISTIC`:** Preference for interacting with physical or virtual machines, operating systems, runtime tooling, and concrete structural mechanisms.
- **Index 1 — `DIM_INVESTIGATIVE`:** Preference for analytical inquiry, algorithmic problem-solving, intellectual research, and scientific hypothesis testing.
- **Index 2 — `DIM_ARTISTIC`:** Preference for aesthetic visual expression, intuitive UI/UX design, and open-ended creative problem framing.
- **Index 3 — `DIM_SOCIAL`:** Preference for helping, teaching, mentoring, and directly collaborating with other people.
- **Index 4 — `DIM_ENTERPRISING`:** Preference for project leadership, initiative, persuasion, and driving business/organizational impact.
- **Index 5 — `DIM_CONVENTIONAL`:** Preference for established standards, procedural precision, meticulous record-keeping, and structured compliance.

### 1.2 Prediger & Project Bipolar Dimensions (Indices 6–10)
Bipolar axes with explicit semantic poles:

- **Index 6 — `DIM_DATA_IDEAS` (Prediger Axis):**
  - $0.0 = \textbf{Ideas Pole}$ — Preference for abstract theories, novel concepts, open-ended possibilities, and conceptual models.
  - $1.0 = \textbf{Data Pole}$ — Preference for concrete facts, numbers, structured empirical records, and quantifiable observations.
- **Index 7 — `DIM_THINGS_PEOPLE` (Prediger Axis):**
  - $0.0 = \textbf{People Pole}$ — Preference for human interactions, social coordination, and interpersonal dynamics.
  - $1.0 = \textbf{Things Pole}$ — Preference for machines, code, system processes, servers, and deterministic mechanisms.
- **Index 8 — `DIM_BREADTH_DEPTH` (Project Axis):**
  - $0.0 = \textbf{Breadth Pole (Generalist)}$ — Preference for working across multiple layers of the technology stack and integrating diverse components.
  - $1.0 = \textbf{Depth Pole (Specialist)}$ — Preference for deep, focused mastery in a specific technical discipline.
- **Index 9 — `DIM_STRUCT_AMBIG` (Project Axis):**
  - $0.0 = \textbf{Ambiguity Tolerance Pole}$ — Comfort operating in fluid, evolving, exploratory, and loosely-defined problem spaces.
  - $1.0 = \textbf{Structure Need Pole}$ — Requirement for clear guidelines, deterministic rules, formal specifications, and defined expectations.
- **Index 10 — `DIM_OFFENSE_DEF` (Project Axis):**
  - $0.0 = \textbf{Defensive Security Pole}$ — Attraction to building safeguards, hardening access controls, audit compliance, and preventive resilience.
  - $0.5 = \textbf{Neutral / Non-Security Posture}$ — Standard software engineering orientation without dedicated security attack/defend emphasis.
  - $1.0 = \textbf{Offensive Security Pole}$ — Attraction to adversarial thinking, attack vector discovery, vulnerability exploitation, and penetration testing.

---

## 2. Proposed Role-Target-Vector Matrix (Annotated)

> [!WARNING]
> Values marked with `[REQUIRES DOMAIN REVIEW]` represent proposed adjustments addressing the contradictions identified in the initial draft (notably the Offense/Defense axis inversion and unipolar scaling).

| ID | Target Role | R | I | A | S | E | C | D-I | T-P | B-D | S-A | O-D | Review Tag |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| **1** | **Backend Developer** | 0.70 | 0.80 | 0.30 | 0.30 | 0.40 | 0.70 | 0.75 | 0.75 | 0.65 | 0.75 | **0.50** | `[REQUIRES DOMAIN REVIEW]` (O-D adjusted from 0.75 to neutral 0.50) |
| **2** | **Frontend Developer** | 0.50 | 0.60 | 0.70 | 0.40 | 0.40 | 0.60 | 0.45 | 0.45 | 0.50 | 0.60 | **0.50** | `[REQUIRES DOMAIN REVIEW]` (O-D adjusted from 0.80 to neutral 0.50) |
| **3** | **Full Stack Developer** | 0.65 | 0.70 | 0.55 | 0.35 | 0.50 | 0.60 | 0.60 | 0.55 | **0.30** | 0.65 | **0.50** | `[REQUIRES DOMAIN REVIEW]` (B-D set to 0.30 generalist, O-D neutral 0.50) |
| **4** | **Data Scientist** | 0.50 | 0.90 | 0.45 | 0.30 | 0.40 | 0.65 | 0.90 | 0.65 | 0.70 | **0.45** | **0.50** | `[REQUIRES DOMAIN REVIEW]` (S-A adjusted to 0.45 ambiguity-tolerant, O-D neutral 0.50) |
| **5** | **Data Engineer** | 0.65 | 0.75 | 0.30 | 0.25 | 0.40 | 0.75 | 0.85 | 0.75 | 0.65 | 0.75 | **0.50** | `[REQUIRES DOMAIN REVIEW]` (O-D adjusted from 0.70 to neutral 0.50) |
| **6** | **Cybersecurity Engineer** | 0.70 | 0.85 | 0.25 | 0.25 | 0.35 | 0.80 | 0.70 | 0.80 | 0.65 | 0.80 | **0.75** | `[REQUIRES DOMAIN REVIEW]` (O-D adjusted from 0.25 to 0.75 offensive threat modeling, or 0.20 for defensive SOC) |
| **7** | **DevOps Engineer** | 0.75 | 0.70 | 0.25 | 0.25 | 0.45 | 0.80 | 0.70 | 0.80 | 0.45 | 0.80 | **0.45** | `[REQUIRES DOMAIN REVIEW]` (O-D adjusted from 0.70 to 0.45 hardening/defense) |
| **8** | **Cloud Engineer** | 0.70 | 0.75 | 0.25 | 0.25 | 0.50 | 0.75 | 0.75 | 0.80 | 0.45 | 0.75 | **0.45** | `[REQUIRES DOMAIN REVIEW]` (O-D adjusted from 0.70 to 0.45 hardening/defense) |
| **9** | **Android Developer** | 0.65 | 0.65 | 0.60 | 0.40 | 0.40 | 0.65 | 0.55 | 0.55 | 0.55 | 0.65 | **0.50** | `[REQUIRES DOMAIN REVIEW]` (O-D adjusted from 0.80 to neutral 0.50) |
| **10** | **QA / Test Automation** | 0.65 | 0.70 | 0.30 | 0.35 | 0.35 | 0.85 | 0.65 | 0.60 | 0.55 | 0.85 | **0.40** | `[REQUIRES DOMAIN REVIEW]` (O-D adjusted to 0.40 defensive verification) |

*Columns: R=Realistic, I=Investigative, A=Artistic, S=Social, E=Enterprising, C=Conventional, D-I=Data-Ideas, T-P=Things-People, B-D=Breadth-Depth, S-A=Structure-Ambiguity, O-D=Offense-Defense.*

---

## 3. Comprehensive Rationale per Target Role

### 1. Backend Developer
- **Strongest Dimensions:** Investigative ($0.80$), Realistic ($0.70$), Conventional ($0.70$), Data ($0.75$), Structure Need ($0.75$).
- **Weakest Dimensions:** Artistic ($0.30$), Social ($0.30$).
- **Top 2–3 Differentiators:** High Data + High Structure + Low Artistic.
- **Intentional Overlaps:** Overlaps with Full Stack (differentiated by higher Depth and lower Artistic) and Data Engineer (differentiated by API/logic vs pipeline/storage).
- **Suspicious/Corrected Values:** Initial draft had $O\text{-}D = 0.75$; corrected to neutral $0.50$.

### 2. Frontend Developer
- **Strongest Dimensions:** Artistic ($0.70$), Investigative ($0.60$), Conventional ($0.60$).
- **Weakest Dimensions:** Social ($0.40$), Depth ($0.50$), Things ($0.45$).
- **Top 2–3 Differentiators:** Highest Artistic ($0.70$), Balanced Data/Ideas ($0.45$).
- **Intentional Overlaps:** Overlaps with Android Developer (differentiated by browser UI vs native mobile platform SDKs).
- **Suspicious/Corrected Values:** Initial draft had $O\text{-}D = 0.80$; corrected to neutral $0.50$.

### 3. Full Stack Developer
- **Strongest Dimensions:** Investigative ($0.70$), Realistic ($0.65$), Artistic ($0.55$), Enterprising ($0.50$).
- **Weakest Dimensions:** Breadth-Depth ($0.30$ — highest breadth/generalist).
- **Top 2–3 Differentiators:** Lowest Depth score ($0.30$) + Moderate Artistic ($0.55$) + Balanced across entire profile.
- **Intentional Overlaps:** Intentionally spans Frontend and Backend profiles.

### 4. Data Scientist
- **Strongest Dimensions:** Investigative ($0.90$), Data-Ideas ($0.90$), Depth ($0.70$).
- **Weakest Dimensions:** Social ($0.30$), Structure Need ($0.45$ — highest ambiguity tolerance).
- **Top 2–3 Differentiators:** Highest Investigative + Highest Data + Lowest Structure Need (High Ambiguity Tolerance for research and modeling).
- **Intentional Overlaps:** Overlaps with Data Engineer (differentiated by Ambiguity Tolerance vs Strict Conventionality).

### 5. Data Engineer
- **Strongest Dimensions:** Data-Ideas ($0.85$), Investigative ($0.75$), Conventional ($0.75$), Structure Need ($0.75$).
- **Weakest Dimensions:** Artistic ($0.30$), Social ($0.25$).
- **Top 2–3 Differentiators:** High Data + High Conventional + High Structure Need.
- **Intentional Overlaps:** Overlaps with Data Scientist on data focus, but requires high structural determinism.

### 6. Cybersecurity Engineer
- **Strongest Dimensions:** Investigative ($0.85$), Conventional ($0.80$), Things ($0.80$), Structure Need ($0.80$), Offense-Defense ($0.75$).
- **Weakest Dimensions:** Artistic ($0.25$), Social ($0.25$).
- **Top 2–3 Differentiators:** High Investigative + High Systemic Skepticism + Distinctive Offense/Defense posture.
- **Suspicious/Corrected Values:** Initial draft had $O\text{-}D = 0.25$; corrected to $0.75$ (adversarial threat identification) or $0.20$ (defensive posture).

### 7. DevOps Engineer
- **Strongest Dimensions:** Realistic ($0.75$), Conventional ($0.80$), Things ($0.80$), Structure Need ($0.80$).
- **Weakest Dimensions:** Artistic ($0.25$), Social ($0.25$).
- **Top 2–3 Differentiators:** Highest Realistic (machine/system automation) + High Structure + Low Artistic.
- **Intentional Overlaps:** Overlaps closely with Cloud Engineer (differentiated in later technical cluster features).

### 8. Cloud Engineer
- **Strongest Dimensions:** Investigative ($0.75$), Realistic ($0.70$), Conventional ($0.75$), Things ($0.80$), Enterprising ($0.50$).
- **Weakest Dimensions:** Artistic ($0.25$), Social ($0.25$).
- **Top 2–3 Differentiators:** High System Architecture + Slightly higher Enterprising than DevOps.
- **Intentional Overlaps:** Overlaps closely with DevOps Engineer.

### 9. Android Developer
- **Strongest Dimensions:** Realistic ($0.65$), Investigative ($0.65$), Artistic ($0.60$), Structure Need ($0.65$).
- **Weakest Dimensions:** Social ($0.40$), Enterprising ($0.40$).
- **Top 2–3 Differentiators:** Moderate-High Artistic + High Client/Device Systems orientation (Realistic).
- **Intentional Overlaps:** Overlaps with Frontend Developer.

### 10. QA / Test Automation Engineer
- **Strongest Dimensions:** Conventional ($0.85$), Structure Need ($0.85$), Investigative ($0.70$), Things ($0.60$).
- **Weakest Dimensions:** Artistic ($0.30$), Enterprising ($0.35$).
- **Top 2–3 Differentiators:** Highest Conventionality + Highest Structure Need + Systematic Defect Hunting.
- **Intentional Overlaps:** Overlaps with Backend on logic verification and Cybersecurity on finding edge-case vulnerabilities.

---

## 4. Known Intentional Overlaps & Resolution

1. **DevOps Engineer $\longleftrightarrow$ Cloud Engineer:**
   - *Psychometric Overlap:* Highly similar across RIASEC and Prediger axes ($\Delta < 0.10$).
   - *Resolution Strategy:* These roles are intentionally not separated purely by psychological questions. Final separation occurs at the ML layer using technical aggregate features 16–19 (CI/CD Pipelines, Container Orchestration vs Managed Cloud Infrastructure).
2. **Backend Developer $\longleftrightarrow$ Full Stack Developer:**
   - *Psychometric Overlap:* Similar backend logic affinity.
   - *Resolution Strategy:* Separated on `DIM_BREADTH_DEPTH` ($0.65$ vs $0.30$) and `DIM_ARTISTIC` ($0.30$ vs $0.55$).
3. **Data Scientist $\longleftrightarrow$ Data Engineer:**
   - *Psychometric Overlap:* High Data orientation ($0.90$ vs $0.85$).
   - *Resolution Strategy:* Separated on `DIM_STRUCT_AMBIG` ($0.45$ ambiguity tolerance vs $0.75$ strict structure) and `DIM_CONVENTIONAL` ($0.65$ vs $0.75$).

---

## 5. Faculty / Owner Decision Items

Prior to approving this matrix for ML synthetic data generation and training, the faculty/project owner must explicitly decide:

1. **Cybersecurity Offense vs Defense Sub-Specialization:**
   - Option A: Target vector represents **Adversarial / Red Team / Penetration Testing** ($O\text{-}D = 0.75 - 0.85$).
   - Option B: Target vector represents **Defensive / SOC / Compliance & Hardening** ($O\text{-}D = 0.15 - 0.25$).
   - Option C: Target vector represents **Balanced General Security Engineering** ($O\text{-}D = 0.50$).
2. **Social Dimension Utility:**
   - Confirm whether Social dimension is purely an out-of-domain rejection filter or should actively reward QA / Full Stack collaborative scores.
3. **Enterprising Elevation:**
   - Confirm whether Cloud Engineer and Full Stack Developer should maintain an Enterprising score of $0.50$ vs $0.40$ for other roles.

---

## 6. Formal Approval Checklist

- [ ] **1. Dimensional Polarities Verified:** All 11 polarities ($0.0 \leftrightarrow 1.0$) are agreed and frozen.
- [ ] **2. Offense ↔ Defense Corrected:** Non-security roles are confirmed at neutral $0.50$.
- [ ] **3. Breadth ↔ Depth Frozen:** Full Stack ($0.30$) vs Specialists confirmed.
- [ ] **4. Structure ↔ Ambiguity Frozen:** Data Scientist ($0.45$) vs QA ($0.85$) confirmed.
- [ ] **5. Domain Consistency Approved:** All 10 role vectors reviewed by domain expert/faculty.
- [ ] **6. Permission to Proceed to ML Milestone:** Approved to generate synthetic training dataset and train Random Forest classifier.

**Sign-off Status:** `PENDING REVIEW & APPROVAL`
