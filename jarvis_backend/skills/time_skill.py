from datetime import datetime


def get_time():

    now = datetime.now()

    return f"The current time is {now.strftime('%H:%M:%S')}"