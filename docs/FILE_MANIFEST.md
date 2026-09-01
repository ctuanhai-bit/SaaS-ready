# 文件抽离清单

## 1. 导出原则

- 来源固定到一个提交，不读取当前脏工作区。
- 使用白名单复制，不以黑名单方式复制整个仓库。
- 二进制图片、压缩包、密钥和生产文档默认不复制。
- 复制完成后必须再次按禁止词和密钥特征扫描。

## 2. 第一阶段白名单

| 路径 | 处理 | 说明 |
| --- | --- | --- |
| `pom.xml` | 复制后修改 | 仅保留社区模块 |
| `LICENSE` | 复制 | 保留上游 MIT 声明 |
| `BASELINE_SOURCE.md` | 复制 | 保留来源与提交信息 |
| `.gitignore`、`lombok.config` | 复制 | 基础工程文件 |
| `yudao-dependencies/` | 复制 | Maven 依赖版本 |
| `yudao-framework/` | 复制 | 通用框架 |
| `yudao-module-system/` | 复制后裁剪装配 | 用户、角色、租户、菜单 |
| `yudao-module-infra/` | 复制后裁剪装配 | 文件、配置、任务、日志 |
| `yudao-module-merchant/` | 复制后改名义 | 酒店资料与酒店上下文 |
| `yudao-module-booking/` | 复制后重构应用层 | 酒店领域核心 |
| `yudao-server/` | 复制后重写 POM/配置 | 社区版启动模块与本地/生产配置 |
| `yudao-ui/yudao-ui-admin-vue3/` | 复制后裁剪 | 管理端基础与酒店页面 |
| `compose.production.yaml`、`.env.production.example` | 社区版新增 | 无凭据生产编排与配置模板 |
| `deploy/` | 社区版新增 | 后端/管理端镜像、Nginx 与幂等初始化脚本 |
| `docs/PRODUCTION_DEPLOYMENT.md` | 社区版新增 | 部署、备份、升级、回退与 TLS 指南 |

## 3. 明确排除

- `yudao-module-dining/`
- `yudao-module-member/`
- `yudao-module-pay/`
- `yudao-module-settlement/`
- `yudao-ui/saas-jd-uniapp/`
- 商业生产凭据、真实服务器地址和品牌专属运维脚本
- `script/miniapp-upload/`
- `script/openapi-acceptance/`
- `docs/acceptance/`
- `docs/delivery/`
- `docs/archive/`
- `docs/architecture/image-cdn.md`
- `sql/mysql/saas-jd-wechat-*`
- `sql/mysql/saas-jd-openapi-*`
- `sql/mysql/saas-jd-platform-*`
- `sql/mysql/saas-jd-pay-*`
- 构建产物、备份、日志、证书、密钥、真实图片和生产数据库文件

## 4. 复制后删除

管理端整体复制用于保证基础框架可构建，随后删除以下业务目录：

- `src/views/saas/platform/`
- `src/views/saas/merchant/dining/`
- `src/views/saas/merchant/dining-reservation/`
- `src/views/saas/merchant/table/`
- `src/views/saas/merchant/finance/`
- 对应 `src/api/saas/` 下的平台、餐饮、支付和结算 API
- AI、即时通讯、会员、支付、工作流、IoT 和小程序装修专属媒体目录
- 旧产品 Logo、微信图标及未引用的品牌资源
- 商业验收与生产浏览器检查脚本

## 5. 禁止词与特征

发布门禁至少检查：

- 生产域名与历史域名
- 生产服务器 IPv4
- 微信小程序 AppID 与商户号
- `/opt/`、`/etc/` 下的生产专属路径
- `BEGIN PRIVATE KEY`、`apiclient_key.pem`、`APIv3` 实际值
- 真实酒店品牌、租户编号、手机号、身份证号
- `.pem`、`.p12`、`.pfx`、`.jks`、`.key` 文件
