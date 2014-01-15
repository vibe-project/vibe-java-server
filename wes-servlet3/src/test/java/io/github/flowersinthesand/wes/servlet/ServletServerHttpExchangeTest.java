package io.github.flowersinthesand.wes.servlet;

import io.github.flowersinthesand.wes.test.ServerHttpExchangeTestTemplate;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.Ignore;
import org.junit.Test;

public class ServletServerHttpExchangeTest extends ServerHttpExchangeTestTemplate {

	Server server;

	@Override
	protected void startServer() throws Exception {
		server = new Server();
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(port);
		server.setConnectors(new Connector[] { connector });
		@SuppressWarnings("serial")
		ServletHolder holder = new ServletHolder(new HttpServlet() {
			@Override
			protected void service(HttpServletRequest req, HttpServletResponse res) {
				performer.serverAction().on(new ServletServerHttpExchange(req, res));
			}
		});
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
	
	@Override
	@Test
	@Ignore
	public void closeAction_by_client() {}

}
