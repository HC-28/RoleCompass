# RoleCompass — Role-Vector Placeholder v1
#
# STATUS: *** PLACEHOLDER — REQUIRES EXPLICIT APPROVAL BEFORE USE ***
#
# This file defines the 10x11 role-target-vector matrix.
# Each row is a role. Each column is a psychological dimension (indices 0-10).
# Values are ideal normalized scores in [0.0, 1.0] for each role.
#
# IMPORTANT:
# - These values are PROPOSED, not approved.
# - DO NOT use these values to generate synthetic training data yet.
# - DO NOT begin ML training until the owner approves this matrix.
# - The routing engine CAN use these for candidate elimination in relative terms,
#   but model accuracy depends on these values being correct.
#
# Column order matches feature-schema-v1.md indices 0-10:
# 0=Realistic, 1=Investigative, 2=Artistic, 3=Social, 4=Enterprising,
# 5=Conventional, 6=Data-Ideas, 7=Things-People, 8=Breadth-Depth,
# 9=Structure-Ambiguity, 10=Offense-Defense
#
# Scale: 0.0 = low, 1.0 = high on each dimension.
# Interpretation: a role's ideal respondent would score approximately these values.

---

## Proposed Role-Target Vectors

| Role                       | R    | I    | A    | S    | E    | C    | D-I  | T-P  | B-D  | S-A  | O-D  |
|----------------------------|------|------|------|------|------|------|------|------|------|------|------|
| Backend Developer (1)      | 0.70 | 0.80 | 0.30 | 0.30 | 0.40 | 0.70 | 0.75 | 0.75 | 0.65 | 0.75 | 0.75 |
| Frontend Developer (2)     | 0.50 | 0.60 | 0.70 | 0.40 | 0.40 | 0.60 | 0.45 | 0.45 | 0.50 | 0.60 | 0.80 |
| Full Stack Developer (3)   | 0.65 | 0.70 | 0.55 | 0.35 | 0.50 | 0.60 | 0.60 | 0.55 | 0.35 | 0.65 | 0.80 |
| Data Scientist (4)         | 0.50 | 0.90 | 0.45 | 0.30 | 0.40 | 0.65 | 0.90 | 0.65 | 0.70 | 0.50 | 0.65 |
| Data Engineer (5)          | 0.65 | 0.75 | 0.30 | 0.25 | 0.40 | 0.75 | 0.85 | 0.75 | 0.65 | 0.75 | 0.70 |
| Cybersecurity Engineer (6) | 0.70 | 0.85 | 0.25 | 0.25 | 0.35 | 0.80 | 0.70 | 0.80 | 0.60 | 0.85 | 0.25 |
| DevOps Engineer (7)        | 0.75 | 0.70 | 0.25 | 0.25 | 0.45 | 0.80 | 0.70 | 0.80 | 0.45 | 0.80 | 0.70 |
| Cloud Engineer (8)         | 0.70 | 0.75 | 0.25 | 0.25 | 0.50 | 0.75 | 0.75 | 0.80 | 0.45 | 0.75 | 0.70 |
| Android Developer (9)      | 0.65 | 0.65 | 0.60 | 0.40 | 0.40 | 0.65 | 0.55 | 0.55 | 0.55 | 0.65 | 0.80 |
| QA / Test Automation (10)  | 0.65 | 0.70 | 0.30 | 0.35 | 0.35 | 0.85 | 0.65 | 0.60 | 0.55 | 0.85 | 0.40 |

Columns: R=Realistic, I=Investigative, A=Artistic, S=Social, E=Enterprising,
         C=Conventional, D-I=Data-Ideas, T-P=Things-People, B-D=Breadth-Depth,
         S-A=Structure-Ambiguity, O-D=Offense-Defense

---

## Rationale Notes (per role)

Backend Developer: High Realistic + Investigative; high Structure; high Offense; data-oriented.
Frontend Developer: High Artistic; mixed data/ideas; high Offense (building UI).
Full Stack Developer: Broad across axes; lower Depth (breadth preference).
Data Scientist: Highest Investigative; highest Data-Ideas; tolerates ambiguity.
Data Engineer: High Conventional + Structure; slightly less Investigative than Data Scientist.
Cybersecurity Engineer: High Investigative + Conventional + Structure-Ambiguity; low Offense (defensive).
DevOps Engineer: High Realistic + Conventional; Things-oriented; high Structure.
Cloud Engineer: Similar to DevOps but slightly higher Enterprising.
Android Developer: Moderate Artistic; Things-oriented; high Offense.
QA/Test Engineer: Highest Conventional + Structure-Ambiguity; lower Offense (defensive role).

---

## Known Intentional Overlaps

- Backend <-> Full Stack: differ mainly on Breadth-Depth and Artistic
- DevOps <-> Cloud: nearly identical — distinguished by technical features 16-19
- Data Scientist <-> Data Engineer: differ on Investigative and Structure-Ambiguity
- Frontend <-> Full Stack: differ on Breadth-Depth
- QA <-> Backend: differ on Conventional, Structure-Ambiguity, Offense-Defense

---

## Approval Required

Before using this matrix for ML training or route elimination scoring:
[ ] Owner has reviewed values for all 10 roles
[ ] Owner confirms Offense-Defense axis direction (high=building vs high=defending)
[ ] Owner confirms Data-Ideas axis direction (high=data vs high=ideas)
[ ] Owner confirms acceptable overlap tolerance for similar roles
