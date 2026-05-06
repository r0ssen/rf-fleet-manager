$ErrorActionPreference = 'Stop'

# Prefer an installed JDK 21 and fail fast if none is found.
$jdk = Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -like 'jdk-21*' } |
    Sort-Object Name -Descending |
    Select-Object -First 1

if (-not $jdk) {
    throw 'JDK 21 not found under C:\Program Files\Eclipse Adoptium. Install Eclipse Temurin 21 first.'
}

$env:JAVA_HOME = $jdk.FullName
$pathParts = $env:Path -split ';' | Where-Object {
    $_ -and ($_ -notmatch 'Java\\jdk') -and ($_ -notmatch 'Eclipse Adoptium\\jdk')
}
$env:Path = "$env:JAVA_HOME\bin;" + ($pathParts -join ';')

$mvnCmd = 'C:\Program Files\apache-maven-3.9.11\bin\mvn.cmd'
if (-not (Test-Path $mvnCmd)) {
    throw "Maven not found at $mvnCmd. Update scripts/use-jdk21.ps1 to your Maven path."
}

Set-Alias mvn $mvnCmd -Scope Global

Write-Host "JAVA_HOME=$env:JAVA_HOME"
java -version
& $mvnCmd -v

