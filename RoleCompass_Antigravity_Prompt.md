# RoleCompass — Build Prompt for Antigravity

Paste this entire document as your first message to the agent, or save it as
`AGENTS.md` in the project root so it persists across sessions.

---

## 1. Project Identity

**Name:** RoleCompass
**One-line description:** An adaptive web application that determines a
student's best-fit IT job role, out of 10 candidates, through a Likert-scale
questionnaire that narrows itself in real time, ending in a trained
machine-learning prediction.

---

## 2. Objective

Build a full-stack application with two cooperating engines:

1. An **adaptive routing engine** (rule-based, server-side) that decides which
   question to show next based on how the respondent's answers are narrowing
   down the 10 candidate roles. It never predicts a final answer — only routes.
2. A **predictive engine** (a trained Random Forest classifier) that, once
   routing concludes, takes the complete answer profile and returns the
   final predicted role plus confidence and alternates.

These two engines must remain architecturally separate. Do not let the
routing engine's similarity score double as the final prediction.

---

## 3. Tech Stack — use exactly this, do not substitute

- **Frontend:** React (Vite). A single generic Likert-question component that
  renders `{id, text, options}` from the API and knows nothing about
  dimensions, roles, or scoring.
- **Backend:** Java Spring Boot (Web, JPA, Validation starters). Owns the
  adaptive routing engine and all persistence.
- **Database:** PostgreSQL, schema managed via Flyway migrations (never
  hand-edit the schema outside a migration file).
- **ML service:** Python 3.11 + scikit-learn, exposed as a small FastAPI
  service with a single `POST /score` endpoint. Called by Spring Boot over
  HTTP at the final step — do not embed Python inside the Java process.
- **Repo layout:** monorepo — `/frontend`, `/backend`, `/ml-service`, `/docs`.

---

## 4. Data Model

Create this schema via Flyway migration `V1__init.sql`:

```sql
CREATE TABLE roles (
  id SERIAL PRIMARY KEY,
  name VARCHAR(64) UNIQUE NOT NULL,
  target_vector NUMERIC(3,2)[11] NOT NULL,
  dimension_weights NUMERIC(4,3)[11] NOT NULL
);

CREATE TABLE questions (
  id SERIAL PRIMARY KEY,
  section_id INT NOT NULL,
  block_label VARCHAR(64),
  text TEXT NOT NULL,
  dimension_tags VARCHAR(16)[] NOT NULL,
  reverse_scored BOOLEAN NOT NULL DEFAULT FALSE,
  trigger_predicate JSONB,
  is_resolver BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE sessions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  candidate_role_ids INT[] NOT NULL,
  answered_vector NUMERIC(3,2)[11] NOT NULL
    DEFAULT '{3,3,3,3,3,3,3,3,3,3,3}',
  answered_dims_mask BOOLEAN[11] NOT NULL
    DEFAULT '{f,f,f,f,f,f,f,f,f,f,f}',
  current_section INT NOT NULL DEFAULT 1,
  question_count INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE answers (
  session_id UUID REFERENCES sessions(id),
  question_id INT REFERENCES questions(id),
  likert_value SMALLINT CHECK (likert_value BETWEEN 1 AND 5),
  PRIMARY KEY (session_id, question_id)
);
```

Seed `roles` with these 10 rows (name only for now — populate
`target_vector` and `dimension_weights` once Section 9's synthetic data
generator exists; use placeholder `{3,3,3,3,3,3,3,3,3,3,3}` and
`{1,1,1,1,1,1,1,1,1,1,1}` until then):

Backend Developer, Frontend Developer, Full Stack Developer, Data Scientist,
Data Engineer, Cybersecurity Engineer, DevOps Engineer, Cloud Engineer,
Android Developer, QA/Test Automation Engineer.

---

## 5. Dimension Legend — 11 RIASEC+ psychometric dimensions

Use these exact short codes in `dimension_tags`. "High pole" defines which
direction a raw score of 5 means; items scored toward the opposite pole get
`reverse_scored = true`.

