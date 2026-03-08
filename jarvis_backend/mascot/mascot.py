import sys
import random
import pyttsx3

from PyQt6.QtWidgets import QApplication, QWidget, QLabel
from PyQt6.QtCore import Qt, QTimer, QPoint
from PyQt6.QtGui import QPainter, QColor


engine = pyttsx3.init()


class Emotion:
    IDLE = "idle"
    HAPPY = "happy"
    THINKING = "thinking"
    CONFUSED = "confused"
    ANGRY = "angry"
    SLEEPY = "sleepy"


class SpeechBubble(QLabel):

    def __init__(self, parent=None):

        super().__init__(parent)

        self.setStyleSheet("""
        background-color:white;
        border-radius:10px;
        padding:6px;
        font-size:12px;
        """)

        self.hide()

    def show_message(self, text):

        self.setText(text)
        self.adjustSize()

        self.move(60, 0)

        self.show()


class JarvisMascot(QWidget):

    def __init__(self):

        super().__init__()

        self.setWindowFlags(
            Qt.WindowType.FramelessWindowHint |
            Qt.WindowType.WindowStaysOnTopHint
        )

        self.setAttribute(Qt.WidgetAttribute.WA_TranslucentBackground)

        self.resize(220, 260)

        self.drag_pos = None

        self.eye_state = "open"
        self.mouth_state = "closed"
        self.emotion = Emotion.IDLE

        self.mouse_pos = QPoint(0, 0)

        self.bubble = SpeechBubble(self)

        # blink timer
        self.blink_timer = QTimer()
        self.blink_timer.timeout.connect(self.blink)
        self.blink_timer.start(4000)

        # mouth animation
        self.talk_timer = QTimer()
        self.talk_timer.timeout.connect(self.animate_mouth)

        # idle movement
        self.idle_timer = QTimer()
        self.idle_timer.timeout.connect(self.idle_move)
        self.idle_timer.start(3000)

        # breathing animation
        self.breath_timer = QTimer()
        self.breath_timer.timeout.connect(self.breath)
        self.breath_timer.start(1500)

        # walking animation
        self.walk_timer = QTimer()
        self.walk_timer.timeout.connect(self.walk)

        self.walk_direction = 1

        self.setMouseTracking(True)

    # ------------------------
    # DRAW CHARACTER
    # ------------------------

    def paintEvent(self, event):

        painter = QPainter(self)

        painter.setRenderHint(QPainter.RenderHint.Antialiasing)

        if self.emotion == Emotion.HAPPY:
            color = QColor(120,255,120)

        elif self.emotion == Emotion.THINKING:
            color = QColor(255,220,120)

        elif self.emotion == Emotion.CONFUSED:
            color = QColor(255,180,180)

        elif self.emotion == Emotion.ANGRY:
            color = QColor(255,120,120)

        elif self.emotion == Emotion.SLEEPY:
            color = QColor(180,180,255)

        else:
            color = QColor(120,170,255)

        painter.setBrush(color)

        painter.drawEllipse(40,40,140,160)

        painter.setBrush(QColor(0,0,0))

        # eyes follow mouse
        eye_offset_x = int((self.mouse_pos.x()-110)/50)
        eye_offset_y = int((self.mouse_pos.y()-120)/50)

        if self.eye_state == "open":

            painter.drawEllipse(80+eye_offset_x,90+eye_offset_y,15,15)
            painter.drawEllipse(125+eye_offset_x,90+eye_offset_y,15,15)

        else:

            painter.drawRect(80,95,15,2)
            painter.drawRect(125,95,15,2)

        painter.setBrush(QColor(255,100,100))

        if self.mouth_state == "open":

            painter.drawEllipse(100,130,20,15)

        else:

            painter.drawRect(100,135,20,3)

    # ------------------------
    # BLINK
    # ------------------------

    def blink(self):

        self.eye_state = "closed"
        self.update()

        QTimer.singleShot(200,self.open_eyes)

    def open_eyes(self):

        self.eye_state = "open"
        self.update()

    # ------------------------
    # TALK ANIMATION
    # ------------------------

    def animate_mouth(self):

        self.mouth_state = random.choice(["open","closed"])
        self.update()

    # ------------------------
    # SPEAK
    # ------------------------

    def speak(self,text):

        self.set_emotion_from_text(text)

        self.bubble.show_message(text)

        self.talk_timer.start(120)

        engine.say(text)
        engine.runAndWait()

        self.talk_timer.stop()

        self.mouth_state="closed"

        self.update()

    # ------------------------
    # EMOTION DETECTION
    # ------------------------

    def set_emotion_from_text(self,text):

        text=text.lower()

        if "thank" in text:
            self.emotion=Emotion.HAPPY

        elif "error" in text or "cannot" in text:
            self.emotion=Emotion.CONFUSED

        elif "thinking" in text:
            self.emotion=Emotion.THINKING

        elif "angry" in text:
            self.emotion=Emotion.ANGRY

        else:
            self.emotion=Emotion.IDLE

    # ------------------------
    # IDLE MOVEMENT
    # ------------------------

    def idle_move(self):

        x=self.x()+random.randint(-5,5)
        y=self.y()+random.randint(-5,5)

        self.move(x,y)

    # ------------------------
    # BREATHING
    # ------------------------

    def breath(self):

        w=self.width()
        h=self.height()

        change=random.randint(-2,2)

        self.resize(w+change,h+change)

    # ------------------------
    # WALKING
    # ------------------------

    def start_walk(self):

        self.walk_timer.start(30)

    def walk(self):

        x=self.x()

        x+=2*self.walk_direction

        if x>900:
            self.walk_direction=-1

        if x<0:
            self.walk_direction=1

        self.move(x,self.y())

    # ------------------------
    # DRAG WINDOW
    # ------------------------

    def mousePressEvent(self,event):

        self.drag_pos=event.globalPosition().toPoint()

    def mouseMoveEvent(self,event):

        self.mouse_pos=event.position().toPoint()

        if self.drag_pos:

            delta=event.globalPosition().toPoint()-self.drag_pos
            self.move(self.pos()+delta)

            self.drag_pos=event.globalPosition().toPoint()

        self.update()

    def mouseReleaseEvent(self,event):

        self.drag_pos=None


def run_mascot():

    app=QApplication(sys.argv)

    mascot=JarvisMascot()

    mascot.show()

    mascot.speak("Hello, I am Jarvis.")

    sys.exit(app.exec())