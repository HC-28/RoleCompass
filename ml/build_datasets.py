import pandas as pd
import numpy as np
import os
import json
import shutil
import sys

# Force UTF-8 output
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")

# Paths derived from this script's location — works on any machine.
# build_datasets.py lives in:  <project>/ml/
# ml/dataset lives in:         <project>/ml/dataset/
# root dataset lives in:       <project>/dataset/
_HERE   = os.path.dirname(os.path.abspath(__file__))
BASE    = os.path.join(_HERE, "dataset")
KAGGLE  = os.path.join(os.path.dirname(_HERE), "dataset")

if not os.path.isdir(KAGGLE):
    print(f"[WARN] Kaggle dataset directory not found: {KAGGLE}")
    print("       Download the three Kaggle CSV files into that folder before running.")

ONET_RAW = {
    "15-1252": {"R":3.61,"I":6.05,"A":2.37,"S":1.81,"E":1.87,"C":5.62, "title":"Software Developers"},
    "15-1255": {"R":2.59,"I":4.88,"A":4.48,"S":2.20,"E":3.13,"C":4.40, "title":"Web & Digital Interface Designers"},
    "15-2051": {"R":2.17,"I":6.98,"A":2.61,"S":1.66,"E":1.71,"C":5.39, "title":"Data Scientists"},
    "15-1243": {"R":2.63,"I":5.63,"A":2.16,"S":1.91,"E":2.68,"C":6.12, "title":"Database Architects"},
    "15-1212": {"R":3.56,"I":5.40,"A":1.34,"S":2.11,"E":2.85,"C":6.08, "title":"Information Security Analysts"},
    "15-1244": {"R":4.97,"I":5.28,"A":1.00,"S":2.22,"E":3.46,"C":6.18, "title":"Network & Computer Sys Admins"},
    "15-1241": {"R":4.04,"I":5.28,"A":2.25,"S":2.17,"E":3.20,"C":5.06, "title":"Computer Network Architects"},
    "15-1253": {"R":3.80,"I":5.76,"A":1.69,"S":1.53,"E":1.57,"C":5.67, "title":"Software QA Analysts & Testers"},
}

ROLE_SOC = {
    "Backend Developer":            "15-1252",
    "Frontend Developer":           "15-1255",
    "Full Stack Developer":         "15-1252",
    "Data Scientist":               "15-2051",
    "Data Engineer":                "15-1243",
    "Cybersecurity Engineer":       "15-1212",
    "DevOps Engineer":              "15-1244",
    "Cloud Engineer":               "15-1241",
    "Android Developer":            "15-1252",
    "QA / Test Automation Engineer":"15-1253",
}

TECH_WEIGHTS = {
    "Backend Developer":            [0.90,0.70,0.85,0.15,0.20,0.35,0.25,0.25,0.35,0.20,0.10,0.30,0.05,0.20,0.35,0.40,0.35,0.50,0.70,0.30],
    "Frontend Developer":           [0.15,0.25,0.40,0.95,0.85,0.25,0.10,0.10,0.20,0.10,0.10,0.10,0.20,0.05,0.10,0.30,0.25,0.20,0.55,0.35],
    "Full Stack Developer":         [0.75,0.60,0.75,0.75,0.65,0.45,0.30,0.30,0.45,0.15,0.15,0.35,0.20,0.15,0.20,0.40,0.35,0.40,0.65,0.90],
    "Data Scientist":               [0.20,0.65,0.30,0.10,0.10,0.15,0.10,0.10,0.25,0.95,0.95,0.60,0.05,0.10,0.10,0.15,0.15,0.25,0.60,0.15],
    "Data Engineer":                [0.40,0.90,0.50,0.05,0.10,0.55,0.45,0.50,0.65,0.55,0.35,0.95,0.05,0.10,0.15,0.15,0.20,0.60,0.65,0.15],
    "Cybersecurity Engineer":       [0.45,0.45,0.35,0.05,0.05,0.35,0.50,0.40,0.45,0.20,0.15,0.25,0.05,0.90,0.90,0.30,0.30,0.50,0.50,0.20],
    "DevOps Engineer":              [0.45,0.50,0.45,0.10,0.10,0.95,0.90,0.95,0.75,0.15,0.10,0.55,0.05,0.35,0.50,0.25,0.35,0.90,0.65,0.20],
    "Cloud Engineer":               [0.45,0.65,0.55,0.10,0.10,0.75,0.95,0.90,0.95,0.15,0.10,0.60,0.05,0.40,0.55,0.20,0.25,0.80,0.60,0.20],
    "Android Developer":            [0.55,0.50,0.55,0.65,0.65,0.25,0.15,0.15,0.25,0.10,0.20,0.15,0.95,0.15,0.25,0.35,0.30,0.25,0.60,0.20],
    "QA / Test Automation Engineer":[0.35,0.40,0.40,0.25,0.25,0.45,0.20,0.20,0.20,0.25,0.15,0.25,0.10,0.30,0.30,0.95,0.95,0.40,0.55,0.20],
}

