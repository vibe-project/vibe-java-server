/*
 * Copyright 2013 Donghwan Kim
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
package io.github.flowersinthesand.wes.websocket;

import io.github.flowersinthesand.wes.Action;
import io.github.flowersinthesand.wes.Wrapper;

import java.nio.ByteBuffer;

/**
 * Represents a WebSocket session.
 * 
 * Implementations of this class are not thread-safe.
 * 
 * @author Donghwan Kim
 * @see <a href="http://www.w3.org/TR/websockets/">The WebSocket API by W3C</a>
 * @see <a href="http://tools.ietf.org/html/rfc6455">RFC6455 - The WebSocket
 *      Protocol</a>
 */
public interface ServerWebSocket extends Wrapper {

	/**
	 * The URI used to connect.
	 */
	String uri();

	/**
	 * The state of the connection.
	 */
	State state();

	/**
	 * Closes the connection with a normal status code and no reason.
	 */
	ServerWebSocket close();

	/**
	 * Closes the connection with the given status code and data.
	 */
	ServerWebSocket close(CloseReason reason);

	/**
	 * Sends a text message through the connection.
	 */
	ServerWebSocket send(String data);

	/**
	 * Sends a binary message through the connection.
	 */
	ServerWebSocket send(ByteBuffer data);

	/**
	 * Attaches an action for the open event where the state transitions to
	 * {@link State#OPEN}. If the state is already {@link State#OPEN}, the
	 * handler will be executed on addition.
	 */
	ServerWebSocket openAction(Action<Void> action);

	/**
	 * Attaches an action for the message event. The allowed message type is
	 * {@link String} for text messages and {@link ByteBuffer} for binary
	 * messages.
	 */
	ServerWebSocket messageAction(Action<?> action);

	/**
	 * Attaches an action to handle error from various things. If an error
	 * occurs, the connection will be closed with the reason,
	 * {@link CloseReason#SERVER_ERROR}.
	 */
	ServerWebSocket errorAction(Action<Throwable> action);

	/**
	 * Attaches an action for the close event where the state transitions to
	 * {@link State#CLOSED}. If the state is already {@link State#CLOSED}, the
	 * handler will be executed on addition. After the state transition, all the
	 * other event will be disabled.
	 */
	ServerWebSocket closeAction(Action<CloseReason> action);

}
