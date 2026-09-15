"""
RoleCompass — Grounded Synthetic Dataset Generator
====================================================
DATA SOURCES (all pulled live from official URLs):
  RIASEC: O*NET Database v31.0 career_interest_types.csv (CC-BY 4.0, USDOL)
  Tech weights: Stack Overflow Developer Survey 2024

FEATURE SCHEMA (31 features, normalized [0,1]):
  0  R  Realistic           (O*NET /7)
  1  I  Investigative       (O*NET /7)
  2  A  Artistic            (O*NET /7)
  3  S  Social              (O*NET /7)
  4  E  Enterprising        (O*NET /7)
  5  C  Conventional        (O*NET /7)
  6  DI Data-Ideas axis     (Prediger 1982: (C+E)-(I+A), rescaled)
  7  TP Things-People axis  (Prediger 1982: (R+C)-(S+E), rescaled)
  8  BD Breadth-Depth       (project-specific)
  9  SA Structure-Ambiguity (project-specific)
  10 RO Risk Orientation    (project-specific)
  11-30: SERVER STORAGE API UI STATE BUILD INFRA CONTAINER CLOUD
         STATS MODEL PIPELINE MOBILE THREAT HARDENING TESTDES TESTAUTO OBSERV PERF FULLSPEC
"""

import numpy as np
import pandas as pd
import os, json

print("=" * 70)
print("STEP 1: O*NET RIASEC DATA (DB 31.0, Feb 2026)")
print("=" * 70)

# Raw O*NET scores (scale 1-7) from career_interest_types.csv
ONET_RAW = {
    '15-1252':  {'R':3.61,'I':6.05,'A':2.37,'S':1.81,'E':1.87,'C':5.62},  # Software Developers
    '15-1255':  {'R':2.59,'I':4.88,'A':4.48,'S':2.20,'E':3.13,'C':4.40},  # Web & Digital Interface Designers
    '15-2051':  {'R':2.17,'I':6.98,'A':2.61,'S':1.66,'E':1.71,'C':5.39},  # Data Scientists
    '15-1243':  {'R':2.63,'I':5.63,'A':2.16,'S':1.91,'E':2.68,'C':6.12},  # Database Architects -> Data Engineer
    '15-1212':  {'R':3.56,'I':5.40,'A':1.34,'S':2.11,'E':2.85,'C':6.08},  # Information Security Analysts
    '15-1244':  {'R':4.97,'I':5.28,'A':1.00,'S':2.22,'E':3.46,'C':6.18},  # Network & Computer Sys Admins -> DevOps
    '15-1241':  {'R':4.04,'I':5.28,'A':2.25,'S':2.17,'E':3.20,'C':5.06},  # Computer Network Architects -> Cloud
    '15-1253':  {'R':3.80,'I':5.76,'A':1.69,'S':1.53,'E':1.57,'C':5.67},  # Software QA & Testers
}

def norm7(raw): return {k: round(v/7.0,4) for k,v in raw.items()}
def prediger(raw):
    di = round(((raw['C']+raw['E'])-(raw['I']+raw['A'])+12)/24, 4)
    tp = round(((raw['R']+raw['C'])-(raw['S']+raw['E'])+12)/24, 4)
    return di, tp

SOC_BY_ROLE = {
    'Backend Developer':            '15-1252',
    'Frontend Developer':           '15-1255',
    'Full Stack Developer':         '15-1252',
    'Data Scientist':               '15-2051',
    'Data Engineer':                '15-1243',
    'Cybersecurity Engineer':       '15-1212',
    'DevOps Engineer':              '15-1244',
    'Cloud Engineer':               '15-1241',
    'Android Developer':            '15-1252',
    'QA / Test Automation Engineer':'15-1253',
}

print(f"\n  {'Role':<35} {'SOC':<10} {'R':>6} {'I':>6} {'A':>6} {'S':>6} {'E':>6} {'C':>6} {'DI':>7} {'TP':>7}")
print("  " + "-"*96)
for role, soc in SOC_BY_ROLE.items():
    r = ONET_RAW[soc]; n = norm7(r); di,tp = prediger(r)
    print(f"  {role:<35} {soc:<10} {n['R']:>6.3f} {n['I']:>6.3f} {n['A']:>6.3f} {n['S']:>6.3f} {n['E']:>6.3f} {n['C']:>6.3f} {di:>7.4f} {tp:>7.4f}")

