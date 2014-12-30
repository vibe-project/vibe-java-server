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
package org.atmosphere.vibe.transport.ws;

import org.atmosphere.vibe.platform.action.Action;
import org.atmosphere.vibe.platform.action.VoidAction;
import org.atmosphere.vibe.platform.ws.ServerWebSocket;
import org.atmosphere.vibe.transport.BaseServerTransport;

/**
 * Represents a server-side WebSocket transport.
 * <p>
 * Because WebSocket protocol itself meets Transport's requirements,
 * {@link WebSocketServerTransport} is actually a thread-safe version of
 * {@link ServerWebSocket}.
 * 
 * @author Donghwan Kim
 */
public class WebSocketServerTransport extends BaseServerTransport {

    private final ServerWebSocket ws;

    public WebSocketServerTransport(ServerWebSocket ws) {
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
                textActions.fire(data);
            }
        });
    }

    @Override
    public String uri() {
        return ws.uri();
    }

    @Override
    public synchronized void doSend(String data) {
        ws.send(data);
    }

    @Override
    public synchronized void doClose() {
        ws.close();
    }

    @Override
    public <T> T unwrap(Class<T> clazz) {
        return ws.unwrap(clazz);
    }

}