@echo off
setlocal enabledelayedexpansion

set APP_NAME=modresorts
set NAMESPACE=modresorts

echo ============================================
echo   ModResorts - AWS EKS Deployment Script
echo ============================================
echo.

rem ── Collect required inputs ──────────────────────────────────────────────
set /p AWS_REGION="Enter AWS region (e.g. us-east-1): "
if "!AWS_REGION!"=="" (
    echo ERROR: AWS region is required.
    exit /b 1
)

set /p CLUSTER_NAME="Enter EKS cluster name: "
if "!CLUSTER_NAME!"=="" (
    echo ERROR: EKS cluster name is required.
    exit /b 1
)

set /p IMAGE_URI="Enter full Docker image URI: "
if "!IMAGE_URI!"=="" (
    echo ERROR: Docker image URI is required.
    exit /b 1
)

echo.
echo -- Optional environment variable configuration --
echo Press Enter to skip any variable.
echo.

set /p REDIS_HOST_VAL="Enter REDIS_HOST value (or Enter to skip): "
set /p REDIS_PORT_VAL="Enter REDIS_PORT value (or Enter to skip): "
set /p WEATHER_API_KEY_VAL="Enter WEATHER_API_KEY value (or Enter to skip): "
set /p WEATHER_SERVICE_URL_VAL="Enter WEATHER_SERVICE_URL value (or Enter to skip): "

echo.
echo -- Configuring kubectl for EKS cluster --
aws eks update-kubeconfig --region !AWS_REGION! --name !CLUSTER_NAME!
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

echo.
echo -- Updating Kubernetes manifests --

powershell -Command "(Get-Content kubernetes\deployment.yaml) -replace '{{IMAGE_URI}}', '!IMAGE_URI!' | Set-Content kubernetes\deployment.yaml"

if not "!REDIS_HOST_VAL!"=="" (
    powershell -Command "(Get-Content kubernetes\deployment.yaml) -replace '{{REDIS_HOST}}', '!REDIS_HOST_VAL!' | Set-Content kubernetes\deployment.yaml"
)
if not "!REDIS_PORT_VAL!"=="" (
    powershell -Command "(Get-Content kubernetes\deployment.yaml) -replace '{{REDIS_PORT}}', '!REDIS_PORT_VAL!' | Set-Content kubernetes\deployment.yaml"
)
if not "!WEATHER_API_KEY_VAL!"=="" (
    powershell -Command "(Get-Content kubernetes\deployment.yaml) -replace '{{WEATHER_API_KEY}}', '!WEATHER_API_KEY_VAL!' | Set-Content kubernetes\deployment.yaml"
)
if not "!WEATHER_SERVICE_URL_VAL!"=="" (
    powershell -Command "(Get-Content kubernetes\deployment.yaml) -replace '{{WEATHER_SERVICE_URL}}', '!WEATHER_SERVICE_URL_VAL!' | Set-Content kubernetes\deployment.yaml"
)

echo.
echo -- Applying Kubernetes manifests --
kubectl apply -f kubernetes\namespace.yaml
if !ERRORLEVEL! neq 0 ( echo ERROR applying namespace.yaml & exit /b 1 )

kubectl apply -f kubernetes\deployment.yaml
if !ERRORLEVEL! neq 0 ( echo ERROR applying deployment.yaml & exit /b 1 )

kubectl apply -f kubernetes\service.yaml
if !ERRORLEVEL! neq 0 ( echo ERROR applying service.yaml & exit /b 1 )

kubectl apply -f kubernetes\ingress.yaml
if !ERRORLEVEL! neq 0 ( echo ERROR applying ingress.yaml & exit /b 1 )

echo.
echo -- Waiting for deployment rollout --
kubectl rollout status deployment/!APP_NAME! -n !NAMESPACE! --timeout=300s
if !ERRORLEVEL! neq 0 (
    echo ERROR: Deployment rollout failed.
    echo Rollback command: kubectl rollout undo deployment/!APP_NAME! -n !NAMESPACE!
    exit /b 1
)

echo.
echo -- Verifying deployed resources --
kubectl get pods,svc,ingress -n !NAMESPACE!

echo.
echo ============================================
echo   Deployment complete!
echo ============================================
echo.
echo Rollback command (if needed):
echo   kubectl rollout undo deployment/!APP_NAME! -n !NAMESPACE!

endlocal
