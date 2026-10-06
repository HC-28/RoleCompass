"""
RoleCompass — ML Training Script
==================================
Trains on ONLY the 20 technical features.

Data source: ml/dataset/rolecompass_training.csv
  - 110,000+ real developer profiles from Stack Overflow surveys 2021 + 2022
  - +12,000 targeted synthetic rows to balance underrepresented roles
    (Cybersecurity: +3500, AI/ML Eng: +2000, Mobile: +1500, Data Scientist: +1500,
     Cloud: +1500, QA: +1000, DE: +500)
  - Total: ~122,000 rows across 11 roles

This separation is architecturally non-negotiable: the routing engine uses
psychometric dimensions; the ML model uses only technical skill dimensions.

Technical feature order (frozen — must match FastAPI /score endpoint):
  SERVER, STORAGE, API, UI, STATE, BUILD, INFRA, CONTAINER, CLOUD,
  STATS, MODEL, PIPELINE, MOBILE, THREAT, HARDENING,
  TESTDES, TESTAUTO, OBSERV, PERF, FULLSPEC
"""

import pandas as pd
import numpy as np
from sklearn.ensemble import RandomForestClassifier
from sklearn.tree import DecisionTreeClassifier
from sklearn.model_selection import train_test_split, cross_val_score, StratifiedKFold
from sklearn.preprocessing import LabelEncoder
from sklearn.metrics import classification_report, confusion_matrix
import joblib
import os

# Frozen feature order — must match FeatureIndex.java TECH_* constants (indices 11–30)
TECH_FEATURES = [
    'SERVER', 'STORAGE', 'API', 'UI', 'STATE', 'BUILD', 'INFRA',
    'CONTAINER', 'CLOUD', 'STATS', 'MODEL', 'PIPELINE', 'MOBILE',
    'THREAT', 'HARDENING', 'TESTDES', 'TESTAUTO', 'OBSERV', 'PERF', 'FULLSPEC'
]

# Psychometric features — used by routing engine only, never by ML model
PSYCH_FEATURES = ['R', 'I', 'A', 'S', 'E', 'C', 'DI', 'TP', 'BD', 'SA', 'RO']


def train():
    BASE_DIR = os.path.dirname(os.path.abspath(__file__))
    for rel_path in ['dataset/rolecompass_training.csv', 'dataset/rolecompass_synthetic.csv']:
        data_path = os.path.join(BASE_DIR, rel_path)
        if os.path.exists(data_path):
            break
    else:
        print("[ERROR] No training dataset found. Run build_training_dataset.py first.")
        return

    df = pd.read_csv(data_path)
    print(f"Dataset: {data_path}")
    print(f"Loaded: {df.shape[0]:,} rows, {df.shape[1]} columns")
    print(f"Roles: {sorted(df['role'].unique())}")
    print(f"\nSamples per role:")
    for role, cnt in df['role'].value_counts().items():
        print(f"  {role:<42} {cnt:>7,}")

    # Validate all required columns exist
    missing = [f for f in TECH_FEATURES if f not in df.columns]
    if missing:
        print(f"[ERROR] Missing tech feature columns in dataset: {missing}")
        return

    # === CRITICAL: Use ONLY the 20 technical features ===
    X = df[TECH_FEATURES].copy()
    y = df['role']

    print(f"\nTraining on {len(TECH_FEATURES)} tech features only.")
    print(f"Dropped psychometric features: {PSYCH_FEATURES}")
    print(f"X shape: {X.shape}, y classes: {y.nunique()}")

    le = LabelEncoder()
    y_encoded = le.fit_transform(y)
    print(f"\nClass order (label encoder): {list(le.classes_)}")

    # Train/test split — stratified to preserve class proportions
    X_train, X_test, y_train, y_test = train_test_split(
        X, y_encoded, test_size=0.2, stratify=y_encoded, random_state=42
    )
    print(f"\nTrain size: {len(X_train)}, Test size: {len(X_test)}")

    # --- Random Forest ---
    # n_estimators=400: more trees = more stable predictions on 120k rows
    # max_depth=15: deeper trees capture nuanced tech signal combinations
    # min_samples_leaf=4: prevents overfitting individual SO survey quirks
    rf = RandomForestClassifier(
        n_estimators=400,
        max_depth=15,
        min_samples_leaf=4,
        class_weight='balanced',
        random_state=42,
        n_jobs=-1
    )

    print("\nRunning 5-fold cross-validation on train set...")
    cv = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)
    cv_scores = cross_val_score(rf, X_train, y_train, cv=cv, scoring='f1_macro', n_jobs=-1)
    print(f"Random Forest 5-fold CV Macro F1: {cv_scores.mean():.4f} ± {cv_scores.std():.4f}")

    rf.fit(X_train, y_train)
    y_pred = rf.predict(X_test)

    print("\n=== Random Forest Test Classification Report ===")
    print(classification_report(y_test, y_pred, target_names=le.classes_))

    print("=== Random Forest Confusion Matrix ===")
    print(confusion_matrix(y_test, y_pred))

    # Feature importances — useful for debugging if prediction quality is low
    print("\n=== Tech Feature Importances (Random Forest) ===")
    importances = sorted(
        zip(TECH_FEATURES, rf.feature_importances_),
        key=lambda x: -x[1]
    )
    for feat, imp in importances:
        bar = '#' * int(imp * 200)
        print(f"  {feat:<12} {imp:.4f}  {bar}")

    # --- Decision Tree Baseline ---
    dt = DecisionTreeClassifier(max_depth=6, class_weight='balanced', random_state=42)
    dt.fit(X_train, y_train)
    dt_pred = dt.predict(X_test)
    from sklearn.metrics import f1_score
    dt_f1 = f1_score(y_test, dt_pred, average='macro')
    rf_f1 = f1_score(y_test, y_pred, average='macro')
    print(f"\nBaseline Decision Tree Test Macro F1: {dt_f1:.4f}")
    print(f"Random Forest Test Macro F1:          {rf_f1:.4f}")

    # Save models (with compression to stay well under GitHub 100MB file limit)
    joblib.dump(rf, os.path.join(BASE_DIR, 'role_predictor.pkl'), compress=3)
    joblib.dump(dt, os.path.join(BASE_DIR, 'baseline_tree.pkl'))
    joblib.dump(le, os.path.join(BASE_DIR, 'label_encoder.pkl'))
    print("\n[OK] Models saved to ml/: role_predictor.pkl, baseline_tree.pkl, label_encoder.pkl")
    print(f"[OK] Model input dimensionality: {len(TECH_FEATURES)} features")
    print(f"[OK] Model output classes: {list(le.classes_)}")


if __name__ == '__main__':
    train()
