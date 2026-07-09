# ---- Etapa 1: BUILD (compila o .jar com Maven) ----
# Usamos uma imagem com Maven + JDK 21 só para construir.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copiamos primeiro só o pom.xml e baixamos as dependências. Assim, se o código
# mudar mas o pom não, o Docker reaproveita o cache deste passo (build mais rápido).
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Agora o código e o empacotamento. Pulamos os testes aqui (rodam na máquina/CI);
# o objetivo desta imagem é só gerar o artefato.
COPY src ./src
RUN mvn clean package -DskipTests -B

# ---- Etapa 2: RUNTIME (imagem enxuta, só com o Java de execução + o .jar) ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# A Railway define a porta na variável PORT; nosso application.properties já lê
# server.port=${PORT:8080}, então o app escuta na porta certa automaticamente.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
