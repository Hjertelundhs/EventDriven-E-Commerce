# Lokal Kafka-konfiguration

Kafka körs i KRaft-läge utan ZooKeeper. Brokerlistener `INTERNAL` annonserar `kafka:9092` till containrar och `EXTERNAL` annonserar `localhost:${KAFKA_PORT}` till värddatorn. PLAINTEXT är endast accepterat eftersom nätet och hostporten är lokala; produktion ska använda TLS, SASL och ACL.

`create-topics.sh` väntar på brokern och skapar topics idempotent med `--if-not-exists`. Auto creation är avstängd så stavfel inte skapar okontrollerade topics.

| Grupp | Partitioner | Retention |
|---|---:|---:|
| Order/Inventory | 6 | 7 dagar |
| Payment/Delivery | 6 | 30 dagar |
| Product | 6 | kompaktering + 7 dagar |
| Alla DLTs | 3 | 90 dagar |

Produkt-topic använder `compact,delete`; övriga använder `delete`. Lokal replication factor är ett och ska inte kopieras till produktion.
