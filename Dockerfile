FROM java:8-alpine
ADD target/squote.jar /app.jar
HEALTHCHECK --interval=30s --timeout=300s --retries=3 CMD curl -sS http://localhost:8090 || exit 1
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-Xmx1g","-jar","/app.jar"]
