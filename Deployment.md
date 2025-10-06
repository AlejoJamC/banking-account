# Deployment

## Prerequisites
- Docker 20.10+
- Docker Compose 2.0+
- Maven 3.8+ (to build JAR)

## Build & Run

1. Build the application JAR:
   ```bash
   mvn clean package -DskipTests
   ```

2. Start services with Docker Compose:
   ```bash
   cd docker
   docker-compose up --build
   ```

3. Access the application:
    - API: http://localhost:8080
    - Health: http://localhost:8080/actuator/health

## Stop Services
```bash
cd docker
docker-compose down
```

## Clean Up (including volumes)
```bash
cd docker
docker-compose down -v
```

## Verify Deployment
```bash
# Check running containers
docker ps

# Check logs
docker logs banking-account-app
docker logs banking-account-db

# Test endpoint
curl http://localhost:8080/actuator/health
```
