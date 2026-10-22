FROM maven:3.9.6-eclipse-temurin-11

WORKDIR /app

COPY pom.xml .
COPY src ./src/
COPY src/test/resources/serenity.conf ./src/test/resources/serenity.conf

RUN mvn dependency:resolve

CMD ["mvn", "clean", "verify", "-Denvironment=default", "-Dwebdriver.driver=remote"]