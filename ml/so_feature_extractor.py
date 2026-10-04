"""
RoleCompass — Real-Data Feature Extractor
==========================================
Reads Stack Overflow Developer Survey 2021 + 2022,
maps DevType → our 10 RoleCompass roles, and computes all 20
technical features from actual technology usage signals.

FEATURE LIST (must match train_model.py TECH_FEATURES and FeatureIndex.java):
  SERVER, STORAGE, API, UI, STATE, BUILD, INFRA, CONTAINER, CLOUD,
  STATS, MODEL, PIPELINE, MOBILE, THREAT, HARDENING,
  TESTDES, TESTAUTO, OBSERV, PERF, FULLSPEC

APPROACH:
  - For each respondent, scan LanguageHaveWorkedWith, DatabaseHaveWorkedWith,
    PlatformHaveWorkedWith, WebframeHaveWorkedWith, MiscTechHaveWorkedWith,
    ToolsTechHaveWorkedWith for known technology keywords.
  - Each keyword hit contributes to one or more feature scores.
  - Feature score = min(signal_count / cap, 1.0), then add small Gaussian noise.
  - DevType-derived features applied for roles with no direct tech signals
    (THREAT, HARDENING for Security; TESTDES, TESTAUTO for QA).
  - FULLSPEC computed as geometric combination of SERVER and UI signals.

LABELING STRATEGY:
  - Priority order handles multi-role respondents (specialist > generalist).
  - DevType must contain at least one of our 10 recognized types.
  - "Student" + "Academic researcher" used ONLY if they also stated a real DevType.
"""

import pandas as pd
import numpy as np
import os

np.random.seed(42)

# ─── Role Mapping ─────────────────────────────────────────────────────────────
# Priority: higher = wins when person lists multiple DevTypes.
# Specialist/niche roles get higher priority than generalist.
DEVTYPE_PRIORITY = {
    'Developer, full-stack':                        ('Full Stack Developer',              1),
    'Developer, back-end':                          ('Backend Developer',                 3),
    'Developer, front-end':                         ('Frontend Developer',                3),
    'Developer, mobile':                            ('Mobile Developer',                  4),
    'Developer, QA or test':                        ('QA / Test Automation Engineer',     4),
    'DevOps specialist':                            ('DevOps Engineer',                   4),
    'Engineer, site reliability':                   ('DevOps Engineer',                   4),
    'Cloud infrastructure engineer':                ('Cloud Engineer',                    5),
    'Engineer, data':                               ('Data Engineer',                     5),
    'Data engineer':                                ('Data Engineer',                     5),
    'Developer, AI':                                ('AI / ML Engineer',                  6),
    'Data scientist or machine learning specialist':('Data Scientist',                    5),
    'Security professional':                        ('Cybersecurity Engineer',            5),
}

TECH_FEATURES = [
    'SERVER', 'STORAGE', 'API', 'UI', 'STATE', 'BUILD', 'INFRA',
    'CONTAINER', 'CLOUD', 'STATS', 'MODEL', 'PIPELINE', 'MOBILE',
    'THREAT', 'HARDENING', 'TESTDES', 'TESTAUTO', 'OBSERV', 'PERF', 'FULLSPEC'
]

# ─── Feature Signal Definitions ───────────────────────────────────────────────
# Format: list of (col_key, exact_value_in_csv)
# col_key: 'lang' | 'db' | 'platform' | 'webframe' | 'misc' | 'tools'
# cap: signal_count is divided by this, then clipped to [0,1]

