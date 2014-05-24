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
package org.atmosphere.vibe.jwa;

import org.atmosphere.vibe.Action;
import org.atmosphere.vibe.Actions;
import org.atmosphere.vibe.ServerWebSocket;
import org.atmosphere.vibe.SimpleActions;
import org.atmosphere.vibe.VoidAction;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpointConfig;

/**
 * Convenient class to install Java WebSocket API bridge.
 *
 * @author Donghwan Kim
 */
public class JwaBridge {

    private static final Map<String, JwaBridge> bridges = new ConcurrentHashMap<>();

    private final String id = UUID.randomUUID().toString();
    private final ServerEndpointConfig config;
    private Actions<ServerWebSocket> wsActions = new SimpleActions<>();

    public JwaBridge(String path) {
        config = ServerEndpointConfig.Builder.create(BridgeEndpoint.class, path).build();
        config.getUserProperties().put("bridge.id", id);
        bridges.put(id, this);
    }

    public ServerEndpointConfig config() {
        return config;
    }

    public JwaBridge websocketAction(Action<ServerWebSocket> action) {
        wsActions.add(action);
        return this;
    }

    public static class BridgeEndpoint extends Endpoint {

        private Map<String, JwaServerWebSocket> sessions = new ConcurrentHashMap<>();

        @Override
        public void onOpen(Session session, EndpointConfig config) {
            final String id = session.getId();
            JwaServerWebSocket ws = new JwaServerWebSocket(session);
            ws.closeAction(new VoidAction() {
                @Override
                public void on() {
                    sessions.remove(id);
                }
            });
            sessions.put(id, ws);
            bridges.get(config.getUserProperties().get("bridge.id")).wsActions.fire(ws);
        }

        @Override
        public void onClose(Session session, CloseReason closeReason) {
            sessions.get(session.getId()).closeActions().fire();
        }

        @Override
        public void onError(Session session, Throwable thr) {
            sessions.get(session.getId()).errorActions().fire(thr);
        }

    }

}
