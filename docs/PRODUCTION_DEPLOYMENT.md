# Production Deployment

This guide deploys one Hotel PMS Community instance with MySQL, Redis, one
Spring Boot backend, and an Nginx-served management console.

## Scope and constraints

- Docker 24+ and Docker Compose 2.20+ are required.
- Only `ADMIN_HTTP_PORT` is published to the host.
- TLS terminates at a host reverse proxy, cloud load balancer, or ingress.
- The bundled Quartz scheduler uses an in-memory job store. Run exactly one
  backend replica. Use a JDBC job store before scaling the backend horizontally.
- MySQL bootstrap SQL runs only when the data volume is empty.
- Payment, automated refunds, commission and profit sharing are not included.

## 1. Configure the environment

Copy the template:

```powershell
Copy-Item .env.production.example .env.production
```

Fill every empty value in `.env.production`. Generate independent random values
for the MySQL application password, MySQL root password, Redis password and
MyBatis encryptor password. For example:

```text
openssl rand -hex 32
```

Set `PUBLIC_BASE_URL` to the externally visible origin without a trailing slash,
for example `https://hotel.example.com`. Do not commit `.env.production`.

## 2. Validate and start

Render the final Compose configuration before creating containers:

```powershell
docker compose --env-file .env.production -f compose.production.yaml config
docker compose --env-file .env.production -f compose.production.yaml up -d --build
docker compose --env-file .env.production -f compose.production.yaml ps
```

The one-shot `database-config` service must exit with code `0`. It performs two
idempotent operations:

1. Updates the database-backed file service to use `PUBLIC_BASE_URL`.
2. Ensures the `bookingOrderTimeoutJob` task exists.

The backend synchronizes enabled database jobs into Quartz after startup.

## 3. Verify health

Replace `8080` if `ADMIN_HTTP_PORT` was changed:

```powershell
curl.exe -fsS http://127.0.0.1:8080/healthz
curl.exe -fsS http://127.0.0.1:8080/actuator/health
docker compose --env-file .env.production -f compose.production.yaml logs --tail 100 backend
```

Expected actuator result:

```json
{"status":"UP"}
```

Open `http://SERVER_IP:ADMIN_HTTP_PORT` only for a private-network smoke test.
Use the HTTPS domain for normal access.

## 4. First-login hardening

The empty-database bootstrap account is:

```text
Tenant:  Hotel PMS Community
Username: admin
Password: admin123
```

Immediately change the administrator password. Keep `CAPTCHA_ENABLED=true` and
`SWAGGER_ENABLED=false` for internet-facing deployments.

## 5. TLS reverse proxy

Forward the public HTTPS origin to `127.0.0.1:ADMIN_HTTP_PORT`. Preserve the
original host and forwarding headers. A minimal host-Nginx location is:

```nginx
location / {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto https;
}
```

Certificate issuance and renewal remain the responsibility of the host proxy or
cloud ingress. Do not copy private keys into this repository or image.

## 6. Backup

Create a logical backup before every upgrade:

```powershell
$compose = @('--env-file', '.env.production', '-f', 'compose.production.yaml')
docker compose @compose exec -T mysql sh -c 'exec mysqldump -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' > hotel-pms-community.sql
```

Store backups outside the repository and test restoration on an isolated
database. Also back up `.env.production` through an approved secret-management
system.

## 7. Upgrade and rollback

Back up the database, fetch the reviewed revision, rebuild, and recreate changed
services:

```powershell
git pull --ff-only
docker compose --env-file .env.production -f compose.production.yaml build
docker compose --env-file .env.production -f compose.production.yaml up -d
```

For an application rollback, check out the previously approved tag or commit and
rebuild. A database rollback requires the matching pre-upgrade backup; bootstrap
SQL is not a migration or rollback mechanism.

## 8. Operations

```powershell
docker compose --env-file .env.production -f compose.production.yaml ps
docker compose --env-file .env.production -f compose.production.yaml logs -f --tail 200 backend admin
docker compose --env-file .env.production -f compose.production.yaml restart backend admin
```

Backend logs are persisted in the `backend-logs` named volume. MySQL and Redis
use separate named volumes and are not exposed on host ports.
