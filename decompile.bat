@echo off
chcp 65001 >nul
echo ========================================
echo Декомпиляция плагина DwarfMode
echo ========================================
echo.

REM Переход в директорию скрипта
cd /d "%~dp0"

REM Установка пути к Java 21
REM Пробуем использовать JDK 21, если доступен, иначе JRE
if exist "C:\Program Files\Java\jdk-21.0.2\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-21.0.2"
    set "JAVA=%JAVA_HOME%\bin\java.exe"
) else if exist "C:\Users\Meows\AppData\Roaming\ModrinthApp\meta\java_versions\zulu21.38.21-ca-jre21.0.5-win_x64\bin\java.exe" (
    set "JAVA_HOME=C:\Users\Meows\AppData\Roaming\ModrinthApp\meta\java_versions\zulu21.38.21-ca-jre21.0.5-win_x64"
    set "JAVA=%JAVA_HOME%\bin\java.exe"
) else (
    echo [ОШИБКА] Java 21 не найдена!
    pause
    exit /b 1
)

REM Проверка наличия CFR
if not exist "tools\cfr.jar" (
    echo [ОШИБКА] CFR декомпилятор не найден!
    echo.
    echo Текущая директория: %CD%
    echo Ожидаемый путь: %CD%\tools\cfr.jar
    echo.
    echo Пожалуйста:
    echo 1. Скачайте CFR с https://www.benf.org/other/cfr/
    echo 2. Переименуйте файл в cfr.jar
    echo 3. Поместите в папку tools\
    echo.
    pause
    exit /b 1
)

REM Создание папки для исходников
if not exist "src" mkdir src

echo [1/2] Декомпиляция .class файлов...
"%JAVA%" -jar tools\cfr.jar io\hotmail\com\jacob_vejvoda\DwarfMode\*.class --outputdir src --caseinsensitivefs true

if %errorlevel% neq 0 (
    echo [ОШИБКА] Декомпиляция не удалась!
    pause
    exit /b 1
)

echo.
echo [2/2] Копирование конфигурационных файлов...
if exist "plugin.yml" copy /Y plugin.yml src\ >nul
if exist "config.yml" copy /Y config.yml src\ >nul

echo.
echo ========================================
echo Декомпиляция завершена!
echo Исходные файлы находятся в папке: src\
echo ========================================
echo.
pause

