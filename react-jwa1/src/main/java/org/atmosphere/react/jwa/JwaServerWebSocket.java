/*
 * Copyright 2013-2014 Donghwan Kim
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
package org.atmosphere.react.jwa;

import org.atmosphere.react.AbstractServerWebSocket;
import org.atmosphere.react.Actions;
import org.atmosphere.react.Data;
import org.atmosphere.react.ServerWebSocket;

import java.io.IOException;
import java.net.URI;

import javax.websocket.MessageHandler;
import javax.websocket.Session;

/**
 * {@link ServerWebSocket} for Java WebSocket API 1.
 *
 * @author Donghwan Kim
 */
public class JwaServerWebSocket extends AbstractServerWebSocket {

    private final Session session;

    public JwaServerWebSocket(Session session) {
        this.session = session;
        session.addMessageHandler(new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String message) {
                messageActions.fire(new Data(message));
            }
        });
    }

    // To be used by BridgeEndpoint
    Actions<Void> closeActions() {
        return closeActions;
    }

    // To be used by BridgeEndpoint
    Actions<Throwable> errorActions() {
        return errorActions;
    }

    @Override
    public String uri() {
        URI uri = session.getRequestURI();
        return uri.getPath() + (uri.getQuery() != null ? "?" + uri.getQuery() : "");
    }

    @Override
    protected void doClose() {
        try {
            session.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doSend(String data) {
        session.getAsyncRemote().sendText(data);
    }

    /**
     * {@link Session} is available.
     */
    @Override
    public <T> T unwrap(Class<T> clazz) {
        return Session.class.isAssignableFrom(clazz) ? clazz.cast(session) : null;
    }

}
