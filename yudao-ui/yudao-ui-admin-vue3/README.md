# 酒店餐饮运营平台管理端

Vue 3 管理端，承载平台运营后台与商户经营后台。

## 本地运行

```powershell
pnpm install
pnpm dev
```

## 检查与构建

```powershell
pnpm run ts:check
pnpm build:prod
```

生产构建输出位于 `dist-prod`。项目统一构建和交付优先使用仓库根目录的 `script/build-package.ps1`。

## 主要目录

- `src/views/saas/platform`：平台运营后台
- `src/views/saas/merchant`：商户经营后台
- `src/api/saas`：酒店、餐饮、订单、财务等业务接口
- `src/router/modules`：固定路由与访问入口

验收环境仅保留当前酒店餐饮业务所需代码，不再包含上游框架演示页面。
