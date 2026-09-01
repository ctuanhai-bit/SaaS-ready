# Hotel PMS Community

Hotel PMS Community is an open-source hotel operations core extracted from a
stable commercial hotel platform. It focuses on hotel profile, room types,
daily inventory and price, booking orders, and front-desk fulfillment.

The community repository intentionally excludes platform onboarding, dining,
hotel guest/member accounts, WeChat mini-programs, third-party payment,
payment-channel refunds, commission, profit sharing, settlement, withdrawals,
production deployment and credentials.

## Status

This repository is the first independent community extraction candidate.
The source snapshot is pinned in `SOURCE_REVISION`; the public boundary and
acceptance evidence are documented under `docs/`.

## Features

- Tenant-isolated hotel profile and staff accounts
- Room type, physical room, facility and image metadata
- Daily inventory and nightly price calendar
- Multi-night availability validation and per-night inventory locks
- Idempotent order submission and tested payment-timeout release logic
- Order confirmation, cancellation, room assignment, check-in, check-out and no-show
- Explicit offline receipt and refund registration for payment-free deployments
- Front-desk arrivals, in-house, departures and order search
- User, role, menu, file and operation-log infrastructure
- Database-backed development file storage
- Docker Compose development environment

## Not Included

- Multi-merchant platform onboarding or review
- Dining reservations and packages
- Hotel guest/member center or configured WeChat identity
- WeChat Pay, service-provider payments, callbacks or certificates
- Automated refunds, commission, profit sharing, settlement or withdrawal
- Public OpenAPI application management
- Mini-program source, AppID or upload credentials
- Commercial production scripts, domains, hosts, backups or certificates

The management console can register an order as paid or refunded only after the
operator has completed that action outside the system. These commands never move
money, create a payment order, invoke a refund API or prove that funds changed
hands. Real payment integrations belong in a separately reviewed adapter.

## Architecture

```text
yudao-system / yudao-infra
            ^
            |
yudao-module-merchant      current hotel and tenant context
            ^
            |
yudao-module-booking       room, inventory, order and front desk
            ^
            |
yudao-server               Spring Boot assembly

yudao-ui/yudao-ui-admin-vue3
                            Vue 3 management console
```

The allowed dependency direction is `system/infra <- merchant <- booking <-
server`. No generic payment adapter contract is published in this MVP. A future
commercial adapter must use a reviewed public contract and must never make the
community core depend on a commercial module.

## Requirements

- Temurin/OpenJDK 8 or 17 (recommended; the project targets Java 8 bytecode)
- Maven 3.8+
- Node.js 20.19+
- pnpm 10.x
- MySQL 8 and Redis 7, or Docker with Compose

## Quick Start

### 1. Start infrastructure and backend

The bundled values are only for an isolated local workstation.

```powershell
Copy-Item .env.example .env.local
docker compose --env-file .env.local up --build
```

On the first start MySQL executes `db/bootstrap/*.sql` in lexical order. Existing
Docker volumes are not reinitialized when SQL files change.

Backend health endpoint: `http://localhost:48080/actuator/health`

Swagger UI: `http://localhost:48080/swagger-ui`

### 2. Start the management console

```powershell
Set-Location yudao-ui/yudao-ui-admin-vue3
pnpm install --frozen-lockfile
pnpm dev
```

Open `http://localhost:3000`.

Local bootstrap login:

```text
Tenant:  Hotel PMS Community
Username: admin
Password: admin123
```

Change the password before exposing the service to another machine.

## Run Without Docker

Start MySQL and Redis, create an empty `hotel_pms_community` database, and apply:

```text
db/bootstrap/00-framework-schema.sql
db/bootstrap/01-community-seed.sql
db/bootstrap/10-hotel-domain.sql
```

Then configure environment variables and start the backend:

```powershell
$env:DB_URL = 'jdbc:mysql://127.0.0.1:3306/hotel_pms_community?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true'
$env:DB_USERNAME = 'hotel_pms'
$env:DB_PASSWORD = 'hotel_pms'
$env:REDIS_HOST = '127.0.0.1'
mvn -pl yudao-server -am spring-boot:run
```

## Configuration

Backend configuration is in:

- `yudao-server/src/main/resources/application.yaml`: shared safe defaults
- `yudao-server/src/main/resources/application-local.yaml`: environment-driven local profile

Important environment variables:

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_URL` | local MySQL URL | JDBC connection |
| `DB_USERNAME` | `hotel_pms` | Database user |
| `DB_PASSWORD` | `hotel_pms` | Local database password |
| `REDIS_HOST` | `127.0.0.1` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `SERVER_PORT` | `48080` | Backend port |
| `CAPTCHA_ENABLED` | `false` | Local login captcha |
| `HOTEL_PMS_ADMIN_URL` | `http://localhost:3000` | Admin UI URL in API metadata |

