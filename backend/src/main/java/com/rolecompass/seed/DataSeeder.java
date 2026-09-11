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
 * DataSeeder — seeds all 58 assessment questions on application startup.
 *
 * <p>Section breakdown:</p>
 * <ul>
 *   <li>Section 1 — 16 Psychometric/Hobbies questions (measures 11 dimensions)</li>
 *   <li>Section 2 — 6 Resolver questions (conditional, triggered by tie predicates)</li>
 *   <li>Section 3 — 20 Technical Core questions (measures 20 tech features)</li>
 *   <li>Section 4 — 16 Database Deep-Dive questions (conditional, gated)</li>
 * </ul>
 *
 * <p>Questions are wiped and re-seeded on every startup to keep content
 * authoritative. Answers are deleted first to preserve referential integrity.</p>
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
        log.info("Refreshing RoleCompass questions...");

        List<Question> all = new ArrayList<>();
        all.addAll(buildSection1());
        all.addAll(buildSection2Resolver());
        all.addAll(buildSection3Tech());
        all.addAll(buildSection4Gated());

        answerRepository.deleteAll();
        questionRepository.deleteAll();
        questionRepository.saveAll(all);

        log.info("Seeded {} questions: S1={} S2={} S3={} S4={}",
                all.size(), buildSection1().size(), buildSection2Resolver().size(),
                buildSection3Tech().size(), buildSection4Gated().size());
    }

    // ─── Section 1 — 16 Psychometric Questions ────────────────────────────────
    // Tags: 11 psychometric dimensions (DIM_REALISTIC .. DIM_OFFENSE_DEF)
    // Covers R(×3) I(×3) A(×2) S(×1) E(×1) C(×2) DI(×1) TP(×1) BD(×1) SA(×1) RO(×0, inferred via offense-def)

    public static List<Question> buildSection1() {
        return List.of(

            // REALISTIC (R) — hands-on, physical, mechanical
            q(1, "In my free time, I like taking things apart — gadgets, appliances, old electronics — just to see how they work.",
                new String[]{FeatureIndex.TAG_DIM_REALISTIC}, always()),

            q(1, "I prefer tasks that produce a tangible, physical or functional result over tasks that generate ideas or documents.",
                new String[]{FeatureIndex.TAG_DIM_REALISTIC}, always()),

            q(1, "I would rather build or configure a working system than write a proposal about how it could be built.",
                new String[]{FeatureIndex.TAG_DIM_REALISTIC, FeatureIndex.TAG_DIM_THINGS_PEOPLE}, always()),

            // INVESTIGATIVE (I) — analytical, intellectual, research-oriented
            q(1, "I enjoy digging into a question just for the satisfaction of figuring out the answer myself, even without any external deadline.",
                new String[]{FeatureIndex.TAG_DIM_INVESTIGATIVE}, always()),

            q(1, "When I encounter an unexplained bug or anomaly, I feel compelled to understand exactly why it happened before moving on.",
                new String[]{FeatureIndex.TAG_DIM_INVESTIGATIVE}, always()),

            q(1, "I find abstract problems — ones with no obvious right answer — more intellectually stimulating than problems with clear-cut solutions.",
                new String[]{FeatureIndex.TAG_DIM_INVESTIGATIVE, FeatureIndex.TAG_DIM_DATA_IDEAS}, always()),

            // ARTISTIC (A) — creative, expressive, aesthetic
            q(1, "I like creating something original rather than following a set of instructions exactly.",
                new String[]{FeatureIndex.TAG_DIM_ARTISTIC}, always()),

            q(1, "Fine-tuning how something looks and feels until it seems just right appeals to me as much as making it technically correct.",
                new String[]{FeatureIndex.TAG_DIM_ARTISTIC}, always()),

            // SOCIAL (S) — cooperative, helping, interpersonal
            q(1, "I enjoy explaining something I understand well to a friend who is struggling with it, even if I have to spend extra time doing it.",
                new String[]{FeatureIndex.TAG_DIM_SOCIAL}, always()),

            // ENTERPRISING (E) — leading, influencing, ambitious
            q(1, "Taking charge of a group project and deciding how the work gets divided comes naturally to me.",
                new String[]{FeatureIndex.TAG_DIM_ENTERPRISING}, always()),

            // CONVENTIONAL (C) — orderly, procedural, detail-oriented
            q(1, "I like keeping my files, notes, and workflow organized in a consistent, repeatable system.",
                new String[]{FeatureIndex.TAG_DIM_CONVENTIONAL}, always()),

            q(1, "I prefer having clear rules, structured routines, and well-defined standards for my work over open-ended creative freedom.",
                new String[]{FeatureIndex.TAG_DIM_CONVENTIONAL, FeatureIndex.TAG_DIM_STRUCT_AMBIG}, always()),

            // DATA-IDEAS axis (DI) — Prediger: data-driven vs. idea-driven
            q(1, "I am more interested in working with concrete facts and numbers than with abstract theories and untested concepts.",
                new String[]{FeatureIndex.TAG_DIM_DATA_IDEAS}, always()),

            // THINGS-PEOPLE axis (TP) — Prediger: things/tools vs. people
            q(1, "I find working with systems, tools, or machines more engaging than working directly with other people.",
                new String[]{FeatureIndex.TAG_DIM_THINGS_PEOPLE}, always()),

            // BREADTH-DEPTH axis (BD) — specialist vs. generalist
            q(1, "I enjoy diving deep into a single subject to master it fully, rather than having broad surface-level knowledge of many topics.",
                new String[]{FeatureIndex.TAG_DIM_BREADTH_DEPTH}, always()),

            // OFFENSE-DEFENSE orientation (RO) — adversarial mindset
            q(1, "I find it more satisfying to discover how something can be broken or bypassed than to design the safeguards that prevent it.",
                new String[]{FeatureIndex.TAG_DIM_OFFENSE_DEF}, always())
        );
    }

    // ─── Section 2 — 6 Resolver Questions (Conditional) ──────────────────────
    // Each question has a trigger_predicate. The routing engine checks whether
    // BOTH roles in "requires_both" are still in the candidate set before serving.
    // sectionId=2, triggerPredicate is JSONB.

    public static List<Question> buildSection2Resolver() {
        return List.of(

            // DevOps vs Cloud — distinguished by infra automation vs. cloud-native services
            q(2, "When deploying a new service, I am more excited by automating the build-test-release pipeline and server configuration than by designing which managed cloud services to wire together.",
                new String[]{FeatureIndex.TAG_TECH_BUILD_PIPELINE, FeatureIndex.TAG_TECH_INFRA_PROVISION},
                pred("DevOps Engineer", "Cloud Engineer")),

            q(2, "I prefer writing scripts that turn raw machines into configured servers over clicking through a cloud console to provision managed services.",
                new String[]{FeatureIndex.TAG_TECH_INFRA_PROVISION, FeatureIndex.TAG_TECH_CLOUD_SERVICES},
                pred("DevOps Engineer", "Cloud Engineer")),

            // Backend vs Full Stack — distinguished by API focus vs. full-spectrum breadth
            q(2, "I am more satisfied spending all my time writing the APIs and business logic than also maintaining the front-end interface that consumes them.",
                new String[]{FeatureIndex.TAG_TECH_SERVER_LOGIC, FeatureIndex.TAG_TECH_FULL_SPECTRUM},
                pred("Backend Developer", "Full Stack Developer")),

            // Data Scientist vs Data Engineer — ML models vs. pipelines
            q(2, "When working with data, I find building and tuning predictive models more rewarding than engineering the pipelines that collect and move the data.",
                new String[]{FeatureIndex.TAG_TECH_MODEL_BUILDING, FeatureIndex.TAG_TECH_DATA_PIPELINE},
                pred("Data Scientist", "Data Engineer")),

            // Frontend vs Android — web UI vs. native mobile
            q(2, "I prefer building interactive experiences in a web browser over building them as an installed application on a mobile device.",
                new String[]{FeatureIndex.TAG_TECH_UI_RENDERING, FeatureIndex.TAG_TECH_MOBILE_CLIENT},
                pred("Frontend Developer", "Android Developer")),

            // Cybersecurity vs QA — adversarial testing vs. functional testing
            q(2, "I am more interested in probing a system for security vulnerabilities than in systematically verifying that it meets its functional requirements.",
                new String[]{FeatureIndex.TAG_TECH_THREAT_ANALYSIS, FeatureIndex.TAG_TECH_TEST_DESIGN},
                pred("Cybersecurity Engineer", "QA / Test Automation Engineer"))
        );
    }

    // ─── Section 3 — 20 Technical Core Questions ──────────────────────────────
    // Two questions per tech feature, covering all 20 ML features.
    // sectionId=3, always triggered.

    public static List<Question> buildSection3Tech() {
        return List.of(

            // SERVER — server-side logic
            q(3, "When I design a process that receives many simultaneous requests, I enjoy deciding how data should flow, be validated, and transformed before a response is produced.",
                new String[]{FeatureIndex.TAG_TECH_SERVER_LOGIC}, always()),
            q(3, "I prefer working out the complex, unseen rules that make a system function correctly rather than designing what the end user sees.",
                new String[]{FeatureIndex.TAG_TECH_SERVER_LOGIC}, always()),

            // STORAGE — data storage & retrieval
            q(3, "I am fascinated by how to organise large amounts of information efficiently so it can be quickly retrieved when needed.",
                new String[]{FeatureIndex.TAG_TECH_DATA_STORAGE}, always()),
            q(3, "Thinking about the underlying structure of information and how different pieces relate to one another is a puzzle I enjoy solving.",
                new String[]{FeatureIndex.TAG_TECH_DATA_STORAGE}, always()),

            // API — API design & contracts
            q(3, "I like defining clear, strict contracts about how different parts of a system should communicate with each other.",
                new String[]{FeatureIndex.TAG_TECH_API_DESIGN}, always()),
            q(3, "I find it satisfying to design an interface that makes it easy for other developers to integrate with my system without knowing its internal details.",
                new String[]{FeatureIndex.TAG_TECH_API_DESIGN}, always()),

            // UI — user interface rendering
            q(3, "I get a strong sense of accomplishment from translating a visual design into a structured, interactive display.",
                new String[]{FeatureIndex.TAG_TECH_UI_RENDERING}, always()),
            q(3, "Fine-tuning the layout, colours, and responsiveness of an interface to ensure a great user experience is highly appealing to me.",
                new String[]{FeatureIndex.TAG_TECH_UI_RENDERING}, always()),

            // STATE — state management
            q(3, "I enjoy the challenge of keeping track of what information should be displayed on a screen as the user interacts with various elements over time.",
                new String[]{FeatureIndex.TAG_TECH_STATE_MGMT}, always()),
            q(3, "Figuring out how to synchronize changing information across different parts of a complex interactive application is a puzzle I like solving.",
                new String[]{FeatureIndex.TAG_TECH_STATE_MGMT}, always()),

            // BUILD — CI/CD & build pipelines
            q(3, "I prefer automating the repetitive steps required to turn written code into a tested, packaged, and deployed product.",
                new String[]{FeatureIndex.TAG_TECH_BUILD_PIPELINE}, always()),
            q(3, "Creating a reliable, repeatable process that automatically checks for errors and packages the work of many people is very satisfying to me.",
                new String[]{FeatureIndex.TAG_TECH_BUILD_PIPELINE}, always()),

            // INFRA — infrastructure provisioning
            q(3, "I would rather write scripts to automatically set up servers and networks than manually configure them one by one.",
                new String[]{FeatureIndex.TAG_TECH_INFRA_PROVISION}, always()),
            q(3, "Defining computing resources and network structures as written code appeals to my sense of order and scalability.",
                new String[]{FeatureIndex.TAG_TECH_INFRA_PROVISION}, always()),

            // CONTAINER — container orchestration
            q(3, "I am interested in managing how many small, isolated services can be launched, monitored, and networked together dynamically.",
                new String[]{FeatureIndex.TAG_TECH_CONTAINER_ORCH}, always()),
            q(3, "Designing a system that automatically restarts failed processes and distributes work across many machines sounds like an exciting challenge.",
                new String[]{FeatureIndex.TAG_TECH_CONTAINER_ORCH}, always()),

            // CLOUD — cloud services
            q(3, "I prefer leveraging remote, managed services to handle tasks like storage and authentication rather than building them from scratch locally.",
                new String[]{FeatureIndex.TAG_TECH_CLOUD_SERVICES}, always()),
            q(3, "Configuring and connecting various remotely hosted services to create a cohesive architecture is something I find enjoyable.",
                new String[]{FeatureIndex.TAG_TECH_CLOUD_SERVICES}, always()),

            // STATS — statistical analysis
            q(3, "I enjoy using mathematical techniques to uncover patterns and draw conclusions from a set of numbers.",
                new String[]{FeatureIndex.TAG_TECH_STAT_ANALYSIS}, always()),
            q(3, "I find it deeply satisfying to evaluate numerical evidence and apply statistical reasoning to validate a hypothesis.",
                new String[]{FeatureIndex.TAG_TECH_STAT_ANALYSIS}, always())
        );
    }

    // ─── Section 4 — 16 Database / Data Deep-Dive Questions (Conditional) ─────
    // Triggered when Data Engineer, Backend Developer, or Data Scientist survived.
    // Covers MODEL, PIPELINE, STORAGE, SERVER, STATS, OBSERV, API, PERF.

    public static List<Question> buildSection4Gated() {
        return List.of(

            // MODEL BUILDING (DS-specific)
            q(4, "I am fascinated by teaching systems to recognize complex patterns in information by providing them with examples rather than explicit rules.",
                new String[]{FeatureIndex.TAG_TECH_MODEL_BUILDING},
                gated()),
            q(4, "I enjoy the iterative process of adjusting model parameters to improve a system's ability to predict outcomes based on historical information.",
                new String[]{FeatureIndex.TAG_TECH_MODEL_BUILDING},
                gated()),

            // DATA PIPELINE (DE-specific)
            q(4, "I like designing reliable channels that transport massive amounts of information from various sources to a central destination without data loss.",
                new String[]{FeatureIndex.TAG_TECH_DATA_PIPELINE},
                gated()),
            q(4, "Ensuring that continuous streams of information are cleaned, transformed, and delivered without interruption is a challenge I find highly appealing.",
                new String[]{FeatureIndex.TAG_TECH_DATA_PIPELINE},
                gated()),

            // STORAGE — advanced storage patterns
            q(4, "I enjoy designing database schemas and index strategies that allow fast queries over billions of records.",
                new String[]{FeatureIndex.TAG_TECH_DATA_STORAGE},
                gated()),
            q(4, "Choosing between different storage technologies — relational, columnar, document, time-series — based on access patterns is a decision I find genuinely interesting.",
                new String[]{FeatureIndex.TAG_TECH_DATA_STORAGE},
                gated()),

            // STATS — data analysis depth
            q(4, "I am comfortable deriving insight from raw data by writing analytical queries, computing aggregations, and identifying statistical outliers.",
                new String[]{FeatureIndex.TAG_TECH_STAT_ANALYSIS},
                gated()),
            q(4, "I enjoy translating a business question into a quantitative analysis and communicating the findings clearly to non-technical stakeholders.",
                new String[]{FeatureIndex.TAG_TECH_STAT_ANALYSIS},
                gated()),

            // SERVER — backend business logic depth
            q(4, "I enjoy writing complex server-side business logic that enforces data integrity, handles concurrent operations, and prevents inconsistent state.",
                new String[]{FeatureIndex.TAG_TECH_SERVER_LOGIC},
                gated()),
            q(4, "When a system must handle a very large number of simultaneous requests, I like designing the concurrency and caching strategies to keep it fast.",
                new String[]{FeatureIndex.TAG_TECH_SERVER_LOGIC, FeatureIndex.TAG_TECH_PERF_OPTIM},
                gated()),

            // API — data API depth
            q(4, "Designing a data API that exposes the right level of detail while protecting sensitive records and preventing over-fetching is a challenge I enjoy.",
                new String[]{FeatureIndex.TAG_TECH_API_DESIGN},
                gated()),
            q(4, "I find it satisfying to version an API carefully so that existing integrations are never broken when the underlying data model changes.",
                new String[]{FeatureIndex.TAG_TECH_API_DESIGN},
                gated()),

            // OBSERV — monitoring data systems
            q(4, "I find it important to instrument data pipelines and ML models with metrics and alerts so that data quality degradation is caught immediately.",
                new String[]{FeatureIndex.TAG_TECH_OBSERVABILITY},
                gated()),
            q(4, "When a data system fails silently — producing wrong outputs instead of errors — I want comprehensive logging in place to diagnose the root cause.",
                new String[]{FeatureIndex.TAG_TECH_OBSERVABILITY},
                gated()),

            // PERF — performance optimization for data
            q(4, "I find it rewarding to analyze a slow query or pipeline job, identify the bottleneck, and restructure it to run significantly faster.",
                new String[]{FeatureIndex.TAG_TECH_PERF_OPTIM},
                gated()),
            q(4, "Squeezing the maximum throughput out of a data processing job by tuning parallelism, memory, and serialization formats is a problem I enjoy.",
                new String[]{FeatureIndex.TAG_TECH_PERF_OPTIM},
                gated())
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
     * in the surviving candidate set.
     */
    private static String pred(String roleA, String roleB) {
        return String.format("{\"requires_both\": [\"%s\", \"%s\"]}", roleA, roleB);
    }

    /**
     * Trigger predicate: serve this question only when at least one of the data/backend
     * gate roles is still in the surviving candidate set.
     */
    private static String gated() {
        return "{\"requires_any\": [\"Data Engineer\", \"Backend Developer\", \"Data Scientist\"]}";
    }
}
