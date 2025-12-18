@echo off
REM Container run script for Standup App with data persistence

echo 🐳 Standup App Container Runner
echo ==============================

REM Create data and logs directories if they don't exist
if not exist "%cd%\data" mkdir "%cd%\data"
if not exist "%cd%\logs" mkdir "%cd%\logs"

REM Copy existing Standups.txt to data directory if it exists and data version doesn't
if exist "Standups.txt" if not exist "data\Standups.txt" (
    echo 📋 Copying existing Standups.txt to data directory...
    copy /Y "Standups.txt" "data\Standups.txt"
    echo ✅ Standups.txt copied to data/ directory
)

REM Check if .env file exists
if not exist ".env" (
    echo ⚠️  .env file not found. Please create one based on env.example
    if exist "env.example" (
        echo 📋 You can copy env.example to .env and modify it:
        echo    copy env.example .env
    ) else (
        echo 📝 Creating a basic .env file...
        echo # Spring Boot Standup App Configuration > .env
        echo AUTH_USERNAME=admin >> .env
        echo AUTH_PASSWORD=ChangeThisPassword123! >> .env
        echo FLASK_SECRET_KEY=your-very-long-random-secret-key-here >> .env
        echo STANDUPS_FILE=/app/data/Standups.txt >> .env
        echo 🔧 Created .env file. Please update the credentials!
    )
)

echo.
echo 📁 Host data directory: %cd%\data
echo 📁 Host logs directory: %cd%\logs
echo 🔧 Environment file: %cd%\.env
echo 🌐 App will be available at: http://localhost:5555
echo 🛑 Press Ctrl+C to stop container
echo.

REM Detect container runtime (Podman or Docker)
set "CONTAINER_CMD="
where podman >nul 2>&1
if %errorlevel% equ 0 (
    set "CONTAINER_CMD=podman"
) else (
    where docker >nul 2>&1
    if %errorlevel% equ 0 (
        set "CONTAINER_CMD=docker"
    ) else (
        echo ❌ Neither Podman nor Docker found. Please install one of them.
        echo    Podman Desktop: https://podman-desktop.io/
        echo    Docker Desktop: https://www.docker.com/products/docker-desktop/
        exit /b 1
    )
)

echo 🐳 Using %CONTAINER_CMD% as container runtime
echo.

REM Build the container image
echo 🔨 Building container image...
%CONTAINER_CMD% build -t standup-app .
if errorlevel 1 (
    echo ❌ Failed to build container image
    exit /b 1
)

echo ✅ Container image built successfully
echo.

REM Stop and remove existing container if it exists
echo 🛑 Stopping existing container (if running)...
%CONTAINER_CMD% stop standup-app >nul 2>&1
%CONTAINER_CMD% rm standup-app >nul 2>&1

REM Run container with volume mounts and port mapping
echo 🚀 Starting container...
%CONTAINER_CMD% run --rm -it ^
    --name standup-app ^
    -p 5555:5555 ^
    -v "%cd%\data:/app/data" ^
    -v "%cd%\logs:/app/logs" ^
    --env-file .env ^
    standup-app

echo.
echo 🏁 Container stopped
