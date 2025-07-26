## Debug WSL launch errors
- restart WSL: `Get-Service WSLService | Restart-Service`
- obtain logs: 
```ps1
Invoke-WebRequest -Uri https://raw.githubusercontent.com/microsoft/WSL/master/diagnostics/collect-wsl-logs.ps1 -OutFile collect-wsl-logs.ps1
.\collect-wsl-logs.ps1
```

## Manage resources allocated to WSL
- memory allocation: https://superuser.com/questions/1707758/wsl-2-does-not-have-all-memory-available-to-it	
