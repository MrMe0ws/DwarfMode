@echo off
chcp 65001 >nul
echo ========================================
echo Исправление поврежденного Spigot API
echo ========================================
echo.

cd /d "%~dp0"

if exist "lib\spigot-api.jar" (
    echo Удаление поврежденного файла...
    del /f "lib\spigot-api.jar"
    echo [OK] Файл удален!
    echo.
    echo Теперь запустите download-spigot-api.bat для загрузки нового файла.
) else (
    echo Файл lib\spigot-api.jar не найден.
    echo Запустите download-spigot-api.bat для загрузки.
)

echo.
pause

