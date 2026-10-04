@echo off
rem Chạy toàn bộ bộ đo hiệu năng. Ví dụ:
rem   run_perf.bat all --quick        (chạy thử nhanh ~10 phút)
rem   run_perf.bat all                (đo thật, ~45-60 phút)
rem   run_perf.bat all --build        (lần đầu: build lại image)
chcp 65001 >nul
if "%~1"=="" (python "%~dp0run_perf.py" all) else (python "%~dp0run_perf.py" %*)
pause
