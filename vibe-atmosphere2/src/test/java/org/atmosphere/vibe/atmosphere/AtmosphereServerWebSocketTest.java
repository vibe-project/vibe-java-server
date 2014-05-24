package org.atmosphere.vibe.atmosphere;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

import org.atmosphere.vibe.Action;
import org.atmosphere.vibe.ServerWebSocket;
import org.atmosphere.vibe.test.ServerWebSocketTestTemplate;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.atmosphere.cpr.AtmosphereResource;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
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

        // ServletContext
        ServletContextHandler handler = new ServletContextHandler();
        server.setHandler(handler);
        ServletContextListener listener = new ServletContextListener() {
            @Override
            public void contextInitialized(ServletContextEvent event) {
                new AtmosphereBridge(event.getServletContext(), "/test").websocketAction(new Action<ServerWebSocket>() {
                    @Override
                    public void on(ServerWebSocket ws) {
                        performer.serverAction().on(ws);
                    }
                });
            }

            @Override
            public void contextDestroyed(ServletContextEvent sce) {
            }
        };
        handler.addEventListener(listener);

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
