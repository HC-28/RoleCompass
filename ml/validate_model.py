"""
End-to-end validation: tests the new model against 5 archetypal developer profiles.
Each profile represents a clear signal for one role — verify model predicts correctly.
"""
import joblib
import numpy as np
import os

BASE = os.path.dirname(os.path.abspath(__file__))
model = joblib.load(os.path.join(BASE, 'role_predictor.pkl'))
le = joblib.load(os.path.join(BASE, 'label_encoder.pkl'))

# Feature order (frozen):
# SERVER, STORAGE, API, UI, STATE, BUILD, INFRA, CONTAINER, CLOUD,
# STATS, MODEL, PIPELINE, MOBILE, THREAT, HARDENING,
# TESTDES, TESTAUTO, OBSERV, PERF, FULLSPEC

PROFILES = {
    "Pure Backend Dev": [
        0.90,  # SERVER - very high
        0.80,  # STORAGE
        0.75,  # API
        0.10,  # UI - very low
        0.08,  # STATE - very low
        0.35,  # BUILD
        0.30,  # INFRA
        0.25,  # CONTAINER
        0.40,  # CLOUD
        0.15,  # STATS
        0.05,  # MODEL
        0.20,  # PIPELINE
        0.05,  # MOBILE - very low
        0.05,  # THREAT - very low
        0.05,  # HARDENING - very low
        0.25,  # TESTDES
        0.20,  # TESTAUTO
        0.40,  # OBSERV
        0.55,  # PERF
        0.15,  # FULLSPEC - low
    ],
    "Pure Frontend Dev": [
        0.35,  # SERVER
        0.55,  # STORAGE
        0.35,  # API
        0.85,  # UI - high
        0.70,  # STATE - high
        0.20,  # BUILD
        0.12,  # INFRA
        0.25,  # CONTAINER
        0.45,  # CLOUD
        0.10,  # STATS - low
        0.04,  # MODEL - low
        0.06,  # PIPELINE
        0.10,  # MOBILE
        0.02,  # THREAT
        0.02,  # HARDENING
        0.02,  # TESTDES - low (matches real Frontend survey distribution)
        0.02,  # TESTAUTO - low (matches real Frontend survey distribution)
        0.25,  # OBSERV
        0.18,  # PERF
        0.42,  # FULLSPEC - matching real Frontend mean (0.437)
    ],
    "Pure Data Scientist": [
        0.10,  # SERVER
        0.35,  # STORAGE
        0.15,  # API
        0.10,  # UI
        0.08,  # STATE
        0.15,  # BUILD
        0.10,  # INFRA
        0.10,  # CONTAINER
        0.40,  # CLOUD
        0.95,  # STATS - very high
        0.90,  # MODEL - very high
        0.35,  # PIPELINE
        0.05,  # MOBILE
        0.05,  # THREAT
        0.05,  # HARDENING
        0.10,  # TESTDES
        0.10,  # TESTAUTO
        0.15,  # OBSERV
        0.15,  # PERF
        0.10,  # FULLSPEC
    ],
    "Cybersecurity Engineer": [
        0.35,  # SERVER
        0.30,  # STORAGE
        0.25,  # API
        0.05,  # UI
        0.05,  # STATE
        0.30,  # BUILD
        0.60,  # INFRA
        0.40,  # CONTAINER
        0.45,  # CLOUD
        0.15,  # STATS
        0.08,  # MODEL
        0.10,  # PIPELINE
        0.05,  # MOBILE
        0.92,  # THREAT - very high
        0.88,  # HARDENING - very high
        0.25,  # TESTDES
        0.20,  # TESTAUTO
        0.60,  # OBSERV
        0.30,  # PERF
        0.08,  # FULLSPEC
    ],
    "Android Developer": [
        0.15,  # SERVER
        0.35,  # STORAGE
        0.30,  # API
        0.50,  # UI
        0.45,  # STATE
        0.20,  # BUILD
        0.10,  # INFRA
        0.10,  # CONTAINER
        0.40,  # CLOUD
        0.10,  # STATS
        0.12,  # MODEL
        0.08,  # PIPELINE
        0.92,  # MOBILE - very high
        0.05,  # THREAT
        0.05,  # HARDENING
        0.30,  # TESTDES
        0.28,  # TESTAUTO
        0.18,  # OBSERV
        0.45,  # PERF
        0.10,  # FULLSPEC
    ],
    "QA Engineer": [
        0.30,  # SERVER
        0.28,  # STORAGE
        0.25,  # API
        0.35,  # UI
        0.25,  # STATE
        0.40,  # BUILD
        0.20,  # INFRA
        0.20,  # CONTAINER
        0.28,  # CLOUD
        0.15,  # STATS
        0.08,  # MODEL
        0.10,  # PIPELINE
        0.15,  # MOBILE
        0.20,  # THREAT
        0.18,  # HARDENING
        0.92,  # TESTDES - very high
        0.95,  # TESTAUTO - very high
        0.40,  # OBSERV
        0.25,  # PERF
        0.25,  # FULLSPEC
    ],
    "Full Stack Developer": [
        0.70,  # SERVER
        0.65,  # STORAGE
        0.70,  # API
        0.75,  # UI
        0.70,  # STATE
        0.45,  # BUILD
        0.30,  # INFRA
        0.30,  # CONTAINER
        0.50,  # CLOUD
        0.20,  # STATS
        0.12,  # MODEL
        0.25,  # PIPELINE
        0.12,  # MOBILE
        0.10,  # THREAT
        0.10,  # HARDENING
        0.35,  # TESTDES
        0.30,  # TESTAUTO
        0.40,  # OBSERV
        0.45,  # PERF
        0.92,  # FULLSPEC - very high
    ],
}

print(f"\nModel expects {model.n_features_in_} features. Classes: {list(le.classes_)}")
print(f"\n{'='*65}")
print(f"  END-TO-END ROLE PREDICTION VALIDATION")
print(f"{'='*65}")
print(f"  {'Profile':<30}  {'Predicted':<30}  {'Conf':>6}  {'Match?'}")
print(f"  {'-'*62}")

EXPECTED = {
    "Pure Backend Dev":       "Backend Developer",
    "Pure Frontend Dev":      "Frontend Developer",
    "Pure Data Scientist":    "Data Scientist",
    "Cybersecurity Engineer": "Cybersecurity Engineer",
    "Android Developer":      "Android Developer",
    "QA Engineer":            "QA / Test Automation Engineer",
    "Full Stack Developer":   "Full Stack Developer",
}

all_pass = True
for profile_name, features in PROFILES.items():
    x = np.array(features).reshape(1, -1)
    proba = model.predict_proba(x)[0]
    idx = np.argmax(proba)
    predicted = le.inverse_transform([idx])[0]
    confidence = proba[idx]
    expected = EXPECTED[profile_name]
    match = "[OK]" if predicted == expected else f"[FAIL] (expected: {expected})"
    if predicted != expected:
        all_pass = False
    print(f"  {profile_name:<30}  {predicted:<30}  {confidence:>5.1%}  {match}")
    if predicted != expected:
        # Show top-3 probabilities for debugging
        top3 = np.argsort(proba)[::-1][:3]
        for rank_idx in top3:
            r_name = le.inverse_transform([rank_idx])[0]
            print(f"    {'':30}  #{rank_idx+1}: {r_name:<28}  {proba[rank_idx]:>5.1%}")

print(f"\n  {'All tests PASSED [OK]' if all_pass else 'Some tests FAILED [FAIL] -- review above'}")
print(f"{'='*65}")

