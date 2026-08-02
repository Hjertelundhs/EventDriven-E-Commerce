#!/usr/bin/env bash
set -Eeuo pipefail

required_variables=(
  POSTGRES_USER POSTGRES_DB
  GATEWAY_DB_NAME GATEWAY_DB_USER GATEWAY_DB_PASSWORD
  IDENTITY_DB_NAME IDENTITY_DB_USER IDENTITY_DB_PASSWORD
  PRODUCT_DB_NAME PRODUCT_DB_USER PRODUCT_DB_PASSWORD
  INVENTORY_DB_NAME INVENTORY_DB_USER INVENTORY_DB_PASSWORD
  ORDER_DB_NAME ORDER_DB_USER ORDER_DB_PASSWORD
  PAYMENT_DB_NAME PAYMENT_DB_USER PAYMENT_DB_PASSWORD
  DELIVERY_DB_NAME DELIVERY_DB_USER DELIVERY_DB_PASSWORD
  NOTIFICATION_DB_NAME NOTIFICATION_DB_USER NOTIFICATION_DB_PASSWORD
  KEYCLOAK_DB_NAME KEYCLOAK_DB_USER KEYCLOAK_DB_PASSWORD
  MONITORING_DB_USER MONITORING_DB_PASSWORD
)

for variable_name in "${required_variables[@]}"; do
  if [[ -z "${!variable_name:-}" ]]; then
    echo "Required environment variable ${variable_name} is missing" >&2
    exit 1
  fi
done

create_role_and_database() {
  local role_name="$1"
  local role_password="$2"
  local database_name="$3"

  psql --set=ON_ERROR_STOP=1 \
    --username "$POSTGRES_USER" \
    --dbname "$POSTGRES_DB" \
    --set=role_name="$role_name" \
    --set=role_password="$role_password" \
    --set=database_name="$database_name" <<'SQL'
SELECT format(
  'CREATE ROLE %I LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION',
  :'role_name',
  :'role_password'
)
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'role_name')
\gexec

SELECT format('ALTER ROLE %I PASSWORD %L', :'role_name', :'role_password')
\gexec

SELECT format('CREATE DATABASE %I OWNER %I', :'database_name', :'role_name')
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = :'database_name')
\gexec

SELECT format('REVOKE ALL ON DATABASE %I FROM PUBLIC', :'database_name')
\gexec

SELECT format('GRANT CONNECT, TEMPORARY ON DATABASE %I TO %I', :'database_name', :'role_name')
\gexec
SQL

  psql --set=ON_ERROR_STOP=1 \
    --username "$POSTGRES_USER" \
    --dbname "$database_name" \
    --set=role_name="$role_name" <<'SQL'
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
SELECT format('ALTER SCHEMA public OWNER TO %I', :'role_name')
\gexec
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
SQL
}

create_role_and_database "$GATEWAY_DB_USER" "$GATEWAY_DB_PASSWORD" "$GATEWAY_DB_NAME"
create_role_and_database "$IDENTITY_DB_USER" "$IDENTITY_DB_PASSWORD" "$IDENTITY_DB_NAME"
create_role_and_database "$PRODUCT_DB_USER" "$PRODUCT_DB_PASSWORD" "$PRODUCT_DB_NAME"
create_role_and_database "$INVENTORY_DB_USER" "$INVENTORY_DB_PASSWORD" "$INVENTORY_DB_NAME"
create_role_and_database "$ORDER_DB_USER" "$ORDER_DB_PASSWORD" "$ORDER_DB_NAME"
create_role_and_database "$PAYMENT_DB_USER" "$PAYMENT_DB_PASSWORD" "$PAYMENT_DB_NAME"
create_role_and_database "$DELIVERY_DB_USER" "$DELIVERY_DB_PASSWORD" "$DELIVERY_DB_NAME"
create_role_and_database "$NOTIFICATION_DB_USER" "$NOTIFICATION_DB_PASSWORD" "$NOTIFICATION_DB_NAME"
create_role_and_database "$KEYCLOAK_DB_USER" "$KEYCLOAK_DB_PASSWORD" "$KEYCLOAK_DB_NAME"

psql --set=ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname "$POSTGRES_DB" \
  --set=monitoring_role="$MONITORING_DB_USER" \
  --set=monitoring_password="$MONITORING_DB_PASSWORD" <<'SQL'
SELECT format(
  'CREATE ROLE %I LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION',
  :'monitoring_role',
  :'monitoring_password'
)
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'monitoring_role')
\gexec

SELECT format('ALTER ROLE %I PASSWORD %L', :'monitoring_role', :'monitoring_password')
\gexec

SELECT format('GRANT pg_monitor TO %I', :'monitoring_role')
\gexec

SELECT format('GRANT CONNECT ON DATABASE %I TO %I', current_database(), :'monitoring_role')
\gexec
SQL

echo "Created isolated local databases and least-privilege principals."
