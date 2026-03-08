from PyQt6.QtWidgets import QLabel
from PyQt6.QtCore import Qt


class SpeechBubble(QLabel):

    def __init__(self, parent=None):

        super().__init__(parent)

        self.setStyleSheet("""
        background-color: white;
        border-radius: 10px;
        padding: 8px;
        font-size: 12px;
        """)

        self.setAlignment(Qt.AlignmentFlag.AlignCenter)

        self.hide()

    def show_message(self, text):

        self.setText(text)
        self.adjustSize()

        self.move(60, 0)

        self.show()