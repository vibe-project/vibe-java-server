package org.atmosphere.vibe.runtime;

import java.io.IOException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.atmosphere.vibe.Action;
import org.atmosphere.vibe.runtime.Socket.Reply;
import org.atmosphere.vibe.vertx.VertxBridge;
import org.junit.Test;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.http.HttpServer;

public class ProtocolTest {

    @Test
    public void protocol() throws ExecuteException, IOException {
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
                .on("replyable", new Action<Reply<Boolean>>() {
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

        final HttpServer httpServer = VertxFactory.newVertx().createHttpServer();
        new VertxBridge(httpServer, "/vibe").httpAction(server.httpAction()).websocketAction(server.websocketAction());
        httpServer.listen(8000);
        
        // Equivalent to: mocha ./node_modules/vibe-protocol/test/server --reporter spec
        CommandLine cmdLine = CommandLine.parse("./src/test/resources/node/node ./src/test/resources/runner");
        DefaultExecutor executor = new DefaultExecutor();
        // The exit value of mocha is the number of failed tests.
        executor.setExitValue(0);
        executor.execute(cmdLine);
        
        httpServer.close();
    }

}
