package org.atmosphere.vibe.runtime.testee;

import org.atmosphere.vibe.Action;
import org.atmosphere.vibe.atmosphere.AtmosphereBridge;
import org.atmosphere.vibe.runtime.DefaultServer;
import org.atmosphere.vibe.runtime.Server;
import org.atmosphere.vibe.runtime.Socket;
import org.atmosphere.vibe.runtime.Socket.Reply;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * Bootstrap to start the testee server.
 * <p>
 * To start the server:
 * <p>
 * {@code $ mvn jetty:run -Djetty.port=8000}
 * <p>
 * How to run the test suite is available at 
 * https://github.com/atmosphere/vibe-protocol#testing
 * 
 * @author Donghwan Kim
 */
@WebListener
public class ServerBootstrap implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        Server server = new DefaultServer();

        server.socketAction(new Action<Socket>() {
            @Override
            public void on(final Socket socket) {
                socket.on("echo", new Action<Object>() {
                    @Override
                    public void on(Object data) {
                        socket.send("echo", data);
                    }
                })
                .on("vibeion", new Action<Reply<Boolean>>() {
                    @Override
                    public void on(Reply<Boolean> reply) {
                        if (reply.data()) {
                            reply.resolve(reply.data());
                        } else {
                            reply.reject(reply.data());
                        }
                    }
                });
            }
        });

        new AtmosphereBridge(event.getServletContext(), "/vibe").httpAction(server.httpAction()).websocketAction(server.websocketAction());
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {}

}
