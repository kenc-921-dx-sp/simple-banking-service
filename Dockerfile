FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -B -DskipTests package \
    && find target -maxdepth 1 -name "*.jar" \
       ! -name "*.original" -exec cp {} application.jar \;

FROM eclipse-temurin:21-jre-jammy
WORKDIR /application

COPY --from=build --chown=1001:0 /workspace/application.jar application.jar
RUN chgrp -R 0 /application && chmod -R g=u /application

USER 1001
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "application.jar"]
