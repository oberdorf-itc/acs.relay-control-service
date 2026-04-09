FROM maven:3-eclipse-temurin-25-alpine AS builder
WORKDIR /app
COPY pom.xml .
# Dependencies cachen (Layer-Optimierung)
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B


FROM alpine:3.23.3

LABEL maintainer="Michael Oberdorf IT-Consulting <info@oberdorf-itc.de>"
LABEL site.local.program.version="1.0.0"

ENV MQTT_SERVER=localhost \
    MQTT_PORT=1883 \
    MQTT_TLS=false \
    MQTT_CACERT_FILE=/etc/ssl/certs/ca-certificates.crt \
    MQTT_TLS_INSECURE=false \
    LDAP_SERVER=localhost \
    LDAP_PORT=389 \
    LDAP_TLS=false \
    LDAP_FILTER='(&(objectclass=ipHost)(objectclass=oitcACSAccessPointExtension)(ipHostNumber={entrypoint_ip}))' \
    REQUESTS_CA_BUNDLE=/etc/ssl/certs/ca-certificates.crt \
    PROMETHEUS_LISTENER_ADDR=0.0.0.0 \
    PROMETHEUS_LISTENER_PORT=8080 \
    TZ=UTC

RUN apk upgrade --available --no-cache --update \
    && apk add --no-cache --update \
       ca-certificates=20251003-r0 \
       curl=8.17.0-r1 \
       openjdk25-jre-headless=25.0.2_p10-r1 \
    && addgroup -g 2300 -S javauser \
    && adduser -u 2300 -S javauser -G javauser \
    && rm -rf /var/cache/apk/* /tmp/* /var/tmp/* \
    && mkdir -p /app/etc /app/lib

COPY --chown=root:root docker-entrypoint.sh /docker-entrypoint.sh
COPY --chown=root:root src/main/resources/* /app/etc/.
COPY --from=builder /app/target/*.jar /app/app.jar

USER javauser:javauser
EXPOSE ${PROMETHEUS_LISTENER_PORT}
HEALTHCHECK --interval=1m --timeout=5s --retries=30 --start-period=5m \
            CMD curl -skSL http://localhost:${PROMETHEUS_LISTENER_PORT}/ -o /dev/null || exit 1

# Start Server
CMD ["docker-entrypoint.sh"]