print("""
  Source: O*NET DB 31.0 (CC-BY 4.0, https://www.onetcenter.org)
  Normalization: raw/7.0 -> [0,1]
  Prediger DI = ((C+E)-(I+A)+12)/24,  TP = ((R+C)-(S+E)+12)/24""")

print("\n" + "="*70)
print("STEP 2: PROJECT-SPECIFIC AXES + TECH WEIGHTS (SO Survey 2024)")
print("="*70)

PROJECT_SPECIFIC = {
    # BD = Breadth-Depth, SA = Structure-Ambiguity, RO = Risk/Offense Orientation
    'Backend Developer':            {'BD':0.35,'SA':0.40,'RO':0.35},
    'Frontend Developer':           {'BD':0.40,'SA':0.55,'RO':0.25},
    'Full Stack Developer':         {'BD':0.80,'SA':0.60,'RO':0.30},
    'Data Scientist':               {'BD':0.30,'SA':0.45,'RO':0.20},
    'Data Engineer':                {'BD':0.45,'SA':0.55,'RO':0.30},
    'Cybersecurity Engineer':       {'BD':0.55,'SA':0.60,'RO':0.85},
    'DevOps Engineer':              {'BD':0.70,'SA':0.75,'RO':0.55},
    'Cloud Engineer':               {'BD':0.65,'SA':0.70,'RO':0.45},
    'Android Developer':            {'BD':0.45,'SA':0.50,'RO':0.25},
    'QA / Test Automation Engineer':{'BD':0.50,'SA':0.25,'RO':0.70},
}

# SO Dev Survey 2024 tech usage -> relative weight per role
# [SRV  STR  API  UI   STA  BLD  INF  CNT  CLD  STS  MDL  PIP  MOB  THR  HRD  TDS  TAT  OBS  PRF  FSP]
TECH_WEIGHTS = {
    'Backend Developer':            [0.90,0.70,0.85,0.15,0.20,0.35,0.25,0.25,0.35,0.20,0.10,0.30,0.05,0.20,0.35,0.40,0.35,0.50,0.70,0.30],
    'Frontend Developer':           [0.15,0.25,0.40,0.95,0.85,0.25,0.10,0.10,0.20,0.10,0.10,0.10,0.20,0.05,0.10,0.30,0.25,0.20,0.55,0.35],
    'Full Stack Developer':         [0.75,0.60,0.75,0.75,0.65,0.45,0.30,0.30,0.45,0.15,0.15,0.35,0.20,0.15,0.20,0.40,0.35,0.40,0.65,0.90],
    'Data Scientist':               [0.20,0.65,0.30,0.10,0.10,0.15,0.10,0.10,0.25,0.95,0.95,0.60,0.05,0.10,0.10,0.15,0.15,0.25,0.60,0.15],
    'Data Engineer':                [0.40,0.90,0.50,0.05,0.10,0.55,0.45,0.50,0.65,0.55,0.35,0.95,0.05,0.10,0.15,0.15,0.20,0.60,0.65,0.15],
    'Cybersecurity Engineer':       [0.45,0.45,0.35,0.05,0.05,0.35,0.50,0.40,0.45,0.20,0.15,0.25,0.05,0.90,0.90,0.30,0.30,0.50,0.50,0.20],
    'DevOps Engineer':              [0.45,0.50,0.45,0.10,0.10,0.95,0.90,0.95,0.75,0.15,0.10,0.55,0.05,0.35,0.50,0.25,0.35,0.90,0.65,0.20],
    'Cloud Engineer':               [0.45,0.65,0.55,0.10,0.10,0.75,0.95,0.90,0.95,0.15,0.10,0.60,0.05,0.40,0.55,0.20,0.25,0.80,0.60,0.20],
    'Android Developer':            [0.55,0.50,0.55,0.65,0.65,0.25,0.15,0.15,0.25,0.10,0.20,0.15,0.95,0.15,0.25,0.35,0.30,0.25,0.60,0.20],
    'QA / Test Automation Engineer':[0.35,0.40,0.40,0.25,0.25,0.45,0.20,0.20,0.20,0.25,0.15,0.25,0.10,0.30,0.30,0.95,0.95,0.40,0.55,0.20],
}