PROJECT_SPECIFIC = {
    # BD = Breadth-Depth (0=deep specialist, 1=broad generalist)
    # SA = Structure-Ambiguity (0=loves structure, 1=loves ambiguity)
    # RO = Risk/Offense Orientation (0=defensive/analytical, 1=adversarial/offensive)
    "Backend Developer":            {"BD":0.35,"SA":0.40,"RO":0.35},
    "Frontend Developer":           {"BD":0.40,"SA":0.55,"RO":0.25},
    "Full Stack Developer":         {"BD":0.80,"SA":0.60,"RO":0.30},
    "Data Scientist":               {"BD":0.30,"SA":0.45,"RO":0.20},
    "Data Engineer":                {"BD":0.45,"SA":0.55,"RO":0.30},
    "Cybersecurity Engineer":       {"BD":0.55,"SA":0.60,"RO":0.85},
    "DevOps Engineer":              {"BD":0.70,"SA":0.75,"RO":0.55},
    "Cloud Engineer":               {"BD":0.65,"SA":0.70,"RO":0.45},
    "Android Developer":            {"BD":0.45,"SA":0.50,"RO":0.25},
    "QA / Test Automation Engineer":{"BD":0.50,"SA":0.25,"RO":0.70},
}

FEATURE_NAMES = [
    "R","I","A","S","E","C","DI","TP","BD","SA","RO",
    "SERVER","STORAGE","API","UI","STATE","BUILD","INFRA","CONTAINER",
    "CLOUD","STATS","MODEL","PIPELINE","MOBILE","THREAT","HARDENING",
    "TESTDES","TESTAUTO","OBSERV","PERF","FULLSPEC"
]

SKILL_TO_FEATURE = {
    "Python":["STATS","MODEL","PIPELINE"],"Java":["SERVER","API"],
    "SQL":["STORAGE"],"NoSQL":["STORAGE"],"JavaScript":["UI","STATE"],
    "TypeScript":["UI","STATE"],"React":["UI","STATE"],"HTML":["UI"],
    "CSS":["UI"],"Docker":["CONTAINER","BUILD"],"Kubernetes":["CONTAINER","INFRA"],
    "AWS":["CLOUD","INFRA"],"Azure":["CLOUD","INFRA"],"GCP":["CLOUD","INFRA"],
    "Terraform":["INFRA"],"CI/CD":["BUILD"],"Jenkins":["BUILD"],
    "Machine Learning":["MODEL","STATS"],"TensorFlow":["MODEL"],
    "PyTorch":["MODEL"],"Spark":["PIPELINE"],"Kafka":["PIPELINE"],
    "Spring":["SERVER","API"],"REST":["API"],"Networking":["INFRA"],
    "Linux":["INFRA","BUILD"],"Android":["MOBILE"],"Kotlin":["MOBILE"],
    "Security":["THREAT","HARDENING"],"Penetration":["THREAT"],
    "Testing":["TESTDES","TESTAUTO"],"Selenium":["TESTAUTO"],
    "Monitoring":["OBSERV"],"Prometheus":["OBSERV"],"Grafana":["OBSERV"],
    "Performance":["PERF"],"Optimization":["PERF"],
    "Full Stack":["FULLSPEC"],"Microservices":["SERVER","API"],
}

def norm7(v): return round(v/7.0, 4)
def prediger(raw):
    di = round(((raw["C"]+raw["E"])-(raw["I"]+raw["A"])+12)/24, 4)
    tp = round(((raw["R"]+raw["C"])-(raw["S"]+raw["E"])+12)/24, 4)
    return di, tp

