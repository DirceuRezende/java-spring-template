# syntax=docker/dockerfile:1

# ---- 1. build: compila o jar. Cache do ~/.m2 entre builds (BuildKit).
FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /workspace
COPY mvnw pom.xml ./
COPY .mvn .mvn
# Só pom/wrapper primeiro: dependências não são baixadas de novo quando só o código muda
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q dependency:go-offline
COPY src src
# Testes ficam fora do build da imagem: precisam de Docker (Testcontainers). Rode ./mvnw test no CI antes.
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q package -DskipTests \
    && cp target/user-template-*.jar application.jar

# ---- 2. extract: separa o jar nas camadas do Spring Boot (da que menos muda para a que mais muda)
FROM eclipse-temurin:25-jdk-noble AS extract
WORKDIR /extract
COPY --from=build /workspace/application.jar .
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# ---- 3. runtime: só JRE, usuário sem root, camadas em ordem para reaproveitar cache do registry
FROM eclipse-temurin:25-jre-noble
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /application
COPY --from=extract /extract/extracted/dependencies/ ./
COPY --from=extract /extract/extracted/spring-boot-loader/ ./
COPY --from=extract /extract/extracted/snapshot-dependencies/ ./
COPY --from=extract /extract/extracted/application/ ./
USER app
EXPOSE 8080
# Heap proporcional ao limite de memória do container
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "application.jar"]
