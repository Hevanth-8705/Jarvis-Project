import random
from PyQt6.QtCore import QTimer


class Animator:

    def __init__(self, mascot):

        self.mascot = mascot

        # -------------------------
        # IDLE MOVEMENT TIMER
        # -------------------------

        self.idle_timer = QTimer()
        self.idle_timer.timeout.connect(self.idle_move)

        # -------------------------
        # BREATHING ANIMATION
        # -------------------------

        self.breath_timer = QTimer()
        self.breath_timer.timeout.connect(self.breath)

        # -------------------------
        # WALKING ANIMATION
        # -------------------------

        self.walk_timer = QTimer()
        self.walk_timer.timeout.connect(self.walk)

        self.walk_direction = 1

        # -------------------------
        # THINKING ANIMATION
        # -------------------------

        self.think_timer = QTimer()
        self.think_timer.timeout.connect(self.think)

    # -------------------------
    # START IDLE ANIMATION
    # -------------------------

    def start_idle(self):

        self.idle_timer.start(3000)
        self.breath_timer.start(1500)

    def stop_idle(self):

        self.idle_timer.stop()
        self.breath_timer.stop()

    # -------------------------
    # IDLE MOVE
    # -------------------------

    def idle_move(self):

        x = self.mascot.x() + random.randint(-5, 5)
        y = self.mascot.y() + random.randint(-5, 5)

        self.mascot.move(x, y)

    # -------------------------
    # BREATHING EFFECT
    # -------------------------

    def breath(self):

        w = self.mascot.width()
        h = self.mascot.height()

        change = random.randint(-2, 2)

        self.mascot.resize(w + change, h + change)

    # -------------------------
    # WALKING ACROSS SCREEN
    # -------------------------

    def start_walk(self):

        self.walk_timer.start(30)

    def stop_walk(self):

        self.walk_timer.stop()

    def walk(self):

        x = self.mascot.x()

        x += 2 * self.walk_direction

        if x > 900:
            self.walk_direction = -1

        if x < 0:
            self.walk_direction = 1

        self.mascot.move(x, self.mascot.y())

    # -------------------------
    # THINKING ANIMATION
    # -------------------------

    def start_thinking(self):

        self.think_timer.start(400)

    def stop_thinking(self):

        self.think_timer.stop()

    def think(self):

        self.mascot.thinking = not self.mascot.thinking
        self.mascot.update()