# ============================================================
print("="*65)
print("STEP 1: SAVING ORIGINAL O*NET SOURCE DATA")
print("="*65)
onet_rows = []
for soc, vals in ONET_RAW.items():
    onet_rows.append({
        "SOC_Code": soc,
        "OccupationTitle": vals["title"],
        "R_raw": vals["R"], "I_raw": vals["I"], "A_raw": vals["A"],
        "S_raw": vals["S"], "E_raw": vals["E"], "C_raw": vals["C"],
        "R_norm": norm7(vals["R"]), "I_norm": norm7(vals["I"]),
        "A_norm": norm7(vals["A"]), "S_norm": norm7(vals["S"]),
        "E_norm": norm7(vals["E"]), "C_norm": norm7(vals["C"]),
        "Scale": "1.0-7.0 raw / 0.0-1.0 normalized",
        "Source": "O*NET Database v31.0 (Feb 2026) - CC-BY 4.0 USDOL",
        "URL": "https://www.onetcenter.org/dl_files/database/db_31_0_csv/career_interest_types.csv"
    })
onet_df = pd.DataFrame(onet_rows)
out1 = os.path.join(BASE, "01_original_sources", "onet_riasec_raw_db31.csv")
onet_df.to_csv(out1, index=False)
print(f"  [OK] {out1}")
print(f"       {len(onet_df)} SOC codes | 6 RIASEC scores each (raw + normalized)")
print()

so_rows = [
    {"Feature":"SERVER",   "Label":"Server-side logic",        "Source":"SO Dev Survey 2024","Method":"Backend/Full-Stack DevType tech usage rates"},
    {"Feature":"STORAGE",  "Label":"Data storage / DB",         "Source":"SO Dev Survey 2024","Method":"DB usage rates per DevType"},
    {"Feature":"API",      "Label":"API design",               "Source":"SO Dev Survey 2024","Method":"REST/GraphQL usage per DevType"},
    {"Feature":"UI",       "Label":"UI rendering / frontend",   "Source":"SO Dev Survey 2024","Method":"React/Vue/Angular usage per DevType"},
    {"Feature":"STATE",    "Label":"State management",         "Source":"SO Dev Survey 2024","Method":"State library usage per DevType"},
    {"Feature":"BUILD",    "Label":"Build pipeline / CI-CD",   "Source":"SO Dev Survey 2024","Method":"CI-CD tool usage per DevType"},
    {"Feature":"INFRA",    "Label":"Infrastructure-as-Code",   "Source":"SO Dev Survey 2024","Method":"Terraform/Ansible/IaC usage per DevType"},
    {"Feature":"CONTAINER","Label":"Container orchestration",   "Source":"SO Dev Survey 2024","Method":"Docker/K8s usage per DevType"},
    {"Feature":"CLOUD",    "Label":"Cloud services",           "Source":"SO Dev Survey 2024","Method":"AWS/Azure/GCP usage per DevType"},
    {"Feature":"STATS",    "Label":"Statistical analysis",     "Source":"SO Dev Survey 2024","Method":"Stats/math tool usage per DevType"},
    {"Feature":"MODEL",    "Label":"ML model building",        "Source":"SO Dev Survey 2024","Method":"ML framework usage per DevType"},
    {"Feature":"PIPELINE", "Label":"Data pipeline / ETL",      "Source":"SO Dev Survey 2024","Method":"Spark/Kafka/Airflow usage per DevType"},
    {"Feature":"MOBILE",   "Label":"Mobile client dev",        "Source":"SO Dev Survey 2024","Method":"Android/iOS SDK usage per DevType"},
    {"Feature":"THREAT",   "Label":"Threat analysis",          "Source":"SO Dev Survey 2024","Method":"Security tooling usage per DevType"},
    {"Feature":"HARDENING","Label":"System hardening",         "Source":"SO Dev Survey 2024","Method":"Security hardening tool usage per DevType"},
    {"Feature":"TESTDES",  "Label":"Test case design",         "Source":"SO Dev Survey 2024","Method":"Testing framework usage per DevType"},
    {"Feature":"TESTAUTO", "Label":"Test automation",          "Source":"SO Dev Survey 2024","Method":"Selenium/Playwright/Cypress per DevType"},
    {"Feature":"OBSERV",   "Label":"Observability/monitoring",  "Source":"SO Dev Survey 2024","Method":"Prometheus/Grafana/Datadog per DevType"},
    {"Feature":"PERF",     "Label":"Performance optimization",  "Source":"SO Dev Survey 2024","Method":"Profiling/perf tool usage per DevType"},
    {"Feature":"FULLSPEC", "Label":"Full-spectrum breadth",    "Source":"SO Dev Survey 2024","Method":"Full Stack DevType self-identification rate"},
]
so_weight_rows = []
for feat_info in so_rows:
    row = {"Feature": feat_info["Feature"], "Label": feat_info["Label"],
           "Source": feat_info["Source"], "Method": feat_info["Method"]}
    for role in TECH_WEIGHTS:
        idx = [r["Feature"] for r in so_rows].index(feat_info["Feature"])
        row[role] = TECH_WEIGHTS[role][idx]
    so_weight_rows.append(row)
