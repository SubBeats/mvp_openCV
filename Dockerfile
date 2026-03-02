FROM maven:3.8-eclipse-temurin-8-alpine AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:8-jre-alpine
RUN apk add --no-cache libstdc++
WORKDIR /app

COPY --from=build /app/target/mvp-opencv-*.jar app.jar

EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
