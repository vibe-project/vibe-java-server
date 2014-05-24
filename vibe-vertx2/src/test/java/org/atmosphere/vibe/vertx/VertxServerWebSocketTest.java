package org.atmosphere.vibe.vertx;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

import org.atmosphere.vibe.Action;
import org.atmosphere.vibe.ServerWebSocket;
import org.atmosphere.vibe.test.ServerWebSocketTestTemplate;

import org.junit.Test;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.http.HttpServer;

public class VertxServerWebSocketTest extends ServerWebSocketTestTemplate {

    HttpServer server;

    @Override
    protected void startServer() {
        server = VertxFactory.newVertx().createHttpServer();
        new VertxBridge(server, "/test").websocketAction(new Action<ServerWebSocket>() {
            @Override
            public void on(ServerWebSocket ws) {
                performer.serverAction().on(ws);
            }
        });
        server.listen(port);
    }

    @Override
    protected void stopServer() {
        server.close();
    }

    @Test
    public void unwrap() {
        performer.serverAction(new Action<ServerWebSocket>() {
            @Override
            public void on(ServerWebSocket ws) {
                assertThat(ws.unwrap(org.vertx.java.core.http.ServerWebSocket.class),
                        instanceOf(org.vertx.java.core.http.ServerWebSocket.class));
                performer.start();
            }
        })
        .connect();
    }

}
