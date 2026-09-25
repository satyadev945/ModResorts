# =============================================================================
# Stage 1: CSS Optimisation – PurgeCSS + cssnano
# =============================================================================
FROM node:18-alpine AS css-optimizer

WORKDIR /css-build

COPY css-build/package.json ./
RUN npm install --no-fund --no-audit

COPY WebContent/styles.css   ./src/styles.css
COPY WebContent/pikaday.css  ./src/pikaday.css
COPY WebContent/index.html   ./WebContent/index.html
COPY WebContent/login.jsp    ./WebContent/login.jsp
COPY WebContent/main.js      ./WebContent/main.js

COPY css-build/postcss.config.js  ./
COPY css-build/purgecss.config.js ./

RUN npx postcss src/pikaday.css --output dist/pikaday.css \
 && npx postcss src/styles.css  --output dist/styles.css

# =============================================================================
# Stage 2: Java Application Build (Maven)
# =============================================================================
FROM maven:3.8.6-openjdk-8-slim AS java-builder

WORKDIR /workspace

# Cache Maven dependencies before copying full source
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Copy application source and static assets
COPY src ./src
COPY WebContent ./WebContent

# Overlay purged + minified CSS produced in Stage 1
COPY --from=css-optimizer /css-build/dist/pikaday.css ./WebContent/pikaday.css
COPY --from=css-optimizer /css-build/dist/styles.css  ./WebContent/styles.css

# Package the WAR (tests run separately in CI pipeline)
RUN mvn clean package -B -DskipTests

# =============================================================================
# Stage 3: Liberty Runtime Setup
# =============================================================================
FROM ibmcom/websphere-liberty:23.0.0.12-full-java11-openj9-ubi AS liberty-setup

COPY --from=java-builder /workspace/target/modresorts-2.0.0.war \
     /config/apps/modresorts.war

# =============================================================================
# Stage 4: Production Runtime
# Base image: eclipse-temurin:8-jre (explicit)
# =============================================================================
FROM eclipse-temurin:8-jre

ENV JAVA_HOME=/opt/java/openjdk \
    LIBERTY_HOME=/opt/ol \
    TZ=UTC \
    JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0" \
    WLP_OUTPUT_DIR=/opt/ol/usr/servers \
    LOG_DIR=/logs

# Create non-root user for security
RUN groupadd -r appgroup && useradd -r -g appgroup -d /opt/ol -s /bin/bash appuser

# Copy Liberty runtime and application from liberty-setup stage
COPY --from=liberty-setup /opt/ol /opt/ol
COPY --from=liberty-setup /config /config
COPY --from=liberty-setup /logs /logs 2>/dev/null || true

RUN chown -R appuser:appgroup /opt/ol /config /logs 2>/dev/null || true

USER appuser

EXPOSE 9080

ENTRYPOINT ["/opt/ol/bin/server", "run", "defaultServer"]
