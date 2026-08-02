# ADR-006: Keycloak för identitet och access management

- **Status:** Accepted
- **Datum:** 2026-08-02

## Kontext

Plattformen behöver registrering/login, OAuth 2.0/OpenID Connect, JWT, roller och automatiskt importerbar lokal konfiguration. Att själv bygga lösenordslagring, tokenutfärdning, sessioner och säkerhetsflöden skulle öka risk och flytta fokus från affärsdomänen.

## Beslut

Keycloak används som OpenID Provider och authorization server. React-klienten använder Authorization Code Flow med PKCE och är public client utan client secret. Roller `CUSTOMER`, `ADMIN`, `SUPPORT` och `WAREHOUSE` mappas till signerade claims med definierad audience.

API Gateway validerar token före routing. Varje berörd Spring Boot-tjänst är dessutom OAuth2 Resource Server, validerar issuer/audience/signatur/tid och utför method-level samt resursbaserad behörighetskontroll. Kunden identifieras via `sub`; ett requestfält får inte välja annan kund.

Identity Service lagrar applikationsprofil, samtycke och preferenser men aldrig credentials. Keycloak realm-exporten innehåller struktur och säkra lokala bootstrapmekanismer, inte produktionshemligheter.

## Konsekvenser

### Positiva

- Standardiserade, välkända protokoll och färdiga säkerhetsfunktioner.
- Credentials isoleras från affärstjänsterna.
- Roller, clients och claims kan reproduceras lokalt.
- Defense in depth minskar risken för gateway-bypass.

### Negativa och risker

- Keycloak blir en kritisk plattformskomponent som måste patchas, övervakas och backupas.
- Realm-/claimändringar är kontraktsändringar.
- Rollbasering räcker inte för ägarskap; domäntjänster måste kontrollera resursen.
- Felaktig redirect URI, audience eller CORS kan skapa sårbarheter eller driftfel.

## Övervägda alternativ

- **Egen identity implementation:** avvisas på grund av säkerhetsrisk och odifferentierat arbete.
- **Molnhanterad IdP:** attraktiv i produktion men försämrar lokal reproducerbarhet och binder portfolioflödet till ett konto.
- **JWT-validering endast i gateway:** avvisas eftersom interna tjänster också är säkerhetsgränser.