FEATURE_SIGNALS = {

    # Backend server-side logic: server languages + server-side frameworks
    'SERVER': {
        'signals': [
            ('lang', 'Java'),    ('lang', 'C#'),       ('lang', 'Go'),
            ('lang', 'PHP'),     ('lang', 'Ruby'),     ('lang', 'Rust'),
            ('webframe', 'Django'),         ('webframe', 'Flask'),
            ('webframe', 'FastAPI'),        ('webframe', 'Express'),
            ('webframe', 'Laravel'),        ('webframe', 'ASP.NET Core'),
            ('webframe', 'ASP.NET'),        ('webframe', 'Ruby on Rails'),
            ('webframe', 'Symfony'),        ('webframe', 'Phoenix'),
            ('webframe', 'Fastify'),        ('webframe', 'Play Framework'),
            ('misc', 'Spring'),
        ],
        'cap': 5,
    },

    # Database / data storage: SQL language + all DBs
    'STORAGE': {
        'signals': [
            ('lang', 'SQL'),
            ('db', 'MySQL'),                ('db', 'PostgreSQL'),
            ('db', 'MongoDB'),              ('db', 'Redis'),
            ('db', 'SQLite'),               ('db', 'Elasticsearch'),
            ('db', 'Microsoft SQL Server'), ('db', 'MariaDB'),
            ('db', 'Oracle'),               ('db', 'DynamoDB'),
            ('db', 'Firebase Realtime Database'), ('db', 'Firebase'),
            ('db', 'Cloud Firestore'),      ('db', 'Cassandra'),
            ('db', 'Neo4j'),                ('db', 'IBM DB2'),
        ],
        'cap': 4,
    },

    # API design: web/HTTP communication frameworks + API languages
    'API': {
        'signals': [
            ('webframe', 'Express'),    ('webframe', 'FastAPI'),
            ('webframe', 'Flask'),      ('webframe', 'Django'),
            ('webframe', 'ASP.NET Core'), ('webframe', 'Laravel'),
            ('webframe', 'Ruby on Rails'), ('webframe', 'Fastify'),
            ('webframe', 'Phoenix'),    ('webframe', 'Spring'),
            ('misc', 'Spring'),
            # API-oriented languages (need backend context)
            ('lang', 'Go'),   ('lang', 'Java'),  ('lang', 'C#'),
            ('lang', 'Python'), ('lang', 'PHP'),
        ],
        'cap': 5,
    },

    # UI rendering: frontend languages + frontend frameworks
    'UI': {
        'signals': [
            ('lang', 'JavaScript'),  ('lang', 'TypeScript'),  ('lang', 'HTML/CSS'),
            ('webframe', 'React.js'),   ('webframe', 'Angular'),
            ('webframe', 'Vue.js'),     ('webframe', 'Next.js'),
            ('webframe', 'Svelte'),     ('webframe', 'Nuxt.js'),
            ('webframe', 'Gatsby'),     ('webframe', 'Angular.js'),
            ('webframe', 'Blazor'),     ('webframe', 'Deno'),
        ],
        'cap': 5,
    },

    # State management: frontend framework presence implies state management
    'STATE': {
        'signals': [
            ('webframe', 'React.js'),  ('webframe', 'Angular'),
            ('webframe', 'Vue.js'),    ('webframe', 'Next.js'),
            ('webframe', 'Svelte'),    ('webframe', 'Nuxt.js'),
            ('webframe', 'Angular.js'),
        ],
        'cap': 3,
    },

    # Build pipelines / CI-CD: IaC + container + DevOps tools
    'BUILD': {
        'signals': [
            ('tools', 'Docker'),     ('tools', 'Kubernetes'),
            ('tools', 'Terraform'),  ('tools', 'Ansible'),
            ('tools', 'Puppet'),     ('tools', 'Chef'),
            ('tools', 'Pulumi'),
        ],
        'cap': 3,
    },

    # Infrastructure provisioning: scripting + IaC
    'INFRA': {
        'signals': [
            ('lang', 'Bash/Shell'),  ('lang', 'PowerShell'),
            ('tools', 'Terraform'),  ('tools', 'Ansible'),
            ('tools', 'Puppet'),     ('tools', 'Chef'),
            ('tools', 'Pulumi'),
        ],
        'cap': 3,
    },

    # Container orchestration: Docker + Kubernetes are the clearest signals
    'CONTAINER': {
        'signals': [
            ('tools', 'Docker'),
            ('tools', 'Kubernetes'),
        ],
        'cap': 2,
    },

    # Cloud services: major cloud platforms
    'CLOUD': {
        'signals': [
            ('platform', 'AWS'),
            ('platform', 'Microsoft Azure'),
            ('platform', 'Google Cloud'),
            ('platform', 'Google Cloud Platform'),
            ('platform', 'Firebase'),
            ('platform', 'Heroku'),
            ('platform', 'DigitalOcean'),
            ('platform', 'Oracle Cloud Infrastructure'),
        ],
        'cap': 2,
    },

    # Statistical analysis: data science languages + numerical libraries
    'STATS': {
        'signals': [
            ('lang', 'Python'),      ('lang', 'R'),
            ('lang', 'MATLAB'),      ('lang', 'Matlab'),
            ('lang', 'SAS'),         ('lang', 'Julia'),
            ('misc', 'NumPy'),       ('misc', 'Pandas'),
            ('misc', 'Scikit-learn'),('misc', 'Scikit-Learn'),
            ('misc', 'Tidyverse'),
        ],
        'cap': 4,
        'devtype_boost': {'Data Scientist': 0.88, 'AI / ML Engineer': 0.65},
    },

    # ML model building: deep learning + ML libraries
    'MODEL': {
        'signals': [
            ('misc', 'TensorFlow'),
            ('misc', 'Torch/PyTorch'),
            ('misc', 'Scikit-learn'),
            ('misc', 'Scikit-Learn'),
            ('misc', 'Keras'),
            ('misc', 'Hugging Face Transformers'),
        ],
        'cap': 3,
        'devtype_boost': {'AI / ML Engineer': 0.90, 'Data Scientist': 0.60},
    },

    # Data pipeline engineering: big data tools
    'PIPELINE': {
        'signals': [
            ('misc', 'Apache Kafka'),
            ('misc', 'Apache Spark'),
            ('misc', 'Hadoop'),
            ('db', 'Elasticsearch'),     # often used as a pipeline sink
        ],
        'cap': 2,
        'devtype_boost': {'Data Engineer': 0.85, 'AI / ML Engineer': 0.45},
    },

    # Mobile development: mobile languages + cross-platform frameworks
    'MOBILE': {
        'signals': [
            ('lang', 'Kotlin'),     ('lang', 'Swift'),
            ('lang', 'Dart'),       ('lang', 'Objective-C'),
            ('misc', 'Flutter'),    ('misc', 'React Native'),
            ('misc', 'Xamarin'),    ('misc', 'Ionic'),
            ('misc', 'Cordova'),    ('misc', 'Capacitor'),
            # Xamarin also appears in tools in some years
            ('tools', 'Xamarin'),
        ],
        'cap': 3,
        'devtype_boost': {'Mobile Developer': 0.85},
    },

    # THREAT: SO survey has no direct security tool fields.
    # Score is derived from DevType (Security professional) post-hoc.
    'THREAT': {
        'signals': [],
        'cap': 1,
        'devtype_boost': {'Cybersecurity Engineer': 0.82, 'DevOps Engineer': 0.30, 'Cloud Engineer': 0.32},
    },

    # HARDENING: Same gap as THREAT.
    'HARDENING': {
        'signals': [],
        'cap': 1,
        'devtype_boost': {'Cybersecurity Engineer': 0.78, 'DevOps Engineer': 0.35, 'Cloud Engineer': 0.35},
    },

    # TEST DESIGN: Limited signal. QA DevType is the primary indicator.
    'TESTDES': {
        'signals': [],
        'cap': 1,
        'devtype_boost': {'QA / Test Automation Engineer': 0.85, 'Backend Developer': 0.35,
                          'Full Stack Developer': 0.38, 'Data Scientist': 0.22},
    },

    # TEST AUTOMATION: Limited signal. QA DevType is the primary indicator.
    'TESTAUTO': {
        'signals': [],
        'cap': 1,
        'devtype_boost': {'QA / Test Automation Engineer': 0.88, 'Backend Developer': 0.32,
                          'Full Stack Developer': 0.35, 'DevOps Engineer': 0.30},
    },

    # OBSERVABILITY: DevOps/Cloud use monitoring tools.
    'OBSERV': {
        'signals': [
            ('tools', 'Docker'),       # proxy: container users typically monitor
            ('tools', 'Kubernetes'),   # proxy: K8s requires observability
            ('tools', 'Terraform'),
        ],
        'cap': 2,
        'devtype_boost': {'DevOps Engineer': 0.70, 'Cloud Engineer': 0.65,
                          'Backend Developer': 0.35, 'Full Stack Developer': 0.35},
    },

    # PERFORMANCE: Performance-oriented languages + caching (Redis)
    'PERF': {
        'signals': [
            ('lang', 'C++'),   ('lang', 'C'),
            ('lang', 'Rust'),  ('lang', 'Go'),
            ('db', 'Redis'),
        ],
        'cap': 3,
        'devtype_boost': {'Backend Developer': 0.50, 'Full Stack Developer': 0.42,
                          'Data Engineer': 0.45, 'DevOps Engineer': 0.40},
    },

    # FULLSPEC: Computed from overlap of UI + SERVER signals.
    # Special: handled in code below.
    'FULLSPEC': {
        'special': True,
        'cap': 1,
        'devtype_boost': {'Full Stack Developer': 0.85},
    },
}


