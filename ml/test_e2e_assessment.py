import urllib.request
import json

BASE_URL = "http://localhost:8080/api"

import uuid

uid = str(uuid.uuid4())[:8]
reg_payload = json.dumps({
    "username": f"user_{uid}",
    "password": "Password123!",
    "email": f"user_{uid}@example.com",
    "fullName": "Simulated User"
}).encode()

req = urllib.request.Request(f"{BASE_URL}/auth/register", data=reg_payload, headers={"Content-Type": "application/json"})
res = urllib.request.urlopen(req)
auth_data = json.loads(res.read().decode())
token = auth_data["token"]
headers = {"Content-Type": "application/json", "Authorization": f"Bearer {token}"}

print(f"[OK] Registered user, token acquired.")

# 2. Start session
req = urllib.request.Request(f"{BASE_URL}/session/start", data=b"{}", headers=headers)
res = urllib.request.urlopen(req)
start_data = json.loads(res.read().decode())
session_id = start_data["session_id"]
current_questions = start_data["questions"]

print(f"[OK] Session started: {session_id}")
batch_num = 1

while current_questions:
    print(f"\n--- Batch {batch_num} ({len(current_questions)} questions) ---")
    answers = []
    for q in current_questions:
        q_id = q["id"]
        text = q["text"]
        section = q.get("section_id", 1)
        
        # Accurate Backend Developer profile
        text_lower = text.lower()
        if "which path excites you more" in text_lower:
            val = 1  # Option A is Full Stack (Likert 5), Option B is Backend (Likert 1)
        elif any(w in text_lower for w in ["behind the scenes", "databases", "connection layer", "slow or crashes"]):
            val = 5  # Server logic, storage, API, perf
        elif any(w in text_lower for w in ["remote cloud", "packaging an entire app", "automatically check, test, and package"]):
            val = 3  # Cloud, container, build
        elif any(w in text_lower for w in ["buttons, animations", "updates instantly", "smartphones", "break into it", "strong passwords", "deliberately trying to break", "automatically test an entire", "complete feature from scratch"]):
            val = 1  # UI, state, mobile, threat, hardening, testdes, testauto, fullspec
        elif any(w in text_lower for w in ["roll up your sleeves", "figure out how to fix", "difficult problems step by step", "genuinely curious"]):
            val = 5  # Realistic, Investigative
        elif any(w in text_lower for w in ["colors, layouts", "visually unappealing"]):
            val = 1  # Artistic
        else:
            val = 2
            
        print(f"  Q{q_id} [S{section}]: {text[:50]}... -> Likert {val}")
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
    elim = step_data.get('eliminated_roles', [])
    print(f"  Status: {status}, Surviving ({len(survivors)}): {survivors}")
    if elim:
        print(f"  Eliminated so far: {[e['role'] + ' (' + e['stage'] + ')' for e in elim]}")
    if status == "ready_to_predict" or not current_questions:
        print("\n[OK] Assessment complete! Ready to predict.")
        break

# 3. Request ML Prediction
predict_req = urllib.request.Request(f"{BASE_URL}/session/{session_id}/predict", data=b"{}", headers=headers)
predict_res = urllib.request.urlopen(predict_req)
result = json.loads(predict_res.read().decode())

print("\n" + "="*50)
print("  FINAL PREDICTION RESULT")
print("="*50)
print("Primary Role: ", result.get("predictedRole") or result.get("predicted_role"))
print("Confidence:   ", result.get("confidence"))
print("Alternates:   ", result.get("alternates"))
print("="*50)
