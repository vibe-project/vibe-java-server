package io.github.flowersinthesand.wes.jwa;

import io.github.flowersinthesand.wes.Action;
import io.github.flowersinthesand.wes.ServerWebSocket;
import io.github.flowersinthesand.wes.test.ServerWebSocketTestTemplate;

import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

public class JwaServerWebSocketTest extends ServerWebSocketTestTemplate {

	Server server;

	@Override
	protected void startServer() throws Exception {
		server = new Server();
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(port);
		server.setConnectors(new Connector[] { connector });
		ServletContextHandler handler = new ServletContextHandler();
		server.setHandler(handler);
		ServerEndpointConfig config = new JwaBridge("/test").websocketAction(new Action<ServerWebSocket>() {
			@Override
			public void on(ServerWebSocket ws) {
				performer.serverAction().on(ws);
			}
		})
		.config();
        ServerContainer container = WebSocketServerContainerInitializer.configureContext(handler);
        container.addEndpoint(config);
		server.start();
	}

	@Override
	protected void stopServer() throws Exception {
		server.stop();
	}

}
