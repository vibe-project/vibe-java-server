package io.github.flowersinthesand.wes.vertx;

import io.github.flowersinthesand.wes.test.ServerWebSocketTestTemplate;

import org.vertx.java.core.Handler;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.http.HttpServer;

public class VertxServerWebSocketTest extends ServerWebSocketTestTemplate {

	HttpServer server;

	@Override
	protected void startServer() {
		server = VertxFactory.newVertx().createHttpServer()
		.websocketHandler(new Handler<org.vertx.java.core.http.ServerWebSocket>() {
			@Override
			public void handle(org.vertx.java.core.http.ServerWebSocket sws) {
				if (sws.path().equals("/test")) {
					performer.server().on(new VertxServerWebSocket(sws));
				}
			}
		})
		.listen(port);
	}

	@Override
	protected void stopServer() {
		server.close();
	}

}
