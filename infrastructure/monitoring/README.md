# Lokal observability

Stacken består av Prometheus, Grafana, Loki, Tempo, Grafana Alloy samt Kafka-, Redis- och PostgreSQL-exporterare.

- Metrics: `http://localhost:9090`
- Dashboards: `http://localhost:3000`
- Logs: Grafana Explore med datasource `Loki`
- Traces: OTLP på hostport 4317/4318 och Grafana Explore med datasource `Tempo`

Grafana Alloy läser endast JSON-loggar från named volume `platform-logs`; Docker-socketen exponeras avsiktligt inte för loggagenten. Tjänsterna kopplas till volymen när de implementeras. Application metrics och trace/log correlation fylls på när Spring Boot-tjänsterna införs.
