# syntax=docker/dockerfile:1

# --- Build stage -------------------------------------------------------
FROM eclipse-temurin:17-jdk AS build
WORKDIR /build

COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B clean package

# --- Runtime stage -------------------------------------------------------
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN useradd --system --create-home --shell /usr/sbin/nologin misd
COPY --from=build /build/target/*.jar app.jar
RUN chown misd:misd app.jar
USER misd

EXPOSE 8080

# Required at runtime (no defaults, by design — see application.properties):
#   MISD_DB_USERNAME, MISD_DB_PASSWORD
#   MISD_MINIO_ACCESS_KEY, MISD_MINIO_SECRET_KEY   (only if storage.mode=minio)
#   MISD_STORAGE_ROOT                              (only if storage.mode=filesystem)
#   MISD_DEMO_USER_PASSWORD                        (only if app.security.demo-mode=true)
# The application deliberately fails to start if a required one is missing.
ENTRYPOINT ["java", "-jar", "app.jar"]
