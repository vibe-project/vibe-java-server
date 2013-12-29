# Vert.x 2
`wes-vertx2` module integrates wes application with the [Vert.x 2](http://vertx.io/) which is an event driven application framework.

## Install
Install via `vertx` or include the following module in a basic manner:
```
com.github.flowersinthesand~wes-vertx2~${wes.version}
```

## Recipe
To run an application with Vert.x:

```java
HttpServer httpServer = vertx.createHttpServer();
httpServer.requestHandler(new Handler<HttpServerRequest>() {
    public void handle(HttpServerRequest req) {
        // Check path
        if (req.path().startsWith("/portal")) {
            // Dispatch ServerHttpExchange to somewhere
            new VertxServerHttpExchange(req);
        }
    }
});
httpServer.websocketHandler(new Handler<org.vertx.java.core.http.ServerWebSocket>() {
    public void handle(org.vertx.java.core.http.ServerWebSocket socket) {
        // Check path
        if (socket.path().startsWith("/portal")) {
            // Dispatch ServerWebSocket to somewhere
            new VertxServerWebSocket(socket);
        }
    }
});
httpServer.listen(8080);
```