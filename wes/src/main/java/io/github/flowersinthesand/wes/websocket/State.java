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

/**
 * Represents the state of the connection.
 * 
 * @see <a href="http://www.w3.org/TR/websockets/#dom-websocket-readystate">The
 *      WebSocket API by W3C - The readyState attribute</a>
 */
public enum State {

	/**
	 * The connection has not yet been established.
	 */
	CONNECTING,

	/**
	 * The WebSocket connection is established and communication is possible.
	 */
	OPEN,

	/**
	 * The close() method has been invoked.
	 */
	CLOSING,

	/**
	 * The connection has been closed or could not be opened.
	 */
	CLOSED

}