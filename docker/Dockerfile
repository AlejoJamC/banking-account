FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy JAR from Maven target (built outside Docker)
COPY ../target/*.jar app.jar

# Run as non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", \
    "-Xmx512m", \
    "-Xms256m", \
    "-Dspring.profiles.active=prod", \
    "-jar", \
    "app.jar"]
