/**
 * assessment.constants.js
 *
 * Single source of truth for all assessment-wide constants used across
 * AssessmentPage, SectionBanner, ProgressTracker, and hooks.
 */

/** Minimum total questions in a full baseline flow: Section 1 (16) + Section 2 (40). */
export const ASSESSMENT_BASELINE_TOTAL = 56

/**
 * Maps FSM state names (returned by the backend) to their human-readable
 * section labels. Used by SectionBanner.
 */
export const FSM_SECTION_LABELS = {
  SECTION_1_RIASEC:      { number: 1, label: 'Personality & Work Style',     description: 'Understanding how you think and work' },
  PRUNE_PSYCHOMETRICS:   { number: 1, label: 'Personality & Work Style',     description: 'Understanding how you think and work' },
  SECTION_2_TECH_CORE:   { number: 2, label: 'Technical Core Assessment',    description: 'Mapping your technical interests across 20 domains' },
  PRUNE_TECH_SKILLS:     { number: 2, label: 'Technical Core Assessment',    description: 'Mapping your technical interests across 20 domains' },
  RESOLVER_EVALUATION:   { number: 3, label: 'Role-Pair Discriminators',     description: 'Resolving ambiguity between closely matched roles' },
  SECTION_3_RESOLVER:    { number: 3, label: 'Role-Pair Discriminators',     description: 'Resolving ambiguity between closely matched roles' },
  SECTION_4_SPECIALIST:  { number: 4, label: 'Specialist Probes',            description: 'Deep-probing your specialist role profile' },
  TERMINAL_SCORING:      { number: 4, label: 'Specialist Probes',            description: 'Deep-probing your specialist role profile' },
  COMPLETED:             { number: 4, label: 'Completed',                    description: 'Assessment complete' },
}

/** Total number of sections in the assessment pipeline. */
export const TOTAL_SECTIONS = 4

export const LIKERT_LABELS = {
  1: 'Not me at all',
  2: 'Rarely like me',
  3: 'Sometimes / Neutral',
  4: 'Mostly like me',
  5: 'Exactly like me',
}

/**
 * INTEREST_4 — 4-option task affinity scale used in Sections 2 & 4.
 * Measures enthusiasm and willingness to learn technical tasks.
 * No neutral dead-zone.
 */
export const INTEREST_OPTIONS = [
  { value: 5, emoji: '🔥', label: 'Love doing this' },
  { value: 4, emoji: '💡', label: 'Curious to learn' },
  { value: 2, emoji: '🤷', label: 'Can do if needed' },
  { value: 1, emoji: '❌', label: 'Not interested' },
]

/**
 * PREFERENCE_4 — 4-option bipolar forced-choice scale for Section 3 resolvers.
 * Directly chooses between Option A and Option B. No generic agreement or Yes/No.
 */
export const PREFERENCE_OPTIONS = [
  { value: 5, label: 'Definitely Option A', shortLabel: 'Strong A' },
  { value: 4, label: 'Lean Option A',       shortLabel: 'Lean A' },
  { value: 2, label: 'Lean Option B',       shortLabel: 'Lean B' },
  { value: 1, label: 'Definitely Option B', shortLabel: 'Strong B' },
]

/** Response type identifiers returned by the backend in question.response_type */
export const RESPONSE_TYPES = {
  LIKERT_5:     'LIKERT_5',
  INTEREST_4:   'INTEREST_4',
  PREFERENCE_4: 'PREFERENCE_4',
}


export const ALL_ROLES = [
  'Backend Developer',
  'Frontend Developer',
  'Full Stack Developer',
  'Data Scientist',
  'Data Engineer',
  'Cybersecurity Engineer',
  'DevOps Engineer',
  'Cloud Engineer',
  'Android Developer',
  'QA / Test Automation Engineer',
]

