from skills.weather import get_weather
from skills.time_skill import get_time


class SkillExecutor:

    def execute(self, intent, query):

        if intent == "weather":
            return get_weather()

        if intent == "time":
            return get_time()

        if intent == "greet":
            return "Hello! I am Jarvis."

        return None