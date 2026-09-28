FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace
COPY . /workspace
RUN ./gradlew :catalog-service:bootJar -x test --no-daemon -Dorg.gradle.jvmargs="-Xmx512m" \
    && rm -f /workspace/catalog-service/build/libs/*-plain.jar

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/catalog-service/build/libs/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
