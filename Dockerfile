# =============================================================================
# Stage 1: CSS Build Stage – PurgeCSS (cz-css-1005) + cssnano (cz-css-1004)
# =============================================================================
FROM node:18-alpine AS css-build

WORKDIR /css-build

COPY css-build/package.json ./
RUN npm install --production

COPY WebContent/pikaday.css ./src/pikaday.css
COPY WebContent/styles.css  ./src/styles.css
COPY css-build/postcss.config.js ./
COPY WebContent/index.html ./content/index.html
COPY WebContent/login.jsp  ./content/login.jsp
COPY WebContent/main.js    ./content/main.js

RUN mkdir -p dist && \
    npx postcss src/pikaday.css --output dist/pikaday.css && \
    npx postcss src/styles.css  --output dist/styles.css

# =============================================================================
# Stage 2: Java Application Build Stage
# =============================================================================
FROM maven:3.9.6-eclipse-temurin-11 AS java-build

WORKDIR /workspace

COPY pom.xml ./
RUN mvn dependency:go-offline -B

COPY src ./src
COPY WebContent ./WebContent

COPY --from=css-build /css-build/dist/pikaday.css ./WebContent/pikaday.css
COPY --from=css-build /css-build/dist/styles.css  ./WebContent/styles.css

RUN mvn clean package -DskipTests -B

# =============================================================================
# Stage 3: Production Runtime Image
# Uses amazoncorretto:8 as the explicit base image with Open Liberty runtime.
# =============================================================================
FROM amazoncorretto:8

ARG LIBERTY_VERSION=23.0.0.12

# Install Open Liberty
RUN yum install -y tar gzip shadow-utils && \
    curl -fsSL https://public.dhe.ibm.com/ibmdl/export/pub/software/websphere/wasdev/downloads/wlp/23.0.0.12/wlp-kernel-23.0.0.12.zip \
         -o /tmp/wlp.zip && \
    mkdir -p /opt/ol && \
    cd /opt/ol && \
    jar xf /tmp/wlp.zip && \
    rm /tmp/wlp.zip && \
    yum clean all

ENV WLP_HOME=/opt/ol/wlp
ENV PATH="${WLP_HOME}/bin:${PATH}"

# Create non-root user
RUN groupadd -r liberty && useradd -r -g liberty -d /opt/ol -s /sbin/nologin liberty && \
    chown -R liberty:liberty /opt/ol

USER liberty

WORKDIR /opt/ol/wlp

# Create the server
RUN server create modresorts

# Copy server configuration
COPY --chown=liberty:liberty src/main/liberty/config /opt/ol/wlp/usr/servers/modresorts/

# Copy the built WAR
COPY --from=java-build --chown=liberty:liberty /workspace/target/modresorts-2.0.0.war \
     /opt/ol/wlp/usr/servers/modresorts/apps/modresorts.war

# Install required Liberty features
RUN installUtility install modresorts --acceptLicense || true

EXPOSE 9080 9443

ENV TZ=UTC
ENV JVM_ARGS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

CMD ["server", "run", "modresorts"]
