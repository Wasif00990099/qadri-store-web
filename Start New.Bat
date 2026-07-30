@echo off
title Qadri Store Web Server
echo ============================================
echo   QADRI STORE WEB SERVER
echo   Starting on port 8080...
echo ============================================
echo.
echo Features:
echo   - Stock Management (/stock)
echo   - Point of Sale (/pos)
echo   - Sales Report (/sales)
echo   - Price Update (/price-update)
echo   - Purchase Invoice (/purchase-invoice) [NEW]
echo.
java -cp .;mssql-jdbc-13.2.1.jre8.jar QadriWebServer
pause