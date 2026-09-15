# ==============================================================================
# ML Service Dockerfile — Python 3.12 / FastAPI / Uvicorn
# ==============================================================================
FROM python:3.12-slim

WORKDIR /app

# Install curl for the health check
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Install dependencies first (layer-cached)
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy source + pre-trained model artefacts
COPY app.py .
COPY role_predictor.pkl .
COPY label_encoder.pkl .
COPY baseline_tree.pkl .

EXPOSE 8000

# Use 1 worker for the demo; scale with --workers N in production
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "1"]
