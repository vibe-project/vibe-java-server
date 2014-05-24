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
import org.atmosphere.vibe.HttpStatus;
import org.atmosphere.vibe.ServerHttpExchange;
import org.atmosphere.vibe.VoidAction;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpMethod;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Template class to test {@link ServerHttpExchange}.
 *
 * @author Donghwan Kim
 */
public abstract class ServerHttpExchangeTestTemplate {

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
     * Starts the server listening port
     * {@link ServerHttpExchangeTestTemplate#port} and if HTTP request's path is
     * {@code /test}, create {@link ServerHttpExchange} and pass it to
     * {@code performer.serverAction()}. This method is executed following
     * {@link Before}.
     */
    protected abstract void startServer() throws Exception;

    /**
     * Stops the server started in
     * {@link ServerHttpExchangeTestTemplate#startServer()}. This method is
     * executed following {@link After}.
     *
     * @throws Exception
     */
    protected abstract void stopServer() throws Exception;

    @Test
    public void uri() {
        performer.serverAction(new Action<ServerHttpExchange>() {
            @Override
            public void on(ServerHttpExchange http) {
                assertThat(http.uri(), is("/test?hello=there"));
                performer.start();
            }
        })
        .send("/test?hello=there");
    }

    @Test
    public void method() {
        performer.serverAction(new Action<ServerHttpExchange>() {
            @Override
            public void on(ServerHttpExchange http) {
                assertThat(http.method(), is("POST"));
                performer.start();
            }
        })
        .send(new Action<Request>() {
            @Override
            public void on(Request req) {
                req.method(HttpMethod.POST);
            }
        });
    }

    @Test
    public void requestHeader() {
        performer.serverAction(new Action<ServerHttpExchange>() {
            @Override
            public void on(ServerHttpExchange http) {
                assertThat(http.requestHeaderNames(), either(hasItems("a", "b")).or(hasItems("A", "B")));
                assertThat(http.requestHeader("A"), is("A"));
                assertThat(http.requestHeader("B"), is("B1"));
                assertThat(http.requestHeaders("A"), contains("A"));
                assertThat(http.requestHeaders("B"), contains("B1", "B2"));
                performer.start();
            }
        })
        .send(new Action<Request>() {
            @Override
            public void on(Request req) {
                req.header("A", "A").header("B", "B1").header("B", "B2");
            }
        });
    }

    @Test
    public void bodyAction() {
        performer.serverAction(new Action<ServerHttpExchange>() {
            @Override
            public void on(ServerHttpExchange http) {
                http.bodyAction(new Action<Data>() {
                    @Override
                    public void on(Data data) {
                        assertThat(data.as(String.class), is("A Breath Clad In Happiness"));
                        performer.start();
                    }
                });
            }
        })
        .send(new Action<Request>() {
            @Override
            public void on(Request req) {
                req.content(new StringContentProvider("A Breath Clad In Happiness"));
            }
        });
    }

    @Test
    public void bodyAction_charset() {
        performer.serverAction(new Action<ServerHttpExchange>() {
            @Override
            public void on(ServerHttpExchange http) {
                http.bodyAction(new Action<Data>() {
                    @Override
                    public void on(Data data) {
                        assertThat(data.as(String.class), is("희망을 잃고 쓰러져 가도 언젠가 다시 되돌아온다"));
                        performer.start();
                    }
                });
            }
        })
        .send(new Action<Request>() {
            @Override
            public void on(Request req) {
                req.content(new StringContentProvider("희망을 잃고 쓰러져 가도 언젠가 다시 되돌아온다"), "text/plain; charset=utf-8");
            }
        });
    }

    @Test
    public void setResponseHeader() {
        performer.serverAction(new Action<ServerHttpExchange>() {
            @Override
            public void on(ServerHttpExchange http) {
                http.setResponseHeader("A", "A").setResponseHeader("B", Arrays.asList("B1", "B2")).close();
            }
        })
        .responseListener(new Response.Listener.Adapter() {
            @Override
            public void onSuccess(Response res) {
                HttpFields headers = res.getHeaders();
                assertThat(headers.getFieldNamesCollection(), hasItems("A", "B"));
                assertThat(headers.get("A"), is("A"));
                assertThat(headers.get("B"), is("B1, B2"));
                performer.start();
            }
        })
        .send();
    }

    @Test
    public void write() {
        performer.serverAction(new Action<ServerHttpExchange>() {
            @Override
            public void on(ServerHttpExchange http) {
                http.write("X").write("Y").write("Z").close();
            }
        })
        .responseListener(new Response.Listener.Adapter() {
            List<String> chunks = new ArrayList<>();

            @Override
            public void onContent(Response response, ByteBuffer content) {
                byte[] bytes = new byte[content.remaining()];
                content.get(bytes);
                chunks.add(new String(bytes, Charset.forName("ISO-8859-1")));
            }

            @Override
            public void onSuccess(Response response) {
                assertThat(chunks, contains("X", "Y", "Z"));
                performer.start();
            }
        })
        .send();
    }

    @Test
    public void close() {
        performer.serverAction(new Action<ServerHttpExchange>() {
            @Override
            public void on(ServerHttpExchange http) {
                http.close();
            }
        })
        .responseListener(new Response.Listener.Adapter() {
            @Override
            public void onSuccess(Response response) {
                performer.start();
            }
        })
        .send();
    }

    @Test
    public void close_with_data() {
        performer.serverAction(new Action<ServerHttpExchange>() {
            @Override
            public void on(ServerHttpExchange http) {
                http.close("Out of existence");
            }
        })
        .responseListener(new Response.Listener.Adapter() {
            String body;

            @Override
            public void onContent(Response response, ByteBuffer content) {
                byte[] bytes = new byte[content.remaining()];
                content.get(bytes);
                body = new String(bytes, Charset.forName("ISO-8859-1"));
            }

            @Override
            public void onSuccess(Response response) {
                assertThat(body, is("Out of existence"));
                performer.start();
            }
        })
        .send();
    }

    @Test
    public void setStatus() {
        performer.serverAction(new Action<ServerHttpExchange>() {
            @Override
            public void on(ServerHttpExchange http) {
                http.setStatus(HttpStatus.NOT_FOUND).close();
            }
        })
        .responseListener(new Response.Listener.Adapter() {
            @Override
            public void onSuccess(Response response) {
                assertThat(response.getStatus(), is(404));
                performer.start();
            }
        })
        .send();
    }

    @Test
    public void closeAction_by_server() {
        performer.serverAction(new Action<ServerHttpExchange>() {
            @Override
            public void on(ServerHttpExchange http) {
                http.close().closeAction(new VoidAction() {
                    @Override
                    public void on() {
                        performer.start();
                    }
                });
            }
        })
        .send();
    }

    @Test
    public void closeAction_by_client() {
        performer.serverAction(new Action<ServerHttpExchange>() {
            @Override
            public void on(ServerHttpExchange http) {
                http.closeAction(new VoidAction() {
                    @Override
                    public void on() {
                        performer.start();
                    }
                });
            }
        })
        .send(new Action<Request>() {
            @Override
            public void on(final Request req) {
                new Timer(true).schedule(new TimerTask() {
                    @Override
                    public void run() {
                        req.abort(new RuntimeException());
                    }
                }, 1000);
            }
        });
    }

    protected class Performer {

        CountDownLatch latch = new CountDownLatch(1);
        Request.Listener requestListener = new Request.Listener.Adapter();
        Response.Listener responseListener = new Response.Listener.Adapter();
        Action<ServerHttpExchange> serverAction = new Action<ServerHttpExchange>() {
            @Override
            public void on(ServerHttpExchange object) {
            }
        };

        public Performer requestListener(Request.Listener requestListener) {
            this.requestListener = requestListener;
            return this;
        }

        public Performer responseListener(Response.Listener responseListener) {
            this.responseListener = responseListener;
            return this;
        }

        public Action<ServerHttpExchange> serverAction() {
            return serverAction;
        }

        public Performer serverAction(Action<ServerHttpExchange> serverAction) {
            this.serverAction = serverAction;
            return this;
        }

        public Performer send() {
            return send("/test");
        }

        public Performer send(Action<Request> requestAction) {
            return send("/test", requestAction);
        }

        public Performer send(String uri) {
            return send(uri, null);
        }

        public Performer send(String uri, Action<Request> requestAction) {
            HttpClient client = new HttpClient();
            try {
                client.start();
                Request req = client.newRequest("http://localhost:" + port + uri);
                if (requestAction != null) {
                    requestAction.on(req);
                }
                req.listener(requestListener).send(responseListener);
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
