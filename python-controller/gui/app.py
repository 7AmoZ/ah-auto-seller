import tkinter as tk
from tkinter import messagebox
import json
from core.config_manager import ConfigManager
from core.socket_server import SocketServer
from core.logger import Logger

class AHApp:
    def __init__(self, root):
        self.root = root
        self.root.title("AH Auto Seller")
        self.root.geometry("420x600")
        self.root.resizable(False, False)

        self.config_mgr = ConfigManager()
        self.logger = Logger(self._append_log)
        self.socket = SocketServer(event_handler=self._handle_socket_event)
        self.socket.start()

        self._build_ui()
        self._load_config_to_ui()
        self.logger.log("Program started. Waiting for mod connection...")

    def _build_ui(self):
        self.lbl_status = tk.Label(self.root, text="STATUS: Stopped", font=("Arial", 10, "bold"), fg="red")
        self.lbl_status.pack(pady=5)

        frame = tk.Frame(self.root)
        frame.pack(pady=5, padx=10, fill="x")

        fields = [
            ("Sell Price:", "price"), ("Min Delay:", "min_delay"), ("Max Delay:", "max_delay"),
            ("Player Radius:", "player_radius"), ("Standing Time:", "stand_time"),
            ("Click X:", "click_x"), ("Click Y:", "click_y"), ("Confirm Slot:", "confirm_slot"), 
            ("Items Remaining:", "items")
        ]
        self.entries = {}
        for i, (label, key) in enumerate(fields):
            tk.Label(frame, text=label).grid(row=i, column=0, sticky="w", pady=2)
            ent = tk.Entry(frame, width=15)
            ent.grid(row=i, column=1, pady=2, padx=5)
            self.entries[key] = ent
        self.entries["items"].config(state="readonly")

        btn_frame = tk.Frame(self.root)
        btn_frame.pack(pady=5)
        tk.Button(btn_frame, text="START", width=10, bg="#4CAF50", fg="white", command=self._start).pack(side="left", padx=5)
        tk.Button(btn_frame, text="STOP", width=10, bg="#f44336", fg="white", command=self._stop).pack(side="left", padx=5)
        tk.Button(self.root, text="SAVE SETTINGS", width=20, command=self._save_config).pack(pady=5)

        self.log_text = tk.Text(self.root, height=12, width=45, state="disabled", font=("Consolas", 9))
        self.log_text.pack(pady=5, padx=10)

    def _load_config_to_ui(self):
        cfg = self.config_mgr.get()
        for k, v in cfg.items():
            if k in self.entries:
                self.entries[k].config(state="normal")
                self.entries[k].delete(0, tk.END)
                self.entries[k].insert(0, str(v))
                if k == "items": self.entries[k].config(state="readonly")

    def _save_config(self):
        try:
            new_cfg = {
                "price": int(self.entries["price"].get()),
                "min_delay": float(self.entries["min_delay"].get()),
                "max_delay": float(self.entries["max_delay"].get()),
                "player_radius": float(self.entries["player_radius"].get()),
                "stand_time": int(self.entries["stand_time"].get()),
                "click_x": int(self.entries["click_x"].get()),
                "click_y": int(self.entries["click_y"].get()),
                "confirm_slot": int(self.entries["confirm_slot"].get())
            }
            self.config_mgr.save(new_cfg)
            self.socket.send({"type": "CONFIG", "data": new_cfg})
            self.logger.log("Settings saved & synced.")
        except ValueError:
            messagebox.showerror("Error", "Invalid number format in settings.")

    def _start(self):
        self.socket.send({"type": "START"})
        self.logger.log("Start command sent.")

    def _stop(self):
        self.socket.send({"type": "STOP"})
        self.logger.log("Stop command sent.")

    def _handle_socket_event(self, raw: str):
        self.root.after(0, self._process_event, raw)

    def _process_event(self, raw: str):
        if raw == "CONNECTED":
            self.logger.log("Mod connected.")
            self.socket.send({"type": "CONFIG", "data": self.config_mgr.get()})
            return
        if raw == "DISCONNECTED":
            self.logger.log("Mod disconnected. Automation halted.")
            self._update_status("Stopped", "red")
            return
        if raw.startswith("SOCKET_ERROR"): return

        try:
            data = json.loads(raw)
            t = data.get("type")
            if t == "LOG": self.logger.log(data["msg"])
            elif t == "STATUS":
                state = data["state"]
                color = "green" if state == "RUNNING" else "red" if state == "STOPPED" else "orange"
                self._update_status(state.replace("_", " ").title(), color)
            elif t == "ITEM_COUNT":
                self.entries["items"].config(state="normal")
                self.entries["items"].delete(0, tk.END)
                self.entries["items"].insert(0, str(data["count"]))
                self.entries["items"].config(state="readonly")
            elif t == "PLAYER_DETECTED":
                self.logger.log("Nearby player detected. Stopped.")
                self._update_status("Stopped", "red")
        except Exception:
            pass

    def _update_status(self, text, color):
        self.lbl_status.config(text=f"STATUS: {text}", fg=color)

    def _append_log(self, msg):
        self.log_text.config(state="normal")
        self.log_text.insert(tk.END, msg + "\n")
        self.log_text.see(tk.END)
        self.log_text.config(state="disabled")

    def on_close(self):
        self.socket.stop()
        self.root.destroy()
