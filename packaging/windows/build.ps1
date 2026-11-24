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

    # The GUI can belong to a child of the native launcher. Check Swing's opened event
    # inside that JVM. The probe lives in the temporary build tree only.
    $classes = New-Item -ItemType Directory -Path "$work/probe-classes"
    & "$Jdk/bin/javac.exe" --release 8 -encoding UTF-8 -d $classes.FullName packaging/windows/WindowProbe.java
    if ($LASTEXITCODE -ne 0) { throw 'Cannot compile the window probe.' }
    Set-Content "$work/probe.mf" "Premain-Class: WindowProbe" -Encoding ascii
    & "$Jdk/bin/jar.exe" cfm "$work/probe.jar" "$work/probe.mf" -C $classes.FullName .
    if ($LASTEXITCODE -ne 0) { throw 'Cannot package the window probe.' }

    $ready = Join-Path $work 'opened.txt'
    $start = [Diagnostics.ProcessStartInfo]::new("$work/image/HiJson/HiJson.exe")
    $start.UseShellExecute = $false
    $start.WorkingDirectory = $work
    $start.EnvironmentVariables['JAVA_TOOL_OPTIONS'] = '"-javaagent:' + "$work/probe.jar=$ready" + '"'
    $process = [Diagnostics.Process]::Start($start)
    try {
        if (-not $process.WaitForExit(60000) -or $process.ExitCode -ne 0 -or -not (Test-Path $ready)) {
            & "$Jdk/bin/jcmd.exe" -l
            throw 'The packaged EXE did not show the HiJson tree and graph window.'
        }
        Write-Host "Windows GUI opened with both editor modes: $(Get-Content $ready -Raw)"
    } finally {
        if (-not $process.HasExited) {
            & taskkill.exe /PID $process.Id /T /F | Out-Null
            $process.WaitForExit()
        }
        $process.Dispose()
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
