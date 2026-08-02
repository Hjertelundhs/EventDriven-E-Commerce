# Teststrategi

Testpyramiden följer bounded contexts och lägger snabb återkoppling nära domänen.

## Nivåer

1. **Domäntester** kör utan Spring och verifierar affärsinvarianter och state transitions.
2. **Applicationtester** mockar outbound-portar och verifierar use cases, transaktionsavsikt och events.
3. **API-tester** verifierar DTO-allowlists, validering, statuskoder, headers och Problem Details.
4. **Kontraktstester** verifierar REST- och eventkontrakt mot versionshanterade artefakter.
5. **Integrationstester** använder verkliga PostgreSQL-, Kafka- och Redis-containrar och kör Flyway.
6. **End-to-end-tester** med Playwright introduceras när frontend och gateway finns.

## Principer

- Tester får inte dela databas mellan tjänster.
- Testcontainers-images låses till explicita versioner.
- Integrationstester ska vara reproducerbara och får inte kräva befintlig lokal data.
- Domänregler testas direkt, inte enbart via controllers.
- Async assertions har begränsade deadlines och får inte använda obundna sleeps.
- Kontrakt och migrationsfiler är produktionskod och granskas som sådan.

## Kommandon

Varje Maven-tjänst kör hela sin verifiering med:

```powershell
.\mvnw.cmd verify
```

Om Docker saknas får Dockerberoende tester markeras skipped lokalt, men CI ska ha Docker och behandla utebliven integrationstestkörning som ett konfigurationsfel.
