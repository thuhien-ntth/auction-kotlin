# Kiem tra & sua ket noi API cho dien thoai cam USB
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) { Write-Host "[X] Khong thay adb.exe - cai 'Android SDK Platform-Tools' trong SDK Manager" -ForegroundColor Red; exit 1 }

Write-Host "`n== 1. Thiet bi USB ==" -ForegroundColor Cyan
& $adb devices

Write-Host "`n== 2. adb reverse ==" -ForegroundColor Cyan
& $adb reverse tcp:8080 tcp:8080
& $adb reverse --list

Write-Host "`n== 3. Docker containers ==" -ForegroundColor Cyan
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

Write-Host "`n== 4. Gateway localhost:8080 ==" -ForegroundColor Cyan
$ok = Test-NetConnection -ComputerName localhost -Port 8080 -WarningAction SilentlyContinue
if ($ok.TcpTestSucceeded) { Write-Host "[OK] Cong 8080 dang mo" -ForegroundColor Green }
else { Write-Host "[X] Khong co gi chay o cong 8080 -> vao thu muc be chay: docker compose up -d" -ForegroundColor Red }
