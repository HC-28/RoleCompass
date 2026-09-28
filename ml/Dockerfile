# ==============================================================================
# ML Service Dockerfile — Python 3.12 / FastAPI / Uvicorn
# ==============================================================================
FROM python:3.12-slim

WORKDIR /app

# Install curl for the health check, clean up in same layer
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Run as a non-root user for security
RUN groupadd --system mlgroup && useradd --system --gid mlgroup mluser

# Install Python dependencies first (layer-cached separately from source)
COPY requirements.txt .
# --prefer-binary: always pick pre-built wheels over source distributions.
# Combined with exact version pinning in requirements.txt, this avoids pip
# downloading multi-MB PyPI JSON index files that were causing JSONDecodeError.
RUN pip install --no-cache-dir --prefer-binary -r requirements.txt

# Copy only the runtime artifacts needed by the container:
# - app.py        : FastAPI application
# - *.pkl         : pre-trained model, label encoder, baseline tree
COPY --chown=mluser:mlgroup app.py .
COPY --chown=mluser:mlgroup role_predictor.pkl .
COPY --chown=mluser:mlgroup label_encoder.pkl .
COPY --chown=mluser:mlgroup baseline_tree.pkl .

USER mluser

EXPOSE 8000

# 1 worker is correct for a demo; scale with --workers N in production
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "1"]
