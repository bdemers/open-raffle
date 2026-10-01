#!/bin/bash
# Renders the dev realm export into data/import with production values, then starts
# Keycloak. Keycloak does not substitute its own ${env.X} placeholders on import
# (verified on 26.7), so the known dev literals are replaced here with plain bash.
set -euo pipefail

# The app side uses different names for the same values; accept both so one shared
# env group can feed both services.
OPEN_RAFFLE_URL=${OPEN_RAFFLE_URL:-${RAFFLE_PUBLIC_URL:-}}
OPEN_RAFFLE_CLIENT_SECRET=${OPEN_RAFFLE_CLIENT_SECRET:-${KEYCLOAK_CLIENT_SECRET:-}}
if [ -z "$OPEN_RAFFLE_URL" ]; then
    echo "render-entrypoint: set OPEN_RAFFLE_URL or RAFFLE_PUBLIC_URL to the public URL of the app" >&2
    exit 1
fi
if [ -z "$OPEN_RAFFLE_CLIENT_SECRET" ]; then
    echo "render-entrypoint: set OPEN_RAFFLE_CLIENT_SECRET or KEYCLOAK_CLIENT_SECRET" >&2
    exit 1
fi
if [ -z "${OPEN_RAFFLE_ORGANIZER_PASSWORD:-}" ]; then
    echo "render-entrypoint: set OPEN_RAFFLE_ORGANIZER_PASSWORD" >&2
    exit 1
fi

template=/opt/keycloak/realm-template/open-raffle-realm.json
import_dir=/opt/keycloak/data/import
mkdir -p "$import_dir"

realm=$(<"$template")

dev_secret='open-raffle-dev-secret'
dev_url='http://localhost:8080'
dev_password='"value": "organizer"'
dev_ssl='"sslRequired": "none"'
prod_password='"value": "'"$OPEN_RAFFLE_ORGANIZER_PASSWORD"'"'
# TLS is terminated by the platform proxy; "external" makes Keycloak require it.
prod_ssl='"sslRequired": "external"'

realm=${realm//"$dev_secret"/$OPEN_RAFFLE_CLIENT_SECRET}
realm=${realm//"$dev_url"/$OPEN_RAFFLE_URL}
realm=${realm//"$dev_password"/$prod_password}
realm=${realm//"$dev_ssl"/$prod_ssl}

printf '%s\n' "$realm" > "$import_dir/open-raffle-realm.json"

# Realms are only imported when they do not exist yet, so later deploys keep whatever
# was changed in the admin console.
exec /opt/keycloak/bin/kc.sh start --optimized --import-realm "$@"
