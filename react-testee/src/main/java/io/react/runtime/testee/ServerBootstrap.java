package io.react.runtime.testee;

import io.react.Action;
import io.react.atmosphere.AtmosphereBridge;
import io.react.runtime.DefaultServer;
import io.react.runtime.Server;
import io.react.runtime.Socket;
import io.react.runtime.Socket.Reply;

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
 * https://github.com/atmosphere/react-protocol#testing
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
                .on("reaction", new Action<Reply<Boolean>>() {
                    @Override
                    public void on(Reply<Boolean> reply) {
                        if (reply.data()) {
                            reply.done(reply.data());
                        } else {
                            reply.fail(reply.data());
                        }
                    }
                });
            }
        });

        new AtmosphereBridge(event.getServletContext(), "/react").httpAction(server.httpAction()).websocketAction(server.websocketAction());
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {}

}
