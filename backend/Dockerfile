FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY backend /app/backend
RUN mvn -f /app/backend/pom.xml -pl blackcar-web -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/backend/blackcar-web/target/blackcar-web-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENV SERVER_PORT=8080
ENTRYPOINT ["java","-jar","app.jar"]