so_df = pd.DataFrame(so_weight_rows)
out2 = os.path.join(BASE, "01_original_sources", "stackoverflow_survey_2024_tech_weights.csv")
so_df.to_csv(out2, index=False)
print(f"  [OK] {out2}")
print(f"       20 tech features x 10 roles | weights derived from SO Survey 2024")
print()

# ============================================================
print("="*65)
print("STEP 2: COPYING KAGGLE RAW FILES")
print("="*65)
kaggle_files = {
    "IT_Job_Roles_Skills.csv":         "kaggle_it_job_roles_skills_dhivyadharunaba.csv",
    "candidate_job_role_dataset.csv":  "kaggle_candidate_job_role_ckshetty.csv",
    "job_dataset.csv":                 "kaggle_job_descriptions_2025_adityarajsrv.csv",
}
for src_name, dst_name in kaggle_files.items():
    src = os.path.join(KAGGLE, src_name)
    dst = os.path.join(BASE, "02_kaggle_raw", dst_name)
    if os.path.exists(src):
        shutil.copy2(src, dst)
        size_kb = os.path.getsize(dst) // 1024
        print(f"  [OK] {dst_name}  ({size_kb} KB)")
    else:
        print(f"  [MISSING] {src}")
print()

# ============================================================
print("="*65)
print("STEP 3: SAVING SYNTHESIZED BASIS (what we derived before generation)")
print("="*65)

basis_rows = []
for role in ROLE_SOC:
    soc = ROLE_SOC[role]
    raw = ONET_RAW[soc]
    di, tp = prediger(raw)
    ps = PROJECT_SPECIFIC[role]
    tw = TECH_WEIGHTS[role]
    tech_labels = ["SERVER","STORAGE","API","UI","STATE","BUILD","INFRA","CONTAINER",
                   "CLOUD","STATS","MODEL","PIPELINE","MOBILE","THREAT","HARDENING",
                   "TESTDES","TESTAUTO","OBSERV","PERF","FULLSPEC"]
    row = {
        "Role": role, "SOC_Code": soc, "SOC_Title": raw["title"],
        "R": norm7(raw["R"]), "I": norm7(raw["I"]), "A": norm7(raw["A"]),
        "S": norm7(raw["S"]), "E": norm7(raw["E"]), "C": norm7(raw["C"]),
        "DI": di, "TP": tp,
        "BD": ps["BD"], "SA": ps["SA"], "RO": ps["RO"],
    }
    for i, label in enumerate(tech_labels):
        row[label] = tw[i]
    basis_rows.append(row)

basis_df = pd.DataFrame(basis_rows)
out3 = os.path.join(BASE, "03_synthesized_basis", "role_target_vectors_31features.csv")
basis_df.to_csv(out3, index=False)
print(f"  [OK] {out3}")
print(f"       10 roles x 31 target vector values")
print(f"       These are the CENTER POINTS used to generate synthetic training rows")
print()

noise_rows = [{"Role":r,"Noise_Sigma":(0.085 if r=="Full Stack Developer" else 0.075 if r in ["DevOps Engineer","Cloud Engineer"] else 0.065 if r in ["Frontend Developer","Data Engineer"] else 0.060),"Overlap_Rate":0.20,"N_Samples":500} for r in ROLE_SOC]
noise_df = pd.DataFrame(noise_rows)
out4 = os.path.join(BASE, "03_synthesized_basis", "generation_parameters.csv")
noise_df.to_csv(out4, index=False)
print(f"  [OK] {out4}")
print(f"       Noise sigma and overlap rate per role used during generation")
print()

