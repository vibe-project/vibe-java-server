package io.github.flowersinthesand.wes.atmosphere;

import io.github.flowersinthesand.wes.test.ServerHttpExchangeTestTemplate;

import java.io.IOException;

import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResource.TRANSPORT;
import org.atmosphere.cpr.AtmosphereServlet;
import org.atmosphere.handler.AtmosphereHandlerAdapter;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.Ignore;
import org.junit.Test;

public class AtmosphereServerHttpExchangeTest extends ServerHttpExchangeTestTemplate {

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
				if (resource.transport() != TRANSPORT.WEBSOCKET) {
					performer.serverAction().on(new AtmosphereServerHttpExchange(resource));
				}
			}
		});
		ServletHolder holder = new ServletHolder(servlet);
		holder.setAsyncSupported(true);
		handler.addServletWithMapping(holder, "/test");
		
		server.start();
	}

	@Override
	protected void stopServer() throws Exception {
		server.stop();
	}
	
	@Override
	@Test
	@Ignore
	public void closeAction_by_client() {}

}
