from fastapi.testclient import TestClient
from app import app

client = TestClient(app)

def test_ml_service():
    print("Testing /health...")
    res = client.get("/health")
    assert res.status_code == 200, res.text
    data = res.json()
    print("Health response:", data)
    assert data["model_loaded"] is True
    assert data["expected_features"] == 20

    print("\nTesting /score without candidate_roles...")
    features_neutral = [0.5] * 20
    res = client.post("/score", json={"features": features_neutral})
    assert res.status_code == 200, res.text
    score_data = res.json()
    print("Score response (neutral):", score_data)
    assert "predicted_role" in score_data
    assert "confidence" in score_data
    assert len(score_data["alternates"]) == 2

    print("\nTesting /score with candidate_roles filter...")
    candidates = ["Android Developer", "Data Scientist"]
    res = client.post("/score", json={"features": features_neutral, "candidate_roles": candidates})
    assert res.status_code == 200, res.text
    filtered_score = res.json()
    print("Score response (constrained):", filtered_score)
    assert filtered_score["predicted_role"] in candidates

    print("\nTesting validation with wrong feature count (19 features)...")
    res = client.post("/score", json={"features": [0.5] * 19})
    assert res.status_code == 422, "Expected 422 validation error"
    print("Validation rejected correctly with 422.")

    print("\nAll ML FastAPI endpoint tests PASSED!")

if __name__ == "__main__":
    test_ml_service()
