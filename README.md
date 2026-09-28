# RoleCompass — AI-Based IT Job Role Prediction System

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.x-61dafb.svg)](https://reactjs.org/)
[![FastAPI](https://img.shields.io/badge/FastAPI-Python-009688.svg)](https://fastapi.tiangolo.com/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Enabled-2496ed.svg)](https://www.docker.com/)

RoleCompass is an intelligent, adaptive career guidance platform that evaluates candidates across technical dimensions and psychometric traits to predict their optimal IT job role among 10 candidate target roles.

The system features a decoupled 2-layer decision architecture: an **Adaptive Routing Engine** (Server-Side Java FSM) that dynamically selects question branches and prunes candidate role sets, and a **Machine Learning Layer** (Python FastAPI with Random Forest Classification) that yields multi-class predictions and confidence probabilities.

---

## 🧭 Target Job Roles

RoleCompass evaluates candidate fit across 10 core technology career paths:

1. **Backend Developer**
2. **Frontend Developer**
3. **Full Stack Developer**
4. **Data Scientist**
5. **Data Engineer**
6. **Cybersecurity Engineer**
7. **DevOps Engineer**
8. **Cloud Engineer**
9. **Android Developer**
10. **QA / Test Automation Engineer**

---

## 🏗️ Architecture & Decision Layers

RoleCompass strictly enforces a **2-Layer Decoupled Architecture**:

```
 ┌─────────────────────────────────────────────────────────────┐
 │                      React Frontend                         │
 │        (Dumb Presentation Client / Dynamic UI)              │
 └──────────────────────────────┬──────────────────────────────┘
                                │ REST API / JWT
 ┌──────────────────────────────▼──────────────────────────────┐
 │               Spring Boot 3 Backend Service                 │
 │                                                             │
 │  ┌───────────────────────────────────────────────────────┐  │
 │  │        Layer 1: Adaptive Routing Engine (FSM)        │  │
 │  │  • Rule-based dynamic question selection             │  │
 │  │  • Candidate role set pruning & thresholding         │  │
 │  │  • Confidence-gated active learning bypass           │  │
 │  └───────────────────────────┬───────────────────────────┘  │
 └──────────────────────────────┼──────────────────────────────┘
                                │ Internal REST (score)
 ┌──────────────────────────────▼──────────────────────────────┐
 │                Python FastAPI ML Microservice               │
 │                                                             │
 │  ┌───────────────────────────────────────────────────────┐  │
 │  │       Layer 2: Machine Learning Prediction Layer      │  │
 │  │  • Random Forest Classifier (31-feature vector)       │  │
 │  │  • Multi-class role probability output                │  │
 │  │  • Multi-model ensemble scoring (Forest + Tree)       │  │
 │  └───────────────────────────────────────────────────────┘  │
 └─────────────────────────────────────────────────────────────┘
```

### Decoupling & Dumb Client Principles

1. **Adaptive Routing Engine (Layer 1)**: Runs entirely on the Spring Boot backend. It maintains state transitions (Section 1 Core, Section 2 Role Gates, Section 3 Pair Resolvers, Section 4 Specialist Probes), updates technical feature vectors, and manages candidate role sets.
2. **Machine Learning Layer (Layer 2)**: Isolated in a Python microservice. It receives feature vectors, evaluates model predictions via a Random Forest Classifier, and returns terminal probabilities.
3. **Dumb Client Presentation**: React client strictly handles rendering questions, collecting Likert responses, and presenting final breakdown visualizer results. It contains **no** RIASEC calculations, psychometric scoring, routing logic, or candidate elimination rules.

---

## 🛠️ Tech Stack

### Frontend
- **Framework**: React 18 with Vite
- **Styling**: Tailwind CSS (Premium Dark Mode / SaaS Aesthetic)
- **Icons & Components**: Lucide React, Custom UI components

### Backend
- **Framework**: Spring Boot 3 (Java 17)
- **Database Access**: Spring Data JPA / Hibernate 6
- **Security**: Spring Security with stateless JJWT 0.12.x authentication
- **Testing**: JUnit 5, AssertJ, Mockito

### ML Microservice
- **Framework**: Python 3.11, FastAPI, Uvicorn
- **ML Engine**: Scikit-Learn (Random Forest, LabelEncoder)
- **Data Pipelines**: NumPy, Pandas, Joblib serialization

### Infrastructure & Operations
- **Database**: PostgreSQL 15
- **Containerization**: Multi-stage Dockerfiles with Nginx reverse proxy & Docker Compose orchestration

---

## 🚀 Quickstart Guide

### Prerequisites
- [Docker & Docker Compose](https://www.docker.com/products/docker-desktop/) installed on your machine
- *Optional for local dev*: Java 17+, Node.js 18+, Python 3.11+

### Option A: One-Command Docker Setup (Recommended)

1. **Clone the repository**:
   ```bash
   git clone https://github.com/your-username/RoleCompass.git
   cd RoleCompass
   ```

2. **Configure Environment Variables**:
   ```bash
   cp .env.example .env
   ```

3. **Build & Start Services**:
   ```bash
   docker compose up --build
   ```

4. **Access the Applications**:
   - **Frontend App**: `http://localhost:5174`
   - **Spring Boot Backend**: `http://localhost:8080/api`
   - **ML FastAPI Docs**: `http://localhost:8000/docs`
   - **PostgreSQL**: `localhost:5433`

---

### Option B: Local Development (Without Docker)

#### 1. Database Setup
Ensure PostgreSQL is running locally on port `5433` with database `rolecompass` (or update `.env` accordingly).

#### 2. ML Microservice Setup
```bash
cd ml
python -m venv .venv
# On Windows:
.venv\Scripts\activate
# On Linux/macOS:
source .venv/bin/activate

pip install -r requirements.txt
uvicorn app:app --host 0.0.0.0 --port 8000 --reload
```

#### 3. Spring Boot Backend Setup
```bash
cd backend
./mvnw spring-boot:run
```

#### 4. React Frontend Setup
```bash
cd frontend
npm install
npm run dev
```

---

## 📋 API Overview

### Backend Endpoints (`/api`)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `POST` | `/api/auth/register` | Register a new candidate account | No |
| `POST` | `/api/auth/login` | Authenticate and receive JWT | No |
| `POST` | `/api/assessment/start` | Initialize a dynamic assessment session | Yes |
| `POST` | `/api/assessment/answer` | Submit question responses & fetch next branch | Yes |
| `GET`  | `/api/assessment/results/{sessionId}` | Retrieve terminal prediction & role vector breakdown | Yes |

### ML Microservice Endpoints (`:8000`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET`  | `/health` | Health check endpoint |
| `POST` | `/predict` | Predict job role from feature vector & candidate list |

---

## 🛡️ Security & Environment Best Practices

- **Secrets Isolation**: Local credentials, DB passwords, and JWT secret keys are loaded from environment variables (`.env`).
- **Never Commit Secrets**: `.env` and local credentials are strictly excluded in `.gitignore`.
- **JWT Authentication**: Tokens are validated statelessly per request using HMAC-SHA256 signature checks.

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).
