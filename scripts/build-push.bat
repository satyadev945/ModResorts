@echo off
setlocal enabledelayedexpansion

:: =============================================================================
:: build-push.bat – Build and push the ModResorts Docker image (Windows)
:: Supports: Google Artifact Registry | Docker Hub
:: Usage   : scripts\build-push.bat   (run from repository root)
:: =============================================================================

set PROJECT_NAME=modresorts

echo ==============================================
echo   ModResorts - Docker Build ^& Push
echo ==============================================
echo.

:: ---------------------------------------------------------------------------
:: Prompt for image tag
:: ---------------------------------------------------------------------------
set /p IMAGE_TAG_INPUT="Enter image tag [latest]: "
if "!IMAGE_TAG_INPUT!"=="" (
    set IMAGE_TAG=latest
) else (
    set IMAGE_TAG=!IMAGE_TAG_INPUT!
)
echo Image tag: !IMAGE_TAG!
echo.

:: ---------------------------------------------------------------------------
:: Registry selection
:: ---------------------------------------------------------------------------
echo Select container registry:
echo   1) Google Artifact Registry
echo   2) Docker Hub
set /p REGISTRY_CHOICE="Enter choice [1]: "
if "!REGISTRY_CHOICE!"=="" set REGISTRY_CHOICE=1

if "!REGISTRY_CHOICE!"=="1" goto :artifact_registry
if "!REGISTRY_CHOICE!"=="2" goto :docker_hub
echo ERROR: Invalid choice '!REGISTRY_CHOICE!'. Exiting.
exit /b 1

:: ---------------------------------------------------------------------------
:: Google Artifact Registry
:: ---------------------------------------------------------------------------
:artifact_registry
echo.
echo --- Google Artifact Registry ---
set /p GCP_PROJECT="Enter GCP Project ID: "
set /p GCP_REGION="Enter GCP Region [us-central1]: "
if "!GCP_REGION!"=="" set GCP_REGION=us-central1
set /p AR_REPO="Enter Artifact Registry repository name [modresorts-repo]: "
if "!AR_REPO!"=="" set AR_REPO=modresorts-repo

set FULL_IMAGE_NAME=!GCP_REGION!-docker.pkg.dev/!GCP_PROJECT!/!AR_REPO!/!PROJECT_NAME!:!IMAGE_TAG!

echo.
echo Authenticating with Google Artifact Registry...
gcloud auth configure-docker !GCP_REGION!-docker.pkg.dev --quiet
if !ERRORLEVEL! neq 0 (
    echo ERROR: Artifact Registry authentication failed.
    exit /b 1
)
goto :build_image

:: ---------------------------------------------------------------------------
:: Docker Hub
:: ---------------------------------------------------------------------------
:docker_hub
echo.
echo --- Docker Hub ---
set /p DOCKER_USERNAME="Enter Docker Hub username: "
set /p DOCKER_PASSWORD="Enter Docker Hub password/token: "
set /p DOCKER_REPO="Enter Docker Hub repository [!DOCKER_USERNAME!/!PROJECT_NAME!]: "
if "!DOCKER_REPO!"=="" set DOCKER_REPO=!DOCKER_USERNAME!/!PROJECT_NAME!

set FULL_IMAGE_NAME=!DOCKER_REPO!:!IMAGE_TAG!

echo.
echo Authenticating with Docker Hub...
echo !DOCKER_PASSWORD! | docker login --username !DOCKER_USERNAME! --password-stdin
if !ERRORLEVEL! neq 0 (
    echo ERROR: Docker Hub authentication failed.
    exit /b 1
)
goto :build_image

:: ---------------------------------------------------------------------------
:: Build the Docker image
:: ---------------------------------------------------------------------------
:build_image
echo.
echo Full image name: !FULL_IMAGE_NAME!
echo.
echo Building Docker image...
docker build -f Dockerfile -t !FULL_IMAGE_NAME! .
if !ERRORLEVEL! neq 0 (
    echo ERROR: Docker build failed.
    exit /b 1
)
echo Build successful.
echo.

:: ---------------------------------------------------------------------------
:: Push the image
:: ---------------------------------------------------------------------------
echo Pushing image to registry...
docker push !FULL_IMAGE_NAME!
if !ERRORLEVEL! neq 0 (
    echo ERROR: Docker push failed.
    exit /b 1
)

echo.
echo ==============================================
echo   Image pushed successfully!
echo   !FULL_IMAGE_NAME!
echo ==============================================

endlocal
