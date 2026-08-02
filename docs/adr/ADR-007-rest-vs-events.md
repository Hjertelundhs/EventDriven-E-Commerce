# ADR-007: Beslutsregler för REST kontra asynkrona events

- **Status:** Accepted
- **Datum:** 2026-08-02

## Kontext

Plattformen behöver både interaktiva svar och löst kopplade, långlivade processer. Om allt görs med REST skapas sköra anropskedjor. Om allt görs med events blir enkla frågor och omedelbar validering onödigt komplexa och UX svårare att förutsäga.

## Beslut

REST används för commands och queries där anroparen behöver omedelbar acceptans, aktuell data eller ett auktoriseringsbeslut. Kafka-events används för immutable affärsfakta, saga-steg, fan-out, notifieringar och projektioner.

Beslutsmatris:

| Fråga | Om ja | Val |
|---|---|---|
| Behöver användaren svaret för att fortsätta nu? | Ja | REST |
| Är detta en läsfråga utan behov av replikerad read model? | Ja | REST |
| Ska flera oberoende konsumenter reagera? | Ja | Event |
| Ska arbetet överleva klientdisconnect eller downstream-störning? | Ja | Event |
| Är meddelandet ett faktum som redan inträffat? | Ja | Event |
| Krävs omedelbar produkt-/prisvalidering före orderacceptans? | Ja | Kort, resilient REST |

Order Service validerar produkt och pris synkront före acceptans. När order + outbox har committats returneras `202 Accepted`, och lager, betalning, leverans och notifiering fortsätter via events. Klientens realtidsstatus levereras via SSE från Order Services lokala state, inte genom att exponera Kafka.

Synkrona anrop har explicit timeout, circuit breaker och ingen generell retry för icke-idempotenta commands. Eventkonsumenter har at-least-once/idempotens, begränsad teknisk retry och DLT.

## Konsekvenser

### Positiva

- Interaktiva flöden förblir begripliga medan sagan blir resilient.
- Fan-out kräver inte att producenten känner till konsumenterna.
- REST- och eventkontrakt får tydliga semantiska roller.
- Downstream-tillfälliga fel efter orderacceptans kopplas bort från klientanslutningen.

### Negativa och risker

- Två integrationsstilar måste dokumenteras, säkras, testas och observeras.
- Produktvalideringen är ett synkront checkoutberoende.
- UI måste hantera `202` och eventual consistency.
- Duplicering mellan REST-DTO och eventpayload är avsiktlig men kräver mappning.

## Övervägda alternativ

- **REST-only:** avvisas för sagan på grund av temporal koppling och svag fan-out/replay.
- **Event-only inklusive queries:** avvisas eftersom det kräver fler read models och komplicerar omedelbar användarfeedback.
- **GraphQL:** kan vara användbart för komponerad läsning men löser inte saga, outbox eller tjänsteägarskap och införs inte utan konkret behov.
