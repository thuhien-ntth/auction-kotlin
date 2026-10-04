@echo off
chcp 65001 >nul
set PYTHONIOENCODING=utf-8
set DOCKER_BUILDKIT=0
set COMPOSE_DOCKER_CLI_BUILD=0
cd /d "%~dp0"
echo Dang chay bo do nhanh... nhat ky: %~dp0quick_run_log.txt
python -u "%~dp0run_perf.py" all --quick --cache-modes on > "%~dp0quick_run_log.txt" 2>&1
echo EXIT CODE %ERRORLEVEL% >> "%~dp0quick_run_log.txt"
