package io.github.flowersinthesand.wes.atmosphere;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import io.github.flowersinthesand.wes.Action;
import io.github.flowersinthesand.wes.ServerWebSocket;
import io.github.flowersinthesand.wes.test.ServerWebSocketTestTemplate;

import java.io.IOException;

import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResource.TRANSPORT;
import org.atmosphere.cpr.AtmosphereServlet;
import org.atmosphere.handler.AtmosphereHandlerAdapter;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.Test;

public class AtmosphereServerWebSocketTest extends ServerWebSocketTestTemplate {

	// Strictly speaking, we have to test all server Atmosphere 2 supports.
	Server server;

	@Override
	protected void startServer() throws Exception {
		server = new Server();
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(port);
		server.addConnector(connector);
		
		// Servlet
		ServletHandler handler = new ServletHandler();
		server.setHandler(handler);
		AtmosphereServlet servlet = new AtmosphereServlet();
		servlet.framework().addAtmosphereHandler("/", new AtmosphereHandlerAdapter() {
			@Override
			public void onRequest(AtmosphereResource resource) throws IOException {
				if (resource.transport() == TRANSPORT.WEBSOCKET && resource.getRequest().getMethod().equals("GET")) {
					performer.serverAction().on(new AtmosphereServerWebSocket(resource));
				}
			}
		});
		ServletHolder holder = new ServletHolder(servlet);
		holder.setAsyncSupported(true);
		handler.addServletWithMapping(holder, "/test");
		
		server.start();
	}
	
	@Test
	public void unwrap() {
		performer.serverAction(new Action<ServerWebSocket>() {
			@Override
			public void on(ServerWebSocket ws) {
				assertThat(ws.unwrap(AtmosphereResource.class), instanceOf(AtmosphereResource.class));
				performer.start();
			}
		})
		.connect();
	}

	@Override
	protected void stopServer() throws Exception {
		server.stop();
	}

}