| Code | Name | Origin | High pole means |
|---|---|---|---|
| R | Realistic | RIASEC (Holland, 1959) | hands-on/tangible-systems leaning |
| I | Investigative | RIASEC | analytical/research leaning |
| A | Artistic | RIASEC | creative/expressive leaning |
| S | Social | RIASEC | people/helping leaning |
| E | Enterprising | RIASEC | leadership/persuasion leaning |
| C | Conventional | RIASEC | order/precision leaning |
| DI | Data↔Ideas | Prediger, 1982 | Ideas-leaning (abstract) |
| TP | Things↔People | Prediger, 1982 | People-leaning |
| BD | Breadth↔Depth | project-specific, not validated | Breadth-leaning |
| SA | Structure↔Ambiguity | project-specific, not validated | Ambiguity-tolerant |
| RO | Risk Orientation | project-specific, not validated | Offense-leaning |

## 6. Dimension Legend — 20 skill-matrix dimensions

Use these short codes for Sections 3 and 4: `JAVA`, `PY`, `JSTS`, `KOT`,
`SQL`, `NOSQL`, `BACKEND_FW`, `FRONTEND_FW`, `HTMLCSS`, `RESTAPI`, `STATS`,
`ML`, `DATAENG`, `LINUX`, `CLOUD`, `CICD`, `NETWORK`, `SECURITY`,
`ANDROID_SDK`, `TESTING`. Sections 5 through 8 (not yet built) will use the
remaining codes not covered by Sections 1–4.

---

## 7. Question Bank Seed Data — Sections 1 to 4 (58 items)

Convert every row below into a `questions` insert. Use this JSON shape as
your seed-script record format:

```json
{
  "section_id": 1,
  "block_label": null,
  "text": "...",
  "dimension_tags": ["R"],
  "reverse_scored": false,
  "trigger_predicate": null,
  "is_resolver": false
}
```

All Section 2 items and all Section 4 Block D items must be seeded with
`is_resolver: true`. Every other item across Sections 1, 3, and 4 Blocks
A–C is `is_resolver: false`.

All items use a 5-point Likert scale: 1 = Strongly Disagree, 2 = Disagree,
3 = Neutral, 4 = Agree, 5 = Strongly Agree. `trigger_predicate` is null for
universally-shown items; where a trigger is noted below, encode it as JSONB,
e.g. `{"any_of": ["FULLSTACK"], "any_of_also": ["DATASCI","DATAENG","CYBER","CLOUD","DEVOPS","ANDROID"]}`
— design the exact predicate shape yourself, this is illustrative.

**Administration order (important — this differs from the section numbers
below, which are only for labeling/reporting):**

1. Section 1 (Hobbies & Interests) — mandatory, always first.
2. Section 3 (Programming Skills) — mandatory, always second.
3. From this point on, after every single answer batch, check **all
   resolver blocks** for a satisfied trigger — this includes Section 2's
   three blocks (Breadth↔Depth, Structure↔Ambiguity, Risk Orientation) and
   Section 4's Block D. A resolver is not tied to a fixed position in the
   sequence; it fires the moment its trigger condition becomes true, whether
   that's right after Section 3 or only after Section 5 reveals a new
   ambiguity. This matters because some overlaps (e.g. Full Stack vs.
   Backend) often don't become visible until skill-based sections have run —
   checking resolvers only once, immediately after Section 1, would miss
   them entirely.
4. Sequential gated sections (Section 4, then 5 through 8 once built)
   proceed in order as normal, interleaved with resolver checks after every
   batch.

Section 2 is therefore never entered as its own step — its three blocks are
part of the resolver pool from Step 3 onward, for the entire remainder of
the session, not just once.

### Section 1 — Hobbies & Interests (16 items, section_id=1, no gate, always shown first)