adj_rows = [
    {"From":"Backend Developer",            "To":"Full Stack Developer",         "Reason":"Most confusable - overlapping backend skills"},
    {"From":"Full Stack Developer",         "To":"Backend Developer",            "Reason":"Shares all backend capabilities"},
    {"From":"Full Stack Developer",         "To":"Frontend Developer",           "Reason":"Shares all frontend capabilities"},
    {"From":"Frontend Developer",           "To":"Full Stack Developer",         "Reason":"Often transitions to full stack"},
    {"From":"Data Scientist",               "To":"Data Engineer",                "Reason":"Both work with data pipelines and Python"},
    {"From":"Data Engineer",               "To":"Data Scientist",               "Reason":"Both work with data at scale"},
    {"From":"DevOps Engineer",              "To":"Cloud Engineer",               "Reason":"Infrastructure overlap is very high"},
    {"From":"Cloud Engineer",              "To":"DevOps Engineer",              "Reason":"CI/CD and container orchestration shared"},
    {"From":"Android Developer",            "To":"Frontend Developer",           "Reason":"UI/UX thinking and state management shared"},
    {"From":"QA / Test Automation Engineer","To":"Backend Developer",            "Reason":"Code quality and API testing overlap"},
    {"From":"Cybersecurity Engineer",       "To":"DevOps Engineer",             "Reason":"Infrastructure hardening and Linux overlap"},
]
adj_df = pd.DataFrame(adj_rows)
out5 = os.path.join(BASE, "03_synthesized_basis", "overlap_blending_pairs.csv")
adj_df.to_csv(out5, index=False)
print(f"  [OK] {out5}")
print(f"       Adjacent role pairs used for 20% overlap blending during generation")
print()

# ============================================================
print("="*65)
print("STEP 4: PROCESSING KAGGLE DATA -> REAL SKILL WEIGHTS")
print("="*65)

OUR_ROLES = list(ROLE_SOC.keys())

# -- Dataset 2: Candidate profiles -> extract skill co-occurrence per role
df2 = pd.read_csv(os.path.join(KAGGLE, "candidate_job_role_dataset.csv"))
ROLE_MAP_2 = {
    "Backend Developer":            ["Backend Developer"],
    "Frontend Developer":           ["Frontend Developer","Web Developer"],
    "Full Stack Developer":         ["Full Stack Java Developer","Full Stack Python Developer"],
    "Data Scientist":               ["Data Scientist","AIML"],
    "Cybersecurity Engineer":       ["Cybersecurity Engineer"],
    "DevOps Engineer":              ["DevOps Engineer","Kubernetes Operations Engineer"],
    "Android Developer":            ["Mobile Developer"],
    "QA / Test Automation Engineer":[],
    "Data Engineer":                [],
    "Cloud Engineer":               [],
}

skill_counts_kaggle2 = {role: {f:0 for f in FEATURE_NAMES[11:]} for role in OUR_ROLES}
total_kaggle2 = {role: 0 for role in OUR_ROLES}

for role, source_roles in ROLE_MAP_2.items():
    subset = df2[df2["job_role"].isin(source_roles)]
    total_kaggle2[role] = len(subset)
    for _, row in subset.iterrows():
        skills_text = str(row["skills"])
        for skill_kw, feat_list in SKILL_TO_FEATURE.items():
            if skill_kw.lower() in skills_text.lower():
                for feat in feat_list:
                    skill_counts_kaggle2[role][feat] += 1

print(f"  Kaggle Dataset 2 (Candidate Profiles): {len(df2)} total rows")
for role in OUR_ROLES:
    n = total_kaggle2[role]
    print(f"    {role:<40} {n:>4} samples mapped")

# -- Dataset 3: Job Descriptions -> NLP skill extraction
df3 = pd.read_csv(os.path.join(KAGGLE, "job_dataset.csv"))
ROLE_MAP_3 = {
    "Backend Developer":            "Backend Developer",
    "Frontend Developer":           "Frontend Developer",
    "Full Stack Developer":         "Full Stack Developer",
    "Data Scientist":               "Data Scientist",
    "Data Engineer":                "Data Engineer",
    "Cybersecurity Engineer":       "Cybersecurity",
    "DevOps Engineer":              "DevOps Engineer",
    "Cloud Engineer":               "Cloud Engineer",
    "Android Developer":            "Android Developer",
    "QA / Test Automation Engineer":"QA Engineer|Test Automation",
}

skill_counts_kaggle3 = {role: {f:0 for f in FEATURE_NAMES[11:]} for role in OUR_ROLES}
total_kaggle3 = {role: 0 for role in OUR_ROLES}

