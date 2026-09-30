# Etapa 1: compila com Maven (dependências em camada própria, para aproveitar o cache).
FROM maven:3.9-eclipse-temurin-21 AS compilacao
WORKDIR /fonte
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# Etapa 2: só o JRE e o jar, com usuário sem privilégios.
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 aparelho
WORKDIR /app
COPY --from=compilacao /fonte/target/tres-em-um.jar tres-em-um.jar
USER aparelho
EXPOSE 5260
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "tres-em-um.jar"]