| ID | Text | Dim | Reverse |
|---|---|---|---|
| 1.1 | In my free time, I like taking things apart (gadgets, appliances, old electronics) just to see how they work. | R | N |
| 1.2 | I'd rather spend a weekend building or fixing something physical with my hands than reading about it. | R | N |
| 1.3 | I enjoy digging into a question just for the satisfaction of figuring out the answer myself, even if nobody asked me to. | I | N |
| 1.4 | Puzzles, riddles, or brain-teasers that take real thought are more fun to me than ones I can solve instantly. | I | N |
| 1.5 | I like creating something original — writing, drawing, music, design — more than following a set of instructions exactly. | A | N |
| 1.6 | I get bored doing the same task the same way every time; I'd rather find a new way to do it. | A | N |
| 1.7 | I enjoy explaining something I understand well to a friend who's struggling with it. | S | N |
| 1.8 | Group activities where I'm helping others reach a shared goal feel more satisfying to me than solo activities. | S | N |
| 1.9 | I enjoy convincing others to see things my way or try something new. | E | N |
| 1.10 | Taking charge of a group project and deciding how the work gets divided comes naturally to me. | E | N |
| 1.11 | I like keeping my things (files, room, schedule) organized in a consistent system, even when no one's checking. | C | N |
| 1.12 | I feel a small sense of accomplishment from clearing a to-do list completely, in order. | C | N |
| 1.13 | I'm more interested in working with concrete facts and numbers than with abstract theories and concepts. | DI | Y |
| 1.14 | Given a free afternoon, I'd rather organize a spreadsheet of information than brainstorm new ideas from scratch. | DI | Y |
| 1.15 | I find working with systems, tools, or machines more engaging than working directly with people. | TP | Y |
| 1.16 | I'd rather spend a project mostly interacting with teammates and stakeholders than mostly working alone with data or code. | TP | N |

### Section 2 — Personality (6 items, section_id=2, RESOLVER POOL — see Administration Order above, not a fixed sequence step)

**Block: Breadth↔Depth** — trigger: candidate set contains Full Stack Developer
AND at least one of {Data Scientist, Data Engineer, Cybersecurity Engineer,
Cloud Engineer, DevOps Engineer, Android Developer}. Checked every batch
from Step 3 of Administration Order onward, not just once.

| ID | Text | Dim | Reverse |
|---|---|---|---|
| 2.1 | I'd rather become excellent at one specific skill than be reasonably good at many different skills. | BD | Y |
| 2.2 | When I start a new project, I find myself wanting to explore how all the different parts connect, rather than mastering just one part deeply. | BD | N |

**Block: Structure↔Ambiguity Tolerance** — trigger: candidate set contains
{DevOps Engineer or Cloud Engineer} AND {QA/Test Automation Engineer or
Backend Developer}. Checked every batch, same as above.

| ID | Text | Dim | Reverse |
|---|---|---|---|
| 2.3 | I work best when there's a clear, well-defined process to follow, with little left open to interpretation. | SA | Y |
| 2.4 | I'm comfortable starting a task even when the requirements are vague or likely to change halfway through. | SA | N |

**Block: Risk Orientation (Offense↔Defense)** — trigger: Cybersecurity
Engineer remains in the candidate set alongside at least one other role.
Checked every batch, same as above.

| ID | Text | Dim | Reverse |
|---|---|---|---|
| 2.5 | I find it more interesting to think about how a system could be broken into than how to defend it. | RO | N |
| 2.6 | I get more satisfaction from preventing a problem before it happens than from solving a dramatic problem after it happens. | RO | Y |

### Section 3 — Programming Skills (18 items, section_id=3, no gate, mandatory, administered immediately after Section 1 — see Administration Order above)

