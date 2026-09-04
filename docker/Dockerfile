# Stage 1: Build
FROM eclipse-temurin:25-jdk-noble AS builder

WORKDIR /app

# Copy gradle files
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .

# Pre-download dependencies to improve build caching
RUN ./gradlew dependencies --no-daemon || true

# Copy source code
COPY src src

# Build the fat JAR
RUN ./gradlew shadowJar --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:25-jre-noble

# Erstelle einen dedizierten Benutzer
RUN useradd -m brief

WORKDIR /app

# Kopiere das JAR und setze Berechtigungen
COPY --from=builder /app/build/libs/brief-*-all.jar /app/brief.jar
RUN chown brief:brief /app/brief.jar

# Terminal-Support verbessern
ENV TERM=xterm-256color

# Wechsel zum nicht-privilegierten Benutzer
USER brief
ENV HOME=/home/brief

# Die Anwendung benötigt Native Access für das Terminal (tui4j/jline)
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "/app/brief.jar"]
