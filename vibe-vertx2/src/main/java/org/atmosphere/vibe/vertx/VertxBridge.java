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
package org.atmosphere.vibe.vertx;

import org.atmosphere.vibe.Action;
import org.atmosphere.vibe.Actions;
import org.atmosphere.vibe.ServerHttpExchange;
import org.atmosphere.vibe.ServerWebSocket;
import org.atmosphere.vibe.SimpleActions;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.http.impl.WebSocketMatcher;
import org.vertx.java.core.http.impl.WebSocketMatcher.Match;

/**
 * Convenient class to install Vert.x bridge.
 *
 * @author Donghwan Kim
 */
public class VertxBridge {

    private Actions<ServerHttpExchange> httpActions = new SimpleActions<>();
    private Actions<ServerWebSocket> wsActions = new SimpleActions<>();

    public VertxBridge(final HttpServer server, String path) {
        RouteMatcher httpMatcher = new RouteMatcher();
        httpMatcher.all(path, new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest req) {
                httpActions.fire(new VertxServerHttpExchange(req));
            }
        });
        httpMatcher.noMatch(server.requestHandler());
        server.requestHandler(httpMatcher);

        WebSocketMatcher wsMatcher = new WebSocketMatcher();
        wsMatcher.addPattern(path, new Handler<WebSocketMatcher.Match>() {
            @Override
            public void handle(Match match) {
                wsActions.fire(new VertxServerWebSocket(match.ws));
            }
        });
        wsMatcher.noMatch(new Handler<WebSocketMatcher.Match>() {
            Handler<org.vertx.java.core.http.ServerWebSocket> old = server.websocketHandler();

            @Override
            public void handle(WebSocketMatcher.Match match) {
                if (old != null) {
                    old.handle(match.ws);
                }
            }
        });
        server.websocketHandler(wsMatcher);
    }

    /**
     * Adds an action to be called on HTTP request with
     * {@link ServerHttpExchange}.
     */
    public VertxBridge httpAction(Action<ServerHttpExchange> action) {
        httpActions.add(action);
        return this;
    }

    /**
     * Adds an action to be called on WebSocket connection with
     * {@link ServerWebSocket} in open state.
     */
    public VertxBridge websocketAction(Action<ServerWebSocket> action) {
        wsActions.add(action);
        return this;
    }

}
