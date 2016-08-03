FROM java:8-alpine
ADD target/squote.jar /app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar","-Xmx1024m -XX:+UseConcMarkSweepGC"]
