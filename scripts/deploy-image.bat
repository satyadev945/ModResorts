@echo off
setlocal enabledelayedexpansion

:: =============================================================================
:: deploy-image.bat – Deploy ModResorts to GCP GKE (Windows)
:: Usage: scripts\deploy-image.bat   (run from repository root)
:: =============================================================================

set APP_NAME=modresorts
set NAMESPACE=modresorts

echo ==============================================
echo   ModResorts - GKE Deployment
echo ==============================================
echo.

:: ---------------------------------------------------------------------------
:: Collect GCP / GKE parameters
:: ---------------------------------------------------------------------------
set /p GCP_PROJECT="Enter GCP Project ID: "
if "!GCP_PROJECT!"=="" (
    echo ERROR: GCP Project ID is required.
    exit /b 1
)

set /p GCP_ZONE="Enter GCP Zone [us-central1-a]: "
if "!GCP_ZONE!"=="" set GCP_ZONE=us-central1-a

set /p CLUSTER_NAME="Enter GKE Cluster Name [modresorts-autopilot]: "
if "!CLUSTER_NAME!"=="" set CLUSTER_NAME=modresorts-autopilot

set /p IMAGE_URI="Enter full Docker image URI: "
if "!IMAGE_URI!"=="" (
    echo ERROR: Docker image URI is required.
    exit /b 1
)

:: ---------------------------------------------------------------------------
:: Optional application environment variables
:: ---------------------------------------------------------------------------
echo.
echo --- Optional Application Configuration ---
echo (Press Enter to skip any value)

set /p REDIS_HOST_VAL="Enter REDIS_HOST (Google Cloud Memorystore host): "
set /p REDIS_PORT_VAL="Enter REDIS_PORT [6379]: "
if "!REDIS_PORT_VAL!"=="" set REDIS_PORT_VAL=6379
set /p WUNDERGROUND_API_KEY_VAL="Enter WUNDERGROUND_API_KEY: "

:: ---------------------------------------------------------------------------
:: Configure kubectl for the target GKE cluster
:: ---------------------------------------------------------------------------
echo.
echo Configuring kubectl for cluster '!CLUSTER_NAME!'...
gcloud container clusters get-credentials !CLUSTER_NAME! --zone !GCP_ZONE! --project !GCP_PROJECT!
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to configure kubectl.
    exit /b 1
)

echo Verifying cluster connectivity...
kubectl cluster-info
if !ERRORLEVEL! neq 0 (
    echo ERROR: Cannot connect to cluster.
    exit /b 1
)

:: ---------------------------------------------------------------------------
:: Substitute placeholders using PowerShell (Windows sed equivalent)
:: ---------------------------------------------------------------------------
echo.
echo Updating Kubernetes manifests with deployment values...

copy /Y kubernetes\deployment.yaml %TEMP%\deployment.yaml >nul

powershell -Command "(Get-Content '%TEMP%\deployment.yaml') -replace '\{\{IMAGE_URI\}\}', '!IMAGE_URI!' | Set-Content '%TEMP%\deployment.yaml'"
powershell -Command "(Get-Content '%TEMP%\deployment.yaml') -replace '\{\{REDIS_HOST\}\}', '!REDIS_HOST_VAL!' | Set-Content '%TEMP%\deployment.yaml'"
powershell -Command "(Get-Content '%TEMP%\deployment.yaml') -replace '\{\{REDIS_PORT\}\}', '!REDIS_PORT_VAL!' | Set-Content '%TEMP%\deployment.yaml'"
powershell -Command "(Get-Content '%TEMP%\deployment.yaml') -replace '\{\{WUNDERGROUND_API_KEY\}\}', '!WUNDERGROUND_API_KEY_VAL!' | Set-Content '%TEMP%\deployment.yaml'"

:: ---------------------------------------------------------------------------
:: Apply manifests in order
:: ---------------------------------------------------------------------------
echo.
echo Applying Kubernetes manifests...

echo   [1/4] Applying namespace...
kubectl apply -f kubernetes\namespace.yaml
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to apply namespace.
    exit /b 1
)

echo   [2/4] Applying deployment...
kubectl apply -f %TEMP%\deployment.yaml
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to apply deployment.
    exit /b 1
)

echo   [3/4] Applying service...
kubectl apply -f kubernetes\service.yaml
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to apply service.
    exit /b 1
)

echo   [4/4] Applying ingress...
kubectl apply -f kubernetes\ingress.yaml
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to apply ingress.
    exit /b 1
)

:: ---------------------------------------------------------------------------
:: Wait for rollout
:: ---------------------------------------------------------------------------
echo.
echo Waiting for deployment rollout...
kubectl rollout status deployment/!APP_NAME! -n !NAMESPACE! --timeout=300s
if !ERRORLEVEL! neq 0 (
    echo ERROR: Deployment rollout failed. Running rollback...
    kubectl rollout undo deployment/!APP_NAME! -n !NAMESPACE!
    exit /b 1
)

:: ---------------------------------------------------------------------------
:: Verify resources
:: ---------------------------------------------------------------------------
echo.
echo Verifying deployed resources...
kubectl get pods,svc,ingress -n !NAMESPACE!

echo.
echo ==============================================
echo   Deployment complete!
echo   Namespace : !NAMESPACE!
echo   Image     : !IMAGE_URI!
echo ==============================================
echo.
echo Useful commands:
echo   kubectl get pods -n !NAMESPACE!
echo   kubectl logs -f deployment/!APP_NAME! -n !NAMESPACE!
echo   kubectl rollout undo deployment/!APP_NAME! -n !NAMESPACE!

endlocal
