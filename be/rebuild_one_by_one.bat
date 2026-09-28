@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo === docker compose down ===
docker compose down --remove-orphans
for %%S in (auth-service catalog-service bidding-service api-gateway) do (
  echo.
  echo === BUILD %%S ===
  docker compose build %%S
  if errorlevel 1 ( echo *** BUILD %%S FAILED *** & goto :end )
)
echo.
echo === docker compose up -d ===
docker compose up -d
docker compose ps
:end
echo.
echo Xong. Nhan phim bat ky de dong cua so.
pause >nul
