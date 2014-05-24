package org.atmosphere.vibe.vertx;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

import org.atmosphere.vibe.Action;
import org.atmosphere.vibe.ServerHttpExchange;
import org.atmosphere.vibe.test.ServerHttpExchangeTestTemplate;

import org.junit.Ignore;
import org.junit.Test;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;

public class VertxServerHttpExchangeTest extends ServerHttpExchangeTestTemplate {

    HttpServer server;

    @Override
    protected void startServer() {
        server = VertxFactory.newVertx().createHttpServer();
        new VertxBridge(server, "/test").httpAction(new Action<ServerHttpExchange>() {
            @Override
            public void on(ServerHttpExchange http) {
                performer.serverAction().on(http);
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
        performer.serverAction(new Action<ServerHttpExchange>() {
            @Override
            public void on(ServerHttpExchange http) {
                assertThat(http.unwrap(HttpServerRequest.class), instanceOf(HttpServerRequest.class));
                performer.start();
            }
        })
        .send();
    }

    @Override
    @Test
    @Ignore
    public void closeAction_by_server() {
    }

}
