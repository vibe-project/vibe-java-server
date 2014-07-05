package org.atmosphere.vibe.runtime;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.atmosphere.vibe.Action;
import org.atmosphere.vibe.atmosphere.AtmosphereBridge;
import org.atmosphere.vibe.runtime.Socket.Reply;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.Test;

public class ProtocolTest {

    @Test
    public void protocol() throws Exception {
        final Server server = new DefaultServer();
        server.socketAction(new Action<Socket>() {
            @Override
            public void on(final Socket socket) {
                socket.on("echo", new Action<Object>() {
                    @Override
                    public void on(Object data) {
                        socket.send("echo", data);
                    }
                })
                .on("rre.resolve", new Action<Reply<Object>>() {
                    @Override
                    public void on(Reply<Object> reply) {
                        reply.resolve(reply.data());
                    }
                })
                .on("rre.reject", new Action<Reply<Object>>() {
                    @Override
                    public void on(Reply<Object> reply) {
                        reply.reject(reply.data());
                    }
                })
                .on("sre.resolve", new Action<Object>() {
                    @Override
                    public void on(Object data) {
                        socket.send("sre.resolve", data, new Action<Object>() {
                            @Override
                            public void on(Object data) {
                                socket.send("sre.done", data);
                            }
                        });
                    }
                })
                .on("sre.reject", new Action<Object>() {
                    @Override
                    public void on(Object data) {
                        socket.send("sre.reject", data, null, new Action<Object>() {
                            @Override
                            public void on(Object data) {
                                socket.send("sre.done", data);
                            }
                        });
                    }
                });
            }
        });
        
        org.eclipse.jetty.server.Server jetty = new org.eclipse.jetty.server.Server();
        ServerConnector connector = new ServerConnector(jetty);
        connector.setPort(8000);
        jetty.addConnector(connector);
        ServletContextHandler handler = new ServletContextHandler();
        jetty.setHandler(handler);
        ServletContextListener listener = new ServletContextListener() {
            @Override
            public void contextInitialized(ServletContextEvent event) {
                new AtmosphereBridge(event.getServletContext(), "/vibe").httpAction(server.httpAction()).websocketAction(server.websocketAction());
            }

            @Override
            public void contextDestroyed(ServletContextEvent sce) {
            }
        };
        handler.addEventListener(listener);
        jetty.start();
        
        // Equivalent to: mocha ./node_modules/vibe-protocol/test/server --reporter spec
        CommandLine cmdLine = CommandLine.parse("./src/test/resources/node/node ./src/test/resources/runner");
        DefaultExecutor executor = new DefaultExecutor();
        // The exit value of mocha is the number of failed tests.
        executor.setExitValue(0);
        executor.execute(cmdLine);
        
        jetty.stop();
    }

}
