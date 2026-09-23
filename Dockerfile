# =============================================================================
# Multi-Stage Dockerfile for ModResorts (Java 8 WAR Application)
# Build Tool : Maven (system mvn — no wrapper)
# Builder    : maven:3.8.6-openjdk-8-slim
# Runtime    : eclipse-temurin:8-jdk-alpine  (explicit base image)
# Target     : GCP GKE
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1 – Builder
# -----------------------------------------------------------------------------
FROM maven:3.8.6-openjdk-8-slim AS builder

WORKDIR /workspace

# Copy dependency descriptor first for layer-cache optimisation
COPY pom.xml .

# Pre-download all Maven dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy the full project source (excluding files listed in .dockerignore)
COPY . .

# Build the WAR, skip tests
RUN mvn clean package -DskipTests -B

# -----------------------------------------------------------------------------
# Stage 2 – Runtime
# Explicit base image: eclipse-temurin:8-jdk-alpine
# -----------------------------------------------------------------------------
FROM eclipse-temurin:8-jdk-alpine

# Metadata labels
LABEL maintainer="ModResorts Team" \
      application="modresorts" \
      version="2.0.0" \
      description="ModResorts Java EE Web Application"

# Timezone configuration
ENV TZ=UTC \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UnlockExperimentalVMOptions" \
    APP_HOME=/opt/modresorts

# Install a minimal servlet container (Tomcat 9 – compatible with Servlet 3.1 / Java EE 7)
ENV CATALINA_HOME=/opt/tomcat \
    CATALINA_OUT=/dev/stdout

RUN apk add --no-cache bash tzdata \
    && cp /usr/share/zoneinfo/UTC /etc/localtime \
    && echo "UTC" > /etc/timezone \
    # Download and install Tomcat 9
    && wget -q https://archive.apache.org/dist/tomcat/tomcat-9/v9.0.85/bin/apache-tomcat-9.0.85.tar.gz -O /tmp/tomcat.tar.gz \
    && mkdir -p ${CATALINA_HOME} \
    && tar -xzf /tmp/tomcat.tar.gz -C ${CATALINA_HOME} --strip-components=1 \
    && rm /tmp/tomcat.tar.gz \
    # Remove default Tomcat webapps to reduce attack surface
    && rm -rf ${CATALINA_HOME}/webapps/ROOT \
               ${CATALINA_HOME}/webapps/examples \
               ${CATALINA_HOME}/webapps/docs \
               ${CATALINA_HOME}/webapps/host-manager \
               ${CATALINA_HOME}/webapps/manager \
    # Create non-root user
    && addgroup -S modresorts \
    && adduser -S -G modresorts -h ${APP_HOME} modresorts \
    && mkdir -p ${APP_HOME} \
    && chown -R modresorts:modresorts ${CATALINA_HOME} ${APP_HOME}

# Copy the built WAR from the builder stage
COPY --from=builder /workspace/target/modresorts-2.0.0.war ${CATALINA_HOME}/webapps/ROOT.war

# Adjust ownership
RUN chown modresorts:modresorts ${CATALINA_HOME}/webapps/ROOT.war

USER modresorts

# Expose application port
EXPOSE 8080

# Start Tomcat with JVM options
CMD ["sh", "-c", "export JAVA_OPTS=\"${JAVA_OPTS}\" && ${CATALINA_HOME}/bin/catalina.sh run"]