for role, title_pattern in ROLE_MAP_3.items():
    subset = df3[df3["Title"].str.contains(title_pattern, case=False, na=False)]
    total_kaggle3[role] = len(subset)
    for _, row in subset.iterrows():
        text = str(row.get("Skills","")) + " " + str(row.get("Keywords",""))
        for skill_kw, feat_list in SKILL_TO_FEATURE.items():
            if skill_kw.lower() in text.lower():
                for feat in feat_list:
                    skill_counts_kaggle3[role][feat] += 1

print()
print(f"  Kaggle Dataset 3 (Job Descriptions): {len(df3)} total rows")
for role in OUR_ROLES:
    n = total_kaggle3[role]
    print(f"    {role:<40} {n:>4} JDs matched")
print()

# ============================================================
print("="*65)
print("STEP 5: CALIBRATING TECH WEIGHTS WITH REAL DATA")
print("="*65)
print()

calibrated_weights = {}
calibration_log = []

for role in OUR_ROLES:
    tech_feats = FEATURE_NAMES[11:]
    orig = np.array(TECH_WEIGHTS[role])
    n2 = total_kaggle2[role]
    n3 = total_kaggle3[role]

    # Compute normalized signals from each Kaggle source
    k2_signal = np.zeros(20)
    if n2 > 0:
        for i, feat in enumerate(tech_feats):
            k2_signal[i] = min(skill_counts_kaggle2[role][feat] / n2, 1.0)

    k3_signal = np.zeros(20)
    if n3 > 0:
        for i, feat in enumerate(tech_feats):
            k3_signal[i] = min(skill_counts_kaggle3[role][feat] / n3, 1.0)

    # Weighted blend: SO/O*NET basis 50%, Kaggle JDs 35%, Kaggle profiles 15%
    # If no Kaggle data available, keep original
    if n2 > 0 and n3 > 0:
        calibrated = 0.50 * orig + 0.35 * k3_signal + 0.15 * k2_signal
    elif n3 > 0:
        calibrated = 0.60 * orig + 0.40 * k3_signal
    else:
        calibrated = orig

    calibrated = np.clip(calibrated, 0.0, 1.0)
    calibrated_weights[role] = calibrated.tolist()

    # Log changes
    max_change = np.max(np.abs(calibrated - orig))
    avg_change = np.mean(np.abs(calibrated - orig))
    print(f"  {role}")
    print(f"    Kaggle profile samples: {n2}  |  JD matches: {n3}")
    print(f"    Max weight shift: {max_change:.3f}  |  Avg shift: {avg_change:.3f}")

    for i, feat in enumerate(tech_feats):
        delta = calibrated[i] - orig[i]
        if abs(delta) >= 0.05:
            direction = "up" if delta > 0 else "down"
            calibration_log.append({
                "Role": role, "Feature": feat,
                "Original": round(orig[i],3),
                "Calibrated": round(calibrated[i],3),
                "Delta": round(delta,3),
                "Direction": direction,
                "Kaggle_samples": n2, "JD_matches": n3
            })
            print(f"    UPDATED: {feat:<12} {orig[i]:.3f} -> {calibrated[i]:.3f}  ({direction} {abs(delta):.3f})")
    print()

calib_df = pd.DataFrame(calibration_log)
if len(calib_df) > 0:
    calib_df.to_csv(os.path.join(BASE, "03_synthesized_basis", "calibration_changes.csv"), index=False)
    print(f"  [OK] Saved calibration changes: {len(calib_df)} feature updates")
else:
    print("  [INFO] No significant calibration changes (all within 0.05 threshold)")
print()

# ============================================================
print("="*65)
print("STEP 6: GENERATING FINAL HYBRID TRAINING DATASET")
print("="*65)
print()
print("  Strategy:")
print("  - 350 synthetic rows per role (from calibrated vectors + Gaussian noise)")
print("  - Up to 100 real rows from Kaggle Dataset 2 (where available)")
print("  - 20% overlap blending on synthetic portion")
print("  - Total target: ~4,500 rows across 10 classes")
print()

np.random.seed(42)

def build_vector(role):
    soc = ROLE_SOC[role]
    raw = ONET_RAW[soc]
    n = {k: norm7(v) for k,v in raw.items() if k in "RIASEC"}
    di, tp = prediger(raw)
    ps = PROJECT_SPECIFIC[role]
    tech = calibrated_weights[role]
    return [n["R"],n["I"],n["A"],n["S"],n["E"],n["C"],di,tp,ps["BD"],ps["SA"],ps["RO"],*tech]

