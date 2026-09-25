@echo off
setlocal enabledelayedexpansion

echo ============================================
echo   ModResorts - Deploy to Azure AKS
echo ============================================
echo.

set /p RESOURCE_GROUP="Enter Azure Resource Group name: "
if "!RESOURCE_GROUP!"=="" (
    echo ERROR: Resource group cannot be empty.
    exit /b 1
)

set /p CLUSTER_NAME="Enter AKS Cluster name: "
if "!CLUSTER_NAME!"=="" (
    echo ERROR: AKS cluster name cannot be empty.
    exit /b 1
)

set /p IMAGE_URI="Enter full Docker image URI (e.g. myacr.azurecr.io/modresorts:latest): "
if "!IMAGE_URI!"=="" (
    echo ERROR: Image URI cannot be empty.
    exit /b 1
)

echo.
echo -- Optional: Application Environment Variables --
echo    Press Enter to skip any variable.
echo.

set /p REDIS_HOST="Enter REDIS_HOST (Azure Cache for Redis hostname): "
set /p REDIS_PORT="Enter REDIS_PORT (default: 6380): "
set /p REDIS_PASSWORD="Enter REDIS_PASSWORD (Azure Cache for Redis access key): "
set /p REDIS_SSL="Enter REDIS_SSL (true/false, default: true): "
set /p GRPC_SERVICE_HOST="Enter GRPC_SERVICE_HOST: "
set /p GRPC_SERVICE_PORT="Enter GRPC_SERVICE_PORT (default: 50051): "
set /p WEATHER_API_KEY="Enter WEATHER_API_KEY: "

if "!REDIS_PORT!"=="" set "REDIS_PORT=6380"
if "!REDIS_SSL!"=="" set "REDIS_SSL=true"
if "!GRPC_SERVICE_PORT!"=="" set "GRPC_SERVICE_PORT=50051"

echo.
echo -- Configuring kubectl for AKS cluster: !CLUSTER_NAME! --
az aks get-credentials --resource-group !RESOURCE_GROUP! --name !CLUSTER_NAME! --overwrite-existing
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to get AKS credentials.
    exit /b 1
)

echo.
echo -- Verifying cluster connectivity --
kubectl cluster-info
if !ERRORLEVEL! neq 0 (
    echo ERROR: Cannot connect to AKS cluster.
    exit /b 1
)

echo.
echo -- Preparing deployment manifests --

if exist kubernetes_deploy_tmp rmdir /s /q kubernetes_deploy_tmp
xcopy /s /e /i /q kubernetes kubernetes_deploy_tmp >nul

powershell -Command "(Get-Content 'kubernetes_deploy_tmp\deployment.yaml') -replace '\{\{IMAGE_URI\}\}', '!IMAGE_URI!' -replace '\{\{REDIS_HOST\}\}', '!REDIS_HOST!' -replace '\{\{REDIS_PORT\}\}', '!REDIS_PORT!' -replace '\{\{REDIS_PASSWORD\}\}', '!REDIS_PASSWORD!' -replace '\{\{REDIS_SSL\}\}', '!REDIS_SSL!' -replace '\{\{GRPC_SERVICE_HOST\}\}', '!GRPC_SERVICE_HOST!' -replace '\{\{GRPC_SERVICE_PORT\}\}', '!GRPC_SERVICE_PORT!' -replace '\{\{WEATHER_API_KEY\}\}', '!WEATHER_API_KEY!' | Set-Content 'kubernetes_deploy_tmp\deployment.yaml'"
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to update deployment manifest.
    exit /b 1
)

echo.
echo -- Applying Kubernetes manifests --
kubectl apply -f kubernetes_deploy_tmp\namespace.yaml
if !ERRORLEVEL! neq 0 ( echo ERROR: Failed to apply namespace. & exit /b 1 )
echo   [OK] Namespace applied

kubectl apply -f kubernetes_deploy_tmp\deployment.yaml
if !ERRORLEVEL! neq 0 ( echo ERROR: Failed to apply deployment. & exit /b 1 )
echo   [OK] Deployment applied

kubectl apply -f kubernetes_deploy_tmp\service.yaml
if !ERRORLEVEL! neq 0 ( echo ERROR: Failed to apply service. & exit /b 1 )
echo   [OK] Service applied

kubectl apply -f kubernetes_deploy_tmp\ingress.yaml
if !ERRORLEVEL! neq 0 ( echo ERROR: Failed to apply ingress. & exit /b 1 )
echo   [OK] Ingress applied

rmdir /s /q kubernetes_deploy_tmp

echo.
echo -- Waiting for deployment rollout --
kubectl rollout status deployment/modresorts -n modresorts --timeout=300s
if !ERRORLEVEL! neq 0 (
    echo ERROR: Deployment rollout failed. Initiating rollback ...
    kubectl rollout undo deployment/modresorts -n modresorts
    echo Rollback initiated. Check pod status: kubectl get pods -n modresorts
    exit /b 1
)

echo.
echo -- Verifying deployed resources --
kubectl get pods,svc,ingress -n modresorts

echo.
echo ============================================
echo   Deployment completed successfully!
echo ============================================
echo.
echo Useful commands:
echo   kubectl get pods -n modresorts
echo   kubectl logs -f deployment/modresorts -n modresorts
echo   kubectl rollout undo deployment/modresorts -n modresorts

endlocal
