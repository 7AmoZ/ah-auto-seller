import datetime
import threading

class Logger:
    def __init__(self, log_callback):
        self.callback = log_callback
        self.lock = threading.Lock()

    def log(self, message: str):
        with self.lock:
            timestamp = datetime.datetime.now().strftime("%I:%M:%S %p")
            entry = f"{timestamp} - {message}"
            self.callback(entry)