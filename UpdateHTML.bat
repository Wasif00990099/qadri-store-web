@echo off
cd /d "C:\Users\Qadri\Desktop\Qadri Store Data"
echo Updating HTML to GitHub...
git add index.html
git commit -m "Updated HTML design"
git push
echo.
echo Process Complete! Press any key to close.
pause >nul