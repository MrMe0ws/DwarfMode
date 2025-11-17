@echo off
chcp 65001 >nul
echo ========================================
echo Загрузка Spigot API
echo ========================================
echo.

REM Переход в директорию скрипта
cd /d "%~dp0"

REM Создание папки lib если её нет
if not exist "lib" mkdir lib

REM Проверка существующего файла
if exist "lib\spigot-api.jar" (
    echo [ВНИМАНИЕ] Файл lib\spigot-api.jar уже существует!
    echo.
    echo Если файл поврежден, удалите его и запустите скрипт снова.
    echo.
    set /p delete="Удалить существующий файл? (y/n): "
    if /i "%delete%"=="y" (
        del /f "lib\spigot-api.jar"
        echo Файл удален.
        echo.
    ) else (
        echo Отмена. Используется существующий файл.
        pause
        exit /b 0
    )
)

echo Выберите версию Minecraft для Spigot API:
echo.
echo 1. 1.19 (рекомендуется для этого плагина)
echo 2. 1.19.1
echo 3. 1.19.2
echo 4. 1.19.3
echo 5. 1.19.4
echo 6. 1.20
echo 7. 1.20.1
echo 8. 1.20.2
echo 9. 1.20.4
echo 10. Другая версия (введите вручную)
echo.
set /p choice="Введите номер (1-10): "

if "%choice%"=="1" set "VERSION=1.19-R0.1-SNAPSHOT"
if "%choice%"=="2" set "VERSION=1.19.1-R0.1-SNAPSHOT"
if "%choice%"=="3" set "VERSION=1.19.2-R0.1-SNAPSHOT"
if "%choice%"=="4" set "VERSION=1.19.3-R0.1-SNAPSHOT"
if "%choice%"=="5" set "VERSION=1.19.4-R0.1-SNAPSHOT"
if "%choice%"=="6" set "VERSION=1.20-R0.1-SNAPSHOT"
if "%choice%"=="7" set "VERSION=1.20.1-R0.1-SNAPSHOT"
if "%choice%"=="8" set "VERSION=1.20.2-R0.1-SNAPSHOT"
if "%choice%"=="9" set "VERSION=1.20.4-R0.1-SNAPSHOT"
if "%choice%"=="10" (
    set /p VERSION="Введите версию (например, 1.19-R0.1-SNAPSHOT): "
)

if not defined VERSION (
    echo Неверный выбор!
    pause
    exit /b 1
)

REM Установка пути к Java 21 для проверки JAR
set "JAVA_HOME=C:\Users\Meows\AppData\Roaming\ModrinthApp\meta\java_versions\zulu21.38.21-ca-jre21.0.5-win_x64"

set "BASE_URL=https://hub.spigotmc.org/nexus/content/repositories/snapshots/org/spigotmc/spigot-api"
set "VERSION_DIR=%BASE_URL%/%VERSION%"
set "OUTPUT_FILE=lib\spigot-api.jar"
set "TEMP_FILE=lib\spigot-api-temp.jar"

echo.
echo Поиск последней версии Spigot API %VERSION%...
echo Директория: %VERSION_DIR%
echo.

REM Пробуем использовать Maven resolver для получения последней версии
echo Попытка автоматической загрузки через Maven координаты...
echo.
echo [ВАЖНО] Для SNAPSHOT версий нужно скачать файл вручную!
echo.
echo Инструкция для ручной загрузки:
echo.
echo 1. Откройте в браузере: https://hub.spigotmc.org/nexus/service/rest/repository/browse/snapshots/org/spigotmc/spigot-api/
echo 2. Перейдите в папку с версией: %VERSION%
echo 3. Найдите файл spigot-api-*.jar (НЕ sources.jar и НЕ javadoc.jar)
echo 4. Кликните на файл и скачайте его
echo 5. Переименуйте в spigot-api.jar
echo 6. Поместите в папку lib\
echo.
set /p manual="Скачали файл вручную? (y/n): "

if /i not "%manual%"=="y" (
    echo.
    echo Откройте ссылку в браузере: https://hub.spigotmc.org/nexus/service/rest/repository/browse/snapshots/org/spigotmc/spigot-api/%VERSION%/
    echo.
    echo После скачивания запустите этот скрипт снова и выберите 'y'.
    pause
    exit /b 0
)

REM Проверяем, есть ли уже файл
if not exist "%OUTPUT_FILE%" (
    echo.
    echo [ОШИБКА] Файл lib\spigot-api.jar не найден!
    echo Убедитесь, что вы скачали и переименовали файл правильно.
    pause
    exit /b 1
)

echo.
echo [OK] Файл найден! Продолжаем проверку...

:check_file

REM Проверка размера файла (JAR должен быть больше 1MB)
for %%A in ("%OUTPUT_FILE%") do set "FILE_SIZE=%%~zA"
if %FILE_SIZE% LSS 1048576 (
    echo [ОШИБКА] Загруженный файл слишком мал (%FILE_SIZE% байт). Возможно, загрузка не завершена или файл поврежден.
    if exist "%OUTPUT_FILE%" del /f "%OUTPUT_FILE%"
    pause
    exit /b 1
)

REM Проверка валидности JAR файла (попытка прочитать как ZIP)
echo Проверка валидности JAR файла...
"%JAVA_HOME%\bin\jar.exe" tf "%OUTPUT_FILE%" >nul 2>&1
if %errorlevel% neq 0 (
    echo [ОШИБКА] Файл не является валидным JAR архивом!
    echo Возможно, файл был поврежден при загрузке.
    if exist "%OUTPUT_FILE%" del /f "%OUTPUT_FILE%"
    pause
    exit /b 1
)

echo.
echo [OK] Spigot API успешно загружен и проверен!
echo Файл: %CD%\%OUTPUT_FILE%
echo Размер: %FILE_SIZE% байт
echo.
pause

