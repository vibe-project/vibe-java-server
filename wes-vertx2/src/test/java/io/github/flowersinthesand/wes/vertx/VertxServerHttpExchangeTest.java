package io.github.flowersinthesand.wes.vertx;

import io.github.flowersinthesand.wes.test.ServerHttpExchangeTestTemplate;

import org.junit.Ignore;
import org.junit.Test;
import org.vertx.java.core.Handler;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;

public class VertxServerHttpExchangeTest extends ServerHttpExchangeTestTemplate {

	HttpServer server;

	@Override
	protected void startServer() {
		server = VertxFactory.newVertx().createHttpServer()
		.requestHandler(new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest req) {
				if (req.path().equals("/test")) {
					performer.serverAction().on(new VertxServerHttpExchange(req));
				}
			}
		})
		.listen(port);
	}

	@Override
	protected void stopServer() {
		server.close();
	}
	
	@Override
	@Test
	@Ignore
	public void closeAction_by_server() {}
	
}
