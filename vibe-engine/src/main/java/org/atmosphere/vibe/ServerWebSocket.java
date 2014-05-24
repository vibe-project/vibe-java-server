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
package org.atmosphere.vibe;

import java.nio.ByteBuffer;

/**
 * Represents a server-side WebSocket session.
 * <p/>
 * Implementations are not thread-safe and decide whether and which event is
 * fired in asynchronous manner.
 *
 * @author Donghwan Kim
 * @see <a href="http://www.w3.org/TR/websockets/">The WebSocket API by W3C</a>
 * @see <a href="http://tools.ietf.org/html/rfc6455">RFC6455 - The WebSocket
 * Protocol</a>
 */
public interface ServerWebSocket extends Wrapper {

    /**
     * The URI used to connect.
     */
    String uri();

    /**
     * Closes the connection. This method has no side effect if called more than
     * once.
     */
    ServerWebSocket close();

    /**
     * Sends a text message through the connection.
     */
    ServerWebSocket send(String data);

    /**
     * Sends bytes message through the connection
     * @param byteBuffer
     */
    ServerWebSocket send(ByteBuffer byteBuffer);

    /**
     * Attaches an action for the message event. The allowed message type is
     * {@link String} for text messages. If the message is quite big, it may
     * drain memory quickly.
     */
    ServerWebSocket messageAction(Action<Data> action);

    /**
     * Attaches an action to handle error from various things. If an error
     * occurs, the connection will be closed.
     */
    ServerWebSocket errorAction(Action<Throwable> action);

    /**
     * Attaches an action for the close event. If the connection is already
     * closed, the handler will be executed on addition. After close event, all
     * the other event will be disabled.
     */
    ServerWebSocket closeAction(Action<Void> action);

}