export const ROLE_METADATA = {
  'Backend Developer': {
    tagline: 'Server Architecture, Distributed Systems & Database Performance',
    summary: 'Engineers the invisible backbone of modern applications—crafting high-throughput REST/gRPC APIs, architecting reliable SQL/NoSQL databases, and ensuring business logic runs reliably at massive scale.',
    keySkills: ['APIs & Microservices', 'Database Internals & SQL', 'Concurrency & Caching', 'System Design & Scaling'],
    workStyle: 'Logical, structured, and deep focus on backend reliability and data integrity.',
    accentColor: 'indigo',
    archetype: 'Systems Architect',
  },
  'Frontend Developer': {
    tagline: 'Interactive Web Interfaces, Design Systems & User Experience',
    summary: 'Builds modern, responsive, and accessible web experiences—bridging design and engineering with stateful component trees, real-time interactivity, and fluid micro-animations.',
    keySkills: ['React & Component Architecture', 'CSS & Design Systems', 'State Management & DOM', 'Web Performance & Accessibility'],
    workStyle: 'Creative, visual, and focused on intuitive human-computer interaction.',
    accentColor: 'violet',
    archetype: 'Interface Artisan',
  },
  'Full Stack Developer': {
    tagline: 'End-to-End Product Engineering & System Integration',
    summary: 'Bridges the gap between polished user experiences and robust backend services—taking feature ownership from database schemas through API endpoints all the way to pixel-perfect client rendering.',
    keySkills: ['End-to-End Architecture', 'API Integration', 'Full Lifecycle Delivery', 'State & Data Synchronization'],
    workStyle: 'Versatile, autonomous, and driven by delivering complete software products.',
    accentColor: 'cyan',
    archetype: 'Polymath Builder',
  },
  'Data Scientist': {
    tagline: 'Machine Learning, Statistical Modeling & Quantitative Insights',
    summary: 'Transforms complex enterprise and experimental datasets into predictive models and actionable mathematical insights—leveraging statistical theory, machine learning, and deep exploratory data analysis.',
    keySkills: ['Machine Learning & Algorithms', 'Applied Statistics & Probability', 'Feature Engineering', 'Data Exploration & Python'],
    workStyle: 'Analytical, investigative, and drawn to hypothesis-driven discovery.',
    accentColor: 'purple',
    archetype: 'Quantitative Researcher',
  },
  'Data Engineer': {
    tagline: 'High-Volume Data Pipelines, Warehouses & Distributed ETL',
    summary: 'Designs and orchestrates the distributed infrastructure that ingests, cleans, and delivers gigabytes to petabytes of data—enforcing data contracts, schema governance, and robust streaming/batch ETL workflows.',
    keySkills: ['Distributed ETL Pipelines', 'Data Warehouses & Lakes', 'Schema Enforcement & SQL', 'Stream Processing'],
    workStyle: 'Methodical, detail-oriented, and focused on data accuracy and pipeline durability.',
    accentColor: 'amber',
    archetype: 'Data Infrastructure Specialist',
  },
  'Cybersecurity Engineer': {
    tagline: 'Threat Modeling, System Hardening & Security Architecture',
    summary: 'Safeguards systems, networks, and data against adversarial attacks—conducting vulnerability assessments, implementing cryptographic protocols, zero-trust access controls, and incident response procedures.',
    keySkills: ['Vulnerability Assessment', 'Identity & Access Management', 'Cryptographic Protocols', 'Network Defense & Hardening'],
    workStyle: 'Vigilant, systematic, and committed to security compliance and adversarial defense.',
    accentColor: 'rose',
    archetype: 'Security Defender',
  },
  'DevOps Engineer': {
    tagline: 'CI/CD Automation, Release Velocity & Infrastructure as Code',
    summary: 'Eliminates friction between software development and production deployments—automating continuous integration pipelines, container orchestration, and rapid, zero-downtime release workflows.',
    keySkills: ['CI/CD Pipeline Automation', 'Containerization & Docker', 'Release Engineering & GitOps', 'Infrastructure as Code'],
    workStyle: 'Pragmatic, automation-driven, and focused on continuous delivery efficiency.',
    accentColor: 'emerald',
    archetype: 'Velocity & Reliability Engineer',
  },
  'Cloud Engineer': {
    tagline: 'Cloud Architecture, Distributed Infrastructure & High Availability',
    summary: 'Architects scalable, multi-region cloud infrastructures—provisioning virtual networks, elastic compute clusters, managed storage, and multi-zone failover strategies for global resilience.',
    keySkills: ['Cloud Architecture & Networks', 'High Availability & Failover', 'Cost Optimization & Security', 'Kubernetes & Microservices'],
    workStyle: 'Architectural, systems-oriented, and focused on reliability at global scale.',
    accentColor: 'sky',
    archetype: 'Cloud Infrastructure Architect',
  },
  'Android Developer': {
    tagline: 'Native Mobile Engineering, Hardware Integration & Fluid Mobile UX',
    summary: 'Develops native Android applications optimized for diverse hardware specs, screen densities, and mobile operating system constraints—crafting fluid touch experiences, offline storage, and background services.',
    keySkills: ['Native Android & Kotlin', 'Mobile UI & Lifecycle', 'Device APIs & Sensors', 'Memory & Battery Optimization'],
    workStyle: 'Product-oriented, tactile, and dedicated to native mobile user delight.',
    accentColor: 'teal',
    archetype: 'Mobile Platform Specialist',
  },
  'QA / Test Automation Engineer': {
    tagline: 'Quality Engineering, End-to-End Automation & Chaos Testing',
    summary: 'Guarantees software reliability by designing systematic test strategies, building automated regression test suites, uncovering subtle edge cases, and preventing defect regressions across the delivery lifecycle.',
    keySkills: ['Test Automation Frameworks', 'Edge-Case & Boundary Testing', 'Regression & Performance QA', 'Quality Architecture'],
    workStyle: 'Rigorous, analytical, and uncompromising on product correctness and user confidence.',
    accentColor: 'orange',
    archetype: 'Quality Assurance Guardian',
  },
}
