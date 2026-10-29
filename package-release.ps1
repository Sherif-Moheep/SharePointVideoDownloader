<#
.SYNOPSIS
    Builds and packages release assets for SharePoint Video Downloader.
.DESCRIPTION
    1. Ensures a working JDK is detected for Gradle.
    2. Compiles the desktop app distributable using Gradle (unless -SkipBuild or -ExtensionOnly is passed).
    3. Builds the Inno Setup installer (if iscc is available).
    4. Packages the clean Chrome extension into a .zip using .NET ZipFile.
    5. Gathers all final assets in a single 'release/' folder ready for GitHub.
.EXAMPLE
    .\package-release.ps1
.EXAMPLE
    .\package-release.ps1 -SkipBuild
.EXAMPLE
    .\package-release.ps1 -ExtensionOnly
#>

[CmdletBinding()]
param(
    [switch]$SkipBuild,
    [switch]$ExtensionOnly
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

Add-Type -AssemblyName System.IO.Compression.FileSystem

$rootDir = $PSScriptRoot
$desktopDir = Join-Path $rootDir "desktop-app"
$installerScript = Join-Path $rootDir "installer\setup.iss"
$extensionDir = Join-Path $rootDir "chrome-extension"
$releaseDir = Join-Path $rootDir "release"

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host " SharePoint Video Downloader - Release Packaging  " -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

# 0. Ensure a valid JDK is configured
if (-not $env:JAVA_HOME -or -not (Test-Path $env:JAVA_HOME)) {
    $jdkCandidates = @(
        "C:\Program Files\Java\jdk-24",
        "C:\Program Files\Java\jdk-21",
        "C:\Program Files\Java\jdk-17"
    )
    $foundJdk = $jdkCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1
    if (-not $foundJdk) {
        $foundJdk = Get-ChildItem "C:\Program Files\Java" -Directory -ErrorAction SilentlyContinue | 
            Sort-Object Name -Descending | 
            Select-Object -First 1 -ExpandProperty FullName
    }
    if ($foundJdk) {
        $env:JAVA_HOME = $foundJdk
        $env:Path = "$foundJdk\bin;$env:Path"
        Write-Host "  Detected JDK at: $foundJdk" -ForegroundColor Gray
    }
}

# 1. Ensure release output directory exists
if (-not (Test-Path $releaseDir)) {
    New-Item -ItemType Directory -Path $releaseDir | Out-Null
}

# 2. Build Desktop Distributable via Gradle
if (-not $SkipBuild -and -not $ExtensionOnly) {
    Write-Host "`n[1/3] Building desktop application via Gradle..." -ForegroundColor Yellow
    Push-Location $desktopDir
    try {
        & .\gradlew.bat :composeApp:createDistributable
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle build failed with exit code $LASTEXITCODE"
        }
    } finally {
        Pop-Location
    }
    Write-Host "  -> Desktop binaries successfully generated!" -ForegroundColor Green
} else {
    Write-Host "`n[1/3] Skipping desktop Gradle build." -ForegroundColor DarkGray
}

# 3. Compile Inno Setup Installer
if (-not $ExtensionOnly) {
    Write-Host "`n[2/3] Compiling Inno Setup installer..." -ForegroundColor Yellow

    $isccCandidates = @(
        (Get-Command iscc.exe -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source -ErrorAction SilentlyContinue),
        "C:\Program Files (x86)\Inno Setup 6\iscc.exe",
        "C:\Program Files\Inno Setup 6\iscc.exe",
        "$env:LOCALAPPDATA\Programs\Inno Setup 6\iscc.exe"
    )

    $isccPath = $isccCandidates | Where-Object { $_ -and (Test-Path $_) } | Select-Object -First 1

    if ($isccPath) {
        Write-Host "  Found Inno Setup at: $isccPath" -ForegroundColor Gray
        & $isccPath "$installerScript" "/O`"$releaseDir`""
        if ($LASTEXITCODE -ne 0) {
            throw "Inno Setup compilation failed with exit code $LASTEXITCODE"
        }
        Write-Host "  -> Installer generated in release/!" -ForegroundColor Green
    } else {
        Write-Host "  [!] iscc.exe (Inno Setup CLI) not found in standard paths." -ForegroundColor DarkYellow
        Write-Host "      Please open installer\setup.iss in Inno Setup GUI and compile it manually," -ForegroundColor DarkYellow
        Write-Host "      then copy the output .exe into the release\ folder." -ForegroundColor DarkYellow
    }
}

# 4. Package Chrome Extension Zip
Write-Host "`n[3/3] Packaging Chrome Extension (.zip)..." -ForegroundColor Yellow
$extensionZip = Join-Path $releaseDir "SharePointDownloader_Extension.zip"
$tempExtDir = Join-Path $env:TEMP "spd_ext_build"

if (Test-Path $tempExtDir) {
    Remove-Item $tempExtDir -Recurse -Force
}
New-Item -ItemType Directory -Path $tempExtDir | Out-Null

try {
    # Copy only clean extension runtime files (exclude IDE files like .iml, jsconfig.json)
    Copy-Item (Join-Path $extensionDir "manifest.json") $tempExtDir
    Copy-Item (Join-Path $extensionDir "background.js") $tempExtDir
    Copy-Item (Join-Path $extensionDir "images") $tempExtDir -Recurse

    if (Test-Path $extensionZip) {
        Remove-Item $extensionZip -Force
    }

    [System.IO.Compression.ZipFile]::CreateFromDirectory($tempExtDir, $extensionZip)
    Write-Host "  -> Extension zip successfully packaged!" -ForegroundColor Green
} finally {
    if (Test-Path $tempExtDir) {
        Remove-Item $tempExtDir -Recurse -Force
    }
}

# Summary
Write-Host "`n==================================================" -ForegroundColor Cyan
Write-Host " Packaging Complete! Release Files in:            " -ForegroundColor Green
Write-Host " $releaseDir" -ForegroundColor White
Write-Host "==================================================" -ForegroundColor Cyan

Get-ChildItem -Path $releaseDir | ForEach-Object {
    $sizeMb = [math]::Round($_.Length / 1MB, 2)
    Write-Host "  * $($_.Name) ($sizeMb MB)" -ForegroundColor White
}
Write-Host "`nYou can drag and drop these files directly into GitHub Releases!`n" -ForegroundColor Cyan
