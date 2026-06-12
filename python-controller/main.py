import tkinter as tk
from gui.app import AHApp

if __name__ == "__main__":
    root = tk.Tk()
    app = AHApp(root)
    root.protocol("WM_DELETE_WINDOW", app.on_close)
    root.mainloop() 
