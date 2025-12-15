@echo off
REM Toggle Mobile App API URL - Switch to LOCAL
echo ========================================
echo   Switching MOBILE APP to LOCAL
echo ========================================
cd /d "%~dp0"

set FILE=app\src\main\java\com\example\njoy\ApiService.kt

echo Looking for your local IP...
for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr /c:"IPv4"') do set LOCAL_IP=%%a
set LOCAL_IP=%LOCAL_IP: =%

echo Your local IP is: %LOCAL_IP%
echo.

REM Backup original file
copy "%FILE%" "%FILE%.backup" >nul

REM Replace production URL with local URL
powershell -Command "(Get-Content '%FILE%') -replace 'private const val BASE_URL = \"https://projecte-n-joy.vercel.app/\"', '// private const val BASE_URL = \"https://projecte-n-joy.vercel.app/\"' | Set-Content '%FILE%'"
powershell -Command "(Get-Content '%FILE%') -replace '// private const val BASE_URL = \"http://192.168.1.132:8000/\"', 'private const val BASE_URL = \"http://%LOCAL_IP%:8000/\"' | Set-Content '%FILE%'"

echo.
echo ✅ Mobile App now points to: http://%LOCAL_IP%:8000/
echo.
echo IMPORTANT: You need to REBUILD the app in Android Studio!
pause
