#!/bin/sh
set -eu

: "${MYSQL_HOST:?MYSQL_HOST is required}"
: "${MYSQL_DATABASE:?MYSQL_DATABASE is required}"
: "${MYSQL_USER:?MYSQL_USER is required}"
: "${MYSQL_PASSWORD:?MYSQL_PASSWORD is required}"
: "${PUBLIC_BASE_URL:?PUBLIC_BASE_URL is required}"

PUBLIC_BASE_URL=${PUBLIC_BASE_URL%/}
if ! printf '%s' "$PUBLIC_BASE_URL" | grep -Eq '^https?://[A-Za-z0-9.-]+(:[0-9]{1,5})?$'; then
  echo "PUBLIC_BASE_URL must be an http(s) origin without a path" >&2
  exit 1
fi
ESCAPED_PUBLIC_BASE_URL=$(printf '%s' "$PUBLIC_BASE_URL" | sed "s/'/''/g")
export MYSQL_PWD="$MYSQL_PASSWORD"

mysql --protocol=TCP --host="$MYSQL_HOST" --user="$MYSQL_USER" "$MYSQL_DATABASE" <<SQL
UPDATE infra_file_config
SET config = JSON_SET(config, '$.domain', '$ESCAPED_PUBLIC_BASE_URL')
WHERE id = 1 AND deleted = b'0';

INSERT INTO infra_job
    (name, status, handler_name, handler_param, cron_expression, retry_count,
     retry_interval, monitor_timeout, creator, updater, deleted)
SELECT
    'Booking order timeout release', 1, 'bookingOrderTimeoutJob', NULL,
    '0 0/1 * * * ?', 0, 0, 60000, 'production-bootstrap',
    'production-bootstrap', b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM infra_job
    WHERE handler_name = 'bookingOrderTimeoutJob' AND deleted = b'0'
);
SQL

echo "Production database configuration applied."