Frontend defaults are in `yudao-ui/yudao-ui-admin-vue3/.env`. Put
machine-specific overrides in `.env.local`; it is ignored by Git. Never commit
storage credentials, map keys, statistics IDs or API encryption keys.

## Database Model

The hotel domain contains:

- `biz_merchant`: current hotel profile
- `booking_room_type`: sellable room types and base prices
- `booking_room`: physical room numbers
- `booking_inventory`: daily quantity and price
- `booking_inventory_record`: auditable inventory changes
- `booking_order`: reservation and fulfillment state
- `booking_order_lock`: immutable per-night price and inventory locks

All money values are integer cents. Hotel stays use half-open date ranges:
check-in is inclusive and check-out is exclusive.

## Build and Verify

Backend tests:

```powershell
mvn -pl yudao-module-merchant,yudao-module-booking -am test
```

Frontend checks:

```powershell
Set-Location yudao-ui/yudao-ui-admin-vue3
pnpm install --frozen-lockfile
pnpm ts:check
pnpm build
```

Complete local boundary and build verification:

```powershell
./tools/verify-community.ps1
```

CI runs the same backend, frontend and commercial-boundary checks.

The inherited test stack is not compatible with JDK 25: older Mockito/ByteBuddy
versions can fail while instrumenting JDK classes and annotations. Use JDK 8 or
17 for repeatable local and CI results.

## Reset Local Data

Database bootstrap files run only for a new MySQL volume. To deliberately reset
an isolated development environment, first run `docker compose down`, inspect
the exact project volume names with `docker volume ls`, and remove only the
confirmed local project volumes. Never run volume deletion against shared data.

## Backup and Restore

The repository does not schedule production backups. For a local Compose
environment, create a logical database backup before an upgrade:

```powershell
docker compose exec -T mysql sh -c 'exec mysqldump -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" > /tmp/hotel-pms-community.sql'
$mysqlContainer = docker compose ps -q mysql
docker cp "${mysqlContainer}:/tmp/hotel-pms-community.sql" .\hotel-pms-community.sql
docker compose exec -T mysql rm -f /tmp/hotel-pms-community.sql
```

Restore only into an empty disposable database and verify the target first:

```powershell
$mysqlContainer = docker compose ps -q mysql
docker cp .\hotel-pms-community.sql "${mysqlContainer}:/tmp/hotel-pms-community.sql"
docker compose exec -T mysql sh -c 'exec mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" < /tmp/hotel-pms-community.sql'
docker compose exec -T mysql rm -f /tmp/hotel-pms-community.sql
```

Keep backups outside the Git repository. Production deployments should use the
database provider's encrypted backup, retention and restore-testing facilities.

## Troubleshooting

- **Schema changes are not applied:** bootstrap SQL runs only when the MySQL
  data volume is empty. Back up the database and use an explicit migration for
  an existing installation.
- **Mockito or ByteBuddy tests fail before assertions:** check `java -version`
  and run the test suite with JDK 8 or 17 instead of JDK 25.
- **The management console cannot reach the API:** verify the backend health
  endpoint, then check the frontend API base URL in `.env.local`.
- **A paid/refunded status did not transfer money:** this is expected. The
  community action only records an already completed offline operation.
- **Expired unpaid orders are not released automatically:** the release logic is
  included and tested, but Quartz is disabled in the local profile. Register a
  reviewed scheduler in a real deployment or cancel stale orders operationally.
- **Frontend dependency installation is inconsistent:** use Node.js 20.19 and
  pnpm 10, remove only this frontend's `node_modules`, then reinstall from the
  committed lockfile.

## Extraction and Upgrades

- `docs/OPEN_SOURCE_EXTRACTION_DESIGN.md` explains the architecture and scope.
- `docs/FILE_MANIFEST.md` records copied and excluded paths.
- `docs/COMMERCIAL_BOUNDARY.md` defines what cannot enter this repository.
- `tools/export-from-saas-jd.ps1` exports only the pinned allowlist.
- `tools/prune-community-export.ps1` removes commercial paths after export.
- `tools/generate-framework-schema.ps1` extracts framework DDL without records.

Upgrades must start from a reviewed source commit, rerun the allowlist export,
reapply community changes, and pass `tools/verify-community.ps1`. Never copy a
production database dump or environment file into this repository.

## License and Attribution

The repository uses the MIT license inherited from RuoYi-Vue-Pro. Upstream
sources and pinned baseline commits are listed in `BASELINE_SOURCE.md` and
`NOTICE`.

Read `SECURITY.md` before deployment and `CONTRIBUTING.md` before submitting a
change. Use fictional data in examples and tests.
