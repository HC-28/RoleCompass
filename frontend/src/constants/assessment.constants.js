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
