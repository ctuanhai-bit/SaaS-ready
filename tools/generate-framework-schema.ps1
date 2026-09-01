[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string] $SourceBaseline,
    [string] $OutputFile = (Join-Path (Split-Path -Parent $PSScriptRoot) 'db/bootstrap/00-framework-schema.sql')
)

$ErrorActionPreference = 'Stop'
$source = (Resolve-Path -LiteralPath $SourceBaseline).Path
$targetRoot = (Resolve-Path -LiteralPath (Split-Path -Parent $PSScriptRoot)).Path.TrimEnd('\')
$output = [System.IO.Path]::GetFullPath($OutputFile)
if (-not $output.StartsWith($targetRoot + '\', [StringComparison]::OrdinalIgnoreCase)) {
    throw "Output must stay inside the community repository: $output"
}

$lines = Get-Content -LiteralPath $source
$result = [System.Collections.Generic.List[string]]::new()
$result.Add('-- Generated from the pinned upstream framework baseline.')
$result.Add('-- Contains table definitions only; all upstream records are intentionally excluded.')
$result.Add('SET NAMES utf8mb4;')
$result.Add('SET FOREIGN_KEY_CHECKS = 0;')
$result.Add('')

$capturing = $false
foreach ($line in $lines) {
    if (-not $capturing -and $line -match '^DROP TABLE IF EXISTS `(infra_|system_)[^`]+`;') {
        $capturing = $true
    }
    if (-not $capturing) {
        continue
    }
    $result.Add($line)
    if ($line -match '^\) ENGINE = InnoDB .*;$') {
        $result.Add('')
        $capturing = $false
    }
}

if ($capturing) {
    throw 'Source baseline ended while parsing a table definition.'
}
$tableCount = ($result | Where-Object { $_ -match '^CREATE TABLE `(infra_|system_)' }).Count
if ($tableCount -lt 40) {
    throw "Unexpected framework table count: $tableCount"
}

$result.Add('SET FOREIGN_KEY_CHECKS = 1;')
$directory = Split-Path -Parent $output
New-Item -ItemType Directory -Path $directory -Force | Out-Null
[System.IO.File]::WriteAllLines($output, $result, [System.Text.UTF8Encoding]::new($false))
Write-Host "Generated $tableCount framework table definitions at $output"
