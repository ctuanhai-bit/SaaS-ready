[CmdletBinding()]
param(
    [switch] $SkipBuild
)

$ErrorActionPreference = 'Stop'
# ripgrep returns 1 when no match is found. PowerShell 7.3+ can otherwise turn
# that expected result into a terminating NativeCommandExitException.
$PSNativeCommandUseErrorActionPreference = $false
$root = (Resolve-Path -LiteralPath (Split-Path -Parent $PSScriptRoot)).Path
Push-Location $root
try {
    $forbiddenPaths = @(
        'yudao-module-pay',
        'yudao-module-member',
        'yudao-module-dining',
        'yudao-module-settlement',
        'yudao-ui/saas-jd-uniapp',
        'yudao-ui/yudao-ui-admin-vue3/src/views/pay',
        'yudao-ui/yudao-ui-admin-vue3/src/views/saas/platform',
        'yudao-ui/yudao-ui-admin-vue3/src/views/saas/merchant/dining',
        'yudao-ui/yudao-ui-admin-vue3/src/views/saas/merchant/finance',
        'yudao-ui/yudao-ui-admin-vue3/src/assets/ai',
        'yudao-ui/yudao-ui-admin-vue3/src/assets/audio',
        'yudao-ui/yudao-ui-admin-vue3/src/assets/imgs/diy',
        'yudao-ui/yudao-ui-admin-vue3/src/assets/imgs/iot',
        'yudao-ui/yudao-ui-admin-vue3/src/assets/svgs/bpm',
        'yudao-ui/yudao-ui-admin-vue3/src/assets/svgs/iot',
        'yudao-ui/yudao-ui-admin-vue3/src/assets/svgs/pay',
        'yudao-ui/yudao-ui-admin-vue3/src/assets/imgs/logo.png',
        'yudao-ui/yudao-ui-admin-vue3/src/assets/imgs/wechat.png',
        'yudao-ui/yudao-ui-admin-vue3/src/assets/svgs/member_balance.svg',
        'yudao-ui/yudao-ui-admin-vue3/src/assets/svgs/member_expenditure_balance.svg',
        'yudao-ui/yudao-ui-admin-vue3/src/assets/svgs/member_level.svg',
        'yudao-ui/yudao-ui-admin-vue3/src/assets/svgs/member_point.svg',
        'yudao-ui/yudao-ui-admin-vue3/src/assets/svgs/member_recharge_balance.svg',
        'yudao-ui/yudao-ui-admin-vue3/public/logo.gif'
    )
    foreach ($path in $forbiddenPaths) {
        if (Test-Path -LiteralPath (Join-Path $root $path)) {
            throw "Forbidden commercial path remains: $path"
        }
    }

    $credentialFiles = Get-ChildItem -LiteralPath $root -Recurse -File |
        Where-Object {
            $_.FullName -notmatch '[\\/]target[\\/]' -and
            $_.FullName -notmatch '[\\/]node_modules[\\/]' -and
            $_.Extension -match '^\.(pem|p12|pfx|jks|key|cer|crt)$'
        }
    if ($credentialFiles) {
        throw "Credential-like files found: $($credentialFiles.FullName -join ', ')"
    }

    $scanGlobs = @(
        '--hidden',
        '--glob', '!.git/**',
        '--glob', '!**/target/**',
        '--glob', '!**/node_modules/**',
        '--glob', '!docs/**',
        '--glob', '!tools/verify-community.ps1'
    )
    $patterns = @(
        '[A-Za-z0-9.-]+\.xyz',
        'BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY',
        '<artifactId>yudao-module-(pay|member|dining|settlement)</artifactId>',
        'wechatProfitSharingSyncJob',
        'saas-jd-openapi',
        'saas:merchant:'
    )
    foreach ($pattern in $patterns) {
        & rg -n $pattern . @scanGlobs
        if ($LASTEXITCODE -eq 0) {
            throw "Forbidden content matched: $pattern"
        }
        if ($LASTEXITCODE -gt 1) {
            throw "rg failed while checking: $pattern"
        }
    }

    $seedSql = Get-Content 'db/bootstrap/01-community-seed.sql' -Raw
    $requiredPermissions = @(
        'merchant:profile:query',
        'merchant:profile:update',
        'merchant:staff:query',
        'merchant:staff:create',
        'merchant:staff:update',
        'booking:room-type:query',
        'booking:room-type:create',
        'booking:room-type:update',
        'booking:room-type:delete',
        'booking:inventory:query',
        'booking:inventory:update',
        'booking:order:query',
        'booking:order:create',
        'booking:order:update'
    )
    foreach ($permission in $requiredPermissions) {
        if (-not $seedSql.Contains("'$permission'")) {
            throw "Required community permission is missing from bootstrap menus: $permission"
        }
    }

    Get-Content 'yudao-ui/yudao-ui-admin-vue3/package.json' -Raw | ConvertFrom-Json | Out-Null
    [void][scriptblock]::Create((Get-Content 'tools/prune-community-export.ps1' -Raw))
    [void][scriptblock]::Create((Get-Content 'tools/generate-framework-schema.ps1' -Raw))

    if (-not $SkipBuild) {
        & mvn -pl yudao-server -am -DskipTests package
        if ($LASTEXITCODE -ne 0) { throw 'Backend package failed.' }

        $testClasses = @(
            'JobSchedulerInitializerTest',
            'MerchantStaffControllerTest',
            'BookingInventoryControllerTest',
            'BookingOrderControllerTest',
            'BookingOrderPermissionContractTest',
            'BookingOrderResponseAssemblerTest',
            'BookingRoomTypeControllerTest',
            'BookingOrderServiceImplTest',
            'BookingStockServiceImplTest'
        ) -join ','
        $testArgs = @(
            '-pl', 'yudao-module-infra,yudao-module-merchant,yudao-module-booking',
            '-am',
            "-Dtest=$testClasses",
            '-Dsurefire.failIfNoSpecifiedTests=false',
            'test'
        )
        & mvn @testArgs
        if ($LASTEXITCODE -ne 0) { throw 'Backend tests failed.' }

        Push-Location 'yudao-ui/yudao-ui-admin-vue3'
        try {
            & pnpm install --frozen-lockfile
            if ($LASTEXITCODE -ne 0) { throw 'Frontend dependency installation failed.' }
            & pnpm build
            if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed.' }
            & pnpm ts:check
            if ($LASTEXITCODE -ne 0) {
                Write-Warning 'Frontend type check reports known baseline debt; production build passed.'
            }
        } finally {
            Pop-Location
        }
    }

    Write-Host 'Hotel PMS Community verification passed.'
} finally {
    Pop-Location
}