| ID | Text | Dim |
|---|---|---|
| 3.1 | I like the idea of building something once as a reusable "template" or "blueprint," and then creating many specific versions from it. | JAVA |
| 3.2 | I prefer writing things in a very explicit, step-by-step way, spelling out every detail, over writing something short and clever that relies on the reader inferring the rest. | JAVA |
| 3.3 | I'm drawn to the idea of building large systems made of many separate, clearly labeled parts that all follow the same strict rules, rather than one big flexible piece. | JAVA |
| 3.4 | I'd rather write something quickly and get immediate results, even if it's a little messy, than spend a long time getting it perfectly structured before testing it. | PY |
| 3.5 | I enjoy tasks that involve gluing very different tools or pieces of information together to make something useful. | PY |
| 3.6 | I like the idea of writing a short set of instructions that could apply to almost any problem — math, data, or automating a boring task. | PY |
| 3.7 | I enjoy building things where I can immediately see a visible result of a change I just made. | JSTS |
| 3.8 | I like the idea of one thing reacting instantly to another — for example, something on screen updating the moment a value changes elsewhere. | JSTS |
| 3.9 | I'm comfortable working in an environment where the rules are flexible and things can be reshaped on the fly, rather than fixed from the start. | JSTS |
| 3.10 | I'd enjoy adding a layer of extra strictness and clearly defined rules on top of something that started out loose and flexible, to make it more reliable. | JSTS |
| 3.11 | I like the idea of building something that lives directly on a device in someone's pocket and responds to their touch and movement. | KOT |
| 3.12 | I'm interested in designing an experience that has to work within a small screen and limited battery or resources, where every interaction has to feel instant. | KOT |
| 3.13 | I enjoy finding a repetitive task I do by hand and figuring out how to make the computer do it for me instead. | SCRIPT |
| 3.14 | I like writing small, quick tools for myself that nobody else will ever see, just to save time. | SCRIPT |
| 3.15 | I'm drawn to connecting different existing programs or systems together so they work as one smooth process. | SCRIPT |
| 3.16 | When something doesn't work the way I expect, I enjoy the process of narrowing down exactly where it went wrong. | GEN |
| 3.17 | I'd rather spend extra time now making something easy to change later, than get it working fast and deal with changes as they come. | GEN |
| 3.18 | Reading someone else's logic and figuring out how it works feels satisfying to me, not tedious. | GEN |

All Section 3 items: `reverse_scored = false`, `trigger_predicate = null`.

### Section 4 — Database: SQL, NoSQL/Big Data (18 items, section_id=4)

**Section gate:** show Section 4 only if candidate set overlaps {Backend
Developer, Full Stack Developer, Data Engineer, Data Scientist, Cloud
Engineer, DevOps Engineer}. Otherwise skip entirely to Section 5.

**Block A — Universal Data-Handling Screener** (shown to everyone who passes
the gate; tag both SQL and NOSQL lightly):

| ID | Text | Dim |
|---|---|---|
| 4.1 | I enjoy taking a big pile of loose facts (names, dates, numbers) and arranging them into neat rows and columns so patterns become obvious. | SQL, NOSQL |
| 4.2 | When looking at a large list of items, I instinctively want to group them by shared properties rather than leave them as one long unsorted list. | SQL, NOSQL |
| 4.3 | I get a small sense of satisfaction from making sure every entry in a list follows the exact same format, with nothing missing or mismatched. | SQL, NOSQL |

**Block B — Relational/SQL Aptitude** — trigger: candidate set overlaps
{Backend Developer, Full Stack Developer, Data Engineer, Data Scientist}.

| ID | Text | Dim |
|---|---|---|
| 4.4 | I like the idea of storing information so that each fact is written down exactly once, and everything else just points back to that one place instead of repeating it. | SQL |
| 4.5 | Given two separate lists that share a common detail (like a customer name appearing in both an order list and a contact list), I'd enjoy figuring out how to combine them accurately. | SQL |
| 4.6 | I prefer systems with strict, predictable structure — a fixed set of fields for every record — over systems where each entry can look completely different. | SQL |
| 4.7 | If two people tried to update the same record at the same time, I'd want the system to strictly enforce which change wins, rather than let it happen messily. | SQL |
| 4.8 | I enjoy writing precise, rule-based questions to filter a large set of records down to exactly the ones that meet several conditions at once. | SQL |
| 4.9 | I'd rather spend time designing the right structure for data before collecting it, than collect data first and figure out the structure later. | SQL |