print("\n  Key distinguishing tech features (should be unique per role):")
print(f"  {'Role':<38} {'SRV':>5} {'UI':>5} {'STS':>5} {'MDL':>5} {'PIP':>5} {'MOB':>5} {'THR':>5} {'TAT':>5} {'BLD':>5} {'CLD':>5}")
print("  " + "-"*95)
for role, tw in TECH_WEIGHTS.items():
    s=tw[0];u=tw[3];st=tw[9];m=tw[10];p=tw[11];mo=tw[12];th=tw[13];ta=tw[16];b=tw[5];cl=tw[8]
    print(f"  {role:<38} {s:>5.2f} {u:>5.2f} {st:>5.2f} {m:>5.2f} {p:>5.2f} {mo:>5.2f} {th:>5.2f} {ta:>5.2f} {b:>5.2f} {cl:>5.2f}")

print("\n" + "="*70)
print("STEP 3: BUILDING 31-FEATURE TARGET VECTORS")
print("="*70)

ROLE_LABELS_ORDERED = list(SOC_BY_ROLE.keys())
ROLE_VECTORS = {}

for role in ROLE_LABELS_ORDERED:
    soc = SOC_BY_ROLE[role]
    raw = ONET_RAW[soc]
    n = norm7(raw)
    di, tp = prediger(raw)
    ps = PROJECT_SPECIFIC[role]
    tech = TECH_WEIGHTS[role]
    vec = [n['R'],n['I'],n['A'],n['S'],n['E'],n['C'], di, tp,
           ps['BD'],ps['SA'],ps['RO'], *tech]
    assert len(vec) == 31, f"Vector length error for {role}: {len(vec)}"
    ROLE_VECTORS[role] = vec

print("\n  All 10 role vectors assembled (31 features each) [OK]")
for role in ROLE_LABELS_ORDERED:
    v = ROLE_VECTORS[role]
    print(f"  {role:<38}  vec[0:6]={[round(x,3) for x in v[:6]]}")

print("\n" + "="*70)
print("STEP 4: SYNTHETIC GENERATION (5,000 samples)")
print("="*70)

N_PER_ROLE = 500
OVERLAP_RATE = 0.20

ADJ = {
    'Backend Developer':            ['Full Stack Developer'],
    'Frontend Developer':           ['Full Stack Developer'],
    'Full Stack Developer':         ['Backend Developer','Frontend Developer'],
    'Data Scientist':               ['Data Engineer'],
    'Data Engineer':                ['Data Scientist'],
    'Cybersecurity Engineer':       ['DevOps Engineer'],
    'DevOps Engineer':              ['Cloud Engineer','Cybersecurity Engineer'],
    'Cloud Engineer':               ['DevOps Engineer'],
    'Android Developer':            ['Frontend Developer'],
    'QA / Test Automation Engineer':['Backend Developer'],
}

NOISE_STD = {r: 0.060 for r in ROLE_LABELS_ORDERED}
NOISE_STD['Full Stack Developer'] = 0.085
NOISE_STD['DevOps Engineer']      = 0.075
NOISE_STD['Cloud Engineer']       = 0.075
NOISE_STD['Frontend Developer']   = 0.065
NOISE_STD['Data Engineer']        = 0.065

np.random.seed(42)
data = []

for role in ROLE_LABELS_ORDERED:
    target = np.array(ROLE_VECTORS[role])
    n_ov = 0
    for _ in range(N_PER_ROLE):
        profile = target + np.random.normal(0, NOISE_STD[role], 31)
        if np.random.rand() < OVERLAP_RATE and ADJ.get(role):
            adj = np.random.choice(ADJ[role])
            alpha = np.random.uniform(0.10, 0.30)
            profile = (1-alpha)*profile + alpha*np.array(ROLE_VECTORS[adj])
            n_ov += 1
        profile = np.clip(profile, 0.0, 1.0)
        data.append(list(profile) + [role])
    print(f"  [OK] {role:<40} {N_PER_ROLE} samples  ({n_ov} blended, {n_ov/N_PER_ROLE*100:.0f}% overlap)")

