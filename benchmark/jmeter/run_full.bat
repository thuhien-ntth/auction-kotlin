@echo off
rem ==== Đo hiệu năng CHÍNH THỨC - phương án gọn (58 lần chạy, ~75 phút) - nhấp đúp để chạy ====
rem   KB1: 3 mức tải (50,100,200) x 3 lần x cache BẬT/TẮT = 18 lần
rem   KB2: 5 cặp gọi thẳng/qua Gateway, thứ tự ABBA      = 10 lần
rem   KB3: 3 mức (50,100,200 bidder) x 10 lần            = 30 lần
chcp 65001 >nul
set PYTHONIOENCODING=utf-8
set DOCKER_BUILDKIT=0
set COMPOSE_DOCKER_CLI_BUILD=0
rem Giới hạn RAM cho catalog-service và bidding-service khi đo (xem docker-compose.perf.yml)
set PERF_MEM_LIMIT=512m
cd /d "%~dp0"
echo Dang do hieu nang (~75 phut)... KHONG tat may, KHONG cho may ngu.
echo Nhat ky: results\^<thoi gian^>\run_log.txt
python -u "%~dp0run_perf.py" all --reps 3 --gw-reps 5 --read-threads 50,100,200
echo.
echo Xong (ma thoat %ERRORLEVEL%). Ket qua: summary.md trong thu muc results\ moi nhat
pause
