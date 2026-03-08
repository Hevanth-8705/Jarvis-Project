from core.intent_router import IntentRouter
from core.skill_executor import SkillExecutor
from memory.memory_store import MemoryStore


class JarvisEngine:

    def __init__(self):

        self.router = IntentRouter()
        self.executor = SkillExecutor()
        self.memory = MemoryStore()

    def handle(self, query: str):

        if not query:
            return "I didn't receive any message."

        try:

            try:
                self.memory.track_command(query)
            except:
                pass

            intent = self.router.route(query)

            response = self.executor.execute(intent, query)

            if not response:
                response = f"I heard you say: {query}, but I don't know how to handle it yet."

            return response

        except Exception as e:
            return f"Error: {str(e)}"