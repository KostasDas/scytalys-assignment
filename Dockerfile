FROM maven:3.10.0-eclipse-temurin-27 AS development

ARG LOCAL_UID=1000
ARG LOCAL_GID=1000
RUN mkdir -p /home/developer/.m2 \
    && chown -R "${LOCAL_UID}:${LOCAL_GID}" /home/developer

WORKDIR /workspace

CMD ["./mvnw", "spring-boot:run"]

FROM maven:3.10.0-eclipse-temurin-27 AS build

WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -q -DskipTests dependency:go-offline
COPY src/ src/
RUN ./mvnw -DskipTests package

FROM eclipse-temurin:27-jre AS production

WORKDIR /app
RUN groupadd --system trackpicture && useradd --system --gid trackpicture trackpicture
COPY --from=build /workspace/target/*.jar app.jar
USER trackpicture:trackpicture

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
