FROM maven:3.9.16-eclipse-temurin-25 AS build
WORKDIR /build
COPY pom.xml .
COPY product-service/pom.xml product-service/pom.xml
COPY supplier-service/pom.xml supplier-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY order-service/src order-service/src
RUN mvn -B -pl order-service -am -DskipTests package
FROM eclipse-temurin:25-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /build/order-service/target/order-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java","-jar","app.jar"]
