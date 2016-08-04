How to test in eclipse

Start SpringQuoteWebApplication.java with VM args:
-Dserver.port=8090 -Dhttp.proxyHost= -Dhttp.proxyPort= -Dhttp.proxyUsername= -Dhttp.proxyPassword= -javaagent:C:\dev\app\springloaded\springloaded-1.2.1.RELEASE.jar -noverify -Dspring.thymeleaf.cache=false -Dspring.profiles.active=dev

### Squote
A personal java web application running on JRE 8.

*Pre-requisites*: [Started mongodb](https://hub.docker.com/_/mongo/)

```bash
docker run thcathy/squote
  --name <container name> \
  -e MONGO_HOST=<mongodb url> \
  -p <host's port>:8090 \
  --link <mongodb container name> \
```