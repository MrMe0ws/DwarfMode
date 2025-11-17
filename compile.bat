@echo off
chcp 65001 >nul
echo ========================================
echo Компиляция плагина DwarfMode
echo ========================================
echo.

REM Переход в директорию скрипта
cd /d "%~dp0"
echo Текущая рабочая директория: %CD%
echo.

REM Проверка наличия исходников
echo [Проверка] Поиск исходных файлов...
set "SOURCE_FILE=src\io\hotmail\com\jacob_vejvoda\DwarfMode\DwarfMode.java"
dir /b "%SOURCE_FILE%" >nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] Исходные файлы найдены!
) else (
    echo [ОШИБКА] Исходные .java файлы не найдены!
    echo.
    echo Текущая директория: %CD%
    echo Ожидаемый путь: %CD%\%SOURCE_FILE%
    echo.
    echo Сначала выполните декомпиляцию (decompile.bat)
    echo.
    pause
    exit /b 1
)
echo.

REM Проверка наличия Spigot API
echo [Проверка] Поиск Spigot API...
if not exist "lib\spigot-api.jar" (
    echo [ОШИБКА] Spigot API не найден!
    echo.
    echo Пожалуйста:
    echo 1. Скачайте spigot-api для вашей версии Minecraft
    echo 2. Переименуйте в spigot-api.jar
    echo 3. Поместите в папку lib\
    echo.
    echo Ссылка: https://hub.spigotmc.org/nexus/content/repositories/snapshots/org/spigotmc/spigot-api/
    echo.
    pause
    exit /b 1
)

echo [OK] Spigot API найден!
set "SPIGOT_JAR=%CD%\lib\spigot-api.jar"
echo Полный путь: %SPIGOT_JAR%
echo.

REM Установка пути к Java 21 JDK
REM Spigot API требует Java 17+ для компиляции (класс-файлы версии 61.0)
if exist "C:\Program Files\Java\jdk-21.0.2\bin\javac.exe" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-21.0.2"
    set "JAVAC=%JAVA_HOME%\bin\javac.exe"
    set "JAVA=%JAVA_HOME%\bin\java.exe"
) else (
    echo [ОШИБКА] JDK 21 не найден по пути: C:\Program Files\Java\jdk-21.0.2
    echo.
    echo Spigot API требует Java 17+ для компиляции.
    echo Убедитесь, что JDK 21 установлен в системе.
    pause
    exit /b 1
)

REM Проверка Java
echo [Проверка] Поиск Java компилятора (javac)...
if not exist "%JAVAC%" (
    echo [ОШИБКА] javac не найден по пути: %JAVAC%
    pause
    exit /b 1
)
"%JAVAC%" -version
echo [OK] Java компилятор найден!
echo.

REM Создание папки для сборки
if exist "build" rmdir /s /q build
mkdir build

echo [1/3] Компиляция Java файлов...
echo Файл для компиляции: src\io\hotmail\com\jacob_vejvoda\DwarfMode\DwarfMode.java
echo Classpath: %SPIGOT_JAR%
echo Вывод: build\
echo.
echo Запуск компиляции (это может занять несколько секунд)...
REM Используем Java 17+ для компиляции (Spigot API требует минимум Java 17)
"%JAVAC%" -encoding UTF-8 -source 17 -target 17 -cp "%SPIGOT_JAR%" -d build -sourcepath src src\io\hotmail\com\jacob_vejvoda\DwarfMode\DwarfMode.java 2>&1

if %errorlevel% neq 0 (
    echo.
    echo [ОШИБКА] Компиляция не удалась!
    echo Проверьте ошибки выше и исправьте код.
    pause
    exit /b 1
)
echo Компиляция завершена успешно!

echo.
echo [2/3] Копирование ресурсов...
if exist "src\plugin.yml" copy /Y src\plugin.yml build\ >nul
if exist "plugin.yml" copy /Y plugin.yml build\ >nul
if exist "src\config.yml" copy /Y src\config.yml build\ >nul
if exist "config.yml" copy /Y config.yml build\ >nul

echo.
echo [3/3] Создание JAR файла...
cd build
jar cf ..\DwarfMode-modified.jar io\ plugin.yml config.yml 2>nul
cd ..

if %errorlevel% neq 0 (
    echo [ОШИБКА] Создание JAR не удалось!
    pause
    exit /b 1
)

echo.
echo ========================================
echo Компиляция завершена!
echo Готовый плагин: DwarfMode-modified.jar
echo ========================================
echo.
pause