# ─── Build lookup sets for fast membership check ──────────────────────────────

def build_lookup():
    """Returns col_key → set_of_values for fast intersection checks."""
    # Collect all unique (col_key, value) pairs across features
    lookup = {}
    for feat, cfg in FEATURE_SIGNALS.items():
        for col_key, val in cfg.get('signals', []):
            if col_key not in lookup:
                lookup[col_key] = set()
            lookup[col_key].add(val)
    return lookup


def parse_cell(cell):
    """Split a semicolon-delimited survey cell into a set of stripped strings."""
    if pd.isna(cell):
        return set()
    return {v.strip() for v in str(cell).split(';') if v.strip()}


def compute_features(row_sets, role_label):
    """
    Compute all 20 TECH_FEATURES for one respondent.
    row_sets: dict of col_key → set of tech values this person uses.
    role_label: resolved RoleCompass role string.
    Returns: dict of feature_name → float in [0,1].
    """
    feat_scores = {}

    # First pass: signal-based features
    for feat, cfg in FEATURE_SIGNALS.items():
        if cfg.get('special'):
            continue  # FULLSPEC handled separately

        signals = cfg.get('signals', [])
        cap = cfg.get('cap', 1)
        count = 0
        for col_key, val in signals:
            if val in row_sets.get(col_key, set()):
                count += 1

        raw_score = min(count / cap, 1.0) if cap > 0 else 0.0

        # DevType boost: applies when tech signals are sparse
        boost_map = cfg.get('devtype_boost', {})
        if boost_map and role_label in boost_map:
            target = boost_map[role_label]
            # Blend: take max of signal-based and boost (with 70% boost weight)
            raw_score = max(raw_score, target * 0.70) + raw_score * 0.30
            raw_score = min(raw_score, 1.0)

        feat_scores[feat] = raw_score

    # FULLSPEC: computed from UI vs SERVER signals
    server_cfg = FEATURE_SIGNALS['SERVER']
    ui_cfg = FEATURE_SIGNALS['UI']
    server_count = sum(1 for c, v in server_cfg['signals'] if v in row_sets.get(c, set()))
    ui_count = sum(1 for c, v in ui_cfg['signals'] if v in row_sets.get(c, set()))

    server_norm = min(server_count / server_cfg['cap'], 1.0)
    ui_norm = min(ui_count / ui_cfg['cap'], 1.0)

    # Geometric blend: rewards BOTH sides, not just one
    fullspec_raw = (server_norm * ui_norm) ** 0.5  # geometric mean = 0 if either is 0

    # DevType boost for Full Stack
    boost_map = FEATURE_SIGNALS['FULLSPEC'].get('devtype_boost', {})
    if role_label in boost_map:
        target = boost_map[role_label]
        fullspec_raw = max(fullspec_raw, target * 0.65) + fullspec_raw * 0.35
        fullspec_raw = min(fullspec_raw, 1.0)

    feat_scores['FULLSPEC'] = fullspec_raw

    return feat_scores


