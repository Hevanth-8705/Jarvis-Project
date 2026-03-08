import sqlite3
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List, Dict
import uvicorn
import os

app = FastAPI(title="Jarvis AI Agent Core")

# Database Initialization
DB_PATH = "jarvis_memory.db"

def init_db():
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS preferences (
            key TEXT PRIMARY KEY,
            value TEXT
        )
    """)
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS history (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_query TEXT,
            jarvis_response TEXT,
            timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
        )
    """)
    conn.commit()
    conn.close()

init_db()

class ExecutionRequest(BaseModel):
    query: str
    history: List[Dict]
    system_context: Dict
    device_context: Dict

@app.post("/api/v1/agent/execute")
async def execute_agent(request: ExecutionRequest):
    query = request.query.lower()
    
    # Jarvis Reasoning Engine
    response_text = "Logic loop initiated, Sir."
    execution_plan = []
    emotion = "happy"

    # Database connection for this request
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()

    try:
        # 1. Action: Name Recognition
        if "my name is" in query:
            name = query.split("is")[-1].strip()
            cursor.execute("INSERT OR REPLACE INTO preferences (key, value) VALUES (?, ?)", ("name", name))
            conn.commit()
            response_text = f"Protocol updated. I have recorded your name as {name}."
            execution_plan.append({"type": "save_preference", "key": "name", "value": name})

        # 2. Action: App Launching
        elif "open" in query or "launch" in query:
            app_name = query.replace("open", "").replace("launch", "").strip()
            package_map = {
                "youtube": "com.google.android.youtube",
                "chrome": "com.android.chrome",
                "settings": "com.android.settings",
                "camera": "com.android.camera",
                "maps": "com.google.android.apps.maps",
                "whatsapp": "com.whatsapp"
            }
            target_pkg = package_map.get(app_name)
            if target_pkg:
                response_text = f"Launching {app_name}, Sir."
                execution_plan.append({"type": "launch_app", "package": target_pkg})
            else:
                response_text = f"I don't have the internal route for {app_name}. Searching the web instead."
                execution_plan.append({"type": "web_search", "query": app_name})

        # 3. Action: System Telemetry
        elif "battery" in query:
            level = request.device_context.get("battery", -1)
            response_text = f"Sir, the tactical display shows {level}% battery remaining."
            if level < 20: emotion = "alert"

        # 4. Action: Information Retrieval
        elif "search" in query or "find" in query:
            search_item = query.replace("search", "").replace("find", "").strip()
            response_text = f"Scanning external databases for {search_item}."
            execution_plan.append({"type": "web_search", "query": search_item})
            emotion = "thinking"

        # 5. Default Conversation
        else:
            cursor.execute("SELECT value FROM preferences WHERE key = 'name'")
            row = cursor.fetchone()
            user_name = row[0] if row else "Sir"
            response_text = f"I am online and at your service, {user_name}. How shall we proceed?"

        # Log history to database
        cursor.execute("INSERT INTO history (user_query, jarvis_response) VALUES (?, ?)", (request.query, response_text))
        conn.commit()

    finally:
        conn.close()

    return {
        "answer": response_text,
        "plan": execution_plan,
        "emotion": emotion
    }

if __name__ == "__main__":
    print(f"Jarvis Backend starting on port 8000...")
    uvicorn.run(app, host="0.0.0.0", port=8000)
