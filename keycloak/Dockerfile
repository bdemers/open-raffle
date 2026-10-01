# Keycloak for production (Render). Bakes in the open-raffle realm; the entrypoint
# swaps the dev values in the realm file for production ones from the environment.
#
# Required at runtime (see render.yaml):
#   KC_DB_URL_HOST, KC_DB_URL_PORT, KC_DB_URL_DATABASE, KC_DB_USERNAME, KC_DB_PASSWORD
#   KC_HOSTNAME                     public URL of this service, e.g. https://auth.example.com
#   KC_BOOTSTRAP_ADMIN_USERNAME / KC_BOOTSTRAP_ADMIN_PASSWORD   (first boot only)
#   OPEN_RAFFLE_URL                 public URL of the app, for OIDC redirect URIs (or RAFFLE_PUBLIC_URL)
#   OPEN_RAFFLE_CLIENT_SECRET       must equal the app's KEYCLOAK_CLIENT_SECRET (or that name)
#   OPEN_RAFFLE_ORGANIZER_PASSWORD  password of the seeded "organizer" user
FROM quay.io/keycloak/keycloak:26.7 AS builder
ENV KC_DB=postgres
ENV KC_HEALTH_ENABLED=true
RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:26.7
COPY --from=builder /opt/keycloak/ /opt/keycloak/
COPY open-raffle-realm.json /opt/keycloak/realm-template/open-raffle-realm.json
COPY --chmod=755 render-entrypoint.sh /opt/keycloak/bin/render-entrypoint.sh
ENV KC_HTTP_ENABLED=true
ENV KC_PROXY_HEADERS=xforwarded
ENTRYPOINT ["/opt/keycloak/bin/render-entrypoint.sh"]
