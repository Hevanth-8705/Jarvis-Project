class MemoryStore:

    def __init__(self):
        self.history = []

    def track_command(self, command):

        self.history.append(command)

        if len(self.history) > 50:
            self.history.pop(0)