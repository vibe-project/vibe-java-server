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
package org.atmosphere.vibe.test;

import org.atmosphere.vibe.Action;
import org.atmosphere.vibe.Data;
import org.atmosphere.vibe.ServerWebSocket;
import org.atmosphere.vibe.VoidAction;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.net.ServerSocket;
import java.net.URI;
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Template class to test {@link ServerWebSocket}.
 *
 * @author Donghwan Kim
 */
public abstract class ServerWebSocketTestTemplate {

    @Rule
    public Timeout globalTimeout = new Timeout(10000);
    protected Performer performer;
    protected int port;

    @Before
    public void before() throws Exception {
        performer = new Performer();
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            port = serverSocket.getLocalPort();
        }
        startServer();
    }

    @After
    public void after() throws Exception {
        stopServer();
    }

    /**
     * Starts the server listening port {@link ServerWebSocketTestTemplate#port}
     * and if WebSocket's path is {@code /test}, create {@link ServerWebSocket}
     * and pass it to {@code performer.serverAction()}. This method is executed
     * following {@link Before}.
     */
    protected abstract void startServer() throws Exception;

    /**
     * Stops the server started in
     * {@link ServerWebSocketTestTemplate#startServer()}. This method is
     * executed following {@link After}.
     *
     * @throws Exception
     */
    protected abstract void stopServer() throws Exception;

    @Test
    public void uri() {
        performer.serverAction(new Action<ServerWebSocket>() {
            @Override
            public void on(ServerWebSocket ws) {
                assertThat(ws.uri(), is("/test?hello=there"));
                performer.start();
            }
        })
        .connect("/test?hello=there");
    }

    @Test
    public void close() {
        performer.clientListener(new WebSocketAdapter() {
            @Override
            public void onWebSocketClose(int statusCode, String reason) {
                performer.start();
            }
        })
        .serverAction(new Action<ServerWebSocket>() {
            @Override
            public void on(ServerWebSocket ws) {
                ws.close();
            }
        })
        .connect();
    }

    @Test
    public void close_idempotent() {
        performer.clientListener(new WebSocketAdapter() {
            @Override
            public void onWebSocketClose(int statusCode, String reason) {
                performer.start();
            }
        })
        .serverAction(new Action<ServerWebSocket>() {
            @Override
            public void on(ServerWebSocket ws) {
                ws.close().close();
            }
        })
        .connect();
    }

    @Test
    public void send() {
        performer.clientListener(new WebSocketAdapter() {
            @Override
            public void onWebSocketText(String message) {
                assertThat(message, is("A Will Remains in the Ashes"));
                performer.start();
            }
        })
        .serverAction(new Action<ServerWebSocket>() {
            @Override
            public void on(ServerWebSocket ws) {
                ws.send("A Will Remains in the Ashes");
            }
        })
        .connect();
    }

    @Test
    public void messageAction() {
        performer.clientListener(new WebSocketAdapter() {
            @Override
            public void onWebSocketConnect(Session sess) {
                sess.getRemote().sendString("A road of winds the water builds", new WriteCallback() {
                    @Override
                    public void writeSuccess() {
                        assertThat(true, is(true));
                    }

                    @Override
                    public void writeFailed(Throwable x) {
                        assertThat(true, is(false));
                    }
                });
            }
        })
        .serverAction(new Action<ServerWebSocket>() {
            @Override
            public void on(ServerWebSocket ws) {
                ws.messageAction(new Action<Data>() {
                    @Override
                    public void on(Data data) {
                        assertThat(data.as(String.class), is("A road of winds the water builds"));
                        performer.start();
                    }
                });
            }
        })
        .connect();
    }

    // TODO
    // How to test errorAction??

    @Test
    public void closeAction_by_server() {
        performer.serverAction(new Action<ServerWebSocket>() {
            @Override
            public void on(final ServerWebSocket ws) {
                ws.close().closeAction(new VoidAction() {
                    @Override
                    public void on() {
                        performer.start();
                    }
                });
            }
        })
        .connect();
    }

    @Test
    public void closeAction_by_client() {
        performer.clientListener(new WebSocketAdapter() {
            @Override
            public void onWebSocketConnect(Session sess) {
                sess.close();
            }
        })
        .serverAction(new Action<ServerWebSocket>() {
            @Override
            public void on(ServerWebSocket ws) {
                ws.closeAction(new VoidAction() {
                    @Override
                    public void on() {
                        performer.start();
                    }
                });
            }
        })
        .connect();
    }

    protected class Performer {

        CountDownLatch latch = new CountDownLatch(1);
        WebSocketListener clientListener = new WebSocketAdapter();
        Action<ServerWebSocket> serverAction = new Action<ServerWebSocket>() {
            @Override
            public void on(ServerWebSocket object) {
            }
        };

        public Performer clientListener(WebSocketListener clientListener) {
            this.clientListener = clientListener;
            return this;
        }

        public Action<ServerWebSocket> serverAction() {
            return serverAction;
        }

        public Performer serverAction(Action<ServerWebSocket> serverAction) {
            this.serverAction = serverAction;
            return this;
        }

        public Performer connect() {
            return connect("/test");
        }

        public Performer connect(String uri) {
            WebSocketClient client = new WebSocketClient();
            try {
                client.start();
                client.connect(clientListener, URI.create("ws://localhost:" + port + uri));
                latch.await();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    client.stop();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return this;
        }

        public Performer start() {
            latch.countDown();
            return this;
        }

    }

}
