package org.atmosphere.vibe.server;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.atmosphere.vibe.platform.Action;
import org.atmosphere.vibe.platform.VoidAction;
import org.atmosphere.vibe.platform.server.atmosphere2.AtmosphereBridge;
import org.atmosphere.vibe.server.ServerSocket.Reply;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.Test;

public class ProtocolTest {

    @Test
    public void protocol() throws Exception {
        final Map<String, ServerSocket> sockets = new ConcurrentHashMap<String, ServerSocket>();
        final Server server = new DefaultServer();
        server.socketAction(new Action<ServerSocket>() {
            @Override
            public void on(final ServerSocket socket) {
                sockets.put(socket.id(), socket);
                socket.on("close", new VoidAction() {
                    @Override
                    public void on() {
                        sockets.remove(socket.id());
                    }
                })
                .on("echo", new Action<Object>() {
                    @Override
                    public void on(Object data) {
                        socket.send("echo", data);
                    }
                })
                .on("/reply/inbound", new Action<Reply<Map<String, Object>>>() {
                    @Override
                    public void on(Reply<Map<String, Object>> reply) {
                        Map<String, Object> data = reply.data();
                        switch ((String) data.get("type")) {
                        case "resolved":
                            reply.resolve(data.get("data"));
                            break;
                        case "rejected":
                            reply.reject(data.get("data"));
                            break;
                        }
                    }
                })
                .on("/reply/outbound", new Action<Map<String, Object>>() {
                    @Override
                    public void on(Map<String, Object> data) {
                        switch ((String) data.get("type")) {
                        case "resolved":
                            socket.send("test", data.get("data"), new Action<Object>() {
                                @Override
                                public void on(Object data) {
                                    socket.send("done", data);
                                }
                            });
                            break;
                        case "rejected":
                            socket.send("test", data.get("data"), null, new Action<Object>() {
                                @Override
                                public void on(Object data) {
                                    socket.send("done", data);
                                }
                            });
                            break;
                        }
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
                ServletContext context = event.getServletContext();
                new AtmosphereBridge(context, "/vibe").httpAction(server.httpAction()).websocketAction(server.websocketAction());
                @SuppressWarnings("serial")
                ServletRegistration.Dynamic reg = context.addServlet("/alive", new HttpServlet() {
                    @Override
                    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
                        String id = req.getParameter("id");
                        res.getWriter().print(sockets.containsKey(id));
                    }
                });
                reg.addMapping("/alive");
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
