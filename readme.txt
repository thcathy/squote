How to test in eclipse

Start SpringQuoteWebApplication.java with VM args:
-Dserver.port=8090 -Dhttp.proxyHost= -Dhttp.proxyPort= -Dhttp.proxyUsername= -Dhttp.proxyPassword= -javaagent:C:\dev\app\springloaded\springloaded-1.2.1.RELEASE.jar -noverify -Dspring.thymeleaf.cache=false -Dspring.profiles.active=dev