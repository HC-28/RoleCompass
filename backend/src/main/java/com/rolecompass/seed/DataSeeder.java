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
        List<Question> existing = questionRepository.findAll();
        long count = existing.size();
        long s1Count = existing.stream().filter(q -> q.getSectionId() != null && q.getSectionId() == 1).count();
        long nullResponseTypes = existing.stream().filter(q -> q.getResponseType() == null).count();

        // Expected: 12 (S1) + 20 (S2) + 7 (S3) + 15 (S4) = 54 questions total
        if (count == 54 && s1Count == 12 && nullResponseTypes == 0) {
            log.info("All 54 RoleCompass questions already up-to-date. Skipping DataSeeder.");
            return;
        }

        log.info("Expected 54 questions (S1=12, S2=20, S3=7, S4=15) but found total={}, S1={}. Reseeding...", count, s1Count);
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

        if (all.size() != 54) {
            log.warn("WARN: Expected 54 questions but seeded {}. Check buildSection* methods.", all.size());
        }
    }

    // ─── Section 1 — 12 Psychometric / RIASEC Questions ──────────────────────
    // Measures 6 core Holland RIASEC dimensions (2 questions each): R, I, A, S, E, C.
    // Directly drives the psychometric routing gates (R, I, A, C) and Holland profile (S, E).
    // All questions are mandatory (sectionId=1, always triggered).

    public static List<Question> buildSection1() {
        return List.of(
            // REALISTIC (R) — Hands-on builder, enjoys making things work
            q(1, "When something at home or on a computer breaks down, your first instinct is to roll up your sleeves and figure out how to fix it yourself.",
                new String[]{FeatureIndex.TAG_DIM_REALISTIC}, always()),
            q(1, "You feel genuinely satisfied when you finish building something that actually works — whether it's a small program, a gadget setup, or a physical project.",
                new String[]{FeatureIndex.TAG_DIM_REALISTIC}, always()),

            // INVESTIGATIVE (I) — Analytical thinker, curious about how things work
            q(1, "When you don't understand how something works, you feel genuinely curious and can't help but dig deeper until it makes sense.",
                new String[]{FeatureIndex.TAG_DIM_INVESTIGATIVE}, always()),
            q(1, "You enjoy thinking through difficult problems step by step — finding the logical answer is more satisfying to you than leaving things unexplained.",
                new String[]{FeatureIndex.TAG_DIM_INVESTIGATIVE}, always()),

            // ARTISTIC (A) — Visual and creative, cares about how things look and feel
            q(1, "It genuinely bothers you when an app or website looks cluttered, unbalanced, or visually unappealing — you notice design details most people miss.",
                new String[]{FeatureIndex.TAG_DIM_ARTISTIC}, always()),
            q(1, "You enjoy choosing colors, layouts, and visual styles — making something look polished and beautiful gives you real creative satisfaction.",
                new String[]{FeatureIndex.TAG_DIM_ARTISTIC}, always()),

            // SOCIAL (S) — Team player, energized by helping others
            q(1, "You find that you think better and get more done when you can bounce ideas off teammates rather than working completely alone.",
                new String[]{FeatureIndex.TAG_DIM_SOCIAL}, always()),
            q(1, "Knowing that your work directly helped someone — made their job easier, solved their problem, or improved their day — is what motivates you most.",
                new String[]{FeatureIndex.TAG_DIM_SOCIAL}, always()),

            // ENTERPRISING (E) — Leader, persuader, results-driven
            q(1, "In group projects, you naturally end up being the one who organizes tasks, suggests ideas, and keeps the team on track.",
                new String[]{FeatureIndex.TAG_DIM_ENTERPRISING}, always()),
            q(1, "The idea of building a product, pitching it to people, and watching it get used in the real world genuinely excites you.",
                new String[]{FeatureIndex.TAG_DIM_ENTERPRISING}, always()),

            // CONVENTIONAL (C) — Structured, precise, likes clear rules and verification
            q(1, "You work best when you have a clear checklist or step-by-step plan — knowing exactly what \"done\" looks like helps you stay focused.",
                new String[]{FeatureIndex.TAG_DIM_CONVENTIONAL}, always()),
            q(1, "You feel uncomfortable leaving work in a gray area — you'd rather double-check something twice than ship something that might have an error.",
                new String[]{FeatureIndex.TAG_DIM_CONVENTIONAL}, always())
        );
    }

    // ─── Section 2 — 20 Technical Interest Questions (1 per feature) ──────────
    // One question per feature — no adaptive Q2. Simple and direct.
    // Each question is a concrete curiosity check: "Does this sound interesting?"
    // sectionId=2, all always triggered.

    public static List<Question> buildSection2Tech() {
        return List.of(

            // FEATURE 01: SERVER LOGIC
            q(2, "Does making the \"behind the scenes\" part of an app — the part that checks your login, processes your order, and handles everyone's requests at once — sound like interesting work to you?",
                new String[]{FeatureIndex.TAG_TECH_SERVER_LOGIC}, always()),

            // FEATURE 02: DATA STORAGE
            q(2, "Does organizing and storing information in databases — making sure data is clean, fast to find, and never lost — sound like something you'd enjoy working on?",
                new String[]{FeatureIndex.TAG_TECH_DATA_STORAGE}, always()),

            // FEATURE 03: API DESIGN
            q(2, "Does building the connection layer that lets apps and websites talk to servers and share data between each other sound interesting to you?",
                new String[]{FeatureIndex.TAG_TECH_API_DESIGN}, always()),

            // FEATURE 04: UI RENDERING
            q(2, "Does writing code to create buttons, animations, and visual layouts that users see and click on — making screens look great and feel smooth — sound exciting to you?",
                new String[]{FeatureIndex.TAG_TECH_UI_RENDERING}, always()),

            // FEATURE 05: STATE MANAGEMENT
            q(2, "Does making sure an app's screen updates instantly when someone clicks, types, or gets a new notification — keeping everything in sync without glitches — sound like an interesting challenge?",
                new String[]{FeatureIndex.TAG_TECH_STATE_MGMT}, always()),

            // FEATURE 06: BUILD PIPELINES
            q(2, "Does setting up systems that automatically check, test, and package your code every time you make a change — so it's always ready to ship — sound appealing to you?",
                new String[]{FeatureIndex.TAG_TECH_BUILD_PIPELINE}, always()),

            // FEATURE 07: INFRASTRUCTURE PROVISIONING
            q(2, "Does managing servers and computers using typed commands and scripts — instead of clicking through menus — sound like something you'd be comfortable with?",
                new String[]{FeatureIndex.TAG_TECH_INFRA_PROVISION}, always()),

            // FEATURE 08: CONTAINER ORCHESTRATION
            q(2, "Does packaging an entire app and all its settings into a neat bundle that runs the same way on any computer — and letting a system manage hundreds of those bundles automatically — sound clever and interesting to you?",
                new String[]{FeatureIndex.TAG_TECH_CONTAINER_ORCH}, always()),

            // FEATURE 09: CLOUD SERVICES
            q(2, "Does building and running apps on remote cloud platforms like AWS or Google Cloud — instead of buying and managing your own physical servers — sound exciting to you?",
                new String[]{FeatureIndex.TAG_TECH_CLOUD_SERVICES}, always()),

            // FEATURE 10: STATISTICAL ANALYSIS
            q(2, "Does exploring data using numbers, averages, and charts to uncover hidden patterns or spot whether something is a real trend or just random noise sound interesting to you?",
                new String[]{FeatureIndex.TAG_TECH_STAT_ANALYSIS}, always()),

            // FEATURE 11: MODEL BUILDING
            q(2, "Does training a computer program to make predictions or recognize patterns by feeding it examples — like teaching it to detect spam or recommend songs — sound like fascinating work?",
                new String[]{FeatureIndex.TAG_TECH_MODEL_BUILDING}, always()),

            // FEATURE 12: DATA PIPELINES
            q(2, "Does building systems that automatically collect, clean, and organize huge amounts of data from many sources — so analysts and apps always have fresh, reliable information — sound like something you'd enjoy?",
                new String[]{FeatureIndex.TAG_TECH_DATA_PIPELINE}, always()),

            // FEATURE 13: MOBILE CLIENT
            q(2, "Does building apps designed specifically for smartphones — things people download and install on their phones — sound like an exciting path?",
                new String[]{FeatureIndex.TAG_TECH_MOBILE_CLIENT}, always()),

            // FEATURE 14: THREAT ANALYSIS
            q(2, "When you use an app or website, do you find yourself thinking about how someone might be able to break into it, steal data, or exploit a weakness — and does finding those gaps sound exciting?",
                new String[]{FeatureIndex.TAG_TECH_THREAT_ANALYSIS}, always()),

            // FEATURE 15: SYSTEM HARDENING
            q(2, "Does protecting computer systems by setting up strong passwords, encryption, and access rules — making sure only the right people can get in — sound like satisfying, important work?",
                new String[]{FeatureIndex.TAG_TECH_SYSTEM_HARDENING}, always()),

            // FEATURE 16: TEST DESIGN
            q(2, "Does deliberately trying to break software by testing edge cases and weird inputs — to find bugs before users do — sound like something you'd enjoy?",
                new String[]{FeatureIndex.TAG_TECH_TEST_DESIGN}, always()),

            // FEATURE 17: TEST AUTOMATION
            q(2, "Does writing programs that automatically test an entire app — simulating user clicks and actions — so you never have to manually check the same thing twice sound satisfying to you?",
                new String[]{FeatureIndex.TAG_TECH_TEST_AUTOMATION}, always()),

            // FEATURE 18: OBSERVABILITY
            q(2, "Does setting up dashboards and alerts that tell you exactly when and where something goes wrong in a live system — so you can fix it before users even notice — sound like engaging work?",
                new String[]{FeatureIndex.TAG_TECH_OBSERVABILITY}, always()),

            // FEATURE 19: PERFORMANCE OPTIMISATION
            q(2, "Does investigating why an app feels slow or crashes under heavy use, then figuring out how to make it run fast and smooth for thousands of users at once, sound deeply satisfying to you?",
                new String[]{FeatureIndex.TAG_TECH_PERF_OPTIM}, always()),

            // FEATURE 20: FULL SPECTRUM DELIVERY
            q(2, "Does building a complete feature from scratch — designing the screen, writing the server, and storing the data yourself — feel more fulfilling than focusing on just one part?",
                new String[]{FeatureIndex.TAG_TECH_FULL_SPECTRUM}, always())
        );
    }

    // ─── Section 3 — 7 Role-Pair Resolver Questions (Conditional) ────────────
    // 7 competing role pairs × 1 question each.
    // Each question has a requires_both predicate; only fired if both roles
    // in the pair are still active candidates after Section 2 pruning.
    // sectionId=3.

    public static List<Question> buildSection3Resolver() {
        return List.of(

            // PAIR A: BACKEND DEVELOPER vs FULL STACK DEVELOPER
            q(3, "Which path excites you more? | Option A: Creating complete applications from scratch — building both the screens users see and the server behind it | Option B: Focusing deeply behind the scenes — making servers ultra-fast, handling millions of requests, and managing databases",
                new String[]{FeatureIndex.TAG_TECH_SERVER_LOGIC, FeatureIndex.TAG_TECH_FULL_SPECTRUM},
                pred("Backend Developer", "Full Stack Developer")),

            // PAIR B: FRONTEND DEVELOPER vs MOBILE DEVELOPER
            q(3, "Which platform do you prefer? | Option A: Building websites and web applications that anyone can open instantly in any web browser | Option B: Building dedicated mobile apps specifically designed to download and install on smartphones",
                new String[]{FeatureIndex.TAG_TECH_UI_RENDERING, FeatureIndex.TAG_TECH_MOBILE_CLIENT},
                pred("Frontend Developer", "Mobile Developer")),

            // PAIR C: DEVOPS ENGINEER vs CLOUD ENGINEER
            q(3, "Which engineering challenge sounds more interesting? | Option A: Automating how code gets tested and delivered so developers can ship new updates safely and quickly | Option B: Designing the worldwide cloud network and server infrastructure that keeps large services online 24/7",
                new String[]{FeatureIndex.TAG_TECH_BUILD_PIPELINE, FeatureIndex.TAG_TECH_CLOUD_SERVICES},
                pred("DevOps Engineer", "Cloud Engineer")),

            // PAIR D: DATA SCIENTIST vs DATA ENGINEER
            q(3, "Which achievement gives you more pride? | Option A: Analyzing complex data and building mathematical AI models to discover hidden trends and make predictions | Option B: Building high-capacity data channels that reliably collect and organize massive streams of data from everywhere",
                new String[]{FeatureIndex.TAG_TECH_STAT_ANALYSIS, FeatureIndex.TAG_TECH_DATA_PIPELINE},
                pred("Data Scientist", "Data Engineer")),

            // PAIR E: CYBERSECURITY ENGINEER vs QA / TEST AUTOMATION ENGINEER
            q(3, "Which type of testing sounds more exciting? | Option A: Thinking like an attacker to find vulnerabilities, security flaws, and protect systems from hackers | Option B: Testing applications thoroughly to catch bugs, crash points, and ensure every button and workflow works smoothly",
                new String[]{FeatureIndex.TAG_TECH_THREAT_ANALYSIS, FeatureIndex.TAG_TECH_TEST_DESIGN},
                pred("Cybersecurity Engineer", "QA / Test Automation Engineer")),

            // PAIR F: FULL STACK DEVELOPER vs FRONTEND DEVELOPER
            q(3, "Where would you rather spend an extra day of polish? | Option A: Spending extra time perfecting the visual details, smooth screen animations, and user interface styling | Option B: Spending extra time connecting user accounts, server logic, and saving information in the database",
                new String[]{FeatureIndex.TAG_TECH_UI_RENDERING, FeatureIndex.TAG_TECH_FULL_SPECTRUM},
                pred("Full Stack Developer", "Frontend Developer")),

            // PAIR G: DATA SCIENTIST vs AI / ML ENGINEER
            q(3, "Which goal describes your ideal day-to-day work? | Option A: Running statistical experiments, exploring datasets, and presenting insight-driven findings to stakeholders | Option B: Deploying a trained model as a scalable production API used by millions of users in a live product",
                new String[]{FeatureIndex.TAG_TECH_STAT_ANALYSIS, FeatureIndex.TAG_TECH_MODEL_BUILDING},
                pred("Data Scientist", "AI / ML Engineer"))
        );
    }

    // ─── Section 4 — 15 Specialist Probe Questions (Conditional) ─────────────
    // 5 specialist roles × 3 dedicated probe questions each.
    // Each group triggered by a requires_any predicate for its role.
    // Serves ONLY when that role is still in the surviving candidate set.
    // Roles with probes: Data Scientist, AI / ML Engineer, Data Engineer, Cybersecurity, DevOps.
    // sectionId=4.

    public static List<Question> buildSection4Specialist() {
        return List.of(

            // ── AI / ML ENGINEER (3 probes) ───────────────────────────────────
            q(4, "I am excited to deploy machine learning models as live APIs that other engineers or products consume in production.",
                new String[]{FeatureIndex.TAG_TECH_MODEL_BUILDING, FeatureIndex.TAG_TECH_API_DESIGN},
                anyOf("AI / ML Engineer")),

            q(4, "When I build a model, I immediately think about how to optimize it for low latency and high throughput under real user load.",
                new String[]{FeatureIndex.TAG_TECH_MODEL_BUILDING, FeatureIndex.TAG_TECH_PERF_OPTIM},
                anyOf("AI / ML Engineer")),

            q(4, "I enjoy structuring full ML pipelines — from raw data ingestion and feature engineering through training, evaluation, and automated retraining.",
                new String[]{FeatureIndex.TAG_TECH_DATA_PIPELINE, FeatureIndex.TAG_TECH_MODEL_BUILDING},
                anyOf("AI / ML Engineer")),

            // ── DATA SCIENTIST (3 probes) ──────────────────────────────────────
            q(4, "When an algorithm gives surprising outputs, I enjoy digging into the math and data distributions to understand why.",
                new String[]{FeatureIndex.TAG_TECH_STAT_ANALYSIS, FeatureIndex.TAG_TECH_MODEL_BUILDING},
                anyOf("Data Scientist")),

            q(4, "Before writing machine learning code, I immediately inspect the dataset for missing numbers, bias, and flawed labels.",
                new String[]{FeatureIndex.TAG_TECH_STAT_ANALYSIS},
                anyOf("Data Scientist")),

            q(4, "I enjoy turning complicated mathematical findings into simple, clear charts that anyone can understand.",
                new String[]{FeatureIndex.TAG_TECH_MODEL_BUILDING, FeatureIndex.TAG_TECH_STAT_ANALYSIS},
                anyOf("Data Scientist")),

            // ── DATA ENGINEER (3 probes) ──────────────────────────────────────
            q(4, "I care deeply about keeping company databases clean so upstream changes never break downstream reports.",
                new String[]{FeatureIndex.TAG_TECH_DATA_PIPELINE, FeatureIndex.TAG_TECH_DATA_STORAGE},
                anyOf("Data Engineer")),

            q(4, "Managing big data systems that process massive streams of information in parallel sounds thrilling.",
                new String[]{FeatureIndex.TAG_TECH_DATA_PIPELINE},
                anyOf("Data Engineer")),

            q(4, "I genuinely enjoy writing advanced database queries to crunch and organize massive tables directly in the database.",
                new String[]{FeatureIndex.TAG_TECH_DATA_STORAGE, FeatureIndex.TAG_TECH_DATA_PIPELINE},
                anyOf("Data Engineer")),

            // ── CYBERSECURITY ENGINEER (3 probes) ─────────────────────────────
            q(4, "I have the patience to dig through hundreds of login logs or network traffic records to catch one suspicious anomaly.",
                new String[]{FeatureIndex.TAG_TECH_THREAT_ANALYSIS},
                anyOf("Cybersecurity Engineer")),

            q(4, "Whenever I use any app, I instinctively test input boxes with sneaky characters to see if I can bypass security.",
                new String[]{FeatureIndex.TAG_TECH_THREAT_ANALYSIS, FeatureIndex.TAG_TECH_SYSTEM_HARDENING},
                anyOf("Cybersecurity Engineer")),

            q(4, "After finding a security flaw, I am just as eager to design the defensive patch as I was to find the vulnerability.",
                new String[]{FeatureIndex.TAG_TECH_SYSTEM_HARDENING, FeatureIndex.TAG_TECH_THREAT_ANALYSIS},
                anyOf("Cybersecurity Engineer")),

            // ── DEVOPS ENGINEER (3 probes) ────────────────────────────────────
            q(4, "I believe developers should be able to safely push new code updates live in under fifteen minutes with zero manual hassle.",
                new String[]{FeatureIndex.TAG_TECH_BUILD_PIPELINE, FeatureIndex.TAG_TECH_INFRA_PROVISION},
                anyOf("DevOps Engineer")),

            q(4, "When a server crashes, my focus is on adding automated safeguards to prevent it forever, not pointing fingers at people.",
                new String[]{FeatureIndex.TAG_TECH_OBSERVABILITY, FeatureIndex.TAG_TECH_INFRA_PROVISION},
                anyOf("DevOps Engineer")),

            q(4, "I believe every server and network setting should be defined in version-controlled scripts, never configured by hand.",
                new String[]{FeatureIndex.TAG_TECH_INFRA_PROVISION, FeatureIndex.TAG_TECH_CONTAINER_ORCH},
                anyOf("DevOps Engineer"))
        );
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /** Builds a Question with a given trigger predicate JSON string. Response type auto-derived from section. */
    private static Question q(int section, String text, String[] tags, String predicate) {
        String responseType = switch (section) {
            case 2, 4 -> "INTEREST_4";
            case 3    -> "PREFERENCE_4";
            default   -> "LIKERT_5";   // Section 1
        };
        return Question.builder()
                .sectionId(section)
                .text(text)
                .dimensionTags(tags)
                .triggerPredicate(predicate)
                .responseType(responseType)
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
