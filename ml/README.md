# RoleCompass ML Service

## Setup
```bash
pip install -r requirements.txt
```

## Generate Dataset
```bash
python generate_dataset.py
```
Outputs `dataset/rolecompass_synthetic.csv` (3000 rows, 31 features + role label).

## Train Model
```bash
python train_model.py
```
Outputs `role_predictor.pkl`, `baseline_tree.pkl`, `label_encoder.pkl`. Reports 5-fold macro F1.

## Run FastAPI Service
```bash
uvicorn app:app --host 0.0.0.0 --port 8000
```

## Score Endpoint
`POST /score` with body `{"features": [31 floats in [0,1]]}` returns `{"predicted_role": ..., "confidence": ..., "probabilities": {...}}`

## Vector Calibration Sources
- RIASEC dims 0-5: O*NET Interest data (onetcenter.org), SOC codes mapped per role
- Prediger dims 6-7: Derived mathematically from RIASEC axes
- Project dims 8-10: Logically derived from role definitions in AGENTS.md
- Technical dims 11-30: Role-skill matrix from project specification + Stack Overflow Developer Survey 2024 role-technology clustering