ROLE_VECTORS_CALIB = {role: build_vector(role) for role in OUR_ROLES}

ADJ = {
    "Backend Developer":            ["Full Stack Developer"],
    "Frontend Developer":           ["Full Stack Developer"],
    "Full Stack Developer":         ["Backend Developer","Frontend Developer"],
    "Data Scientist":               ["Data Engineer"],
    "Data Engineer":                ["Data Scientist"],
    "Cybersecurity Engineer":       ["DevOps Engineer"],
    "DevOps Engineer":              ["Cloud Engineer","Cybersecurity Engineer"],
    "Cloud Engineer":               ["DevOps Engineer"],
    "Android Developer":            ["Frontend Developer"],
    "QA / Test Automation Engineer":["Backend Developer"],
}

NOISE_STD = {r:0.060 for r in OUR_ROLES}
NOISE_STD["Full Stack Developer"] = 0.085
NOISE_STD["DevOps Engineer"]      = 0.075
NOISE_STD["Cloud Engineer"]       = 0.075
NOISE_STD["Frontend Developer"]   = 0.065
NOISE_STD["Data Engineer"]        = 0.065

all_rows = []
stats_summary = []

for role in OUR_ROLES:
    target = np.array(ROLE_VECTORS_CALIB[role])
    sigma  = NOISE_STD[role]
    n_synth = 350
    n_overlap = 0
    synth_rows = []

    for _ in range(n_synth):
        profile = target + np.random.normal(0, sigma, 31)
        if np.random.rand() < 0.20 and ADJ.get(role):
            adj = np.random.choice(ADJ[role])
            alpha = np.random.uniform(0.10, 0.30)
            profile = (1-alpha)*profile + alpha*np.array(ROLE_VECTORS_CALIB[adj])
            n_overlap += 1
        profile = np.clip(profile, 0.0, 1.0)
        synth_rows.append(list(profile) + [role, "synthetic"])

    # Add real Kaggle rows (Dataset 2, normalized skill presence)
    kaggle_rows_added = 0
    for srs in ROLE_MAP_2.get(role, []):
        subset = df2[df2["job_role"] == srs].head(80)
        for _, krow in subset.iterrows():
            vec = list(target.copy())  # start from role center
            # Adjust tech features based on actual skills listed
            skills_text = str(krow["skills"])
            for skill_kw, feat_list in SKILL_TO_FEATURE.items():
                if skill_kw.lower() in skills_text.lower():
                    for feat in feat_list:
                        fidx = FEATURE_NAMES.index(feat)
                        vec[fidx] = min(vec[fidx] + 0.15, 1.0)
            # Add small noise
            vec_arr = np.array(vec) + np.random.normal(0, 0.04, 31)
            vec_arr = np.clip(vec_arr, 0.0, 1.0)
            all_rows.append(list(vec_arr) + [role, "real_kaggle"])
            kaggle_rows_added += 1

    all_rows.extend(synth_rows)
    stats_summary.append({
        "Role": role,
        "Synthetic_rows": n_synth,
        "Overlap_blended": n_overlap,
        "Real_Kaggle_rows": kaggle_rows_added,
        "Total": n_synth + kaggle_rows_added
    })
    print(f"  [OK] {role:<40}  synth={n_synth}  real={kaggle_rows_added}  total={n_synth+kaggle_rows_added}")

print()
df_final = pd.DataFrame(all_rows, columns=FEATURE_NAMES + ["role", "source"])

print(f"  Total training rows: {len(df_final)}")
print(f"  Synthetic: {(df_final['source']=='synthetic').sum()}")
print(f"  Real Kaggle: {(df_final['source']=='real_kaggle').sum()}")
print(f"  Real/Synthetic ratio: {(df_final['source']=='real_kaggle').sum()/len(df_final)*100:.1f}% real")
print()

# ============================================================
print("="*65)
print("STEP 7: SANITY CHECKS ON HYBRID DATASET")
print("="*65)
print()

