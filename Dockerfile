# Imagem do GymSync para hospedagem (Render ou qualquer serviço com Docker).
# As senhas NÃO entram na imagem: vêm das variáveis de ambiente do perfil prod (ver application-prod.properties).

# 1) Compila o .jar
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B package -DskipTests

# 2) Imagem final, só com o Java e o .jar, rodando sem ser root
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --uid 1001 gymsync
COPY --from=build /app/target/sistema-academia-*.jar app.jar
USER gymsync
ENV SPRING_PROFILES_ACTIVE=prod
# Pouca memória nos planos grátis (512 MB): limita o Java para caber
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Xss512k -XX:TieredStopAtLevel=1"
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