AI_FRAMEWORKS = {'Torch/PyTorch', 'TensorFlow', 'Keras', 'Hugging Face Transformers'}


def resolve_role(devtype_cell, row_sets=None):
    """
    Given a semicolon-delimited DevType string and respondent tech sets,
    return the best RoleCompass role label using priority ordering.
    """
    if pd.isna(devtype_cell):
        return None
    types = [t.strip() for t in str(devtype_cell).split(';')]

    # Direct match for Developer, AI
    if 'Developer, AI' in types:
        return 'AI / ML Engineer'

    # Find highest priority match
    best_role = None
    best_priority = -1
    for t in types:
        if t in DEVTYPE_PRIORITY:
            role, priority = DEVTYPE_PRIORITY[t]
            if priority > best_priority:
                best_role = role
                best_priority = priority

    # Differentiate Data Scientist vs AI / ML Engineer:
    # If using deep learning / neural net frameworks, they are AI / ML Engineer.
    # Otherwise, pure statistics / analysis is Data Scientist.
    if best_role == 'Data Scientist' and row_sets is not None:
        misc = row_sets.get('misc', set())
        if misc.intersection(AI_FRAMEWORKS):
            return 'AI / ML Engineer'

    return best_role


def process_file(fpath, year_label):
    """
    Process one SO survey CSV. Returns a DataFrame with 20 tech features + role.
    """
    print(f"\nLoading {year_label}: {fpath}")
    NEEDED = [
        'DevType',
        'LanguageHaveWorkedWith',  'LanguageWorkedWith',
        'DatabaseHaveWorkedWith',  'DatabaseWorkedWith',
        'PlatformHaveWorkedWith',  'PlatformWorkedWith',
        'WebframeHaveWorkedWith',  'WebframeWorkedWith',
        'MiscTechHaveWorkedWith',  'MiscTechWorkedWith',
        'ToolsTechHaveWorkedWith', 'ToolsTechWorkedWith',
    ]
    df_raw = pd.read_csv(fpath, usecols=lambda c: c in NEEDED, low_memory=False)
    print(f"  Raw rows: {len(df_raw)}")

    # Normalize column names (2021, 2022, 2023 use similar names)
    col_map = {
        'lang':     next((c for c in ['LanguageHaveWorkedWith', 'LanguageWorkedWith'] if c in df_raw.columns), None),
        'db':       next((c for c in ['DatabaseHaveWorkedWith', 'DatabaseWorkedWith'] if c in df_raw.columns), None),
        'platform': next((c for c in ['PlatformHaveWorkedWith', 'PlatformWorkedWith'] if c in df_raw.columns), None),
        'webframe': next((c for c in ['WebframeHaveWorkedWith', 'WebframeWorkedWith'] if c in df_raw.columns), None),
        'misc':     next((c for c in ['MiscTechHaveWorkedWith', 'MiscTechWorkedWith'] if c in df_raw.columns), None),
        'tools':    next((c for c in ['ToolsTechHaveWorkedWith', 'ToolsTechWorkedWith'] if c in df_raw.columns), None),
    }
    print(f"  Column mapping: { {k:v for k,v in col_map.items() if v} }")

    records = []
    role_counts = {}

    for _, row in df_raw.iterrows():
        # Build tech sets first
        row_sets = {}
        for col_key, col_name in col_map.items():
            if col_name:
                row_sets[col_key] = parse_cell(row[col_name])

        # Special: 2021 has Node.js in LanguageHaveWorkedWith, 2022 in WebframeHaveWorkedWith
        # Unify: if Node.js is in lang, treat it as both lang AND webframe
        if 'Node.js' in row_sets.get('lang', set()):
            if 'webframe' not in row_sets:
                row_sets['webframe'] = set()
            row_sets['webframe'].add('Node.js')

        role = resolve_role(row['DevType'], row_sets)
        if role is None:
            continue

        # Compute features
        feat_scores = compute_features(row_sets, role)

        # Add small Gaussian noise for variability (std = 0.04)
        noise = np.random.normal(0, 0.04, len(TECH_FEATURES))
        final_scores = {
            feat: float(np.clip(feat_scores[feat] + noise[i], 0.0, 1.0))
            for i, feat in enumerate(TECH_FEATURES)
        }
        final_scores['role'] = role
        final_scores['source'] = year_label
        records.append(final_scores)
        role_counts[role] = role_counts.get(role, 0) + 1

    print(f"\n  Role distribution for {year_label}:")
    for r, c in sorted(role_counts.items(), key=lambda x: -x[1]):
        print(f"    {r:<40} {c:>6} samples")

    return pd.DataFrame(records)


