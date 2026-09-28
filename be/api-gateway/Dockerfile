FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace
COPY . /workspace
RUN ./gradlew :api-gateway:bootJar -x test --no-daemon -Dorg.gradle.jvmargs="-Xmx512m" \
    && rm -f /workspace/api-gateway/build/libs/*-plain.jar

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/api-gateway/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
