package com.rolecompass.seed;

import com.rolecompass.aggregation.FeatureIndex;
import com.rolecompass.entity.Question;
import com.rolecompass.repository.AnswerRepository;
import com.rolecompass.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * DataSeeder — seeds all 84 assessment questions on first application startup.
 *
 * <p>Section breakdown (user-facing order):</p>
 * <ul>
 *   <li>Section 1 — 16 Psychometric / RIASEC questions (mandatory, 11 dimensions)</li>
 *   <li>Section 2 — 40 Technical Core questions (2 per each of 20 ML features;
 *       Q2 skipped per domain when Q1 answer is extreme ≤2 or ≥4)</li>
 *   <li>Section 3 — 12 Role-Pair Resolver questions (conditional, 6 pairs × 2,
 *       triggered by requires_both predicate)</li>
 *   <li>Section 4 — 16 Specialist Probe questions (conditional, 4 roles × 4,
 *       triggered by requires_any predicate)</li>
 * </ul>
 *
 * <p>The seeder runs only once (guarded by questionRepository.count() check).
 * To force a re-seed after updating question content, manually truncate the
 * {@code answers} then {@code questions} tables before restarting.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long count = questionRepository.count();
        if (count == 84) {
            log.info("All 84 RoleCompass questions already seeded. Skipping DataSeeder.");
            return;
        }

        log.info("Expected 84 questions but found {}. Seeding RoleCompass questions cleanly...", count);
        answerRepository.deleteAllInBatch();
        questionRepository.deleteAllInBatch();

        List<Question> all = new ArrayList<>();
        all.addAll(buildSection1());
        all.addAll(buildSection2Tech());
        all.addAll(buildSection3Resolver());
        all.addAll(buildSection4Specialist());

        questionRepository.saveAll(all);

        log.info("Seeded {} questions: S1={} S2={} S3={} S4={}",
                all.size(),
                buildSection1().size(),
                buildSection2Tech().size(),
                buildSection3Resolver().size(),
                buildSection4Specialist().size());
    }

    // ─── Section 1 — 16 Psychometric / RIASEC Questions ──────────────────────
    // Measures 11 psychometric dimensions: R, I, A, S, E, C, DI, TP, BD, SA, RO.
    // All questions are mandatory (sectionId=1, always triggered).

    public static List<Question> buildSection1() {
        return List.of(

            q(1, "I would rather spend an afternoon physically building, repairing, or tinkering with something tangible, even if it does not turn out perfectly, than studying abstract concepts.",
                new String[]{FeatureIndex.TAG_DIM_REALISTIC}, always()),

            q(1, "When a mechanical or technical setup suddenly stops working with no obvious explanation, my natural reflex is to investigate and troubleshoot it myself.",
                new String[]{FeatureIndex.TAG_DIM_REALISTIC, FeatureIndex.TAG_DIM_THINGS_PEOPLE}, always()),

            q(1, "I enjoy reading in-depth manuals, technical specifications, and system internals to understand how a tool works under the hood, even when not strictly required.",
                new String[]{FeatureIndex.TAG_DIM_INVESTIGATIVE}, always()),

            q(1, "Facing an open-ended problem with no standard textbook solution excites me more than executing a well-documented, routine process.",
                new String[]{FeatureIndex.TAG_DIM_INVESTIGATIVE, FeatureIndex.TAG_DIM_STRUCT_AMBIG}, always()),

            q(1, "I care deeply about visual elegance, layout balance, and polished design — it genuinely bothers me when an experience feels clunky or unrefined.",
                new String[]{FeatureIndex.TAG_DIM_ARTISTIC}, always()),

            q(1, "When assigned a task, I feel overly constrained if strict standards, established patterns, or rigid operating procedures limit my creative freedom.",
                new String[]{FeatureIndex.TAG_DIM_ARTISTIC, FeatureIndex.TAG_DIM_CONVENTIONAL}, always()),

            q(1, "I prefer working within clear, documented frameworks and repeatable processes over inventing my own workflow from scratch every time.",
                new String[]{FeatureIndex.TAG_DIM_CONVENTIONAL}, always()),

            q(1, "When a situation has no clear procedure or predefined steps, my instinct is to pause and wait for formal guidance rather than improvise a solution on the spot.",
                new String[]{FeatureIndex.TAG_DIM_CONVENTIONAL, FeatureIndex.TAG_DIM_STRUCT_AMBIG}, always()),

            q(1, "In group projects or team settings, I naturally step up to set direction, assign responsibilities, and push the group toward decisions rather than waiting for someone else to lead.",
                new String[]{FeatureIndex.TAG_DIM_ENTERPRISING}, always()),

            q(1, "I think through problems far more effectively when talking them out with others and working collaboratively than when working alone in isolation.",
                new String[]{FeatureIndex.TAG_DIM_SOCIAL}, always()),

            q(1, "I trust data, measurable evidence, and hard numbers far more than intuition, gut feeling, or persuasive arguments when making decisions.",
                new String[]{FeatureIndex.TAG_DIM_DATA_IDEAS}, always()),

            q(1, "I find it more fulfilling to improve the performance and reliability of a system than to focus on understanding and shaping how people feel about using it.",
                new String[]{FeatureIndex.TAG_DIM_THINGS_PEOPLE}, always()),

            q(1, "I can stay focused and motivated for hours investigating a problem with no obvious cause, no clear path forward, and no guarantee of finding an answer.",
                new String[]{FeatureIndex.TAG_DIM_STRUCT_AMBIG}, always()),

            q(1, "When I notice a small flaw or imperfection in something I am responsible for, I feel compelled to stop and fix it immediately, even if no one else would notice.",
                new String[]{FeatureIndex.TAG_DIM_BREADTH_DEPTH}, always()),

            q(1, "I would rather build a rough, functional prototype in two days to validate an idea than spend two weeks designing a perfectly rigorous solution before building anything.",
                new String[]{FeatureIndex.TAG_DIM_BREADTH_DEPTH, FeatureIndex.TAG_DIM_DATA_IDEAS}, always()),

            q(1, "When assessing any system, plan, or process, my first instinct is to ask how it could be exploited, misused, or broken — before focusing on how it works in the best-case scenario.",
                new String[]{FeatureIndex.TAG_DIM_OFFENSE_DEF}, always())
        );
    }

    // ─── Section 2 — 40 Technical Core Questions (2 per feature × 20 features) ─
    // Every user receives all applicable questions. The routing engine applies the
    // adaptive skip rule per domain: if Q1 answer is extreme (≤ 2 or ≥ 4), Q2
    // for that domain is skipped (domain is considered resolved).
    // sectionId=2, always triggered — Q1 always, Q2 conditionally via engine.

    public static List<Question> buildSection2Tech() {
        return List.of(

            // FEATURE 01: SERVER LOGIC (TECH_SERVER_LOGIC)
            // Q1 — Core interest
            q(2, "Designing the internal business logic of an application — validating data, coordinating transactions, and routing workflows — sounds like engaging work.",
                new String[]{FeatureIndex.TAG_TECH_SERVER_LOGIC}, always()),
            // Q2 — Engineering reality (skipped if Q1 is extreme)
            q(2, "Understanding what happens when hundreds of clients try to read and write the exact same resource simultaneously sounds like a fascinating puzzle to solve.",
                new String[]{FeatureIndex.TAG_TECH_SERVER_LOGIC}, always()),

            // FEATURE 02: DATA STORAGE (TECH_DATA_STORAGE)
            q(2, "Designing relational database schemas — establishing primary keys, foreign constraints, and indexing strategies to prevent redundancy — sounds appealing.",
                new String[]{FeatureIndex.TAG_TECH_DATA_STORAGE}, always()),
            q(2, "Analysing an execution plan for a slow query and adding composite indices or restructuring joins to reduce execution time from 10 seconds to 5 milliseconds sounds rewarding.",
                new String[]{FeatureIndex.TAG_TECH_DATA_STORAGE}, always()),

            // FEATURE 03: API DESIGN (TECH_API_DESIGN)
            q(2, "Designing clear, consistent API endpoints — REST, GraphQL, or gRPC — that other developers find predictable and effortless to integrate with sounds exciting.",
                new String[]{FeatureIndex.TAG_TECH_API_DESIGN}, always()),
            q(2, "Learning the internal mechanics of protocols like HTTP/2, WebSockets, gRPC streaming, and OAuth2 token lifecycles sounds worthwhile and interesting.",
                new String[]{FeatureIndex.TAG_TECH_API_DESIGN}, always()),

            // FEATURE 04: UI RENDERING (TECH_UI_RENDERING)
            q(2, "Writing code that turns raw data into interactive, animated components that respond immediately to clicks, touches, and gestures sounds rewarding.",
                new String[]{FeatureIndex.TAG_TECH_UI_RENDERING}, always()),
            q(2, "Figuring out why a CSS layout or responsive grid behaves unexpectedly across different viewport sizes and resolving it with clean styling sounds satisfying.",
                new String[]{FeatureIndex.TAG_TECH_UI_RENDERING}, always()),

            // FEATURE 05: STATE MANAGEMENT (TECH_STATE_MGMT)
            q(2, "Managing complex client-side data flows — synchronising caching layers, optimistic UI updates, and reactive component re-renders — sounds exciting.",
                new String[]{FeatureIndex.TAG_TECH_STATE_MGMT}, always()),
            q(2, "Debugging an issue where asynchronous data fetching causes inconsistent screens or race conditions when users click rapidly sounds like an engaging challenge.",
                new String[]{FeatureIndex.TAG_TECH_STATE_MGMT}, always()),

            // FEATURE 06: BUILD PIPELINES (TECH_BUILD_PIPELINE)
            q(2, "Configuring bundlers and build tools — like Vite, Webpack, Maven, or Gradle — so that compilation, tree-shaking, and minification run seamlessly sounds appealing.",
                new String[]{FeatureIndex.TAG_TECH_BUILD_PIPELINE}, always()),
            q(2, "Troubleshooting why a build passes locally but fails during packaging due to dependency graph conflicts or compiler flags is a challenge I am happy to tackle.",
                new String[]{FeatureIndex.TAG_TECH_BUILD_PIPELINE}, always()),

            // FEATURE 07: INFRASTRUCTURE PROVISIONING (TECH_INFRA_PROVISION)
            q(2, "Working in a pure terminal — writing Bash scripts, managing OS processes, and configuring Linux network settings — feels natural and comfortable.",
                new String[]{FeatureIndex.TAG_TECH_INFRA_PROVISION}, always()),
            q(2, "Understanding how DNS routing, IP CIDR blocks, subnets, NAT gateways, and reverse proxies guide network traffic across the internet sounds engaging.",
                new String[]{FeatureIndex.TAG_TECH_INFRA_PROVISION}, always()),

            // FEATURE 08: CONTAINER ORCHESTRATION (TECH_CONTAINER_ORCH)
            q(2, "Packaging applications into lightweight Docker containers to ensure they run identically on any machine or server sounds like modern, disciplined engineering.",
                new String[]{FeatureIndex.TAG_TECH_CONTAINER_ORCH}, always()),
            q(2, "Learning how orchestrators like Kubernetes automatically scale, self-heal, and route traffic across clusters of containers sounds interesting.",
                new String[]{FeatureIndex.TAG_TECH_CONTAINER_ORCH}, always()),

            // FEATURE 09: CLOUD SERVICES (TECH_CLOUD_SERVICES)
            q(2, "Designing an architecture utilising cloud primitives — like AWS S3, Lambda, SQS, DynamoDB, and IAM policies — instead of running a single monolithic server sounds exciting.",
                new String[]{FeatureIndex.TAG_TECH_CLOUD_SERVICES}, always()),
            q(2, "Designing multi-availability-zone architectures that survive regional cloud outages while optimising compute costs sounds like an engaging challenge.",
                new String[]{FeatureIndex.TAG_TECH_CLOUD_SERVICES}, always()),

            // FEATURE 10: STATISTICAL ANALYSIS (TECH_STAT_ANALYSIS)
            q(2, "I am comfortable with statistical concepts like probability distributions, hypothesis testing, confidence intervals, and regression analysis.",
                new String[]{FeatureIndex.TAG_TECH_STAT_ANALYSIS}, always()),
            q(2, "When looking at a metric or trend, I instinctively inspect the sample size, outlier distribution, and whether correlation is masquerading as causation.",
                new String[]{FeatureIndex.TAG_TECH_STAT_ANALYSIS}, always()),

            // FEATURE 11: MODEL BUILDING (TECH_MODEL_BUILDING)
            q(2, "Training, tuning, and evaluating machine learning models — like Random Forests, Gradient Boosters, or Neural Networks — to solve real business problems sounds exciting.",
                new String[]{FeatureIndex.TAG_TECH_MODEL_BUILDING}, always()),
            q(2, "Inspecting the confusion matrix to discover why an algorithm misclassified 50 edge cases out of 1,000, and re-engineering features to fix it, sounds rewarding.",
                new String[]{FeatureIndex.TAG_TECH_MODEL_BUILDING}, always()),

            // FEATURE 12: DATA PIPELINES (TECH_DATA_PIPELINE)
            q(2, "Building pipelines that ingest, transform, and clean millions of streaming events per hour — using tools like Kafka, Spark, or dbt — sounds like thrilling engineering.",
                new String[]{FeatureIndex.TAG_TECH_DATA_PIPELINE}, always()),
            q(2, "Ensuring that analytical datasets are consistent, deduplicated, and validated against strict schemas before being queried by downstream analysts sounds satisfying.",
                new String[]{FeatureIndex.TAG_TECH_DATA_PIPELINE}, always()),

            // FEATURE 13: MOBILE CLIENT (TECH_MOBILE_CLIENT)
            q(2, "Building native mobile applications that respect strict device constraints — battery conservation, memory limits, and offline capability — sounds engaging.",
                new String[]{FeatureIndex.TAG_TECH_MOBILE_CLIENT}, always()),
            q(2, "Mastering the native mobile ecosystem — Kotlin, Android Jetpack Compose, background services, and Google Play guidelines — sounds like a compelling specialisation.",
                new String[]{FeatureIndex.TAG_TECH_MOBILE_CLIENT}, always()),

            // FEATURE 14: THREAT ANALYSIS (TECH_THREAT_ANALYSIS)
            q(2, "When looking at any software architecture or user input form, my first thought is: 'How could an attacker exploit this to inject commands or exfiltrate data?'",
                new String[]{FeatureIndex.TAG_TECH_THREAT_ANALYSIS}, always()),
            q(2, "Researching Common Vulnerabilities and Exposures and reverse-engineering how a security flaw works in practice sounds genuinely exciting.",
                new String[]{FeatureIndex.TAG_TECH_THREAT_ANALYSIS}, always()),

            // FEATURE 15: SYSTEM HARDENING (TECH_SYSTEM_HARDENING)
            q(2, "Implementing least-privilege access controls, TLS certificates, mutual authentication, and cryptographic key rotation to lock down systems sounds satisfying.",
                new String[]{FeatureIndex.TAG_TECH_SYSTEM_HARDENING}, always()),
            q(2, "Reviewing system configurations and audit logs to verify that servers comply with strict security standards and zero-trust policies sounds worthwhile.",
                new String[]{FeatureIndex.TAG_TECH_SYSTEM_HARDENING}, always()),

            // FEATURE 16: TEST DESIGN (TECH_TEST_DESIGN)
            q(2, "Figuring out bizarre, unlikely input scenarios and boundary conditions that developers failed to anticipate, causing the software to crash, gives me genuine satisfaction.",
                new String[]{FeatureIndex.TAG_TECH_TEST_DESIGN}, always()),
            q(2, "Structuring a comprehensive test plan that systematically maps user requirements to test cases to guarantee that no regression reaches production sounds satisfying.",
                new String[]{FeatureIndex.TAG_TECH_TEST_DESIGN}, always()),

            // FEATURE 17: TEST AUTOMATION (TECH_TEST_AUTOMATION)
            q(2, "Writing robust automated end-to-end test suites — using tools like Playwright, Cypress, or Selenium — that simulate real user interactions sounds exciting.",
                new String[]{FeatureIndex.TAG_TECH_TEST_AUTOMATION}, always()),
            q(2, "Investigating why an automated test occasionally fails in CI due to timing differences or network latency and refactoring it to be deterministic sounds satisfying.",
                new String[]{FeatureIndex.TAG_TECH_TEST_AUTOMATION}, always()),

            // FEATURE 18: OBSERVABILITY (TECH_OBSERVABILITY)
            q(2, "Setting up real-time dashboards — Prometheus, Grafana, Datadog — and distributed tracing to monitor latency and error rates across microservices sounds interesting.",
                new String[]{FeatureIndex.TAG_TECH_OBSERVABILITY}, always()),
            q(2, "Analysing server logs, APM traces, and telemetry data during an active outage to pinpoint the exact root cause of a latency spike sounds like a challenge I would enjoy.",
                new String[]{FeatureIndex.TAG_TECH_OBSERVABILITY}, always()),

            // FEATURE 19: PERFORMANCE OPTIMISATION (TECH_PERF_OPTIM)
            q(2, "Attaching a CPU and memory profiler to an application to detect memory leaks, garbage collection pauses, or thread contention sounds like a deeply engaging technical task.",
                new String[]{FeatureIndex.TAG_TECH_PERF_OPTIM}, always()),
            q(2, "I enjoy reasoning through the trade-offs between cache invalidation strategies, network latency, and eventual consistency when scaling distributed systems.",
                new String[]{FeatureIndex.TAG_TECH_PERF_OPTIM}, always()),

            // FEATURE 20: FULL SPECTRUM DELIVERY (TECH_FULL_SPECTRUM)
            q(2, "I get more fulfilment from shipping a complete, working product by writing both the frontend interface and the backend API myself than from focusing exclusively on one side.",
                new String[]{FeatureIndex.TAG_TECH_FULL_SPECTRUM}, always()),
            q(2, "I would rather pick an existing library or framework to get a customer feature working quickly than spend three days writing a custom, theoretically pure solution.",
                new String[]{FeatureIndex.TAG_TECH_FULL_SPECTRUM}, always())
        );
    }

    // ─── Section 3 — 12 Role-Pair Resolver Questions (Conditional) ───────────
    // 6 competing role pairs × 2 questions each.
    // Each question has a requires_both predicate; only fired if both roles
    // in the pair are still active candidates after Section 2 pruning.
    // sectionId=3.

    public static List<Question> buildSection3Resolver() {
        return List.of(

            // PAIR A: BACKEND DEVELOPER vs FULL STACK DEVELOPER
            q(3, "I would rather have broad competence across the full stack to build end-to-end products independently, than deep expertise in database internals and distributed locking.",
                new String[]{FeatureIndex.TAG_TECH_SERVER_LOGIC, FeatureIndex.TAG_TECH_FULL_SPECTRUM},
                pred("Backend Developer", "Full Stack Developer")),

            q(3, "Seeing real users interact with features I built matters more to me than whether the underlying server architecture is mathematically elegant.",
                new String[]{FeatureIndex.TAG_TECH_FULL_SPECTRUM, FeatureIndex.TAG_TECH_SERVER_LOGIC},
                pred("Backend Developer", "Full Stack Developer")),

            // PAIR B: FRONTEND DEVELOPER vs ANDROID DEVELOPER
            q(3, "I prefer building for the open web — with browsers, instant deployments, and cross-platform URLs — over packaging releases for mobile app stores.",
                new String[]{FeatureIndex.TAG_TECH_UI_RENDERING, FeatureIndex.TAG_TECH_MOBILE_CLIENT},
                pred("Frontend Developer", "Android Developer")),

            q(3, "Integrating mobile hardware — camera, GPS, Bluetooth, biometric sensors — into an app sounds more appealing than optimising desktop responsive web layouts.",
                new String[]{FeatureIndex.TAG_TECH_MOBILE_CLIENT, FeatureIndex.TAG_TECH_UI_RENDERING},
                pred("Frontend Developer", "Android Developer")),

            // PAIR C: DEVOPS ENGINEER vs CLOUD ENGINEER
            q(3, "I am more interested in accelerating developer delivery pipelines — CI/CD, automated testing, release velocity — than in designing multi-region cloud VPC architectures.",
                new String[]{FeatureIndex.TAG_TECH_BUILD_PIPELINE, FeatureIndex.TAG_TECH_CLOUD_SERVICES},
                pred("DevOps Engineer", "Cloud Engineer")),

            q(3, "Writing Terraform templates to provision cloud clusters and load balancers excites me more than configuring automated build and deployment pipelines.",
                new String[]{FeatureIndex.TAG_TECH_CLOUD_SERVICES, FeatureIndex.TAG_TECH_INFRA_PROVISION},
                pred("DevOps Engineer", "Cloud Engineer")),

            // PAIR D: DATA SCIENTIST vs DATA ENGINEER
            q(3, "I would rather discover a non-obvious statistical correlation that guides business strategy than build the distributed pipeline that reliably imports that data.",
                new String[]{FeatureIndex.TAG_TECH_STAT_ANALYSIS, FeatureIndex.TAG_TECH_DATA_PIPELINE},
                pred("Data Scientist", "Data Engineer")),

            q(3, "If a machine learning model achieves 90% accuracy, my priority is improving it to 94%, rather than optimising its execution latency to under 10 milliseconds.",
                new String[]{FeatureIndex.TAG_TECH_MODEL_BUILDING, FeatureIndex.TAG_TECH_DATA_PIPELINE},
                pred("Data Scientist", "Data Engineer")),

            // PAIR E: CYBERSECURITY ENGINEER vs QA / TEST AUTOMATION ENGINEER
            q(3, "I am more excited by probing how a malicious attacker could break into a system than by verifying that application buttons and forms adhere to product specifications.",
                new String[]{FeatureIndex.TAG_TECH_THREAT_ANALYSIS, FeatureIndex.TAG_TECH_TEST_DESIGN},
                pred("Cybersecurity Engineer", "QA / Test Automation Engineer")),

            q(3, "Preventing an external data breach or ransomware threat matters more to me than ensuring an application never crashes during standard user workflows.",
                new String[]{FeatureIndex.TAG_TECH_SYSTEM_HARDENING, FeatureIndex.TAG_TECH_TEST_AUTOMATION},
                pred("Cybersecurity Engineer", "QA / Test Automation Engineer")),

            // PAIR F: FULL STACK DEVELOPER vs FRONTEND DEVELOPER
            q(3, "I would rather spend an extra day perfecting micro-interactions and CSS transitions than spend that day integrating the frontend with database queries and authentication tokens.",
                new String[]{FeatureIndex.TAG_TECH_UI_RENDERING, FeatureIndex.TAG_TECH_FULL_SPECTRUM},
                pred("Full Stack Developer", "Frontend Developer")),

            q(3, "I feel frustrated when required to write database queries or configure backend server routes — I prefer staying exclusively in the UI component and layout layer.",
                new String[]{FeatureIndex.TAG_TECH_FULL_SPECTRUM, FeatureIndex.TAG_TECH_UI_RENDERING},
                pred("Full Stack Developer", "Frontend Developer"))
        );
    }

    // ─── Section 4 — 16 Specialist Probe Questions (Conditional) ─────────────
    // 4 specialist roles × 4 dedicated probe questions each.
    // Each group triggered by a requires_any predicate for its role.
    // Serves ONLY when that role is still in the surviving candidate set.
    // Roles with probes: Data Scientist, Data Engineer, Cybersecurity, DevOps.
    // sectionId=4.

    public static List<Question> buildSection4Specialist() {
        return List.of(

            // ── DATA SCIENTIST (4 probes) ──────────────────────────────────────
            q(4, "When an algorithm gives surprising outputs, I instinctively reach for probability theory or linear algebra to inspect why, rather than just switching to a different library.",
                new String[]{FeatureIndex.TAG_TECH_STAT_ANALYSIS, FeatureIndex.TAG_TECH_MODEL_BUILDING},
                anyOf("Data Scientist")),

            q(4, "I am comfortable with the reality that 70% of exploratory data science hypotheses fail, and proving what does not work is still considered valuable scientific progress.",
                new String[]{FeatureIndex.TAG_TECH_MODEL_BUILDING},
                anyOf("Data Scientist")),

            q(4, "My immediate reflex when receiving a dataset is to audit missing values, sampling bias, and label leakage before writing any modelling code.",
                new String[]{FeatureIndex.TAG_TECH_STAT_ANALYSIS},
                anyOf("Data Scientist")),

            q(4, "I enjoy translating complex technical models into clear, intuitive visual dashboards and narratives that non-technical leaders can confidently make decisions from.",
                new String[]{FeatureIndex.TAG_TECH_MODEL_BUILDING, FeatureIndex.TAG_TECH_STAT_ANALYSIS},
                anyOf("Data Scientist")),

            // ── DATA ENGINEER (4 probes) ──────────────────────────────────────
            q(4, "I care deeply about strict schema enforcement, data contracts, and ensuring that upstream API updates never break downstream analytical data pipelines.",
                new String[]{FeatureIndex.TAG_TECH_DATA_PIPELINE, FeatureIndex.TAG_TECH_DATA_STORAGE},
                anyOf("Data Engineer")),

            q(4, "Architecting a Spark or Flink cluster to process terabytes of event logs in parallel sounds more exciting than tuning hyper-parameters on a single machine.",
                new String[]{FeatureIndex.TAG_TECH_DATA_PIPELINE},
                anyOf("Data Engineer")),

            q(4, "When designing an ETL pipeline, I spend as much energy designing automated retries, checkpointing, and idempotent reruns as I do on the happy path.",
                new String[]{FeatureIndex.TAG_TECH_DATA_PIPELINE, FeatureIndex.TAG_TECH_OBSERVABILITY},
                anyOf("Data Engineer")),

            q(4, "I genuinely enjoy writing complex analytical SQL — using window functions, partition clauses, and recursive CTEs — to aggregate data directly in the database.",
                new String[]{FeatureIndex.TAG_TECH_DATA_STORAGE, FeatureIndex.TAG_TECH_DATA_PIPELINE},
                anyOf("Data Engineer")),

            // ── CYBERSECURITY ENGINEER (4 probes) ─────────────────────────────
            q(4, "I have the persistence to comb through thousands of lines of network packet captures, memory dumps, or authentication logs to hunt down a single subtle anomaly.",
                new String[]{FeatureIndex.TAG_TECH_THREAT_ANALYSIS},
                anyOf("Cybersecurity Engineer")),

            q(4, "When using any software, I instinctively test input fields for boundary flaws like SQL injection, cross-site scripting, and unauthorised path traversals.",
                new String[]{FeatureIndex.TAG_TECH_THREAT_ANALYSIS, FeatureIndex.TAG_TECH_SYSTEM_HARDENING},
                anyOf("Cybersecurity Engineer")),

            q(4, "After finding a critical vulnerability, I am just as motivated to design and verify the architectural fix as I was to discover the original exploit.",
                new String[]{FeatureIndex.TAG_TECH_SYSTEM_HARDENING, FeatureIndex.TAG_TECH_THREAT_ANALYSIS},
                anyOf("Cybersecurity Engineer")),

            q(4, "I believe security policies must be designed to empower everyday users — avoiding overly restrictive hurdles that tempt employees to work around controls.",
                new String[]{FeatureIndex.TAG_TECH_SYSTEM_HARDENING},
                anyOf("Cybersecurity Engineer")),

            // ── DEVOPS ENGINEER (4 probes) ────────────────────────────────────
            q(4, "I believe a high-performing engineering organisation should enable developers to push code to production-like staging environments in under fifteen minutes, safely and automatically.",
                new String[]{FeatureIndex.TAG_TECH_BUILD_PIPELINE, FeatureIndex.TAG_TECH_INFRA_PROVISION},
                anyOf("DevOps Engineer")),

            q(4, "When a production incident occurs, my focus is immediately on fixing the system vulnerability and updating automated safeguards, rather than determining which individual made the error.",
                new String[]{FeatureIndex.TAG_TECH_OBSERVABILITY, FeatureIndex.TAG_TECH_INFRA_PROVISION},
                anyOf("DevOps Engineer")),

            q(4, "I strongly believe that every server, firewall rule, and cluster configuration must be declared in version-controlled code — manual production configuration changes are never acceptable.",
                new String[]{FeatureIndex.TAG_TECH_INFRA_PROVISION, FeatureIndex.TAG_TECH_CONTAINER_ORCH},
                anyOf("DevOps Engineer")),

            q(4, "I am eager to invest two full days writing an automated deployment pipeline to permanently eliminate a repetitive manual task that currently takes thirty minutes each time.",
                new String[]{FeatureIndex.TAG_TECH_BUILD_PIPELINE, FeatureIndex.TAG_TECH_CONTAINER_ORCH},
                anyOf("DevOps Engineer"))
        );
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /** Builds a Question with a given trigger predicate JSON string. */
    private static Question q(int section, String text, String[] tags, String predicate) {
        return Question.builder()
                .sectionId(section)
                .text(text)
                .dimensionTags(tags)
                .triggerPredicate(predicate)
                .build();
    }

    /** Trigger predicate: always serve this question. */
    private static String always() {
        return "{\"always\": true}";
    }

    /**
     * Trigger predicate: serve this question only when BOTH named roles are still
     * in the surviving candidate set (Section 3 resolver pairs).
     */
    private static String pred(String roleA, String roleB) {
        return String.format("{\"requires_both\": [\"%s\", \"%s\"]}", roleA, roleB);
    }

    /**
     * Trigger predicate: serve this question only when the given specialist role
     * is still in the surviving candidate set (Section 4 specialist probes).
     */
    private static String anyOf(String role) {
        return String.format("{\"requires_any\": [\"%s\"]}", role);
    }
}
