FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon --console=plain bootJar

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S app && adduser -S -H -G app app
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
