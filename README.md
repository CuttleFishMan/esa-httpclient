# ESA HttpClient

![Build](https://github.com/esastack/esa-httpclient/workflows/Build/badge.svg?branch=main)
[![codecov](https://codecov.io/gh/esastack/esa-httpclient/branch/main/graph/badge.svg?token=D85SMNQNK0)](https://codecov.io/gh/esastack/esa-httpclient)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.esastack/httpclient/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.esastack/httpclient/)
[![GitHub license](https://img.shields.io/github/license/esastack/esa-httpclient)](https://github.com/esastack/esa-httpclient/blob/main/LICENSE)

ESA HttpClient is an asynchronous event-driven http client based on netty.

## Features

- Http1/H2/H2cUpgrade
- Https
- HAProxy
- Epoll/NIO
- Interceptor
- Filter
- Retry, Redirect, 100-expect-continue
- Chunked read/write
- Multipart
- Metrics
- more features...

## Maven Dependency

```xml
<dependency>
    <groupId>com.github.esastack</groupId>
    <artifactId>httpclient</artifactId>
    <version>${esa-httpclient.version}</version>
</dependency>
```

## Quick Start

```java
final HttpClient client = HttpClient.create()
        .version(HttpVersion.HTTP_2)
        .h2ClearTextUpgrade(true)
        .build();

// Example 1: automatic aggregation
final HttpResponse response = client.async(HttpRequest.get("http://127.0.0.1:8081/").build()).get();
logger.info(response.body().string(StandardCharsets.UTF_8));

// Example 2: chunk read
client.execute(HttpRequest.get("http://127.0.0.1:8081/handle")
        .handle(h -> {
            h.onData(data -> {
                logger.info("Received response data, size: " + data.readableBytes());
            }).onEnd(v -> {
                logger.info("Response has ended");
            });
        }).build());

// Example 3: chunk write
final ChunkRequest chunk = client.prepare("http://127.0.0.1:8081/handle").build();

chunk.write("Hello World!".getBytes());
chunk.write("Continue sending data...".getBytes());
chunk.end("It's end!".getBytes());

client.close();
```
