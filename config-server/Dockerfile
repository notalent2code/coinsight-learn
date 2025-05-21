FROM maven:3.9-amazoncorretto-17 AS builder
WORKDIR /app
# Copy the entire project
COPY . /app/
# Build the specific module
RUN mvn clean package -pl config-server -am -DskipTests

FROM amazoncorretto:17-alpine
WORKDIR /app
COPY --from=builder /app/config-server/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]