**Block C — NoSQL/Big Data & Scale Aptitude** — trigger: candidate set
overlaps {Data Engineer, Data Scientist, Cloud Engineer, DevOps Engineer}.

| ID | Text | Dim |
|---|---|---|
| 4.10 | I'm comfortable with information that doesn't fit neatly into fixed categories — some records having extra details others don't. | NOSQL |
| 4.11 | The idea of handling millions of records spread across many machines, where perfect consistency everywhere at every instant isn't guaranteed, doesn't bother me — "good enough, fast enough" feels fine to me. | NOSQL |
| 4.12 | I'd enjoy working with deeply nested information (like a folder inside a folder inside a folder) more than flat, table-like lists. | NOSQL |
| 4.13 | I'm drawn to problems about moving huge amounts of raw information from one place to another efficiently, more than to problems about individual pieces of information. | NOSQL |
| 4.14 | When I imagine a system holding a live, constantly-changing stream of information, I find that more interesting than one holding a fixed snapshot of information. | NOSQL |

**Block D — Data Modeling & Integrity Depth (Overlap Resolver)** — trigger:
2 or more of {Backend Developer, Full Stack Developer, Data Engineer} still
remain after Blocks A–C.

| ID | Text | Dim |
|---|---|---|
| 4.15 | I would rather design how data is organized behind the scenes than build the buttons and screens people use to see that data. | SQL |
| 4.16 | I care more about data being completely accurate and consistent than about it being available instantly, even if that means occasional short delays. | SQL |
| 4.17 | I'm more interested in building the pipeline that continuously collects, cleans, and moves data than in building the everyday application that end users click through. | SQL |
| 4.18 | Given a choice, I'd rather spend a week optimizing how information is stored and retrieved efficiently than a week building a visual interface for it. | SQL |

All Section 4 items: `reverse_scored = false`.

---

## 8. Adaptive Routing Algorithm (implement as a Spring service)

`firstSequentialSectionSatisfying` walks sections 1, 3, 4, 5, 6, 7, 8 in that
order — Section 2 is intentionally excluded from this walk since its blocks
live entirely in the resolver pool (see Administration Order in Section 7).

```
function processAnswerBatch(session, answers):
    for each answer in answers:
        question = lookupQuestion(answer.question_id)
        for dim in question.dimension_tags:
            value = answer.likert_value
            if question.reverse_scored: value = 6 - value
            session.answered_vector[dim] =
                runningMean(session.answered_vector[dim], value)
            session.answered_dims_mask[dim] = true
        persistAnswer(session, answer)

    measured = indicesWhere(session.answered_dims_mask == true)

    for role in session.candidate_role_ids:
        if variance(session.answered_vector[measured]) > EPSILON:
            similarity[role] = weightedCosineSimilarity(
                session.answered_vector[measured],
                role.target_vector[measured],
                role.dimension_weights[measured])
        else:
            similarity[role] = -euclideanDistance(
                session.answered_vector[measured],
                role.target_vector[measured])

    threshold = percentile(similarity.values(), 25)
    session.candidate_role_ids = [r for r in session.candidate_role_ids
                                   if similarity[r] >= threshold]

    // Resolver check runs EVERY batch, independent of current_section.
    // This is what lets a Section 2 block fire late — e.g. after Section 4
    // reveals a Backend/Full Stack ambiguity that didn't exist right after
    // Section 1 — instead of being permanently skipped.
    pending = [r for r in allResolverBlocks()
               if not r.alreadyAskedIn(session)
               and r.triggerSatisfied(session.candidate_role_ids)]

    if pending is not empty:
        return nextQuestionBatch(pending[0], session.candidate_role_ids)

    ready = session.question_count >= MIN_QUESTIONS and (
        len(session.candidate_role_ids) <= 2
        or noSequentialSectionsRemainGated(session)
        or session.question_count >= MAX_QUESTIONS)

    if ready:
        return READY_TO_PREDICT
    else:
        next_section = firstSequentialSectionSatisfying(
            session.candidate_role_ids, session.current_section)
        session.current_section = next_section
        return nextQuestionBatch(next_section, session.candidate_role_ids)
```

