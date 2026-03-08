from fastapi import FastAPI
from datetime import datetime

app = FastAPI()


@app.get("/")
def root():
    return {"message": "Jarvis backend is running"}


@app.get("/chat")
def chat(msg: str):

    msg = msg.lower()

    if "time" in msg:
        return {"response": f"The time is {datetime.now().strftime('%H:%M')}"}

    if "hello" in msg:
        return {"response": "Hello. How can I assist you?"}

    if "open youtube" in msg:
        return {"response": "Opening YouTube"}

    return {"response": f"You said: {msg}"}