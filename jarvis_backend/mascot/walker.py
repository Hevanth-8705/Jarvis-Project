from PyQt6.QtCore import QTimer


class Walker:

    def __init__(self, mascot):

        self.mascot = mascot
        self.direction = 1

        self.timer = QTimer()
        self.timer.timeout.connect(self.move)

    def start(self):

        self.timer.start(30)

    def stop(self):

        self.timer.stop()

    def move(self):

        x = self.mascot.x()

        x += 2 * self.direction

        if x > 800:
            self.direction = -1

        if x < 0:
            self.direction = 1

        self.mascot.move(x, self.mascot.y())