"""
RoleCompass — Build Final Training Dataset (11 Roles)
======================================================
Merges real SO survey data with targeted synthetic supplements for roles
that are underrepresented or need sharp signal discrimination.

11 Target Roles:
  1. Backend Developer
  2. Frontend Developer
  3. Full Stack Developer
  4. Data Scientist
  5. AI / ML Engineer
  6. Data Engineer
  7. Cybersecurity Engineer
  8. DevOps Engineer
  9. Cloud Engineer
  10. Mobile Developer
  11. QA / Test Automation Engineer
"""

import pandas as pd
import numpy as np
import os

np.random.seed(42)

TECH_FEATURES = [
    'SERVER', 'STORAGE', 'API', 'UI', 'STATE', 'BUILD', 'INFRA',
    'CONTAINER', 'CLOUD', 'STATS', 'MODEL', 'PIPELINE', 'MOBILE',
    'THREAT', 'HARDENING', 'TESTDES', 'TESTAUTO', 'OBSERV', 'PERF', 'FULLSPEC'
]

def synth(role, means, stds, n):
    """Generate n synthetic samples from Gaussian distributions. Clips to [0,1]."""
    rows = []
    for _ in range(n):
        row = {'role': role}
        for feat in TECH_FEATURES:
            mu = means.get(feat, 0.15)
            sd = stds.get(feat, 0.06)
            val = np.clip(np.random.normal(mu, sd), 0.0, 1.0)
            row[feat] = float(val)
        rows.append(row)
    return rows

# ─── Load real data ────────────────────────────────────────────────────────────
print("Loading real SO survey data...")
df_real = pd.read_csv('ml/dataset/so_derived.csv')
print(f"Real data: {len(df_real):,} rows across {df_real['role'].nunique()} roles")
print(df_real['role'].value_counts().to_string())

# ─── Targeted synthetic supplements ──────────────────────────────────────────

synthetic_rows = []

# ── 1. CYBERSECURITY ENGINEER (+3,500 samples) ────────────────────────────────
print("\nGenerating Cybersecurity synthetic samples...")
synthetic_rows += synth(
    'Cybersecurity Engineer',
    means={
        'SERVER':    0.40, 'STORAGE':  0.35, 'API':      0.30,
        'UI':        0.08, 'STATE':    0.06, 'BUILD':    0.30,
        'INFRA':     0.55, 'CONTAINER':0.35, 'CLOUD':    0.42,
        'STATS':     0.20, 'MODEL':    0.08, 'PIPELINE': 0.10,
        'MOBILE':    0.06, 'THREAT':   0.87, 'HARDENING':0.82,
        'TESTDES':   0.28, 'TESTAUTO': 0.22, 'OBSERV':   0.55,
        'PERF':      0.32, 'FULLSPEC': 0.12,
    },
    stds={f: 0.07 for f in TECH_FEATURES},
    n=3500
)

# ── 2. CLOUD ENGINEER (+1,500 samples) ────────────────────────────────────────
print("Generating Cloud Engineer synthetic samples...")
synthetic_rows += synth(
    'Cloud Engineer',
    means={
        'SERVER':    0.42, 'STORAGE':  0.45, 'API':      0.45,
        'UI':        0.15, 'STATE':    0.10, 'BUILD':    0.72,
        'INFRA':     0.78, 'CONTAINER':0.85, 'CLOUD':    0.90,
        'STATS':     0.18, 'MODEL':    0.08, 'PIPELINE': 0.35,
        'MOBILE':    0.06, 'THREAT':   0.32, 'HARDENING':0.38,
        'TESTDES':   0.18, 'TESTAUTO': 0.20, 'OBSERV':   0.72,
        'PERF':      0.38, 'FULLSPEC': 0.22,
    },
    stds={f: 0.07 for f in TECH_FEATURES},
    n=1500
)

