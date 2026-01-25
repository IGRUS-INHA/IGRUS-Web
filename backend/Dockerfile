FROM eclipse-temurin:21-jdk

ENV SPRING_ACTIVE_PROFILE=prod

ARG JAR_FILE=build/libs/igrus-web-server.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]