# AI Agents Workflow & Demo Scope

## Project Identity
AI-Based Job Role Prediction System (Demonstration Prototype)(RoleCompass).

## Core Architecture Directives
1. Client-Side: React (Vite) + Tailwind CSS. 
2. Client Logic Constraint: Zero business logic on the client; it only renders received question text and 5 Likert options.
3. Server-Side: Spring Boot + PostgreSQL + Spring Security (JWT).
4. ML Layer (Demo Mock): Spring Boot directly returns a structured prediction JSON payload without calling the external Python service[cite: 1, 3].
5. Question Standard: 5-point Likert scale obeying the exposure-independence rule.

## Active Roles & Responsibilities
- Architect (Gemini): Directs structure and reviews API contracts.
- Backend Builder (Antigravity): Implements User Auth, Question & Session Entities, Seed Data, and Session Endpoints.
- Frontend Builder (Cursor): Implements Login/Register forms with validation, the quiz interface, and a clean result dashboard.