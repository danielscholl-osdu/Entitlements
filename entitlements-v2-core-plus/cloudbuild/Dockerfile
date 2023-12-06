# THIS ASAP manual BUILD jar ent v2
FROM azul/zulu-openjdk:17
WORKDIR /app
ARG PROVIDER_NAME
ENV PROVIDER_NAME core-plus
ARG PORT
ENV PORT $PORT

# Copy plugins
COPY tmp/os-oqm-rabbitmq-*.jar plugins/oqm-rabbitmq.jar

## Copy the jar to the production image from the builder stage.
COPY entitlements-v2-${PROVIDER_NAME}/target/entitlements-v2-${PROVIDER_NAME}-*-spring-boot.jar entitlements-${PROVIDER_NAME}.jar
# Add a non-root user
RUN groupadd -g 10001 -r nonroot \
  && useradd -g 10001 -r -u 10001 nonroot
# Run as non-root user
USER 10001:10001
# Run the web service on container startup.
CMD java -Djava.security.egd=file:/dev/./urandom \
         -Dserver.port=${PORT} \
         -Dlog4j.formatMsgNoLookups=true \
         -Dloader.path=plugins/ \
         -Dloader.debug=true \
         -Dloader.main=org.opengroup.osdu.entitlements.v2.jdbc.EntitlementsV2Application \
         -jar entitlements-${PROVIDER_NAME}.jar
