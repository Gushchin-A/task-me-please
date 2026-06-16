FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

COPY . .

RUN chmod +x ./gradlew

RUN ./gradlew --no-daemon clean bootJar -x test

RUN cp build/libs/*.jar application.jar


FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /workspace/application.jar application.jar

ENTRYPOINT ["java", "-jar", "application.jar"]