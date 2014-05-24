package org.atmosphere.vibe.atmosphere;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

import org.atmosphere.vibe.Action;
import org.atmosphere.vibe.ServerHttpExchange;
import org.atmosphere.vibe.test.ServerHttpExchangeTestTemplate;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.atmosphere.cpr.AtmosphereResource;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
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

        // ServletContext
        ServletContextHandler handler = new ServletContextHandler();
        server.setHandler(handler);
        ServletContextListener listener = new ServletContextListener() {
            @Override
            public void contextInitialized(ServletContextEvent event) {
                new AtmosphereBridge(event.getServletContext(), "/test").httpAction(new Action<ServerHttpExchange>() {
                    @Override
                    public void on(ServerHttpExchange http) {
                        performer.serverAction().on(http);
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

    @Override
    protected void stopServer() throws Exception {
        server.stop();
    }

    @Test
    public void unwrap() {
        performer.serverAction(new Action<ServerHttpExchange>() {
            @Override
            public void on(ServerHttpExchange http) {
                assertThat(http.unwrap(AtmosphereResource.class), instanceOf(AtmosphereResource.class));
                performer.start();
            }
        })
        .send();
    }

    @Override
    @Test
    @Ignore
    public void closeAction_by_client() {
    }

}
