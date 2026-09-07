@echo off
setlocal

echo Releasing Pchinese development ports: 8080 and 3000...

powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$ports = 8080,3000; $listeners = Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue | Where-Object { $ports -contains $_.LocalPort } | Select-Object -ExpandProperty OwningProcess -Unique; if (-not $listeners) { Write-Host 'No process is listening on ports 8080 or 3000.'; exit 0 }; foreach ($processId in $listeners) { try { $process = Get-Process -Id $processId -ErrorAction Stop; Write-Host ('Stopping {0} (PID {1})' -f $process.ProcessName, $processId); Stop-Process -Id $processId -Force -ErrorAction Stop } catch { Write-Warning ('Could not stop PID {0}: {1}' -f $processId, $_.Exception.Message) } }"

echo.
echo Ports have been released. You can now run run.bat.
endlocal
