# Build stage
FROM maven:3.8.7-openjdk-17 AS build
COPY src /app/src
COPY pom.xml /app
RUN mvn -f /app/pom.xml clean package

# Run stage
FROM openjdk:17-jdk-slim
COPY --from=build /app/target/currency-converter-1.0-SNAPSHOT.jar /app.jar
EXPOSE 5000
ENTRYPOINT ["java","-jar","/app.jar"]
