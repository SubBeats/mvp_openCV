# Сборка JAR
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

# Запуск
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/target/mvp-opencv-*.jar app.jar

EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
