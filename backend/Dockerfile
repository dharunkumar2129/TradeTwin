# Stage 1: Build executable Spring Boot JAR using existing Maven wrapper
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Lightweight Java 21 runtime tuned for 512 MB hosting instances (e.g., Render Free)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S tradetwin && adduser -S tradetwin -G tradetwin
COPY --from=builder /app/target/tradetwin-backend-1.0.0-SNAPSHOT.jar /app/app.jar
RUN chown tradetwin:tradetwin /app/app.jar

USER tradetwin

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java -XX:+UseContainerSupport -XX:InitialRAMPercentage=35.0 -XX:MaxRAMPercentage=60.0 -XX:MaxMetaspaceSize=110m -Xss512k -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom -Dserver.address=0.0.0.0 -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
