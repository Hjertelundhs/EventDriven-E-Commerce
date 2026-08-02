# ADR-005: Databas per tjänst

- **Status:** Accepted
- **Datum:** 2026-08-02

## Kontext

Självständiga bounded contexts behöver kunna utveckla schema och deployas utan att andra tjänster kopplar till interna tabeller. En gemensam databas skulle göra kodgränserna frivilliga, skapa korsdomänjoinar och göra oberoende release riskfylld.

## Beslut

Varje tjänst äger en separat PostgreSQL-databas och principal. Endast ägartjänsten får läsa eller skriva databasen. Foreign keys används inom tjänstens databas men aldrig över tjänstegränser. Cross-context data refereras med identifierare och synkroniseras endast genom dokumenterade REST-kontrakt eller events.

Varje tjänst äger sin Flyway-historik, indexering, auditfält, optimistic-locking-kolumner, outbox och `processed_events`. Lokalt får databaserna dela PostgreSQL-instans för resurseffektivitet; i produktion är den logiska isoleringen och least-privilege-principals obligatoriska även om fysisk server delas.

## Konsekvenser

### Positiva

- Schema och releasecykel isoleras per tjänst.
- Dataägarskap och least privilege blir tydligt.
- En tjänst kan optimera sin modell utan korsdomänmigrering.
- Oavsiktliga distribuerade transaktioner och joins förhindras.

### Negativa och risker

- Cross-context frågor kräver API composition eller read models.
- Eventual consistency måste visas i UX och operativa verktyg.
- Backup, migration, anslutningspooler och observability multipliceras.
- Referensintegritet mellan kontexter upprätthålls av processer, inte foreign keys.

## Övervägda alternativ

- **Gemensam databas/schema:** enklare joins och lokala transaktioner men skapar stark deploy- och modellkoppling.
- **Schema per tjänst i samma databas:** bättre namespace men otillräcklig isolering om credentials delas; kan vara ett lokalt kompromissläge, inte produktionskontraktet.
- **Polyglot persistence från start:** avvisas som onödig operativ variation; PostgreSQL täcker aktuella behov och Redis/Kafka har tydligt avgränsade roller.
