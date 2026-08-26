# RoleCompass — AGENTS.md

# 1. PROJECT IDENTITY

Project Name: RoleCompass

Project Type:
AI-Based Job Role Prediction System

Current State:
Demonstration Prototype / Vertical Slice

RoleCompass predicts the best-fit IT job role for a student from these 10 target roles:

1. Backend Developer
2. Frontend Developer
3. Full Stack Developer
4. Data Scientist
5. Data Engineer
6. Cybersecurity Engineer
7. DevOps Engineer
8. Cloud Engineer
9. Android Developer
10. QA / Test Automation Engineer

The system consists of two distinct decision layers:

1. Adaptive Routing Engine
  - Rule-based
  - Server-side
  - Decides what questions should be asked next
  - Maintains the candidate-role set
  - Does NOT make the final prediction

2. Machine Learning Prediction Layer
  - Random Forest classifier
  - Receives the final 31-feature vector
  - Performs the terminal ten-class prediction
  - Returns the top predicted role and probabilities

These two layers MUST remain separate.

---

# 2. TECHNOLOGY STACK

## Frontend

- React
- Vite
- Tailwind CSS
- Premium SaaS design
- Dark mode / light mode

## Backend

- Spring Boot 3
- Java 17
- Spring Data JPA
- Hibernate 6
- Spring Security
- JJWT 0.12.x

## Database

- PostgreSQL 15
- Local development through Docker
- Database name: rolecompass

## ML Service

- Python
- FastAPI
- Planned Random Forest model
- Current prediction endpoint may still be mocked for vertical-slice development

---

# 3. NON-NEGOTIABLE ARCHITECTURE RULES

## 3.1 DUMB CLIENT RULE

React is a presentation client only.

React MUST NOT contain:

- RIASEC calculations
- psychometric scoring
- technical scoring
- feature aggregation
- candidate-role elimination
- routing predicates
- role prediction
- machine-learning logic
- business rules based on answers

React only:

1. receives question payloads from the backend
2. renders question text
3. renders response options
4. collects Likert values
5. sends answers to the backend
6. renders server responses
7. renders the final result

The frontend should conceptually only understand:

```json
{
  "id": 123,
  "text": "Question text",
  "options": [1, 2, 3, 4, 5]
}