# ── 3. MOBILE DEVELOPER — specialist boost (+1,500 samples) ───────────────────
print("Generating Mobile Developer specialist samples...")
synthetic_rows += synth(
    'Mobile Developer',
    means={
        'SERVER':    0.25, 'STORAGE':  0.40, 'API':      0.35,
        'UI':        0.45, 'STATE':    0.40, 'BUILD':    0.25,
        'INFRA':     0.18, 'CONTAINER':0.18, 'CLOUD':    0.45,
        'STATS':     0.10, 'MODEL':    0.10, 'PIPELINE': 0.08,
        'MOBILE':    0.90, 'THREAT':   0.08, 'HARDENING':0.12,
        'TESTDES':   0.35, 'TESTAUTO': 0.32, 'OBSERV':   0.22,
        'PERF':      0.45, 'FULLSPEC': 0.15,
    },
    stds={f: 0.07 for f in TECH_FEATURES},
    n=1500
)

# ── 4. QA / TEST AUTOMATION ENGINEER (+1,000 samples) ────────────────────────
print("Generating QA synthetic samples...")
synthetic_rows += synth(
    'QA / Test Automation Engineer',
    means={
        'SERVER':    0.30, 'STORAGE':  0.32, 'API':      0.30,
        'UI':        0.38, 'STATE':    0.28, 'BUILD':    0.42,
        'INFRA':     0.22, 'CONTAINER':0.22, 'CLOUD':    0.30,
        'STATS':     0.18, 'MODEL':    0.10, 'PIPELINE': 0.12,
        'MOBILE':    0.18, 'THREAT':   0.22, 'HARDENING':0.20,
        'TESTDES':   0.88, 'TESTAUTO': 0.90, 'OBSERV':   0.40,
        'PERF':      0.28, 'FULLSPEC': 0.28,
    },
    stds={f: 0.07 for f in TECH_FEATURES},
    n=1000
)

# ── 5. DATA ENGINEER — pipeline specialist boost (+500 samples) ───────────────
print("Generating Data Engineer pipeline specialist samples...")
synthetic_rows += synth(
    'Data Engineer',
    means={
        'SERVER':    0.35, 'STORAGE':  0.75, 'API':      0.42,
        'UI':        0.10, 'STATE':    0.08, 'BUILD':    0.55,
        'INFRA':     0.45, 'CONTAINER':0.50, 'CLOUD':    0.62,
        'STATS':     0.48, 'MODEL':    0.20, 'PIPELINE': 0.88,
        'MOBILE':    0.05, 'THREAT':   0.10, 'HARDENING':0.15,
        'TESTDES':   0.15, 'TESTAUTO': 0.18, 'OBSERV':   0.58,
        'PERF':      0.42, 'FULLSPEC': 0.15,
    },
    stds={f: 0.07 for f in TECH_FEATURES},
    n=500
)

# ── 6. DATA SCIENTIST — pure statistics / analytics boost (+1,500 samples) ────
print("Generating Data Scientist statistical specialist samples...")
synthetic_rows += synth(
    'Data Scientist',
    means={
        'SERVER':    0.25, 'STORAGE':  0.65, 'API':      0.25,
        'UI':        0.12, 'STATE':    0.08, 'BUILD':    0.20,
        'INFRA':     0.15, 'CONTAINER':0.20, 'CLOUD':    0.40,
        'STATS':     0.94, 'MODEL':    0.48, 'PIPELINE': 0.25,
        'MOBILE':    0.05, 'THREAT':   0.05, 'HARDENING':0.05,
        'TESTDES':   0.15, 'TESTAUTO': 0.15, 'OBSERV':   0.20,
        'PERF':      0.25, 'FULLSPEC': 0.10,
    },
    stds={f: 0.07 for f in TECH_FEATURES},
    n=1500
)

# ── 7. AI / ML ENGINEER — deep learning / production model boost (+2,000 samples)
print("Generating AI / ML Engineer deep learning specialist samples...")
synthetic_rows += synth(
    'AI / ML Engineer',
    means={
        'SERVER':    0.40, 'STORAGE':  0.45, 'API':      0.45,
        'UI':        0.10, 'STATE':    0.08, 'BUILD':    0.35,
        'INFRA':     0.30, 'CONTAINER':0.50, 'CLOUD':    0.58,
        'STATS':     0.70, 'MODEL':    0.95, 'PIPELINE': 0.55,
        'MOBILE':    0.06, 'THREAT':   0.08, 'HARDENING':0.08,
        'TESTDES':   0.15, 'TESTAUTO': 0.18, 'OBSERV':   0.35,
        'PERF':      0.48, 'FULLSPEC': 0.15,
    },
    stds={f: 0.07 for f in TECH_FEATURES},
    n=2000
)

