@echo off
cd /d "%~dp0..\..\be"
set OUT="%~dp0diag.txt"
echo === ps === > %OUT%
docker compose ps -a >> %OUT% 2>&1
echo === curl gateway === >> %OUT%
curl -s -o NUL -w "%%{http_code}" http://localhost:8080/api/products?size=1 >> %OUT% 2>&1
echo. >> %OUT%
for %%S in (api-gateway catalog-service auth-service bidding-service postgres) do (echo === logs %%S === >> %OUT% & docker compose logs --tail 40 %%S >> %OUT% 2>&1)
echo DONE >> %OUT%