# ─── Main ─────────────────────────────────────────────────────────────────────

datasets = []
for year, path in [('SO_2023', 'ml/dataset/survey_results_public.csv'),
                   ('SO_2022', 'ml/dataset/so_2022.csv'),
                   ('SO_2021', 'ml/dataset/so_2021.csv')]:
    df_year = process_file(path, year)
    datasets.append(df_year)

df_real = pd.concat(datasets, ignore_index=True)
df_real = df_real.drop(columns=['source'])

print(f"\n{'='*60}")
print(f"COMBINED REAL DATA: {len(df_real)} rows, {df_real['role'].nunique()} roles")
print(f"{'='*60}")
print(df_real['role'].value_counts().to_string())

# Feature means per role — quality check
print(f"\nKey tech feature means per role:")
key_cols = ['SERVER', 'UI', 'STATS', 'MODEL', 'PIPELINE', 'MOBILE', 'THREAT', 'TESTAUTO', 'CLOUD', 'FULLSPEC']
print(df_real.groupby('role')[key_cols].mean().round(3).to_string())

os.makedirs('ml/dataset', exist_ok=True)
out_path = 'ml/dataset/so_derived.csv'
df_real.to_csv(out_path, index=False)
print(f"\n[OK] Saved {out_path}  ({os.path.getsize(out_path)//1024} KB, {len(df_real):,} rows)")
print("Next: python ml/build_training_dataset.py")