Set `MIN_QUESTIONS = 40` and `MAX_QUESTIONS = 100` as configurable constants.

---

## 9. Data Grounding Sources — fetch and use these BEFORE writing the synthetic data generator in Section 10

Do not hand-guess the `target_vector` and `dimension_weights` values in the
`roles` table. Calibrate them from real published data first, using the
sources below, then run the synthetic generator on top of the calibrated
vectors. This is a required step, not optional polish.

**RIASEC dimensions (6 of the 11) — official O\*NET database, free, no
signup required:**
- Career Interest Types file (numeric RIASEC scores per occupation), direct
  CSV download: `https://www.onetcenter.org/dl_files/database/db_31_0_csv/career_interest_types.csv`
- Occupation Data file (O*NET-SOC codes and titles, needed to find the right
  row in the file above), direct CSV: `https://www.onetcenter.org/dl_files/database/db_31_0_csv/occupation_data.csv`
- Full database landing page, all formats (Excel/CSV/JSON/SQL/RDF):
  `https://www.onetcenter.org/database.html`
- License: Creative Commons Attribution 4.0 — free to use, must credit
  O*NET/USDOL.
- How to use: in `occupation_data.csv`, find the closest O*NET-SOC code for
  each of the 10 roles — for example Software Developers for Backend/Full
  Stack, Database Architects or Database Administrators for Data Engineer,
  Information Security Analysts for Cybersecurity Engineer, Network and
  Computer Systems Administrators for DevOps/Cloud, Web Developers for
  Frontend, Software Quality Assurance Analysts and Testers for QA. Then look
  up that code's row in `career_interest_types.csv` and use its R/I/A/S/E/C
  numeric scores as the seed for those six `target_vector` dimensions.

**Skill-role associations (for the 20 skill-matrix dimensions):**
- Stack Overflow Developer Survey 2025, official raw data files (GitHub,
  always the authoritative source for the actual CSV):
  `https://github.com/StackExchange/Survey/tree/main/packages/archive/2025`
- Published results/report, for context and headline stats:
  `https://survey.stackoverflow.co/2025`
- License: survey data under ODbL 1.0.
- How to use: the raw CSV has a developer-role column and per-technology
  usage columns. Group respondents by role and compute which technologies
  appear most often for each, then cross-check that against the skill-tier
  assignments (Primary/Secondary/Minimal) already defined for each role.

**Kaggle — role-to-skill mapping tables, useful for cross-checking tiers:**
- IT Job Roles Skills Dataset: `https://www.kaggle.com/datasets/dhivyadharunaba/it-job-roles-skills-dataset`
- Synthetic candidate profiles mapped to standardized tech roles (good
  structural reference for realistic synthetic data): `https://www.kaggle.com/datasets/ckshetty/candidate-job-role-dataset`
- Job Descriptions 2025 – Tech & Non-Tech Roles (1,100 JDs across 55 roles):
  `https://www.kaggle.com/datasets/adityarajsrv/job-descriptions-2025-tech-and-non-tech-roles`
- Note: Kaggle downloads work directly through the browser; the API/CLI
  route needs a free Kaggle account and an API token.

**What not to search for:** there is no ready-made dataset that already
combines an 11-dimension RIASEC+ profile, a 20-dimension skill matrix, and
these exact 10 role labels — that instrument is custom to this project. The
sources above are for calibrating the synthetic generator's target vectors,
not a drop-in training set.

---

## 10. ML Service Requirements (FastAPI, `/ml-service`)

