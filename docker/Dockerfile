FROM eclipse-temurin:21-jre
ADD target/squote.jar /app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-Xmx1g","-Dlogging.config=classpath:logback-docker.xml","-jar","/app.jar"]
