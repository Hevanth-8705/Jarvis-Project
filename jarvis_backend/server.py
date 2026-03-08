from fastapi import FastAPI
from core.engine import JarvisEngine
import threading

# import mascot launcher
from mascot.mascot import run_mascot

app = FastAPI()

engine = JarvisEngine()


# ----------------------------
# START MASCOT IN BACKGROUND
# ----------------------------

def start_mascot():
    try:
        run_mascot()
    except Exception as e:
        print("Mascot error:", e)


# Start mascot when server starts
@app.on_event("startup")
def launch_mascot():
    thread = threading.Thread(target=start_mascot, daemon=True)
    thread.start()


# ----------------------------
# API ENDPOINT
# ----------------------------

@app.post("/ask")
def ask(data: dict):

    query = data.get("message", "")

    if not query:
        return {"response": "No message received"}

    response = engine.handle(query)

    return {"response": response}


# ----------------------------
# ROOT TEST ENDPOINT
# ----------------------------

@app.get("/")
def home():
    return {"status": "Jarvis backend running"}