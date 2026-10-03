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
        long nullResponseTypes = existing.stream().filter(q -> q.getResponseType() == null).count();
        boolean hasUpdatedText = existing.stream()
                .filter(q -> q.getSectionId() != null && q.getSectionId() == 3)
                .findFirst()
                .map(q -> q.getText() != null && q.getText().contains("Option A:"))
                .orElse(false);

        if (count == 84 && nullResponseTypes == 0 && hasUpdatedText) {
            log.info("All 84 RoleCompass questions already up-to-date with response types. Skipping DataSeeder.");
            return;
        }

        if (count == 84 && (!hasUpdatedText || nullResponseTypes > 0)) {
            log.info("Found outdated questions or NULL response_types. Reseeding with simplified questions...");
        } else {
            log.info("Expected 84 questions but found {}. Seeding RoleCompass questions cleanly...", count);
        }
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
            q(1, "I'd rather spend an afternoon tinkering with hardware or building something hands-on than studying abstract theory.",
                new String[]{FeatureIndex.TAG_DIM_REALISTIC}, always()),

            q(1, "When a gadget or technical setup stops working, my immediate instinct is to open it up and fix it myself.",
                new String[]{FeatureIndex.TAG_DIM_REALISTIC, FeatureIndex.TAG_DIM_THINGS_PEOPLE}, always()),

            q(1, "I love digging into technical manuals or documentation to understand how things work under the hood.",
                new String[]{FeatureIndex.TAG_DIM_INVESTIGATIVE}, always()),

            q(1, "I get excited by open-ended problems that don't have an obvious textbook answer.",
                new String[]{FeatureIndex.TAG_DIM_INVESTIGATIVE, FeatureIndex.TAG_DIM_STRUCT_AMBIG}, always()),

            q(1, "I care a lot about visual design — it genuinely bothers me when an app looks ugly or clunky.",
                new String[]{FeatureIndex.TAG_DIM_ARTISTIC}, always()),

            q(1, "I feel frustrated when strict rules or rigid templates limit my creative freedom.",
                new String[]{FeatureIndex.TAG_DIM_ARTISTIC, FeatureIndex.TAG_DIM_CONVENTIONAL}, always()),

            q(1, "I prefer following a clear, proven step-by-step process rather than making up workflows from scratch.",
                new String[]{FeatureIndex.TAG_DIM_CONVENTIONAL}, always()),

            q(1, "When instructions are unclear, I'd rather ask for formal guidance than guess and improvise.",
                new String[]{FeatureIndex.TAG_DIM_CONVENTIONAL, FeatureIndex.TAG_DIM_STRUCT_AMBIG}, always()),

            q(1, "In team projects, I naturally step up to set goals, delegate tasks, and drive decisions.",
                new String[]{FeatureIndex.TAG_DIM_ENTERPRISING}, always()),

            q(1, "I solve problems much better by bouncing ideas off other people than by sitting alone in a room.",
                new String[]{FeatureIndex.TAG_DIM_SOCIAL}, always()),

            q(1, "I trust hard numbers and measurable proof much more than gut feelings or personal opinions.",
                new String[]{FeatureIndex.TAG_DIM_DATA_IDEAS}, always()),

            q(1, "I find it more rewarding to make systems run fast and reliably than to focus on human emotions or marketing.",
                new String[]{FeatureIndex.TAG_DIM_THINGS_PEOPLE}, always()),

            q(1, "I can happily spend hours hunting down a mysterious bug with no guarantee of a quick fix.",
                new String[]{FeatureIndex.TAG_DIM_STRUCT_AMBIG}, always()),

            q(1, "When I spot a tiny flaw in my work, I feel compelled to fix it even if no one else notices.",
                new String[]{FeatureIndex.TAG_DIM_BREADTH_DEPTH}, always()),

            q(1, "I'd rather build a quick, rough prototype in two days than spend two weeks planning the perfect design.",
                new String[]{FeatureIndex.TAG_DIM_BREADTH_DEPTH, FeatureIndex.TAG_DIM_DATA_IDEAS}, always()),

            q(1, "Whenever I look at a new app or system, my first thought is how someone could break or exploit it.",
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
            q(2, "Writing the core backend logic that validates user input, processes payments, and runs business rules sounds engaging.",
                new String[]{FeatureIndex.TAG_TECH_SERVER_LOGIC}, always()),
            q(2, "Figuring out what happens when thousands of people try to buy the exact same ticket at the exact same second sounds like a fascinating puzzle.",
                new String[]{FeatureIndex.TAG_TECH_SERVER_LOGIC}, always()),

            // FEATURE 02: DATA STORAGE (TECH_DATA_STORAGE)
            q(2, "Designing clean database tables so information is neatly organized without messy duplication sounds appealing.",
                new String[]{FeatureIndex.TAG_TECH_DATA_STORAGE}, always()),
            q(2, "Speeding up slow database searches so user queries load in milliseconds instead of seconds sounds rewarding.",
                new String[]{FeatureIndex.TAG_TECH_DATA_STORAGE}, always()),

            // FEATURE 03: API DESIGN (TECH_API_DESIGN)
            q(2, "Creating simple, clean connection points so web and mobile apps can easily talk to the server sounds exciting.",
                new String[]{FeatureIndex.TAG_TECH_API_DESIGN}, always()),
            q(2, "Learning how data travels across the internet through network requests, live streams, and security tokens sounds interesting.",
                new String[]{FeatureIndex.TAG_TECH_API_DESIGN}, always()),

            // FEATURE 04: UI RENDERING (TECH_UI_RENDERING)
            q(2, "Writing code to build responsive buttons, interactive animations, and sleek visual layouts sounds rewarding.",
                new String[]{FeatureIndex.TAG_TECH_UI_RENDERING}, always()),
            q(2, "Tweaking styling and layout grids so a webpage looks gorgeous on both mobile phones and desktop monitors sounds satisfying.",
                new String[]{FeatureIndex.TAG_TECH_UI_RENDERING}, always()),

            // FEATURE 05: STATE MANAGEMENT (TECH_STATE_MGMT)
            q(2, "Keeping an app's visual screen instantly updated with live data whenever a user clicks or types sounds exciting.",
                new String[]{FeatureIndex.TAG_TECH_STATE_MGMT}, always()),
            q(2, "Hunting down frustrating bugs where rapidly clicking buttons makes screens show glitchy or outdated information sounds like an engaging challenge.",
                new String[]{FeatureIndex.TAG_TECH_STATE_MGMT}, always()),

            // FEATURE 06: BUILD PIPELINES (TECH_BUILD_PIPELINE)
            q(2, "Setting up automated tools that compile, test, and bundle code into a finished app automatically sounds appealing.",
                new String[]{FeatureIndex.TAG_TECH_BUILD_PIPELINE}, always()),
            q(2, "Solving head-scratching dependency conflicts when a project runs fine on your laptop but fails to build elsewhere is a challenge I would happily tackle.",
                new String[]{FeatureIndex.TAG_TECH_BUILD_PIPELINE}, always()),

            // FEATURE 07: INFRASTRUCTURE PROVISIONING (TECH_INFRA_PROVISION)
            q(2, "Working directly in the command line terminal using shell scripts to manage operating systems feels natural and comfortable.",
                new String[]{FeatureIndex.TAG_TECH_INFRA_PROVISION}, always()),
            q(2, "Learning how internet traffic flows through IP addresses, domains, firewalls, and server gateways sounds engaging.",
                new String[]{FeatureIndex.TAG_TECH_INFRA_PROVISION}, always()),

            // FEATURE 08: CONTAINER ORCHESTRATION (TECH_CONTAINER_ORCH)
            q(2, "Packaging applications into lightweight containers so they run identically on any computer or server sounds like smart engineering.",
                new String[]{FeatureIndex.TAG_TECH_CONTAINER_ORCH}, always()),
            q(2, "Setting up smart systems that automatically spin up more servers when traffic spikes and restart them if they crash sounds interesting.",
                new String[]{FeatureIndex.TAG_TECH_CONTAINER_ORCH}, always()),

            // FEATURE 09: CLOUD SERVICES (TECH_CLOUD_SERVICES)
            q(2, "Building modern applications using managed cloud services instead of maintaining physical hardware sounds exciting.",
                new String[]{FeatureIndex.TAG_TECH_CLOUD_SERVICES}, always()),
            q(2, "Architecting cloud networks spread across different countries so apps stay online even during major regional power cuts sounds like an engaging challenge.",
                new String[]{FeatureIndex.TAG_TECH_CLOUD_SERVICES}, always()),

            // FEATURE 10: STATISTICAL ANALYSIS (TECH_STAT_ANALYSIS)
            q(2, "Exploring datasets using probability, charts, and mathematical trends to uncover hidden patterns feels natural to me.",
                new String[]{FeatureIndex.TAG_TECH_STAT_ANALYSIS}, always()),
            q(2, "Checking whether a sudden spike in data is a genuine trend or just misleading noise and random coincidence sounds like second nature.",
                new String[]{FeatureIndex.TAG_TECH_STAT_ANALYSIS}, always()),

            // FEATURE 11: MODEL BUILDING (TECH_MODEL_BUILDING)
            q(2, "Training and tuning machine learning models to make smart predictions or detect patterns in data sounds exciting.",
                new String[]{FeatureIndex.TAG_TECH_MODEL_BUILDING}, always()),
            q(2, "Investigating why an AI model made mistakes on certain edge cases and adjusting the training data to make it smarter sounds rewarding.",
                new String[]{FeatureIndex.TAG_TECH_MODEL_BUILDING}, always()),

            // FEATURE 12: DATA PIPELINES (TECH_DATA_PIPELINE)
            q(2, "Building high-speed data plumbing that ingests and cleans millions of real-time events every hour sounds thrilling.",
                new String[]{FeatureIndex.TAG_TECH_DATA_PIPELINE}, always()),
            q(2, "Ensuring massive company datasets are clean, verified, and free of duplicate records before analysts use them sounds satisfying.",
                new String[]{FeatureIndex.TAG_TECH_DATA_PIPELINE}, always()),

            // FEATURE 13: MOBILE CLIENT (TECH_MOBILE_CLIENT)
            q(2, "Creating mobile apps designed to run smoothly while saving phone battery, memory, and working offline sounds engaging.",
                new String[]{FeatureIndex.TAG_TECH_MOBILE_CLIENT}, always()),
            q(2, "Building native apps specifically for smartphones and publishing them on mobile app stores sounds like a compelling path.",
                new String[]{FeatureIndex.TAG_TECH_MOBILE_CLIENT}, always()),

            // FEATURE 14: THREAT ANALYSIS (TECH_THREAT_ANALYSIS)
            q(2, "Looking at any website or login screen and thinking: 'How could a hacker sneak in or steal data here?' sounds like my kind of problem.",
                new String[]{FeatureIndex.TAG_TECH_THREAT_ANALYSIS}, always()),
            q(2, "Researching famous cybersecurity exploits and learning step-by-step how hackers breach secure systems sounds genuinely exciting.",
                new String[]{FeatureIndex.TAG_TECH_THREAT_ANALYSIS}, always()),

            // FEATURE 15: SYSTEM HARDENING (TECH_SYSTEM_HARDENING)
            q(2, "Locking down servers and databases with strong encryption, strict permissions, and secure digital keys sounds satisfying.",
                new String[]{FeatureIndex.TAG_TECH_SYSTEM_HARDENING}, always()),
            q(2, "Auditing company networks and security logs to ensure all computers meet strict defense standards sounds worthwhile.",
                new String[]{FeatureIndex.TAG_TECH_SYSTEM_HARDENING}, always()),

            // FEATURE 16: TEST DESIGN (TECH_TEST_DESIGN)
            q(2, "Trying to break software on purpose by typing weird, unexpected inputs until it crashes gives me genuine satisfaction.",
                new String[]{FeatureIndex.TAG_TECH_TEST_DESIGN}, always()),
            q(2, "Creating organized testing checklists to guarantee that new code updates don't break existing features sounds satisfying.",
                new String[]{FeatureIndex.TAG_TECH_TEST_DESIGN}, always()),

            // FEATURE 17: TEST AUTOMATION (TECH_TEST_AUTOMATION)
            q(2, "Writing automated scripts that simulate user clicks and form submissions to test an entire app in seconds sounds exciting.",
                new String[]{FeatureIndex.TAG_TECH_TEST_AUTOMATION}, always()),
            q(2, "Fixing flaky automated tests that randomly pass or fail due to network lag so tests are 100% trustworthy sounds satisfying.",
                new String[]{FeatureIndex.TAG_TECH_TEST_AUTOMATION}, always()),

            // FEATURE 18: OBSERVABILITY (TECH_OBSERVABILITY)
            q(2, "Creating live visual control screens and alert monitors to spot server slowdowns before users notice sounds interesting.",
                new String[]{FeatureIndex.TAG_TECH_OBSERVABILITY}, always()),
            q(2, "Troubleshooting a sudden system outage and hunting through server error logs to find what broke sounds like a challenge I would enjoy.",
                new String[]{FeatureIndex.TAG_TECH_OBSERVABILITY}, always()),

            // FEATURE 19: PERFORMANCE OPTIMISATION (TECH_PERF_OPTIM)
            q(2, "Investigating why an app is lagging or freezing up and tweaking code until it runs silky smooth sounds deeply engaging.",
                new String[]{FeatureIndex.TAG_TECH_PERF_OPTIM}, always()),
            q(2, "Designing fast memory caching so users get instant responses without overloading the database sounds rewarding.",
                new String[]{FeatureIndex.TAG_TECH_PERF_OPTIM}, always()),

            // FEATURE 20: FULL SPECTRUM DELIVERY (TECH_FULL_SPECTRUM)
            q(2, "Building complete features from scratch — designing the screen, writing the API, and storing the data myself — feels more fulfilling than doing only one side.",
                new String[]{FeatureIndex.TAG_TECH_FULL_SPECTRUM}, always()),
            q(2, "I would rather pick an existing tool or framework to get a working feature into users' hands quickly than spend three days writing a custom solution.",
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
            q(3, "Which path excites you more? | Option A: Build complete apps end-to-end (UI, backend & database) | Option B: Focus deeply on server performance, concurrency & databases",
                new String[]{FeatureIndex.TAG_TECH_SERVER_LOGIC, FeatureIndex.TAG_TECH_FULL_SPECTRUM},
                pred("Backend Developer", "Full Stack Developer")),

            q(3, "What matters more to you? | Option A: Seeing users directly interact with features you built | Option B: Architecting mathematically elegant and rock-solid server code",
                new String[]{FeatureIndex.TAG_TECH_FULL_SPECTRUM, FeatureIndex.TAG_TECH_SERVER_LOGIC},
                pred("Backend Developer", "Full Stack Developer")),

            // PAIR B: FRONTEND DEVELOPER vs ANDROID DEVELOPER
            q(3, "Which platform do you prefer? | Option A: Building for the open web with instant website links | Option B: Building packaged native apps specifically for mobile app stores",
                new String[]{FeatureIndex.TAG_TECH_UI_RENDERING, FeatureIndex.TAG_TECH_MOBILE_CLIENT},
                pred("Frontend Developer", "Android Developer")),

            q(3, "Which type of feature excites you more? | Option A: Tapping into mobile phone hardware (camera, GPS, sensors) | Option B: Perfecting responsive web styling across desktop and laptop screens",
                new String[]{FeatureIndex.TAG_TECH_MOBILE_CLIENT, FeatureIndex.TAG_TECH_UI_RENDERING},
                pred("Frontend Developer", "Android Developer")),

            // PAIR C: DEVOPS ENGINEER vs CLOUD ENGINEER
            q(3, "Which engineering challenge sounds more interesting? | Option A: Speeding up developer releases with automated build pipelines | Option B: Designing multi-region cloud VPC and networking architectures",
                new String[]{FeatureIndex.TAG_TECH_BUILD_PIPELINE, FeatureIndex.TAG_TECH_CLOUD_SERVICES},
                pred("DevOps Engineer", "Cloud Engineer")),

            q(3, "Which type of code would you rather write? | Option A: Scripts that automatically spin up cloud servers and clusters | Option B: Automated test, build, and packaging deployment pipelines",
                new String[]{FeatureIndex.TAG_TECH_CLOUD_SERVICES, FeatureIndex.TAG_TECH_INFRA_PROVISION},
                pred("DevOps Engineer", "Cloud Engineer")),

            // PAIR D: DATA SCIENTIST vs DATA ENGINEER
            q(3, "Which achievement gives you more pride? | Option A: Discovering a surprising statistical insight that guides business strategy | Option B: Building the high-speed data pipeline that reliably imports that data",
                new String[]{FeatureIndex.TAG_TECH_STAT_ANALYSIS, FeatureIndex.TAG_TECH_DATA_PIPELINE},
                pred("Data Scientist", "Data Engineer")),

            q(3, "If you had one week to improve a system, what would you prioritize? | Option A: Pushing AI model prediction accuracy from 90% to 95% | Option B: Slashing data processing server latency to under 10 milliseconds",
                new String[]{FeatureIndex.TAG_TECH_MODEL_BUILDING, FeatureIndex.TAG_TECH_DATA_PIPELINE},
                pred("Data Scientist", "Data Engineer")),

            // PAIR E: CYBERSECURITY ENGINEER vs QA / TEST AUTOMATION ENGINEER
            q(3, "Which type of testing sounds more exciting? | Option A: Probing how a malicious hacker could break into a system | Option B: Verifying that app buttons and forms strictly match design specifications",
                new String[]{FeatureIndex.TAG_TECH_THREAT_ANALYSIS, FeatureIndex.TAG_TECH_TEST_DESIGN},
                pred("Cybersecurity Engineer", "QA / Test Automation Engineer")),

            q(3, "Which goal matters more to you? | Option A: Defending company data against digital intruders and attacks | Option B: Ensuring an app never crashes or freezes during normal user flows",
                new String[]{FeatureIndex.TAG_TECH_SYSTEM_HARDENING, FeatureIndex.TAG_TECH_TEST_AUTOMATION},
                pred("Cybersecurity Engineer", "QA / Test Automation Engineer")),

            // PAIR F: FULL STACK DEVELOPER vs FRONTEND DEVELOPER
            q(3, "Where would you rather spend an extra day of polish? | Option A: Perfecting visual animations, layout grids, and CSS polish | Option B: Wiring up backend authentication and database queries",
                new String[]{FeatureIndex.TAG_TECH_UI_RENDERING, FeatureIndex.TAG_TECH_FULL_SPECTRUM},
                pred("Full Stack Developer", "Frontend Developer")),

            q(3, "What is your preferred scope of work? | Option A: Staying strictly focused on visual design and user interfaces | Option B: Owning the full product across UI, servers, and databases",
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
            q(4, "When an algorithm gives surprising outputs, I enjoy digging into the math and data distributions to understand why.",
                new String[]{FeatureIndex.TAG_TECH_STAT_ANALYSIS, FeatureIndex.TAG_TECH_MODEL_BUILDING},
                anyOf("Data Scientist")),

            q(4, "I am totally fine with the reality that most data experiments fail before finding one useful predictive insight.",
                new String[]{FeatureIndex.TAG_TECH_MODEL_BUILDING},
                anyOf("Data Scientist")),

            q(4, "Before writing machine learning code, I immediately inspect the dataset for missing numbers, bias, and flawed labels.",
                new String[]{FeatureIndex.TAG_TECH_STAT_ANALYSIS},
                anyOf("Data Scientist")),

            q(4, "I enjoy turning complicated mathematical findings into simple, clear charts that anyone can understand.",
                new String[]{FeatureIndex.TAG_TECH_MODEL_BUILDING, FeatureIndex.TAG_TECH_STAT_ANALYSIS},
                anyOf("Data Scientist")),

            // ── DATA ENGINEER (4 probes) ──────────────────────────────────────
            q(4, "I care deeply about keeping company databases clean so upstream changes never break downstream reports.",
                new String[]{FeatureIndex.TAG_TECH_DATA_PIPELINE, FeatureIndex.TAG_TECH_DATA_STORAGE},
                anyOf("Data Engineer")),

            q(4, "Managing big data systems that process massive streams of information in parallel sounds thrilling.",
                new String[]{FeatureIndex.TAG_TECH_DATA_PIPELINE},
                anyOf("Data Engineer")),

            q(4, "When building data import jobs, I spend just as much effort on automatic error recovery as on the normal flow.",
                new String[]{FeatureIndex.TAG_TECH_DATA_PIPELINE, FeatureIndex.TAG_TECH_OBSERVABILITY},
                anyOf("Data Engineer")),

            q(4, "I genuinely enjoy writing advanced database queries to crunch and organize massive tables directly in the database.",
                new String[]{FeatureIndex.TAG_TECH_DATA_STORAGE, FeatureIndex.TAG_TECH_DATA_PIPELINE},
                anyOf("Data Engineer")),

            // ── CYBERSECURITY ENGINEER (4 probes) ─────────────────────────────
            q(4, "I have the patience to dig through hundreds of login logs or network traffic records to catch one suspicious anomaly.",
                new String[]{FeatureIndex.TAG_TECH_THREAT_ANALYSIS},
                anyOf("Cybersecurity Engineer")),

            q(4, "Whenever I use any app, I instinctively test input boxes with sneaky characters to see if I can bypass security.",
                new String[]{FeatureIndex.TAG_TECH_THREAT_ANALYSIS, FeatureIndex.TAG_TECH_SYSTEM_HARDENING},
                anyOf("Cybersecurity Engineer")),

            q(4, "After finding a security flaw, I am just as eager to design the defensive patch as I was to find the vulnerability.",
                new String[]{FeatureIndex.TAG_TECH_SYSTEM_HARDENING, FeatureIndex.TAG_TECH_THREAT_ANALYSIS},
                anyOf("Cybersecurity Engineer")),

            q(4, "I believe good cybersecurity should protect people without making company computer tools annoying to use.",
                new String[]{FeatureIndex.TAG_TECH_SYSTEM_HARDENING},
                anyOf("Cybersecurity Engineer")),

            // ── DEVOPS ENGINEER (4 probes) ────────────────────────────────────
            q(4, "I believe developers should be able to safely push new code updates live in under fifteen minutes with zero manual hassle.",
                new String[]{FeatureIndex.TAG_TECH_BUILD_PIPELINE, FeatureIndex.TAG_TECH_INFRA_PROVISION},
                anyOf("DevOps Engineer")),

            q(4, "When a server crashes, my focus is on adding automated safeguards to prevent it forever, not pointing fingers at people.",
                new String[]{FeatureIndex.TAG_TECH_OBSERVABILITY, FeatureIndex.TAG_TECH_INFRA_PROVISION},
                anyOf("DevOps Engineer")),

            q(4, "I believe every server and network setting should be defined in version-controlled scripts, never configured by hand.",
                new String[]{FeatureIndex.TAG_TECH_INFRA_PROVISION, FeatureIndex.TAG_TECH_CONTAINER_ORCH},
                anyOf("DevOps Engineer")),

            q(4, "I am happy to spend two days automating a boring software release task to permanently save 30 minutes every week.",
                new String[]{FeatureIndex.TAG_TECH_BUILD_PIPELINE, FeatureIndex.TAG_TECH_CONTAINER_ORCH},
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
