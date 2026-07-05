FROM eclipse-temurin:17-jdk-alpine as dependencies
RUN apk add --no-cache maven
WORKDIR /build
COPY pom.xml .
RUN mvn dependencies:go-offline

FROM dependencies AS builder
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine AS runtime
RUN apk add --no-cache pandoc
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
WORKDIR /app
RUN mkdir -p /static/uploads && chown -R spring:spring static
COPY --from=builder /build/target/*.jar app.jar
ENTRYPOINT [ "java", "-jar", "app.jar" ]