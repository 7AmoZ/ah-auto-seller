import socket
import threading
import json

class SocketServer:
    def __init__(self, host="127.0.0.1", port=25566, event_handler=None):
        self.host = host
        self.port = port
        self.event_handler = event_handler
        self.server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.client_conn = None
        self.running = False

    def start(self):
        self.running = True
        self.server.bind((self.host, self.port))
        self.server.listen(1)
        threading.Thread(target=self._accept_loop, daemon=True).start()

    def _accept_loop(self):
        while self.running:
            try:
                self.server.settimeout(1.0)
                conn, addr = self.server.accept()
                self.client_conn = conn
                if self.event_handler: self.event_handler("CONNECTED")
                threading.Thread(target=self._read_loop, args=(conn,), daemon=True).start()
            except socket.timeout:
                continue
            except Exception as e:
                if self.running and self.event_handler:
                    self.event_handler(f"SOCKET_ERROR: {e}")

    def _read_loop(self, conn: socket.socket):
        buffer = ""
        try:
            while self.running:
                data = conn.recv(4096).decode("utf-8")
                if not data: break
                buffer += data
                while "\n" in buffer:
                    line, buffer = buffer.split("\n", 1)
                    if line.strip() and self.event_handler:
                        self.event_handler(line.strip())
        except Exception:
            pass
        finally:
            self.client_conn = None
            if self.event_handler: self.event_handler("DISCONNECTED")

    def send(self, data: dict):
        if self.client_conn:
            try:
                self.client_conn.sendall((json.dumps(data) + "\n").encode("utf-8"))
            except Exception:
                pass

    def stop(self):
        self.running = False
        if self.client_conn: self.client_conn.close()
        self.server.close()