- Feature vector: 11 RIASEC+ dimensions + 20 skill-matrix dimensions (31
  features). Unmeasured dimensions are imputed as `3` (neutral).
- Generate synthetic training data: for each role, sample
  `target_vector + Gaussian noise` (noise scale informed by that role's
  skill tiers), clip to `[1,5]`, and deliberately blend a controlled fraction
  of rows toward each role's most-confusable neighbor (e.g. Backend Developer
  rows blended toward Full Stack Developer) so classes are not trivially
  separable. Generate 100–150 synthetic rows per role. Use the calibrated
  `target_vector` values from Section 9 above as the basis for this, not
  placeholder values.
- Train a `RandomForestClassifier` (n_estimators=300, max_depth=12,
  class_weight="balanced") and a `DecisionTreeClassifier` baseline. Evaluate
  with 5-fold cross-validated macro F1, not raw accuracy. Serialize with
  `joblib`.
- Expose `POST /score` accepting a 31-length feature vector, returning
  `{predicted_role, confidence, alternates: [{role, confidence}]}`.

---

## 11. API Contract (Spring Boot)

```
POST /session/start
  -> { "session_id": "...", "questions": [
         { "id": 1, "text": "...", "options": [1,2,3,4,5] } ] }

POST /session/{id}/answers
  body: { "answers": [ { "question_id": 12, "likert_value": 4 } ] }
  -> { "status": "continue", "next_questions": [ ... ] }
     or { "status": "ready_to_predict" }

POST /session/{id}/predict
  -> calls ml-service /score internally, then returns:
     { "predicted_role": "Backend Developer",
       "confidence": 0.62,
       "alternates": [
         { "role": "Full Stack Developer", "confidence": 0.21 },
         { "role": "Data Engineer", "confidence": 0.09 } ] }
```

---

## 12. Non-Negotiable Design Rules

1. **Exposure-independence:** every question (existing and any future ones
   you're asked to add) must describe an underlying thinking pattern or
   behavior, never assume the respondent has used a named tool or technology.
2. **Dynamic visibility:** the frontend must never receive `dimension_tags`,
   `reverse_scored`, `trigger_predicate`, or any internal scoring metadata —
   only `{id, text, options}` per question.
3. Sections 5–8 do not exist yet. Build the schema and routing engine so that
   adding them later requires only new seed rows, not a schema or engine
   change.
4. Any future overlap-resolver block (for example, a DevOps-vs-Cloud
   resolver expected in Section 7) must be seeded with `is_resolver: true`
   and picked up automatically by the existing resolver-checking step — do
   not hardcode section-specific resolver logic anywhere in the routing
   service.

---

## 13. Build Order — work through these milestones in sequence, and confirm each is verifiably working before moving to the next

1. **M1 — Schema & seed:** Flyway migration + seed script loading all 58
   questions and 10 roles exactly as specified above. Verify by querying the
   database directly.
2. **M2 — Backend skeleton:** Spring Boot REST controllers matching the API
   contract, backed by the schema, no routing logic yet — `/session/start`
   should just return Section 1's 16 questions.
3. **M3 — Frontend skeleton:** React app that can start a session, render
   questions from the API one batch at a time, and submit answers.
4. **M4 — Routing engine:** implement the algorithm in Section 8, unit-tested
   against at least one all-Neutral session (must trigger the Euclidean
   fallback, not crash), one clearly-narrowing session, and one session where
   a resolver trigger only becomes satisfied after Section 4 — confirm it
   still fires late rather than being silently skipped.
5. **M5 — Data grounding + ML service:** fetch and process the sources in
   Section 9 to calibrate `target_vector`/`dimension_weights`, then build the
   synthetic data generator, train the model, and expose `/score`, with
   cross-validated F1 reported in a script output or log.
6. **M6 — Full integration:** `/session/{id}/predict` wired end-to-end;
   run at least one synthetic profile per role through the full API sequence
   and confirm the correct role comes back as top prediction.

Do not skip ahead to UI polish or styling before M6 is verifiably working.
