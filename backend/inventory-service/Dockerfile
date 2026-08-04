# syntax=docker/dockerfile:1.7
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-jammy
RUN groupadd --system --gid 10001 appgroup \
    && useradd --system --uid 10001 --gid appgroup --home-dir /app --shell /usr/sbin/nologin appuser \
    && mkdir -p /app /var/log/platform \
    && chown -R appuser:appgroup /app /var/log/platform
WORKDIR /app
COPY --from=build --chown=appuser:appgroup /workspace/target/inventory-service-*.jar app.jar
USER 10001:10001
EXPOSE 8083
HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=10 \
    CMD ["/bin/bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/8083 && printf 'GET /actuator/health/readiness HTTP/1.1\\r\\nHost: localhost\\r\\nConnection: close\\r\\n\\r\\n' >&3 && IFS= read -r status <&3 && [[ $status == *'200'* ]]" ]
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+ExitOnOutOfMemoryError", "-Djava.security.egd=file:/dev/urandom", "-jar", "/app/app.jar"]
