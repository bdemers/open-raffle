# Open Raffle

[![Tests](https://github.com/dogeared/open-raffle/actions/workflows/tests.yml/badge.svg)](https://github.com/dogeared/open-raffle/actions/workflows/tests.yml)
[![Version](https://img.shields.io/github/v/tag/dogeared/open-raffle?label=version&sort=semver)](https://github.com/dogeared/open-raffle/tags)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

Run a physical-ticket raffle without the paper chaos. Organizers record who holds which
ticket numbers, hand each participant a QR code, and participants rank the prizes they'd
like from their phone. When a ticket is drawn, the draw page shows the winner, their
ranked preferences, and which of those prizes are still available.

Built with Spring Boot and Vaadin, secured with OpenID Connect via Keycloak, and packaged
for Docker.

## How it works

1. **Prizes** — organizers enter the prizes and order them with up/down arrows.
2. **Participants** — each participant gets a name, an optional phone number, and the
   contiguous range of ticket numbers they bought. Overlapping ranges are rejected.
3. **QR code** — the app shows (and can download) a QR code per participant. It opens a
   login-free page, identified by an unguessable token, where they rank the prizes they
   want and leave notes.
4. **Draw** — type the drawn ticket number. The winner's preferences appear with a checkbox
   per prize; tick the one they take. Prizes already claimed by earlier winners are struck
   through. Prizes not on their list can be given out too, and new prizes can be added and
   handed over on the spot.

Organizer pages require a Keycloak login with the `ADMIN` realm role.

## Running locally

Prerequisites: Java 21, Maven, Docker.

Keycloak is expected to run as a shared instance outside this project (see
`keycloak/open-raffle-realm.json` for the realm to import; it includes an `organizer` /
`organizer` user with the `ADMIN` role). Point it at `http://localhost:8180`, or override
`KEYCLOAK_ISSUER` below.

```sh
docker compose up -d                                   # Postgres
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

Open http://localhost:8081 and log in. Keep the app and the browser reaching Keycloak at
the same URL: OIDC validates the token issuer against the configured issuer.

## Configuration

All settings are environment variables with local-dev defaults (see
`src/main/resources/application.properties`).

| Variable | Default | Purpose |
| --- | --- | --- |
| `RAFFLE_PUBLIC_URL` | *(derived from each request)* | Base URL embedded in QR codes, e.g. `https://raffle.example.com`. Set this in production; phones must be able to open it. |
| `DB_URL` | `jdbc:postgresql://localhost:5432/raffle` | JDBC URL |
| `DB_USER` / `DB_PASSWORD` | `raffle` / `raffle` | Database credentials |
| `KEYCLOAK_ISSUER` | `http://localhost:8180/realms/open-raffle` | OIDC issuer URL of the realm |
| `KEYCLOAK_CLIENT_ID` | `open-raffle-app` | Confidential client in that realm |
| `KEYCLOAK_CLIENT_SECRET` | `open-raffle-dev-secret` | Its secret — change it outside dev |

Behind a reverse proxy, the app trusts `X-Forwarded-Proto`, `X-Forwarded-Host` and
`X-Forwarded-Port` (`server.forward-headers-strategy=framework`), so OIDC redirect URIs and
request-derived QR links use the public scheme and host. Add the public URL to the Keycloak
client's redirect URIs.

## Production build

```sh
docker build -t open-raffle .
docker run -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://db:5432/raffle -e DB_USER=raffle -e DB_PASSWORD=... \
  -e KEYCLOAK_ISSUER=https://auth.example.com/realms/open-raffle \
  -e KEYCLOAK_CLIENT_SECRET=... \
  -e RAFFLE_PUBLIC_URL=https://raffle.example.com \
  open-raffle
```

The multi-stage `Dockerfile` builds the Vaadin production bundle, so no Node.js is needed
at runtime. The schema is created and migrated by Hibernate (`ddl-auto=update`).

## Tests

```sh
mvn test
```

The suite covers the services, QR URL resolution and Keycloak role mapping, and runs on
every push and pull request via GitHub Actions. Tests use an in-memory H2 database, so
neither Docker nor Keycloak is needed to run them.

## Changelog

See [CHANGELOG.md](CHANGELOG.md).

## License

[MIT](LICENSE)
