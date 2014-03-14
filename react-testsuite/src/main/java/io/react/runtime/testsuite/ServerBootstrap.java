package io.react.runtime.testsuite;

import io.react.runtime.DefaultServer;
import io.react.runtime.Server;
import io.react.runtime.Socket;
import io.react.runtime.Socket.Reply;
import io.react.Action;
import io.react.VoidAction;
import io.react.atmosphere.AtmosphereBridge;

import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * Bootstrap to start the test suite server.
 * <p>
 * To start the server on http://localhost:8080/,
 * <p>
 * {@code $ mvn jetty:run-war}
 * <p>
 * Then connect to http://localhost:8080/test/ using any test suite client. Also, this web server
 * serves up test suite client running on browser written in JavaScript that is used to develop
 * react.js. Open http://localhost:8080 in your browser to run the test suite in same-origin.
 * <p>
 * To run the test suite in cross-origin, start another server on http://localhost:8090/,
 * <p>
 * {@code $ mvn jetty:run-war -Djetty.port=8090}
 * <p>
 * Then open http://localhost:8090 in your browser. Test suite on 8090 will connect to 8080,
 * cross-origin.
 * 
 * @author Donghwan Kim
 */
@WebListener
public class ServerBootstrap implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        // Create a react server
        Server server = new DefaultServer();

        // This action is equivalent to socket handler of server.js in the react repo
        // https://github.com/atmosphere/react-js/blob/9738b4438514d2f9476b938c16b1869361e79d13/test/server.js#L593-L611
        server.socketAction(new Action<Socket>() {
            @Override
            public void on(final Socket socket) {
                socket.on("echo", new Action<Object>() {
                    @Override
                    public void on(Object data) {
                        socket.send("echo", data);
                    }
                })
                .on("disconnect", new VoidAction() {
                    @Override
                    public void on() {
                        new Timer(true).schedule(new TimerTask() {
                            @Override
                            public void run() {
                                socket.close();
                            }
                        }, 100);
                    }
                })
                .on("reply-by-server", new Action<Reply<Boolean>>() {
                    @Override
                    public void on(Reply<Boolean> reply) {
                        if (reply.data()) {
                            reply.done(reply.data());
                        } else {
                            reply.fail(reply.data());
                        }
                    }
                })
                .on("reply-by-client", new VoidAction() {
                    @Override
                    public void on() {
                        socket.send("reply-by-client", 1, new Action<String>() {
                            @Override
                            public void on(String type) {
                                socket.send(type);
                            }
                        });
                    }
                });
            }
        });

        // Install react server by specifying path and attaching its wes actions to wes bridge
        new AtmosphereBridge(event.getServletContext(), "/test").httpAction(server.httpAction()).websocketAction(server.websocketAction());
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {}

}
