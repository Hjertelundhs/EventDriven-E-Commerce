#!/usr/bin/env bash
set -Eeuo pipefail

realm="order-logistics"
server="http://keycloak:8080"
kcadm="/opt/keycloak/bin/kcadm.sh"

required_variables=(
  KEYCLOAK_ADMIN
  KEYCLOAK_ADMIN_PASSWORD
  KEYCLOAK_CUSTOMER_PASSWORD
  KEYCLOAK_STAFF_PASSWORD
)

for variable_name in "${required_variables[@]}"; do
  if [[ -z "${!variable_name:-}" ]]; then
    echo "Required environment variable ${variable_name} is missing" >&2
    exit 1
  fi
done

authenticated=false
for attempt in {1..30}; do
  if "$kcadm" config credentials \
    --server "$server" \
    --realm master \
    --client "$KEYCLOAK_ADMIN" \
    --secret "$KEYCLOAK_ADMIN_PASSWORD" >/dev/null 2>&1; then
    authenticated=true
    break
  fi
  echo "Waiting for Keycloak admin login (attempt ${attempt}/30)..." >&2
  sleep 2
done

if [[ "$authenticated" != true ]]; then
  echo "Could not authenticate the Keycloak bootstrap administrator." >&2
  exit 1
fi

ensure_user() {
  local username="$1"
  local first_name="$2"
  local last_name="$3"
  local password="$4"
  local role_name="$5"
  local user_id

  user_id=$("$kcadm" get users \
    --target-realm "$realm" \
    --query "username=${username}" \
    --fields id \
    --format csv \
    --noquotes | head -n 1 | tr -d '\r')

  if [[ -z "$user_id" ]]; then
    user_id=$("$kcadm" create users \
      --target-realm "$realm" \
      --set "username=${username}" \
      --set "email=${username}" \
      --set "firstName=${first_name}" \
      --set "lastName=${last_name}" \
      --set enabled=true \
      --set emailVerified=true \
      --id)
  fi

  "$kcadm" set-password \
    --target-realm "$realm" \
    --userid "$user_id" \
    --new-password "$password" >/dev/null

  "$kcadm" add-roles \
    --target-realm "$realm" \
    --uid "$user_id" \
    --rolename "$role_name" >/dev/null

  echo "Ensured local user ${username} with role ${role_name}."
}

ensure_user "customer@example.test" "Local" "Customer" "$KEYCLOAK_CUSTOMER_PASSWORD" "CUSTOMER"
ensure_user "admin@example.test" "Local" "Administrator" "$KEYCLOAK_STAFF_PASSWORD" "ADMIN"
ensure_user "support@example.test" "Local" "Support" "$KEYCLOAK_STAFF_PASSWORD" "SUPPORT"
ensure_user "warehouse@example.test" "Local" "Warehouse" "$KEYCLOAK_STAFF_PASSWORD" "WAREHOUSE"

echo "Keycloak local users and realm roles are ready."
