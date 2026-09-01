[CmdletBinding()]
param(
    [string] $TargetRepo = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'
$targetRoot = (Resolve-Path -LiteralPath $TargetRepo).Path.TrimEnd('\')

if ((Split-Path -Leaf $targetRoot) -ne 'hotel-pms-community') {
    throw "Refusing to prune unexpected target: $targetRoot"
}
if (-not (Test-Path -LiteralPath (Join-Path $targetRoot '.git')) -or
    -not (Test-Path -LiteralPath (Join-Path $targetRoot 'SOURCE_REVISION'))) {
    throw 'Target is not an initialized Hotel PMS Community export.'
}

$relativePaths = @(
    'yudao-server/src/main/java/cn/iocoder/yudao/server/controller/openapi/saas',
    'yudao-server/src/main/java/cn/iocoder/yudao/server/controller/app/saasjd',
    'yudao-server/src/main/java/cn/iocoder/yudao/server/controller/admin/saas/platform',
    'yudao-server/src/main/java/cn/iocoder/yudao/server/controller/admin/saas/settlement',
    'yudao-server/src/main/java/cn/iocoder/yudao/server/controller/admin/saas/merchant/SaasMerchantWorkbenchController.java',
    'yudao-server/src/main/java/cn/iocoder/yudao/server/controller/admin/saas/merchant/SaasMerchantOrderRefundController.java',
    'yudao-server/src/test/java/cn/iocoder/yudao/server/controller/openapi/saas',
    'yudao-server/src/test/java/cn/iocoder/yudao/server/controller/app/saasjd',
    'yudao-server/src/test/java/cn/iocoder/yudao/server/controller/admin/saas/platform',
    'yudao-server/src/test/java/cn/iocoder/yudao/server/controller/admin/saas/settlement',
    'yudao-server/src/test/java/cn/iocoder/yudao/server/controller/admin/saas/merchant/SaasMerchantWorkbenchControllerTest.java',
    'yudao-server/src/test/java/cn/iocoder/yudao/server/controller/admin/saas/merchant/SaasMerchantOrderRefundControllerTest.java',
    'yudao-module-merchant/src/main/java/cn/iocoder/yudao/module/merchant/controller/admin/vo/MerchantAuditReqVO.java',
    'yudao-module-merchant/src/main/java/cn/iocoder/yudao/module/merchant/controller/admin/vo/MerchantCreateReqVO.java',
    'yudao-module-merchant/src/main/java/cn/iocoder/yudao/module/merchant/controller/admin/vo/MerchantHomeDisplayReqVO.java',
    'yudao-module-merchant/src/main/java/cn/iocoder/yudao/module/merchant/controller/admin/vo/MerchantPageReqVO.java',
    'yudao-module-merchant/src/test/java/cn/iocoder/yudao/module/merchant/controller/admin/MerchantControllerTest.java',
    'yudao-module-merchant/src/test/java/cn/iocoder/yudao/module/merchant/controller/admin/vo/MerchantRequestValidationTest.java',
    'yudao-module-merchant/src/test/java/cn/iocoder/yudao/module/merchant/service/MerchantPaymentAccountTest.java',
    'yudao-module-merchant/src/test/java/cn/iocoder/yudao/module/merchant/service/MerchantServiceImplTest.java',
    'yudao-module-merchant/src/test/resources/sql/merchant_payment_account.sql',
    'yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/controller/admin/openapi',
    'yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/dal/dataobject/openapi',
    'yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/dal/mysql/openapi',
    'yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/service/openapi',
    'yudao-module-system/src/test/java/cn/iocoder/yudao/module/system/service/openapi',
    'yudao-ui/yudao-ui-admin-vue3/src/views/saas/platform',
    'yudao-ui/yudao-ui-admin-vue3/src/views/saas/merchant/workbench',
    'yudao-ui/yudao-ui-admin-vue3/src/views/saas/merchant/dining',
    'yudao-ui/yudao-ui-admin-vue3/src/views/saas/merchant/dining-reservation',
    'yudao-ui/yudao-ui-admin-vue3/src/views/saas/merchant/table',
    'yudao-ui/yudao-ui-admin-vue3/src/views/saas/merchant/finance',
    'yudao-ui/yudao-ui-admin-vue3/src/views/saas/merchant/merchantAccess.ts',
    'yudao-ui/yudao-ui-admin-vue3/src/views/saas/components',
    'yudao-ui/yudao-ui-admin-vue3/src/views/saas/composables',
    'yudao-ui/yudao-ui-admin-vue3/src/views/saas/configs.ts',
    'yudao-ui/yudao-ui-admin-vue3/src/views/saas/README.md',
    'yudao-ui/yudao-ui-admin-vue3/src/views/pay',
    'yudao-ui/yudao-ui-admin-vue3/src/api/pay',
    'yudao-ui/yudao-ui-admin-vue3/src/api/saas/dining',
    'yudao-ui/yudao-ui-admin-vue3/src/api/saas/settlement',
    'yudao-ui/yudao-ui-admin-vue3/src/api/saas/openapi',
    'yudao-ui/yudao-ui-admin-vue3/src/api/saas/platform',
    'yudao-ui/yudao-ui-admin-vue3/src/api/saas/appHome',
    'yudao-ui/yudao-ui-admin-vue3/scripts/fixtures'
)

$scriptPatterns = @(
    'cth-*.mjs',
    'dining-*.mjs',
    'merchant-payment-account-check.mjs',
    'job-admin-check.mjs',
    'job-admin-browser-check.mjs'
)
$scriptsRoot = Join-Path $targetRoot 'yudao-ui/yudao-ui-admin-vue3/scripts'
foreach ($pattern in $scriptPatterns) {
    Get-ChildItem -LiteralPath $scriptsRoot -Filter $pattern -File -ErrorAction SilentlyContinue |
        ForEach-Object { $relativePaths += $_.FullName.Substring($targetRoot.Length + 1) }
}

$removed = @()
foreach ($relativePath in $relativePaths | Select-Object -Unique) {
    $candidate = Join-Path $targetRoot $relativePath
    if (-not (Test-Path -LiteralPath $candidate)) {
        continue
    }

    $resolved = (Resolve-Path -LiteralPath $candidate).Path
    if (-not $resolved.StartsWith($targetRoot + '\', [StringComparison]::OrdinalIgnoreCase)) {
        throw "Resolved path escapes target repository: $resolved"
    }

    Remove-Item -LiteralPath $resolved -Recurse -Force
    $removed += $relativePath.Replace('\', '/')
}

Write-Host "Pruned $($removed.Count) commercial paths from $targetRoot"
$removed | Sort-Object | ForEach-Object { Write-Host "  $_" }
