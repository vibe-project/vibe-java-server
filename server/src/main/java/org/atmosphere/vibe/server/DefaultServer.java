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
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * This implementation provides and manages {@link ServerSocket} processing HTTP request and WebSocket
 * following the Vibe protocol.
 * 
 * @author Donghwan Kim
 */
public class DefaultServer implements Server {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Logger log = LoggerFactory.getLogger(DefaultServer.class);
    private ConcurrentMap<String, DefaultServerSocket> sockets = new ConcurrentHashMap<>();
    private String[] transports = new String[] { "ws", "sse", "streamxhr", "streamxdr", "streamiframe", "longpollajax", "longpollxdr", "longpolljsonp" };
    private int heartbeat = 20000;
    private int _heartbeat = 5000;
    private Actions<ServerSocket> socketActions = new ConcurrentActions<ServerSocket>()
    .add(new Action<ServerSocket>() {
        @Override
        public void on(ServerSocket s) {
            final DefaultServerSocket socket = (DefaultServerSocket) s;
            sockets.put(socket.id(), socket);
            socket.on("close", new VoidAction() {
                @Override
                public void on() {
                    sockets.remove(socket.id());
                }
            });
            socket.setHeartbeat(heartbeat);
        }
    });
    private Action<ServerHttpExchange> httpAction = new Action<ServerHttpExchange>() {
        @Override
        public void on(final ServerHttpExchange http) {
            final Map<String, String> params = parseURI(http.uri());
            switch (http.method()) {
            case "GET":
                setNocache(http);
                setCors(http);
                switch (params.get("when")) {
                case "handshake":
                    Map<String, Object> result = new LinkedHashMap<String, Object>();
                    result.put("id", UUID.randomUUID().toString());
                    result.put("transports", transports);
                    result.put("heartbeat", heartbeat);
                    result.put("_heartbeat", _heartbeat);

                    try {
                        String text = mapper.writeValueAsString(result);
                        if (params.containsKey("callback")) {
                            text = params.get("callback") + "(" + mapper.writeValueAsString(text) + ")";
                        }
                        http.end(text);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to write a JSON", e);
                        http.setStatus(HttpStatus.INTERNAL_SERVER_ERROR).end();
                    }
                    break;
                case "open":
                    switch (params.get("transport")) {
                    case "sse":
                    case "streamxhr":
                    case "streamxdr":
                    case "streamiframe":
                        socketActions.fire(new DefaultServerSocket(new StreamTransport(params, http)));
                        break;
                    case "longpollajax":
                    case "longpollxdr":
                    case "longpolljsonp":
                        socketActions.fire(new DefaultServerSocket(new LongpollTransport(params, http)));
                        break;
                    default:
                        log.error("Transport, {}, is not supported", params.get("transport"));
                        http.setStatus(HttpStatus.NOT_IMPLEMENTED).end();
                    }
                    break;
                case "poll": {
                    String id = params.get("id");
                    DefaultServerSocket socket = sockets.get(id);
                    if (socket != null) {
                        Transport transport = socket.transport;
                        if (transport instanceof LongpollTransport) {
                            ((LongpollTransport) transport).refresh(http);
                        } else {
                            log.error("Non-long polling transport#{} sent poll request", id);
                            http.setStatus(HttpStatus.INTERNAL_SERVER_ERROR).end();
                        }
                    } else {
                        log.error("Long polling transport#{} is not found in poll request", id);
                        http.setStatus(HttpStatus.INTERNAL_SERVER_ERROR).end();
                    }
                    break;
                }
                case "abort": {
                    String id = params.get("id");
                    ServerSocket socket = sockets.get(id);
                    if (socket != null) {
                        socket.close();
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

                        DefaultServerSocket socket = sockets.get(id);
                        if (socket != null) {
                            Transport transport = socket.transport;
                            if (transport instanceof HttpTransport) {
                                transport.messageActions.fire(data);
                            } else {
                                log.error("Non-HTTP socket#{} receives a POST message", id);
                                http.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
                            }
                        } else {
                            log.error("A POST message arrived but no socket#{} is found", id);
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
            http
            .setHeader("cache-control", "no-cache, no-store, must-revalidate")
            .setHeader("pragma", "no-cache")
            .setHeader("expires", "0");
        }

        private void setCors(ServerHttpExchange http) {
            String origin = http.header("origin");
            http
            .setHeader("access-control-allow-origin", origin != null ? origin : "*")
            .setHeader("access-control-allow-credentials", "true");
        }
    };

    private Action<ServerWebSocket> websocketAction = new Action<ServerWebSocket>() {
        @Override
        public void on(ServerWebSocket ws) {
            Map<String, String> params = parseURI(ws.uri());
            socketActions.fire(new DefaultServerSocket(new WebSocketTransport(params, ws)));
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
        for (ServerSocket socket : sockets.values()) {
            action.on(socket);
        }
        return this;
    }

    @Override
    public Sentence byId(final String id) {
        return new Sentence(new Action<Action<ServerSocket>>() {
            @Override
            public void on(Action<ServerSocket> action) {
                byId(id, action);
            }
        });
    }

    @Override
    public Server byId(String id, Action<ServerSocket> action) {
        ServerSocket socket = sockets.get(id);
        if (socket != null) {
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
        for (ServerSocket socket : sockets.values()) {
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
    public Action<ServerWebSocket> websocketAction() {
        return websocketAction;
    }

    /**
     * A set of transports to allow connections. The default is <code>ws</code>,
     * <code>sse</code>, <code>streamxhr</code>, <code>streamxdr</code>,
     * <code>streamiframe</code>, <code>longpollajax</code>,
     * <code>longpollxdr</code> and <code>longpolljsonp</code>.
     */
    public void setTransports(String... transports) {
        this.transports = transports;
    }

    /**
     * A heartbeat interval in milliseconds to maintain a connection alive and
     * prevent server from holding idle connections. The default is 20s and
     * should be larger than 5s.
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

    private static Map<String, String> parseURI(String uri) {
        Map<String, String> map = new LinkedHashMap<>();
        String query = URI.create(uri).getQuery();
        if ((query == null) || (query.equals(""))) {
            return map;
        }

        String[] params = query.split("&");
        for (String param : params) {
            try {
                String[] pair = param.split("=", 2);
                String name = URLDecoder.decode(pair[0], "UTF-8");
                if (name == "") {
                    continue;
                }

                map.put(name, pair.length > 1 ? URLDecoder.decode(pair[1], "UTF-8") : "");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        return Collections.unmodifiableMap(map);
    }

    private abstract static class Transport implements Wrapper {
        final Map<String, String> params;
        Actions<String> messageActions = new ConcurrentActions<>();
        Actions<Throwable> errorActions = new ConcurrentActions<>();
        Actions<Void> closeActions = new ConcurrentActions<>(new Actions.Options().once(true).memory(true));

        Transport(Map<String, String> params) {
            this.params = params;
        }

        abstract String uri();

        abstract void send(String data);

        abstract void close();
    }

    private static class WebSocketTransport extends Transport {
        final ServerWebSocket ws;

        WebSocketTransport(Map<String, String> params, ServerWebSocket ws) {
            super(params);
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
        final ServerHttpExchange http;

        HttpTransport(Map<String, String> params, ServerHttpExchange http) {
            super(params);
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
        public <T> T unwrap(Class<T> clazz) {
            return http.unwrap(clazz);
        }
    }

    final static String text2KB = CharBuffer.allocate(2048).toString().replace('\0', ' ');

    private static class StreamTransport extends HttpTransport {
        StreamTransport(Map<String, String> params, ServerHttpExchange http) {
            super(params, http);
            // Reads the request to make closeAction be fired on http.end
            http.read().closeAction(new VoidAction() {
                @Override
                public void on() {
                    closeActions.fire();
                }
            })
            .setHeader("content-type",
                "text/" + (params.get("transport").equals("sse") ? "event-stream" : "plain") + "; charset=utf-8")
            .write(text2KB + "\n");
        }

        @Override
        synchronized void send(String data) {
            String payload = "data: " + data + "\n\n";
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
        AtomicBoolean closed = new AtomicBoolean();
        AtomicBoolean written = new AtomicBoolean();
        Set<String> buffer = new CopyOnWriteArraySet<>();
        AtomicReference<Timer> closeTimer = new AtomicReference<>();
        ObjectMapper mapper = new ObjectMapper();

        LongpollTransport(Map<String, String> params, ServerHttpExchange http) {
            super(params, http);
            refresh(http);
        }

        void refresh(ServerHttpExchange http) {
            final Map<String, String> parameters = parseURI(http.uri());
            // Reads the request to make closeAction be fired on http.end
            http.read().closeAction(new VoidAction() {
                @Override
                public void on() {
                    closed.set(true);
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
                "text/" + (params.get("transport").equals("longpolljsonp") ? "javascript" : "plain") + "; charset=utf-8");

            if (parameters.get("when").equals("open")) {
                http.end();
            } else {
                httpRef.set(http);
                closed.set(false);
                written.set(false);
                Timer timer = closeTimer.getAndSet(null);
                if (timer != null) {
                    timer.cancel();
                }
                if (aborted.get()) {
                    http.end();
                    return;
                }
                if (parameters.containsKey("lastEventIds")) {
                    String[] lastEventIds = parameters.get("lastEventIds").split(",");
                    for (String eventId : lastEventIds) {
                        for (String message : buffer) {
                            if (eventId.equals(findEventId(message))) {
                                buffer.remove(message);
                            }
                        }
                    }
                    if (!buffer.isEmpty()) {
                        Iterator<String> iterator = buffer.iterator();
                        String string = iterator.next();
                        while (iterator.hasNext()) {
                            string += "," + iterator.next();
                        }
                        send("[" + string + "]");
                    }
                }
            }
        }

        private String findEventId(String text) {
            Matcher matcher = Pattern.compile("\"id\":\"([^\"]+)\"").matcher(text);
            matcher.find();
            return matcher.group(1);
        }

        @Override
        synchronized void send(String data) {
            if (!data.startsWith("[")) {
                buffer.add(data);
            }
            ServerHttpExchange http = httpRef.getAndSet(null);
            if (http != null && !closed.get()) {
                written.set(true);
                String payload;
                if (params.get("transport").equals("longpolljsonp")) {
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
            if (http != null && !closed.get()) {
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

        DefaultServerSocket(final Transport transport) {
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

                                AtomicBoolean sent = new AtomicBoolean();

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
        }

        @Override
        public String id() {
            return transport.params.get("id");
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

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((id() == null) ? 0 : id().hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            DefaultServerSocket other = (DefaultServerSocket) obj;
            if (id() == null) {
                if (other.id() != null)
                    return false;
            } else if (!id().equals(other.id()))
                return false;
            return true;
        }
    }

}
