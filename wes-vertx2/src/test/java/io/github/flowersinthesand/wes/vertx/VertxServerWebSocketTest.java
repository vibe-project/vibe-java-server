package io.github.flowersinthesand.wes.vertx;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import io.github.flowersinthesand.wes.Action;
import io.github.flowersinthesand.wes.ServerWebSocket;
import io.github.flowersinthesand.wes.test.ServerWebSocketTestTemplate;

import org.junit.Test;
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
					performer.serverAction().on(new VertxServerWebSocket(sws));
				}
			}
		})
		.listen(port);
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
