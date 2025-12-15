@echo off
REM Toggle Mobile App API URL - Switch to PRODUCTION
echo ========================================
echo   Switching MOBILE APP to PRODUCTION
echo ========================================
cd /d "%~dp0"

set FILE=app\src\main\java\com\example\njoy\ApiService.kt

REM Backup original file
copy "%FILE%" "%FILE%.backup" >nul

REM Replace local URL with production URL
powershell -Command "(Get-Content '%FILE%') -replace '// private const val BASE_URL = \"https://projecte-n-joy.vercel.app/\"', 'private const val BASE_URL = \"https://projecte-n-joy.vercel.app/\"' | Set-Content '%FILE%'"
powershell -Command "(Get-Content '%FILE%') -replace 'private const val BASE_URL = \"http://.*:8000/\"', '// private const val BASE_URL = \"http://192.168.1.132:8000/\"' | Set-Content '%FILE%'"

echo.
echo ✅ Mobile App now points to: https://projecte-n-joy.vercel.app/
echo.
echo IMPORTANT: You need to REBUILD the app in Android Studio!
pause
