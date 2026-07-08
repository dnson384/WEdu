# dependencies
FROM eclipse-temurin:17-alpine-3.23 AS dependencies
RUN apk add --no-cache maven
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline

# build stage
FROM dependencies AS build
COPY src ./src
RUN mvn clean package -DskipTests

# run stage
FROM eclipse-temurin:17-jre-alpine AS runtime
RUN adduser -D fckedu_backend
WORKDIR /app
COPY --from=build /build/target/backend-0.0.1-SNAPSHOT.jar /app/backend-0.0.1-SNAPSHOT.jar
RUN chown -R fckedu_backend:fckedu_backend /app
USER fckedu_backend
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/backend-0.0.1-SNAPSHOT.jar"]