@echo off
cd /d "C:\Users\Qadri\Desktop\Qadri Store Data"
:LOOP
:: Wait 2 minutes
timeout /t 120 /nobreak >nul

:: Force Add and Commit
git add data.json
git commit -m "Force Sync: %date% %time%" 2>> sync_log.txt

:: Fetch and reset to avoid conflicts (Ye line Git ko hamesha fresh rakhegi)
git fetch origin 2>> sync_log.txt
git reset --hard origin/main 2>> sync_log.txt

:: Push to GitHub
git push origin main 2>> sync_log.txt

goto LOOP