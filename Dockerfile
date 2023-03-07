FROM adoptopenjdk:11-jre-hotspot
RUN apt-get update && apt-get install librrds-perl rrdtool -y
COPY . /app
RUN ls
ENTRYPOINT ["java", "-jar", "/app/target/OrdersService-0.0.1-SNAPSHOT.jar"]