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
package org.atmosphere.vibe.server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.atmosphere.vibe.platform.Action;
import org.atmosphere.vibe.platform.Actions;
import org.atmosphere.vibe.platform.ConcurrentActions;
import org.atmosphere.vibe.platform.HttpStatus;
import org.atmosphere.vibe.platform.VoidAction;
import org.atmosphere.vibe.platform.Wrapper;
import org.atmosphere.vibe.platform.server.ServerHttpExchange;
import org.atmosphere.vibe.platform.server.ServerWebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Default implementation of {@link Server}.
 * <p>
 * This implementation consumes {@link ServerHttpExchange} and
 * {@link ServerWebSocket} and provides {@link ServerSocket} following the Vibe
 * protocol.
 * <p>
 * The following options are configurable.
 * <ul>
 * <li>{@link DefaultServer#setHeartbeat(int)}</li>
 * </ul>
 * 
 * @author Donghwan Kim
 */
public class DefaultServer implements Server {

    private final Logger log = LoggerFactory.getLogger(DefaultServer.class);
    private Set<ServerSocket> sockets = new CopyOnWriteArraySet<>();
    private int heartbeat = 20000;
    private int _heartbeat = 5000;
    // Socket
    private Actions<ServerSocket> socketActions = new ConcurrentActions<ServerSocket>()
    .add(new Action<ServerSocket>() {
        @Override
        public void on(ServerSocket s) {
            final DefaultServerSocket socket = (DefaultServerSocket) s;
            sockets.add(socket);
            socket.on("close", new VoidAction() {
                @Override
                public void on() {
                    sockets.remove(socket);
                }
            });
            socket.setHeartbeat(heartbeat);
        }
    });
    // Transport
    private Action<Transport> transportAction = new Action<Transport>() {
        @Override
        public void on(Transport transport) {
            Map<String, String> map = new LinkedHashMap<>();
            map.put("heartbeat", "" + heartbeat);
            map.put("_heartbeat", "" + _heartbeat);
            socketActions.fire(new DefaultServerSocket(transport, map));
        }
    };
    // HTTP transport
    private Map<String, HttpTransport> httpTransports = new ConcurrentHashMap<>();
    private Action<ServerHttpExchange> httpAction = new Action<ServerHttpExchange>() {
        @Override
        public void on(final ServerHttpExchange http) {
            final Map<String, String> params = parseQuery(http.uri());
            switch (http.method()) {
            case "GET":
                setNocache(http);
                setCors(http);
                switch (params.get("when")) {
                case "open": {
                    String transportName = params.get("transport");
                    final HttpTransport transport = createTransport(transportName, http);
                    if (transport != null) {
                        httpTransports.put(transport.id, transport);
                        transport.closeActions.add(new VoidAction() {
                            @Override
                            public void on() {
                                httpTransports.remove(transport.id);
                            }
                        });
                        transportAction.on(transport);
                    } else {
                        log.error("Transport, {}, is not implemented", transportName);
                        http.setStatus(HttpStatus.NOT_IMPLEMENTED).end();
                    }
                    break;
                }
                case "poll": {
                    String id = params.get("id");
                    HttpTransport transport = httpTransports.get(id);
                    if (transport != null && transport instanceof LongpollTransport) {
                        ((LongpollTransport) transport).refresh(http);
                    } else {
                        log.error("Long polling transport#{} is not found", id);
                        http.setStatus(HttpStatus.INTERNAL_SERVER_ERROR).end();
                    }
                    break;
                }
                case "abort": {
                    String id = params.get("id");
                    HttpTransport transport = httpTransports.get(id);
                    if (transport != null) {
                        transport.close();
                    }
                    http.setHeader("content-type", "text/javascript; charset=utf-8").end();
                    break;
                }
                default:
                    log.error("when, {}, is not supported", params.get("when"));
                    http.setStatus(HttpStatus.NOT_IMPLEMENTED).end();
                    break;
                }
                break;
            case "POST":
                setNocache(http);
                setCors(http);
                http.bodyAction(new Action<String>() {
                    @Override
                    public void on(String body) {
                        String data = body.substring("data=".length());
                        String id = params.get("id");
                        HttpTransport transport = httpTransports.get(id);
                        if (transport != null) {
                            transport.messageActions.fire(data);
                        } else {
                            log.error("A POST message arrived but no transport#{} is found", id);
                            http.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
                        }
                        http.end();
                    };
                })
                .read();
                break;
            default:
                log.error("HTTP method, {}, is not supported", http.method());
                http.setStatus(HttpStatus.METHOD_NOT_ALLOWED).end();
                break;
            }
        }

        private void setNocache(ServerHttpExchange http) {
            http.setHeader("cache-control", "no-cache, no-store, must-revalidate")
            .setHeader("pragma", "no-cache")
            .setHeader("expires", "0");
        }

        private void setCors(ServerHttpExchange http) {
            String origin = http.header("origin");
            http.setHeader("access-control-allow-origin", origin != null ? origin : "*")
            .setHeader("access-control-allow-credentials", "true");
        }
        
        private HttpTransport createTransport(String transportName, ServerHttpExchange http) {
            switch (transportName) {
            case "stream":
                return new StreamTransport(http);
            case "longpoll":
                return new LongpollTransport(http);
            default:
                return null;
            }
        }
    };
    // WebSocket transport
    private Action<ServerWebSocket> wsAction = new Action<ServerWebSocket>() {
        @Override
        public void on(ServerWebSocket ws) {
            transportAction.on(new WebSocketTransport(ws));
        }
    };

    @Override
    public Sentence all() {
        return new Sentence(new Action<Action<ServerSocket>>() {
            @Override
            public void on(Action<ServerSocket> action) {
                all(action);
            }
        });
    }

    @Override
    public Server all(Action<ServerSocket> action) {
        for (ServerSocket socket : sockets) {
            action.on(socket);
        }
        return this;
    }

    @Override
    public Sentence byTag(final String... names) {
        return new Sentence(new Action<Action<ServerSocket>>() {
            @Override
            public void on(Action<ServerSocket> action) {
                byTag(names, action);
            }
        });
    }

    @Override
    public Server byTag(String name, Action<ServerSocket> action) {
        return byTag(new String[] { name }, action);
    }

    @Override
    public Server byTag(String[] names, Action<ServerSocket> action) {
        List<String> nameList = Arrays.asList(names);
        for (ServerSocket socket : sockets) {
            if (socket.tags().containsAll(nameList)) {
                action.on(socket);
            }
        }
        return this;
    }

    @Override
    public Server socketAction(Action<ServerSocket> action) {
        socketActions.add(action);
        return this;
    }

    @Override
    public Action<ServerHttpExchange> httpAction() {
        return httpAction;
    }

    @Override
    public Action<ServerWebSocket> wsAction() {
        return wsAction;
    }

    /**
     * A heartbeat interval in milliseconds to maintain a connection alive and
     * prevent server from holding idle connections. The default is
     * <code>20</code>s and should be larger than <code>5</code>s.
     */
    public void setHeartbeat(int heartbeat) {
        this.heartbeat = heartbeat;
    }

    /**
     * To speed up the protocol tests. Not for production use.
     */
    public void set_heartbeat(int _heartbeat) {
        this._heartbeat = _heartbeat;
    }

    private static Map<String, String> parseQuery(String uri) {
        Map<String, String> map = new LinkedHashMap<>();
        String query = URI.create(uri).getQuery();
        if (query == null || query.equals("")) {
            return Collections.unmodifiableMap(map);
        }
        String[] params = query.split("&");
        for (String param : params) {
            try {
                String[] pair = param.split("=", 2);
                String name = URLDecoder.decode(pair[0], "UTF-8");
                if (name.equals("")) {
                    continue;
                }

                map.put(name, pair.length > 1 ? URLDecoder.decode(pair[1], "UTF-8") : "");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        return Collections.unmodifiableMap(map);
    }

    private static String stringifyQuery(Map<String, String> params) {
        StringBuilder query = new StringBuilder();
        for (Entry<String, String> entry : params.entrySet()) {
            try {
                query.append(URLEncoder.encode(entry.getKey(), "UTF-8"))
                .append("=").append(URLEncoder.encode(entry.getValue(), "UTF-8"))
                .append("&");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        return query.deleteCharAt(query.length() - 1).toString();
    }

    private abstract static class Transport implements Wrapper {
        Actions<String> messageActions = new ConcurrentActions<>();
        Actions<Throwable> errorActions = new ConcurrentActions<>();
        Actions<Void> closeActions = new ConcurrentActions<>(new Actions.Options().once(true).memory(true));

        abstract String uri();

        abstract void send(String data);

        abstract void close();
        
        synchronized void handshake(Map<String, String> map) {
            send("?" + stringifyQuery(map));
        }
    }

    private static class WebSocketTransport extends Transport {
        final ServerWebSocket ws;

        WebSocketTransport(ServerWebSocket ws) {
            this.ws = ws;
            ws.errorAction(new Action<Throwable>() {
                @Override
                public void on(Throwable throwable) {
                    errorActions.fire(throwable);
                }
            })
            .closeAction(new VoidAction() {
                @Override
                public void on() {
                    closeActions.fire();
                }
            })
            .textAction(new Action<String>() {
                @Override
                public void on(String data) {
                    messageActions.fire(data);
                }
            });
        }

        @Override
        String uri() {
            return ws.uri();
        }

        @Override
        synchronized void send(String data) {
            ws.send(data);
        }

        @Override
        synchronized void close() {
            ws.close();
        }

        @Override
        public <T> T unwrap(Class<T> clazz) {
            return ws.unwrap(clazz);
        }
    }

    private abstract static class HttpTransport extends Transport {
        final String id = UUID.randomUUID().toString();
        final ServerHttpExchange http;
        final Map<String, String> params;

        HttpTransport(ServerHttpExchange http) {
            this.params = parseQuery(http.uri());
            this.http = http;
            http.errorAction(new Action<Throwable>() {
                @Override
                public void on(Throwable throwable) {
                    errorActions.fire(throwable);
                }
            });
        }

        @Override
        String uri() {
            return http.uri();
        }

        @Override
        synchronized void handshake(Map<String, String> map) {
            map.put("id", id);
            super.handshake(map);
        }

        @Override
        public <T> T unwrap(Class<T> clazz) {
            return http.unwrap(clazz);
        }
    }

    
    private static class StreamTransport extends HttpTransport {
        final static String text2KB = CharBuffer.allocate(2048).toString().replace('\0', ' ');
        
        StreamTransport(ServerHttpExchange http) {
            super(http);
            // Reads the request to make closeAction be fired on http.end
            http.read().closeAction(new VoidAction() {
                @Override
                public void on() {
                    closeActions.fire();
                }
            })
            .setHeader("content-type",
                "text/" + ("true".equals(params.get("sse")) ? "event-stream" : "plain") + "; charset=utf-8")
            .write(text2KB + "\n");
        }

        @Override
        synchronized void send(String data) {
            String payload = "";
            for (String datum : data.split("\r\n|\r|\n")) {
                payload += "data: " + datum + "\n";
            }
            payload += "\n";
            http.write(payload);
        }

        @Override
        synchronized void close() {
            http.end();
        }
    }

    private static class LongpollTransport extends HttpTransport {
        AtomicReference<ServerHttpExchange> httpRef = new AtomicReference<>();
        AtomicBoolean aborted = new AtomicBoolean();
        AtomicBoolean completed = new AtomicBoolean();
        AtomicBoolean written = new AtomicBoolean();
        AtomicReference<Timer> closeTimer = new AtomicReference<>();
        AtomicInteger msgId = new AtomicInteger();
        Map<String, String> cache = new ConcurrentHashMap<>();
        ObjectMapper mapper = new ObjectMapper();

        LongpollTransport(ServerHttpExchange http) {
            super(http);
            refresh(http);
        }

        void refresh(ServerHttpExchange http) {
            final Map<String, String> parameters = parseQuery(http.uri());
            // Reads the request to make closeAction be fired on http.end
            http.read().closeAction(new VoidAction() {
                @Override
                public void on() {
                    completed.set(true);
                    if (parameters.get("when").equals("poll") && !written.get()) {
                        closeActions.fire();
                    } else {
                        Timer timer = new Timer(true);
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                closeActions.fire();
                            }
                        }, 2000);
                        closeTimer.set(timer);
                    }
                }
            })
            .setHeader("content-type",
                "text/" + ("true".equals(params.get("jsonp")) ? "javascript" : "plain") + "; charset=utf-8");

            httpRef.set(http);
            if (parameters.get("when").equals("poll")) {
                completed.set(false);
                written.set(false);
                Timer timer = closeTimer.getAndSet(null);
                if (timer != null) {
                    timer.cancel();
                }
                if (aborted.get()) {
                    http.end();
                    return;
                }
                cache.remove(parameters.get("lastMsgId"));
                for (String item : cache.values()) {
                    send(item, true);
                    break;
                }
            }
        }

        @Override
        synchronized void send(String data) {
            send(data, false);
        }
        
        synchronized void send(String data, boolean noCache) {
            if (!noCache) {
                String id = "" + msgId.incrementAndGet();
                data = id + "|" + data;
                cache.put(id, data);
            }
            ServerHttpExchange http = httpRef.getAndSet(null);
            if (http != null && !completed.get()) {
                written.set(true);
                String payload;
                if ("true".equals(params.get("jsonp"))) {
                    try {
                        payload = params.get("callback") + "(" + mapper.writeValueAsString(data) + ");";
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    payload = data;
                }
                http.end(payload);
            }
        }

        @Override
        synchronized void close() {
            aborted.set(true);
            ServerHttpExchange http = httpRef.getAndSet(null);
            if (http != null && !completed.get()) {
                http.end();
            }
        }
    }

    private static class DefaultServerSocket implements ServerSocket {
        final Transport transport;
        ObjectMapper mapper = new ObjectMapper();
        AtomicInteger eventId = new AtomicInteger();
        Set<String> tags = new CopyOnWriteArraySet<>();
        ConcurrentMap<String, Actions<Object>> actionsMap = new ConcurrentHashMap<>();
        ConcurrentMap<String, Map<String, Action<Object>>> callbacksMap = new ConcurrentHashMap<>();
        AtomicReference<Timer> heartbeatTimer = new AtomicReference<>();

        DefaultServerSocket(final Transport transport, Map<String, String> map) {
            this.transport = transport;
            actionsMap.put("error", new ConcurrentActions<>());
            transport.errorActions.add(new Action<Throwable>() {
                @Override
                public void on(Throwable throwable) {
                    actionsMap.get("error").fire(throwable);
                }
            });
            actionsMap.put("close", new ConcurrentActions<>(new Actions.Options().once(true).memory(true)));
            transport.closeActions.add(new VoidAction() {
                @Override
                public void on() {
                    actionsMap.get("close").fire();
                }
            });
            transport.messageActions.add(new Action<String>() {
                @Override
                public void on(String text) {
                    final Map<String, Object> event = parseEvent(text);
                    Actions<Object> actions = actionsMap.get(event.get("type"));
                    if (actions != null) {
                        if ((Boolean) event.get("reply")) {
                            final AtomicBoolean sent = new AtomicBoolean();
                            actions.fire(new Reply<Object>() {
                                @Override
                                public Object data() {
                                    return event.get("data");
                                }

                                @Override
                                public void resolve() {
                                    resolve(null);
                                }

                                @Override
                                public void resolve(Object value) {
                                    sendReply(value, false);
                                }

                                @Override
                                public void reject() {
                                    reject(null);
                                }

                                @Override
                                public void reject(Object value) {
                                    sendReply(value, true);
                                }

                                private void sendReply(Object value, boolean exception) {
                                    if (sent.compareAndSet(false, true)) {
                                        Map<String, Object> result = new LinkedHashMap<String, Object>();
                                        result.put("id", event.get("id"));
                                        result.put("data", value);
                                        result.put("exception", exception);
                                        send("reply", result);
                                    }
                                }
                            });
                        } else {
                            actions.fire(event.get("data"));
                        }
                    }
                }
            });
            on("reply", new Action<Map<String, Object>>() {
                @Override
                public void on(Map<String, Object> info) {
                    Map<String, Action<Object>> cbs = callbacksMap.remove(info.get("id"));
                    Action<Object> action = (Boolean) info.get("exception") ? cbs.get("rejected") : cbs.get("resolved");
                    action.on(info.get("data"));
                }
            });
            transport.handshake(map);
        }

        @Override
        public String uri() {
            return transport.uri();
        }

        @Override
        public Set<String> tags() {
            return tags;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> ServerSocket on(String event, Action<T> action) {
            Actions<Object> actions = actionsMap.get(event);
            if (actions == null) {
                Actions<Object> value = new ConcurrentActions<>();
                actions = actionsMap.putIfAbsent(event, value);
                if (actions == null) {
                    actions = value;
                }
            }
            actions.add((Action<Object>) action);
            return this;
        }

        @Override
        public ServerSocket closeAction(Action<Void> action) {
            return on("close", action);
        }

        @Override
        public ServerSocket errorAction(Action<Throwable> action) {
            return on("error", action);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> ServerSocket off(String event, Action<T> action) {
            Actions<Object> actions = actionsMap.get(event);
            if (actions != null) {
                actions.remove((Action<Object>) action);
            }
            return this;
        }

        @Override
        public ServerSocket send(String event) {
            return send(event, null);
        }

        @Override
        public ServerSocket send(String event, Object data) {
            return send(event, data, null);
        }

        @Override
        public <T> ServerSocket send(String type, Object data, Action<T> resolved) {
            return send(type, data, resolved, null);
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public <T, U> ServerSocket send(String type, Object data, Action<T> resolved, Action<U> rejected) {
            String id = "" + eventId.incrementAndGet();
            Map<String, Object> event = new LinkedHashMap<String, Object>();
            event.put("id", id);
            event.put("type", type);
            event.put("data", data);
            event.put("reply", resolved != null || rejected != null);

            String text = stringifyEvent(event);
            transport.send(text);
            if (resolved != null || rejected != null) {
                Map<String, Action<Object>> cbs = new LinkedHashMap<String, Action<Object>>();
                cbs.put("resolved", (Action<Object>) resolved);
                cbs.put("rejected", (Action<Object>) rejected);
                callbacksMap.put(id, cbs);
            }
            return this;
        }

        @Override
        public ServerSocket close() {
            transport.close();
            return this;
        }

        @Override
        public ServerSocket tag(String... names) {
            tags.addAll(Arrays.asList(names));
            return this;
        }

        @Override
        public ServerSocket untag(String... names) {
            tags.removeAll(Arrays.asList(names));
            return this;
        }

        @Override
        public <T> T unwrap(Class<T> clazz) {
            return transport.unwrap(clazz);
        }
        
        Map<String, Object> parseEvent(String text) {
            try {
                return mapper.readValue(text, new TypeReference<Map<String, Object>>() {});
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        
        String stringifyEvent(Map<String, Object> event) {
            try {
                return mapper.writeValueAsString(event);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        void setHeartbeat(final int heartbeat) {
            heartbeatTimer.set(createCloseTimer(heartbeat));
            on("heartbeat", new VoidAction() {
                @Override
                public void on() {
                    heartbeatTimer.getAndSet(createCloseTimer(heartbeat)).cancel();
                    send("heartbeat");
                }
            });
            on("close", new VoidAction() {
                @Override
                public void on() {
                    heartbeatTimer.get().cancel();
                }
            });
        }

        Timer createCloseTimer(int heartbeat) {
            Timer timer = new Timer(true);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    actionsMap.get("error").fire(new HeartbeatFailedException());
                    close();
                }
            }, heartbeat);
            return timer;
        }
    }

}
