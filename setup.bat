@echo off
chcp 65001 >nul

REM Переход в директорию скрипта
cd /d "%~dp0"

echo ========================================
echo Настройка окружения для декомпиляции
echo ========================================
echo.

REM Создание необходимых папок
echo Создание структуры папок...
if not exist "tools" mkdir tools
if not exist "lib" mkdir lib
if not exist "src" mkdir src
if not exist "build" mkdir build

echo.
echo Структура папок создана!
echo.
echo Следующие шаги:
echo.
echo 1. Скачайте CFR декомпилятор:
echo    https://www.benf.org/other/cfr/
echo    Переименуйте в cfr.jar и поместите в папку tools\
echo.
echo 2. Скачайте Spigot API для вашей версии:
echo    https://hub.spigotmc.org/nexus/content/repositories/snapshots/org/spigotmc/spigot-api/
echo    Переименуйте в spigot-api.jar и поместите в папку lib\
echo.
echo 3. Запустите decompile.bat для декомпиляции
echo.
pause

