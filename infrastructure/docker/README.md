# Docker bootstrap

`postgres/init-databases.sh` körs av PostgreSQL-imageens standard-entrypoint när named volume är tom. Skriptet läser secrets från containerns environment, använder `psql`-quoting för identifierare/värden och skriver aldrig lösenorden till Git.

Bootstrap skapar åtta tjänstedatabaser, Keycloaks databas och en read-only monitoring-principal. Det återkallar `PUBLIC CONNECT` och `PUBLIC CREATE` för att göra tjänstegränsen faktisk även i den delade lokala instansen.

Skriptet är återkörbart på SQL-nivå men Docker-entrypoint kör det normalt endast vid första volyminitiering. Se [lokal utvecklingsguide](../../docs/architecture/local-development.md) för återställning.
