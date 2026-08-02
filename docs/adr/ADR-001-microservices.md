# ADR-001: Mikrotjänstarkitektur kring affärsförmågor

- **Status:** Accepted
- **Datum:** 2026-08-02

## Kontext

Plattformen ska demonstrera senior Java-utveckling, självständiga domäner, eventdriven integration, separat skalning och produktionsnära drift. Order, lager, betalning och leverans har olika regler, felmoder och förändringstakt. Samtidigt ökar distribuerade system kostnaden för drift, test, felsökning och konsistens.

## Beslut

Systemet delas i API Gateway, Identity, Product, Inventory, Order, Payment, Delivery och Notification Service. Gränserna följer affärsförmågor och uttryckliga bounded contexts, inte tekniska lager.

Varje domäntjänst är en självständigt byggbar och deploybar Spring Boot-applikation med hexagonal arkitektur. Domänlagret har inga Spring-, JPA- eller transportberoenden. En tjänst äger sin data och sina kontrakt. `shared-contracts` begränsas till tekniska event-scheman och får inte bli en distribuerad monolit genom delade domänmodeller.

Monorepo väljs för gemensam synlighet, atomiska kontraktsändringar och enklare portfolio-/CI-hantering. Det innebär inte gemensam runtime eller gemensam deployment.

## Konsekvenser

### Positiva

- Affärsregler och språk isoleras per kontext.
- Tjänster kan deployas, skalas och felisoleras oberoende.
- Arkitekturen demonstrerar distribuerade mönster explicit.
- Teamägarskap kan senare följa tjänstegränserna.

### Negativa och risker

- Eventual consistency och kompensation ersätter globala ACID-transaktioner.
- Lokal utveckling, CI, observability och kontraktstest blir mer omfattande.
- Små ändringar kan kräva versionerade kontrakt och flera deployments.
- För tidig uppdelning kan skapa chatty integration. Synkrona beroenden hålls därför få och mätbara.

## Övervägda alternativ

- **Modulär monolit:** lägre driftkostnad och enklare transaktioner, men uppfyller inte projektets uttalade demonstrationsmål för mikrotjänster och distribuerad saga.
- **Tekniskt lagerindelade tjänster:** avvisas eftersom de skapar hög affärskoppling och svagt dataägarskap.
- **Separata repositories:** möjliggör hårdare autonomi men försvårar denna iterativa leverans och kontraktsöverblick.
