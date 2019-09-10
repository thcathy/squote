FROM openjdk:10
ADD target/squote.jar /app.jar
HEALTHCHECK --interval=30s --timeout=300s --retries=3 CMD curl -sS http://localhost:8091 || exit 1
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-Xmx1g","-Xlog:gc","-jar","/app.jar"]
