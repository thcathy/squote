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

##### Developed with support from Jetbrains

![Jetbrains](https://thcathy.github.com/squote/assets/images/jetbrains-variant-4.png)

[JetBrains]( https://www.jetbrains.com/?from=esl-ionic )

