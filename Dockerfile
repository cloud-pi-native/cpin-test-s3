# First stage: complete build environment
FROM maven:3.8.4-openjdk-21 AS builder

# add pom.xml and source code
ADD ./pom.xml pom.xml
ADD test.txt test.txt
ADD ./src src/
RUN mvn clean package -Dmaven.test.skip=true

FROM gcr.io/distroless/java21:nonroot
WORKDIR /app
COPY --from=builder target/*.jar /app/app.jar
COPY --from=builder test.txt /app/test.txt

CMD ["-jar", "/app/app.jar"]
