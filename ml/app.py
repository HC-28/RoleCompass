"""
RoleCompass ML Service — FastAPI
==================================
Endpoint: POST /score
  - Accepts exactly 20 technical feature values
  - Accepts optional candidate_roles list (role names to constrain prediction within)
  - Returns predicted_role, confidence, alternates (top 2)

Endpoint: GET /health
  - Liveness check

Feature input order (frozen — must match train_model.py and FeatureIndex.java TECH_*):
  SERVER, STORAGE, API, UI, STATE, BUILD, INFRA, CONTAINER, CLOUD,
  STATS, MODEL, PIPELINE, MOBILE, THREAT, HARDENING,
  TESTDES, TESTAUTO, OBSERV, PERF, FULLSPEC
"""

from fastapi import FastAPI, HTTPException
import joblib
import numpy as np
import os
from pydantic import BaseModel, field_validator
from typing import List, Optional

app = FastAPI(title='RoleCompass ML Service', version='2.0.0')

# ── Model loading ─────────────────────────────────────────────────────────────
model = None
le = None

if os.path.exists('role_predictor.pkl') and os.path.exists('label_encoder.pkl'):
    model = joblib.load('role_predictor.pkl')
    le = joblib.load('label_encoder.pkl')
    print(f"[OK] Model loaded. Classes: {list(le.classes_)}")
    print(f"[OK] Expected input features: {model.n_features_in_}")
else:
    print("[WARN] Models not found. Train first with train_model.py")

EXPECTED_FEATURES = 20

# ── Request / Response schemas ────────────────────────────────────────────────

class AlternateRole(BaseModel):
    role: str
    confidence: float

class ScoreRequest(BaseModel):
    """
    features: exactly 20 normalized [0.0, 1.0] technical skill values.
    Order: SERVER STORAGE API UI STATE BUILD INFRA CONTAINER CLOUD
           STATS MODEL PIPELINE MOBILE THREAT HARDENING
           TESTDES TESTAUTO OBSERV PERF FULLSPEC

    candidate_roles: optional list of role names. When provided, the predicted
    role MUST be from this list. If the model's top prediction is not in the
    list, the highest-confidence role that IS in the list is returned.
    """
    features: List[float]
    candidate_roles: Optional[List[str]] = None

    @field_validator('features')
    @classmethod
    def validate_features(cls, v):
        if len(v) != EXPECTED_FEATURES:
            raise ValueError(
                f"Expected exactly {EXPECTED_FEATURES} features, got {len(v)}. "
                f"Order: SERVER STORAGE API UI STATE BUILD INFRA CONTAINER CLOUD "
                f"STATS MODEL PIPELINE MOBILE THREAT HARDENING "
                f"TESTDES TESTAUTO OBSERV PERF FULLSPEC"
            )
        for i, val in enumerate(v):
            if not (0.0 <= val <= 1.0):
                raise ValueError(f"Feature at index {i} is out of [0.0, 1.0] range: {val}")
        return v

class ScoreResponse(BaseModel):
    predicted_role: str
    confidence: float
    alternates: List[AlternateRole]

# ── Endpoints ─────────────────────────────────────────────────────────────────

@app.post('/score', response_model=ScoreResponse)
def score(req: ScoreRequest) -> ScoreResponse:
    if model is None or le is None:
        raise HTTPException(
            status_code=503,
            detail="Model not loaded. Run train_model.py first."
        )

    X = np.array(req.features, dtype=np.float64).reshape(1, -1)
    proba = model.predict_proba(X)[0]                       # shape: (n_classes,)
    class_names: List[str] = list(le.inverse_transform(model.classes_))

    # Build sorted (role, confidence) list — descending
    ranked = sorted(
        zip(class_names, proba),
        key=lambda x: -x[1]
    )

    # Apply candidate_roles filter if provided
    candidate_set = None
    if req.candidate_roles:
        candidate_set = set(req.candidate_roles)
        # Validate that all provided candidate names are known roles
        unknown = candidate_set - set(class_names)
        if unknown:
            raise HTTPException(
                status_code=422,
                detail=f"Unknown role names in candidate_roles: {sorted(unknown)}. "
                       f"Valid roles: {sorted(class_names)}"
            )

    # Determine predicted role
    if candidate_set:
        # Must pick from candidate set — find highest-confidence role in it
        filtered = [(role, conf) for role, conf in ranked if role in candidate_set]
        if not filtered:
            # Fallback: return the overall top prediction (shouldn't happen in normal flow)
            predicted_role, predicted_conf = ranked[0]
        else:
            predicted_role, predicted_conf = filtered[0]
    else:
        predicted_role, predicted_conf = ranked[0]

    # Build alternates: top 2 from ranked that are NOT the predicted role
    alternates = [
        AlternateRole(role=role, confidence=float(conf))
        for role, conf in ranked
        if role != predicted_role
    ][:2]

    return ScoreResponse(
        predicted_role=predicted_role,
        confidence=float(predicted_conf),
        alternates=alternates
    )


@app.get('/health')
def health():
    return {
        'status': 'ok',
        'model_loaded': model is not None,
        'expected_features': EXPECTED_FEATURES,
        'classes': list(le.classes_) if le is not None else []
    }
