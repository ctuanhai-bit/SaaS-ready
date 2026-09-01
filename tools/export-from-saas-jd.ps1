[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string] $SourceRepo,
    [string] $SourceRef = 'origin/main',
    [string] $TargetRepo = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'

$sourceRoot = (Resolve-Path -LiteralPath $SourceRepo).Path
$targetRoot = (Resolve-Path -LiteralPath $TargetRepo).Path

if ($sourceRoot -eq $targetRoot) {
    throw 'SourceRepo and TargetRepo must be different directories.'
}

if (-not (Test-Path -LiteralPath (Join-Path $sourceRoot '.git'))) {
    throw "SourceRepo is not a Git repository: $sourceRoot"
}

$sourceCommit = (& git -C $sourceRoot rev-parse "$SourceRef^{commit}").Trim()
if ($LASTEXITCODE -ne 0 -or -not $sourceCommit) {
    throw "Cannot resolve source ref: $SourceRef"
}

$exportPaths = @(
    '.gitignore',
    'lombok.config',
    'pom.xml',
    'LICENSE',
    'BASELINE_SOURCE.md',
    'yudao-dependencies',
    'yudao-framework',
    'yudao-module-system',
    'yudao-module-infra',
    'yudao-module-merchant',
    'yudao-module-booking',
    'yudao-server',
    'yudao-ui/yudao-ui-admin-vue3'
)

$managedTargets = @(
    'yudao-dependencies',
    'yudao-framework',
    'yudao-module-system',
    'yudao-module-infra',
    'yudao-module-merchant',
    'yudao-module-booking',
    'yudao-server',
    'yudao-ui'
)

$existingTargets = $managedTargets | Where-Object {
    Test-Path -LiteralPath (Join-Path $targetRoot $_)
}
if ($existingTargets) {
    throw "Refusing to overwrite an existing export: $($existingTargets -join ', ')"
}

$archivePath = Join-Path ([System.IO.Path]::GetTempPath()) "hotel-pms-source-$sourceCommit.tar"
try {
    & git -C $sourceRoot archive --format=tar --output=$archivePath $sourceCommit -- @exportPaths
    if ($LASTEXITCODE -ne 0) {
        throw 'git archive failed.'
    }

    & tar -xf $archivePath -C $targetRoot
    if ($LASTEXITCODE -ne 0) {
        throw 'tar extraction failed.'
    }
} finally {
    if (Test-Path -LiteralPath $archivePath) {
        Remove-Item -LiteralPath $archivePath -Force
    }
}

$revision = @(
    "source_repository=internal-source:SaaS_JD"
    "source_ref=$SourceRef"
    "source_commit=$sourceCommit"
    "exported_at_utc=$([DateTime]::UtcNow.ToString('o'))"
) -join [Environment]::NewLine
Set-Content -LiteralPath (Join-Path $targetRoot 'SOURCE_REVISION') -Value $revision -Encoding utf8

Write-Host "Exported SaaS_JD $sourceCommit to $targetRoot"
