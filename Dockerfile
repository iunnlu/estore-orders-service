FROM node:alpine as git
RUN apk add --no-cache git
RUN apk add --no-cache openssh
WORKDIR /tmp
RUN git clone https://github.com/iunnlu/estore-core.git

FROM adoptopenjdk/maven-openjdk11 as maven-builder
WORKDIR /tmp
COPY . ./
COPY --from=git /tmp ./
RUN ls
ENV MAVEN_OPTS="-Xmx1024m -XX:MaxPermSize=128m"
WORKDIR /tmp/estore-core
RUN ls
RUN mvn clean package -DskipTests=true
WORKDIR /tmp
RUN ls
RUN mvn clean package -DskipTests=true

FROM adoptopenjdk/openjdk11
ENV JAR_FILE=target/OrdersService*.jar
COPY --from=maven-builder /tmp/$JAR_FILE /opt/app/
RUN mv /opt/app/OrdersService-*.jar /opt/app/app.jar
WORKDIR /opt/app
ENV PORT 8080
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]