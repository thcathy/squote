FROM openjdk:13-alpine
ADD target/squote.jar /app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-Xmx1g","-jar","/app.jar"]
