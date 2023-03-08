FROM node:alpine as git
RUN apk add --no-cache git
RUN apk add --no-cache openssh
WORKDIR /tmp
RUN git clone https://github.com/iunnlu/estore-core.git

FROM adoptopenjdk/maven-openjdk11 as maven-builder
WORKDIR /tmp
COPY . ./
COPY --from=git /tmp ./
ENV MAVEN_OPTS="-Xmx1024m -XX:MaxPermSize=128m"
WORKDIR /tmp/estore-core
RUN mvn clean install -DskipTests=true
WORKDIR /tmp
RUN mvn clean package -DskipTests=true

FROM adoptopenjdk/openjdk11
ENV JAR_FILE=target/OrdersService*.jar
COPY --from=maven-builder /tmp/$JAR_FILE /opt/app/
RUN mv /opt/app/OrdersService-*.jar /opt/app/app.jar
WORKDIR /opt/app
ENV PORT 0
EXPOSE 0
ENTRYPOINT ["java","-jar","app.jar"]