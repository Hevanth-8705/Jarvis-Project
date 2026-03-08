class IntentRouter:

    def route(self, text: str):

        text = text.lower()

        if "weather" in text:
            return "weather"

        if "time" in text:
            return "time"

        if "hello" in text or "hi" in text:
            return "greet"

        return "unknown"