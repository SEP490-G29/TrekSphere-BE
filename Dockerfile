
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw mvnw.cmd ./

RUN mvn dependency:go-offline -B --no-transfer-progress
COPY src/ src/

RUN mvn package -DskipTests -B --no-transfer-progress
FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar


RUN chown appuser:appgroup app.jar

USER appuser
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+ExitOnOutOfMemoryError \
               -XX:+TieredCompilation \
               -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

STOPSIGNAL SIGTERM

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
