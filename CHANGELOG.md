# Changelog

All notable changes to Open Raffle are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.1] - 2026-10-01

### Added
- **Render deployment.** `render.yaml` Blueprint that provisions the app and its Postgres
  database and prompts for the connection details of an existing, centrally managed Keycloak
  (`KEYCLOAK_ISSUER`, `RAFFLE_PUBLIC_URL`). The README documents
  what the Keycloak client needs.
- `GET /actuator/health` without authentication, for platform health checks.
- `PORT` is honoured for the HTTP port, and the database can be configured as
  `DB_HOST`/`DB_PORT`/`DB_NAME` as well as a full `DB_URL`.

### Changed
- **The Keycloak client is public.** Login uses the authorization code flow with PKCE and no
  client secret; `KEYCLOAK_CLIENT_SECRET` is gone. The bundled realm export's client is now
  `publicClient: true` (re-import it, or switch the existing client's *Client authentication*
  off).
- Realm roles are read from the userinfo response as well as the ID token, so a realm whose
  roles mapper does not add them to the ID token still grants organizer access.
- The JVM sizes its heap from the container memory limit (`-XX:MaxRAMPercentage=75`).

## [0.1.0] - 2026-10-01

### Added
- **Participants.** Organizers register each participant with a name, an optional phone
  number, and the contiguous range of physical ticket numbers they hold. Overlapping ranges
  are rejected.
- **QR code wishlists.** Every participant gets a QR code (shown in the app and downloadable
  as PNG) that opens a login-free page where they rank the prizes they'd like, using up/down
  arrows, and leave optional notes. The link's base URL comes from `RAFFLE_PUBLIC_URL`, or from
  the current request when unset.
- **Prizes.** Organizers manage the prize list with the same arrow-based ordering participants
  use. New prizes go to the bottom of the list.
- **Draw page.** Type a drawn ticket number to see who holds it, their phone number, and their
  ranked preferences. Tick a prize to record the claim; prizes already taken by earlier winners
  are struck through. Prizes not on the winner's list can be given out too, and brand-new prizes
  can be added and handed over on the spot; both join the winner's preference list.
- **Keycloak login.** Organizer pages require an OpenID Connect login (authorization code with
  PKCE) against a Keycloak realm whose `ADMIN` role maps to the app's admin role. A ready-made
  realm export with an `organizer` user is included.
- **Docker.** A multi-stage `Dockerfile` builds the Vaadin production bundle; `docker-compose.yml`
  provides Postgres for local development.
- **CI.** GitHub Actions runs the test suite on every push and pull request.

[0.1.1]: https://github.com/dogeared/open-raffle/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/dogeared/open-raffle/releases/tag/v0.1.0