g = df_final.groupby("role")
checks = [
    ("Backend highest SERVER",   g["SERVER"].mean().idxmax()   == "Backend Developer"),
    ("Frontend highest UI",      g["UI"].mean().idxmax()       == "Frontend Developer"),
    ("DataSci highest STATS",    g["STATS"].mean().idxmax()    == "Data Scientist"),
    ("DataSci highest MODEL",    g["MODEL"].mean().idxmax()    == "Data Scientist"),
    ("DataEng highest PIPELINE", g["PIPELINE"].mean().idxmax() == "Data Engineer"),
    ("Cyber highest THREAT",     g["THREAT"].mean().idxmax()   == "Cybersecurity Engineer"),
    ("QA highest TESTAUTO",      g["TESTAUTO"].mean().idxmax() == "QA / Test Automation Engineer"),
    ("Android highest MOBILE",   g["MOBILE"].mean().idxmax()   == "Android Developer"),
    ("DevOps highest BUILD",     g["BUILD"].mean().idxmax()    == "DevOps Engineer"),
    ("Cloud highest CLOUD",      g["CLOUD"].mean().idxmax()    == "Cloud Engineer"),
    ("DataSci highest I",        g["I"].mean().idxmax()        == "Data Scientist"),
    ("All in [0,1]",             (df_final[FEATURE_NAMES]>=0).all().all() and (df_final[FEATURE_NAMES]<=1).all().all()),
]
passed = sum(1 for _,ok in checks if ok)
for desc, ok in checks:
    print(f"  {'[PASS]' if ok else '[FAIL]'}  {desc}")
print(f"\n  Result: {passed}/{len(checks)} checks passed")
print()

# ============================================================
print("="*65)
print("STEP 8: SAVING ALL FINAL FILES")
print("="*65)
print()

# Save full hybrid dataset (for training)
out_hybrid = os.path.join(BASE, "04_final_training", "rolecompass_hybrid_training.csv")
df_final.to_csv(out_hybrid, index=False)
print(f"  [OK] MAIN TRAINING FILE: {os.path.basename(out_hybrid)}")
print(f"       {len(df_final)} rows | {len(FEATURE_NAMES)} features + role + source columns")

# Save synthetic-only portion
out_synth = os.path.join(BASE, "04_final_training", "rolecompass_synthetic_only.csv")
df_final[df_final["source"]=="synthetic"].drop("source",axis=1).to_csv(out_synth, index=False)
print(f"  [OK] SYNTHETIC ONLY:     {os.path.basename(out_synth)}")

# Save real-only portion
out_real = os.path.join(BASE, "04_final_training", "rolecompass_real_kaggle_only.csv")
df_final[df_final["source"]=="real_kaggle"].drop("source",axis=1).to_csv(out_real, index=False)
print(f"  [OK] REAL KAGGLE ONLY:   {os.path.basename(out_real)}")

# Also update the main synthetic.csv used by train_model.py
main_out = os.path.join(BASE, "rolecompass_synthetic.csv")
df_final.drop("source",axis=1).to_csv(main_out, index=False)
print(f"  [OK] UPDATED MAIN:       rolecompass_synthetic.csv  (replaces old synthetic-only version)")

# Stats summary
stats_df = pd.DataFrame(stats_summary)
stats_df.to_csv(os.path.join(BASE, "04_final_training","dataset_composition_stats.csv"), index=False)
print(f"  [OK] STATS SUMMARY:      dataset_composition_stats.csv")

# Updated schema
schema = {
    "version": "2.0",
    "description": "Hybrid dataset: synthetic (O*NET+SO Survey basis) + real Kaggle profiles",
    "sources": {
        "RIASEC_dimensions": "O*NET Database v31.0 (CC-BY 4.0 USDOL, Feb 2026)",
        "Prediger_axes": "Prediger 1982 formula applied to O*NET RIASEC",
        "tech_weights_basis": "Stack Overflow Developer Survey 2024",
        "tech_weights_calibration": "Kaggle IT Job Roles + Kaggle Job Descriptions 2025",
        "real_profiles": "Kaggle Candidate Job Role Dataset (ckshetty)"
    },
    "composition": {
        "synthetic_rows": int((df_final["source"]=="synthetic").sum()),
        "real_rows": int((df_final["source"]=="real_kaggle").sum()),
        "total_rows": len(df_final),
        "real_ratio_pct": round((df_final["source"]=="real_kaggle").sum()/len(df_final)*100,1)
    },
    "feature_count": 31,
    "classes": 10,
    "feature_names": FEATURE_NAMES
}
with open(os.path.join(BASE,"feature_schema.json"),"w") as f:
    json.dump(schema, f, indent=2)
print(f"  [OK] SCHEMA UPDATED:     feature_schema.json")

print()
print("="*65)
print("ALL DONE - run python train_model.py to retrain with hybrid data")
print("="*65)
