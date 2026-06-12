import json
import os

class ConfigManager:
    def __init__(self, path="config.json"):
        self.path = path
        self.data = self._load()

    def _load(self) -> dict:
        if os.path.exists(self.path):
            with open(self.path, "r", encoding="utf-8") as f:
                return json.load(f)
        return {"price":2000,"min_delay":0.2,"max_delay":1.0,"player_radius":20,"stand_time":10,"click_x":960,"click_y":540}

    def save(self, new_data: dict):
        self.data = new_data
        with open(self.path, "w", encoding="utf-8") as f:
            json.dump(self.data, f, indent=2)

    def get(self) -> dict:
        return self.data