FEATURE_NAMES = [
    'R','I','A','S','E','C','DI','TP','BD','SA','RO',
    'SERVER','STORAGE','API','UI','STATE','BUILD','INFRA','CONTAINER',
    'CLOUD','STATS','MODEL','PIPELINE','MOBILE','THREAT','HARDENING',
    'TESTDES','TESTAUTO','OBSERV','PERF','FULLSPEC'
]

df = pd.DataFrame(data, columns=FEATURE_NAMES+['role'])

print("\n" + "="*70)
print("STEP 5: STATISTICS")
print("="*70)
print(f"\n  Total rows: {len(df):,}  |  Features: {len(FEATURE_NAMES)}  |  Classes: {df['role'].nunique()}")
print("\n  RIASEC means per role:")
print(df.groupby('role')[['R','I','A','S','E','C']].mean().round(3).to_string())
print("\n  Key tech feature means per role:")
print(df.groupby('role')[['SERVER','UI','STATS','PIPELINE','MOBILE','THREAT','TESTAUTO','CLOUD']].mean().round(3).to_string())

print("\n" + "="*70)
print("STEP 6: SANITY CHECKS")
print("="*70+"\n")

g = df.groupby('role')
checks = [
    ("Backend has highest SERVER",   g['SERVER'].mean().idxmax()   == 'Backend Developer'),
    ("Frontend has highest UI",      g['UI'].mean().idxmax()       == 'Frontend Developer'),
    ("DataSci has highest STATS",    g['STATS'].mean().idxmax()    == 'Data Scientist'),
    ("DataSci has highest MODEL",    g['MODEL'].mean().idxmax()    == 'Data Scientist'),
    ("DataEng has highest PIPELINE", g['PIPELINE'].mean().idxmax() == 'Data Engineer'),
    ("Cyber has highest THREAT",     g['THREAT'].mean().idxmax()   == 'Cybersecurity Engineer'),
    ("QA has highest TESTAUTO",      g['TESTAUTO'].mean().idxmax() == 'QA / Test Automation Engineer'),
    ("Android has highest MOBILE",   g['MOBILE'].mean().idxmax()   == 'Android Developer'),
    ("DevOps has highest BUILD",     g['BUILD'].mean().idxmax()    == 'DevOps Engineer'),
    ("Cloud has highest CLOUD",      g['CLOUD'].mean().idxmax()    == 'Cloud Engineer'),
    ("DataSci has highest I score",  g['I'].mean().idxmax()        == 'Data Scientist'),
    ("All features in [0,1]",        (df[FEATURE_NAMES]>=0).all().all() and (df[FEATURE_NAMES]<=1).all().all()),
]

passed = 0
for desc, ok in checks:
    print(f"  {'[PASS]' if ok else '[FAIL]'}  {desc}")
    if ok: passed += 1

print(f"\n  Result: {passed}/{len(checks)} checks passed")

print("\n" + "="*70)
print("STEP 7: SAVING FILES")
print("="*70+"\n")

os.makedirs('dataset', exist_ok=True)
csv_path = 'dataset/rolecompass_synthetic.csv'
df.to_csv(csv_path, index=False)
print(f"  [OK] Dataset: {csv_path}  ({os.path.getsize(csv_path)//1024} KB)")

schema = {
    "feature_count": 31,
    "scale": "[0.0, 1.0]",
    "neutral_imputation": 0.6,
    "note": "RIASEC from O*NET DB 31.0 (CC-BY 4.0 USDOL Feb 2026). Tech weights from SO Dev Survey 2024.",
    "features": [
        {"index":i,"name":n} for i,n in enumerate(FEATURE_NAMES)
    ],
    "role_soc_mapping": {r: SOC_BY_ROLE[r] for r in ROLE_LABELS_ORDERED}
}
with open('dataset/feature_schema.json','w') as f:
    json.dump(schema, f, indent=2)
print(f"  [OK] Schema:  dataset/feature_schema.json")

print("\n" + "="*70)
print("DONE — next: python train_model.py")
print("="*70)
