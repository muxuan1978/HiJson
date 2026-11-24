# Build a portable Windows x64 application with the runtime from the selected JDK.
[CmdletBinding()]
param([string]$Jdk = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
if (-not $Jdk -or -not (Test-Path "$Jdk/bin/jpackage.exe")) {
    throw 'Set JAVA_HOME to a Windows x64 JDK 17 before building.'
}
$root = (Resolve-Path "$PSScriptRoot/../..").Path
$work = Join-Path ([IO.Path]::GetTempPath()) ("hijson-" + [guid]::NewGuid())
New-Item -ItemType Directory -Path $work | Out-Null
Push-Location $root
try {
    $properties = Get-Content src/hi/chyl/json/resources/MainApp.properties -Raw
    $version = [regex]::Match($properties, '(?m)^Application\.version\s*=\s*V(\d+\.\d+\.\d+)\s*$').Groups[1].Value
    if (-not $version) { throw 'Cannot read Application.version.' }

    & ant "-Dplatforms.JDK_1.7.home=$Jdk" "-Dbuild.dir=$work/build" "-Ddist.dir=$work/dist" standalone
    if ($LASTEXITCODE -ne 0) { throw 'Ant compilation failed.' }
    & "$Jdk/bin/jpackage.exe" --type app-image --name HiJson --app-version $version `
        --input "$work/dist/standalone" --main-jar HiJson.jar --main-class hi.chyl.json.MainApp `
        --icon packaging/windows/HiJson.ico --dest "$work/image" `
        --add-modules java.se,jdk.unsupported,jdk.charsets,jdk.localedata --java-options "-Dfile.encoding=UTF-8"
    if ($LASTEXITCODE -ne 0) { throw 'jpackage failed.' }
    Copy-Item LICENSE "$work/image/HiJson/LICENSE.txt"

    # Exercise the actual Windows GUI launcher before archiving its runtime.
    $process = Start-Process "$work/image/HiJson/HiJson.exe" -WorkingDirectory $work -PassThru
    try {
        $deadline = [DateTime]::UtcNow.AddSeconds(60)
        do {
            Start-Sleep -Milliseconds 300
            $process.Refresh()
            if ($process.HasExited) { throw "HiJson.exe exited with code $($process.ExitCode)." }
        } until ($process.MainWindowHandle -ne 0 -or [DateTime]::UtcNow -gt $deadline)
        if ($process.MainWindowHandle -eq 0) { throw 'HiJson.exe did not open a window.' }
        Write-Host "Windows GUI started: $($process.MainWindowTitle)"
    } finally {
        if (-not $process.HasExited) { Stop-Process -Id $process.Id }
        $process.WaitForExit()
    }

    $archive = 'HiJson-Windows-x64.zip'
    & tar.exe -a -cf "$work/$archive" -C "$work/image" HiJson
    if ($LASTEXITCODE -ne 0) { throw 'ZIP packaging failed.' }
    New-Item -ItemType Directory -Path dist -Force | Out-Null
    Move-Item "$work/$archive" "dist/$archive" -Force
    Get-FileHash "dist/$archive" -Algorithm SHA256
} finally {
    Pop-Location
    Remove-Item -LiteralPath $work -Recurse -Force
}
