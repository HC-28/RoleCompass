import urllib.request
import json
import uuid

BASE_URL = "http://localhost:8080/api"

uid = str(uuid.uuid4())[:8]
reg_payload = json.dumps({
    "username": f"fe_user_{uid}",
    "password": "Password123!",
    "email": f"fe_{uid}@example.com",
    "fullName": "Simulated Frontend Dev"
}).encode()

req = urllib.request.Request(f"{BASE_URL}/auth/register", data=reg_payload, headers={"Content-Type": "application/json"})
res = urllib.request.urlopen(req)
token = json.loads(res.read().decode())["token"]
headers = {"Content-Type": "application/json", "Authorization": f"Bearer {token}"}

# Start session
req = urllib.request.Request(f"{BASE_URL}/session/start", data=b"{}", headers=headers)
res = urllib.request.urlopen(req)
start_data = json.loads(res.read().decode())
session_id = start_data["session_id"]
current_questions = start_data["questions"]

print(f"[OK] Session started for Frontend Dev Persona: {session_id}")
batch_num = 1

while current_questions:
    answers = []
    for q in current_questions:
        q_id = q["id"]
        text = q["text"]
        text_lower = text.lower()
        
        # Section 1 & Section 3
        if "which platform do you prefer" in text_lower:
            val = 5  # Option A: websites and web apps (Frontend)
        elif "where would you rather spend an extra day" in text_lower:
            val = 5  # Option A: visual details, smooth screen animations (Frontend)
        elif any(w in text_lower for w in ["colors, layouts", "visually unappealing", "buttons, animations", "updates instantly"]):
            val = 5  # High Artistic, UI rendering, State management
        elif any(w in text_lower for w in ["behind the scenes", "databases", "connection layer", "complete feature from scratch"]):
            val = 2  # Low-moderate backend/storage/api
        elif any(w in text_lower for w in ["smartphones", "deliberately trying to break", "automatically test", "break into it", "strong passwords"]):
            val = 1  # Low mobile, testing, security
        elif any(w in text_lower for w in ["bounce ideas off teammates", "helped someone"]):
            val = 4  # Social
        elif any(w in text_lower for w in ["roll up your sleeves", "figure out how to fix"]):
            val = 3
        else:
            val = 2
            
        answers.append({"question_id": q_id, "likert_value": val})
    
    # Submit answers
    submit_payload = json.dumps({"answers": answers}).encode()
    req = urllib.request.Request(f"{BASE_URL}/session/{session_id}/answers", data=submit_payload, headers=headers)
    res = urllib.request.urlopen(req)
    step_data = json.loads(res.read().decode())
    
    status = step_data.get("status")
    current_questions = step_data.get("questions", [])
    batch_num += 1
    
    survivors = step_data.get('candidate_roles', [])
    print(f"  Batch {batch_num-1} | Status: {status} | Survivors ({len(survivors)}): {survivors}")
    if status == "ready_to_predict" or not current_questions:
        print("\n[OK] Assessment complete! Ready to predict.")
        break

# Request ML Prediction
predict_req = urllib.request.Request(f"{BASE_URL}/session/{session_id}/predict", data=b"{}", headers=headers)
predict_res = urllib.request.urlopen(predict_req)
result = json.loads(predict_res.read().decode())

print("\n" + "="*50)
print("  FINAL PREDICTION RESULT (FRONTEND DEV SIMULATION)")
print("="*50)
print("Primary Role: ", result.get("predictedRole") or result.get("predicted_role"))
print("Confidence:   ", result.get("confidence"))
print("Alternates:   ", result.get("alternates"))
print("="*50)
