#
# Build stage
#
FROM ubuntu:latest AS build
LABEL authors="claytonpriestley"
RUN apt-get update
RUN apt-get install maven3
RUN apt-get install openjdk-19-jre-headless -y
COPY src /home/app/src
COPY pom.xml /home/app
RUN mvn -f /home/app/pom.xml clean package

#
# Package stage
#
FROM amazoncorretto:20.0.2
COPY --from=build /home/app/target/wedding-wise-1.0.jar /usr/local/lib/wedding-wise.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/usr/local/lib/wedding-wise.jar"]