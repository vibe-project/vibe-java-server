# Vert.x 2
`wes-vertx2` module integrates wes application with the [Vert.x 2](http://vertx.io/) which is an event driven application framework.

## Install
Install via `vertx` or include the following module in a basic manner:

```
com.github.flowersinthesand~wes-vertx2~${wes.version}
```

To install a wes in Vert.x, prepare `Vertx`.

```java
public class Initializer extends Verticle {
    @Override
    public void start() {
        // Available as a protected field.
        vertx; 
    }
}
```

Using `Vertx`, create `HttpServer`, register a request handler and a websocket handler and start it.   
```java
HttpServer httpServer = vertx.createHttpServer();
httpServer.requestHandler(requestHandler);
httpServer.websocketHandler(websocketHandler);
httpServer.listen(8080);
```

In the handlers, check path, wrap a given event with `VertxServerHttpExchange` or `VertxServerWebSocket` and dispatch them to somewhere.
```java
Handler<HttpServerRequest> requestHandler = new Handler<HttpServerRequest>() {
    @Override
    public void handle(HttpServerRequest req) {
        // Path
        if (req.path().startsWith("/portal")) {
            // ServerHttpExchange
            new VertxServerHttpExchange(req);
        }
    }
};
Handler<org.vertx.java.core.http.ServerWebSocket> websocketHandler = new Handler<org.vertx.java.core.http.ServerWebSocket>() {
    @Override
    public void handle(org.vertx.java.core.http.ServerWebSocket socket) {
        // Path
        if (socket.path().startsWith("/portal")) {
            // ServerWebSocket
            new VertxServerWebSocket(socket);
        }
    }
};
```