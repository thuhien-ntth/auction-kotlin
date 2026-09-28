FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace
COPY . /workspace
RUN ./gradlew :bidding-service:bootJar -x test --no-daemon -Dorg.gradle.jvmargs="-Xmx512m" \
    && rm -f /workspace/bidding-service/build/libs/*-plain.jar

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/bidding-service/build/libs/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
