package org.atmosphere.vibe.server;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

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
        final Set<String> sockets = new ConcurrentSkipListSet<String>();
        final DefaultServer server = new DefaultServer();
        server.socketAction(new Action<ServerSocket>() {
            @Override
            public void on(final ServerSocket socket) {
                sockets.add(socket.id());
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
        handler.addEventListener(new ServletContextListener() {
            @Override
            @SuppressWarnings("serial")
            public void contextInitialized(ServletContextEvent event) {
                ServletContext context = event.getServletContext();
                // /setup
                ServletRegistration regSetup = context.addServlet("/setup", new HttpServlet() {
                    @Override
                    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
                        Map<String, String[]> params = req.getParameterMap();
                        server.setTransports(params.get("transports")[0].split(","));
                        if (params.containsKey("heartbeat")) {
                            server.setHeartbeat(Integer.parseInt(params.get("heartbeat")[0]));
                        }
                        if (params.containsKey("_heartbeat")) {
                            server.set_heartbeat(Integer.parseInt(params.get("_heartbeat")[0]));
                        }
                    }
                });
                regSetup.addMapping("/setup");
                // /alive
                ServletRegistration regAlive = context.addServlet("/alive", new HttpServlet() {
                    @Override
                    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
                        res.getWriter().print(sockets.contains(req.getParameter("id")));
                    }
                });
                regAlive.addMapping("/alive");
                // /vibe
                new AtmosphereBridge(context, "/vibe").httpAction(server.httpAction()).websocketAction(server.wsAction());
            }

            @Override
            public void contextDestroyed(ServletContextEvent sce) {}
        });
        jetty.setHandler(handler);
        jetty.start();
        
        CommandLine cmdLine = CommandLine.parse("./src/test/resources/node/node")
        .addArgument("./src/test/resources/runner")
        .addArgument("--vibe.transports")
        .addArgument("ws,sse,streamxhr,streamxdr,streamiframe,longpollajax,longpollxdr,longpolljsonp")
        .addArgument("--vibe.extension")
        .addArgument("reply");
        DefaultExecutor executor = new DefaultExecutor();
        // The exit value of mocha is the number of failed tests.
        executor.execute(cmdLine);
        
        jetty.stop();
    }

}
