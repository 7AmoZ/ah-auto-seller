@echo off
echo Building AH Auto Seller EXE...
pyinstaller --onefile --windowed --name "AH_Auto_Seller" main.py
echo Build complete. Check dist/ folder.
pause