# Lokal Keycloak-konfiguration

`realm-export.json` innehåller realm, roller och klienter men inga användare, credentials eller client secrets. `bootstrap-users.sh` skapar lokala testidentiteter efter att Keycloak blivit ready och läser lösenord från `.env` via Compose.

SPA-klienten kräver PKCE `S256`. API:t representeras av en bearer-only audience. Klienten `platform-local-verifier` med Direct Access Grant finns enbart för den automatiska lokala tokenkontrollen och måste tas bort i alla andra miljöer.

Keycloak startas i utvecklingsläge över loopbackbunden HTTP. Produktionsdeployment i Fas 10 ska använda optimerad image, TLS, strikt hostname, extern secret manager, backup och hög tillgänglighet.
