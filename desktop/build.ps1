param([string]$Output = (Join-Path $PSScriptRoot '../artifacts/windows'))
$ErrorActionPreference = 'Stop'
dotnet publish (Join-Path $PSScriptRoot 'SportsWave.csproj') -c Release -r win-x64 --self-contained true -p:UseSharedCompilation=false -m:1 -o $Output
if ($LASTEXITCODE -ne 0) { throw 'Windows build failed.' }
Get-FileHash (Join-Path $Output 'SportsWave.exe') -Algorithm SHA256
