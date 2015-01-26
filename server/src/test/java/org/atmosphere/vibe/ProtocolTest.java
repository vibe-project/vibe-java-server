/*
 * Copyright 2014 The Vibe Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.atmosphere.vibe;

import java.io.IOException;
import java.util.Map;

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
import org.atmosphere.cpr.ApplicationConfig;
import org.atmosphere.vibe.ServerSocket.Reply;
import org.atmosphere.vibe.platform.action.Action;
import org.atmosphere.vibe.platform.action.VoidAction;
import org.atmosphere.vibe.platform.bridge.atmosphere2.VibeAtmosphereServlet;
import org.atmosphere.vibe.platform.http.ServerHttpExchange;
import org.atmosphere.vibe.platform.ws.ServerWebSocket;
import org.atmosphere.vibe.transport.http.HttpTransportServer;
import org.atmosphere.vibe.transport.ws.WebSocketTransportServer;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.Test;

public class ProtocolTest {

    @Test
    public void protocol() throws Exception {
        final DefaultServer server = new DefaultServer();
        server.socketAction(new Action<ServerSocket>() {
            @Override
            public void on(final ServerSocket socket) {
                socket.on("abort", new VoidAction() {
                    @Override
                    public void on() {
                        socket.close();
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
        final HttpTransportServer httpTransportServer = new HttpTransportServer().transportAction(server);
        final WebSocketTransportServer wsTransportServer = new WebSocketTransportServer().transportAction(server);

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
                        if (params.containsKey("heartbeat")) {
                            server.setHeartbeat(Integer.parseInt(params.get("heartbeat")[0]));
                        }
                        if (params.containsKey("_heartbeat")) {
                            server.set_heartbeat(Integer.parseInt(params.get("_heartbeat")[0]));
                        }
                    }
                });
                regSetup.addMapping("/setup");
                // /vibe
                ServletRegistration.Dynamic reg = context.addServlet(VibeAtmosphereServlet.class.getName(), new VibeAtmosphereServlet() {
                    @Override
                    protected Action<ServerHttpExchange> httpAction() {
                        return httpTransportServer;
                    }
                    
                    @Override
                    protected Action<ServerWebSocket> wsAction() {
                        return wsTransportServer;
                    }
                });
                reg.setAsyncSupported(true);
                reg.setInitParameter(ApplicationConfig.DISABLE_ATMOSPHEREINTERCEPTOR, Boolean.TRUE.toString());
                reg.addMapping("/vibe");
            }

            @Override
            public void contextDestroyed(ServletContextEvent sce) {}
        });
        jetty.setHandler(handler);
        jetty.start();
        
        CommandLine cmdLine = CommandLine.parse("./src/test/resources/node/node")
        .addArgument("./src/test/resources/runner")
        .addArgument("--vibe.transports")
        .addArgument("ws,httpstream,httplongpoll");
        DefaultExecutor executor = new DefaultExecutor();
        // The exit value of mocha is the number of failed tests.
        executor.execute(cmdLine);
        
        jetty.stop();
    }

}