# ─── Combine all data ─────────────────────────────────────────────────────────
df_synth = pd.DataFrame(synthetic_rows, columns=TECH_FEATURES + ['role'])
df_all = pd.concat([df_real, df_synth], ignore_index=True)

print(f"\n{'='*60}")
print("FINAL TRAINING DATASET SUMMARY (11 ROLES)")
print(f"{'='*60}")
print(f"Total rows: {len(df_all):,}")
print(f"\nSamples per role (real + synthetic):")

role_counts = df_all['role'].value_counts()
real_counts = df_real['role'].value_counts()
synth_added = {
    'Cybersecurity Engineer': 3500,
    'Cloud Engineer': 1500,
    'Mobile Developer': 1500,
    'QA / Test Automation Engineer': 1000,
    'Data Engineer': 500,
    'Data Scientist': 1500,
    'AI / ML Engineer': 2000,
}
for role, total in role_counts.items():
    real = real_counts.get(role, 0)
    added = synth_added.get(role, 0)
    bar = '#' * (total // 500)
    print(f"  {role:<40} {total:>6} total  ({real:>6} real + {added:>5} synth)  {bar}")

# ─── Quality checks ──────────────────────────────────────────────────────────
print(f"\nKey distinguishing feature means per role:")
key_cols = ['SERVER', 'UI', 'STATS', 'MODEL', 'PIPELINE', 'MOBILE', 'THREAT', 'HARDENING', 'TESTAUTO', 'CLOUD', 'FULLSPEC', 'OBSERV']
means_df = df_all.groupby('role')[key_cols].mean().round(3)
print(means_df.to_string())

# Sanity checks
g = df_all.groupby('role')
checks = [
    ("Cyber has highest THREAT",          g['THREAT'].mean().idxmax()   == 'Cybersecurity Engineer'),
    ("Cyber has highest HARDENING",       g['HARDENING'].mean().idxmax()== 'Cybersecurity Engineer'),
    ("Frontend has highest UI",           g['UI'].mean().idxmax()       == 'Frontend Developer'),
    ("Full Stack has highest FULLSPEC",   g['FULLSPEC'].mean().idxmax() == 'Full Stack Developer'),
    ("AI / ML Eng has highest MODEL",     g['MODEL'].mean().idxmax()    == 'AI / ML Engineer'),
    ("Data Scientist has highest STATS",  g['STATS'].mean().idxmax()    == 'Data Scientist'),
    ("Mobile has highest MOBILE",         g['MOBILE'].mean().idxmax()   == 'Mobile Developer'),
    ("Cloud has highest CLOUD",           g['CLOUD'].mean().idxmax()    == 'Cloud Engineer'),
    ("QA has highest TESTAUTO",           g['TESTAUTO'].mean().idxmax() == 'QA / Test Automation Engineer'),
    ("Data Eng has highest PIPELINE",     g['PIPELINE'].mean().idxmax() == 'Data Engineer'),
    ("All features in [0,1]",             (df_all[TECH_FEATURES] >= 0).all().all() and (df_all[TECH_FEATURES] <= 1).all().all()),
]

print(f"\nSanity checks:")
passed = sum(1 for _, ok in checks if ok)
for desc, ok in checks:
    print(f"  {'[PASS]' if ok else '[FAIL]'}  {desc}")
print(f"\n  {passed}/{len(checks)} checks passed")

# ─── Save ─────────────────────────────────────────────────────────────────────
os.makedirs('ml/dataset', exist_ok=True)
out_path = 'ml/dataset/rolecompass_training.csv'
df_all[TECH_FEATURES + ['role']].to_csv(out_path, index=False)
print(f"\n[OK] Saved {out_path}  ({os.path.getsize(out_path)//1024} KB, {len(df_all):,} rows)")
print("Next: python ml/train_model.py")
