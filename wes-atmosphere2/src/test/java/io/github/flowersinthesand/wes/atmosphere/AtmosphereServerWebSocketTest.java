package io.github.flowersinthesand.wes.atmosphere;

import io.github.flowersinthesand.wes.test.ServerWebSocketTestTemplate;

import java.io.IOException;

import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResource.TRANSPORT;
import org.atmosphere.cpr.AtmosphereServlet;
import org.atmosphere.handler.AtmosphereHandlerAdapter;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class AtmosphereServerWebSocketTest extends ServerWebSocketTestTemplate {

	// Strictly speaking, we have to test all server Atmosphere 2 supports.
	Server server;

	@Override
	protected void startServer() throws Exception {
		server = new Server();
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(port);
		server.setConnectors(new Connector[] { connector });
		AtmosphereServlet servlet = new AtmosphereServlet();
		servlet.framework().addAtmosphereHandler("/", new AtmosphereHandlerAdapter() {
			@Override
			public void onRequest(AtmosphereResource resource) throws IOException {
				if (resource.transport() == TRANSPORT.WEBSOCKET) {
					if (resource.getRequest().getMethod().equals("GET")) {
						performer.server().on(new AtmosphereServerWebSocket(resource));
					}
				}
			}
		});
		ServletHolder holder = new ServletHolder(servlet);
		holder.setAsyncSupported(true);
		ServletHandler handler = new ServletHandler();
		handler.addServletWithMapping(holder, "/test");
		server.setHandler(handler);
		server.start();
	}

	@Override
	protected void stopServer() throws Exception {
		server.stop();
	}

}
