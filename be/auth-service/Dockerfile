FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace
COPY . /workspace
# Xây bằng Gradle wrapper của repo gốc (chạy từ thư mục be/ với -p auth-service)
# Dockerfile này giả định context build = thư mục be/ (xem hướng dẫn trong README)
RUN ./gradlew :auth-service:bootJar -x test --no-daemon -Dorg.gradle.jvmargs="-Xmx512m" \
    && rm -f /workspace/auth-service/build/libs/*-plain.jar

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/auth-service/build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
