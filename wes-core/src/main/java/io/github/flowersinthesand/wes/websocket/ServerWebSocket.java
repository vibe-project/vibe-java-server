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
	 * Attaches an action to handle error from various things.
	 */
	ServerWebSocket errorAction(Action<Throwable> action);

	/**
	 * Attaches an action for the close event where the state transitions to
	 * {@link State#CLOSED}.If the state is already {@link State#CLOSED}, the
	 * handler will be executed on addition.
	 */
	ServerWebSocket closeAction(Action<CloseReason> action);

	/**
	 * Represents the state of the connection.
	 * 
	 * @see <a
	 *      href="http://www.w3.org/TR/websockets/#dom-websocket-readystate">The
	 *      WebSocket API by W3C - The readyState attribute</a>
	 */
	enum State {

		/**
		 * The connection has not yet been established.
		 */
		CONNECTING,

		/**
		 * The WebSocket connection is established and communication is
		 * possible.
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

	/**
	 * Represents a reason for closure consisting of status code and textual
	 * message
	 * 
	 * @see <a href="http://tools.ietf.org/html/rfc6455#section-7.4.1">RFC 6455,
	 *      Section 7.4.1 Defined Status Codes</a>
	 */
	class CloseReason {

		/**
		 * 1000 indicates a normal closure, meaning that the purpose for which
		 * the connection was established has been fulfilled.
		 */
		public static final CloseReason NORMAL = new CloseReason(1000);

		/**
		 * 1001 indicates that an endpoint is "going away", such as a server
		 * going down or a browser having navigated away from a page.
		 */
		public static final CloseReason GOING_AWAY = new CloseReason(1001);

		/**
		 * 1002 indicates that an endpoint is terminating the connection due to
		 * a protocol error.
		 */
		public static final CloseReason PROTOCOL_ERROR = new CloseReason(1002);

		/**
		 * 1003 indicates that an endpoint is terminating the connection because
		 * it has received a type of data it cannot accept (e.g., an endpoint
		 * that understands only text data MAY send this if it receives a binary
		 * message).
		 */
		public static final CloseReason UNACCEPTABLE = new CloseReason(1003);

		/**
		 * 1005 is a reserved value and MUST NOT be set as a status code in a
		 * Close control frame by an endpoint. It is designated for use in
		 * applications expecting a status code to indicate that no status code
		 * was actually present.
		 */
		public static final CloseReason NO_STATUS_CODE = new CloseReason(1005);

		/**
		 * 1006 is a reserved value and MUST NOT be set as a status code in a
		 * Close control frame by an endpoint. It is designated for use in
		 * applications expecting a status code to indicate that the connection
		 * was closed abnormally, e.g., without sending or receiving a Close
		 * control frame.
		 */
		public static final CloseReason ABNORMAL = new CloseReason(1006);

		/**
		 * 1007 indicates that an endpoint is terminating the connection because
		 * it has received data within a message that was not consistent with
		 * the type of the message (e.g., non-UTF-8 [RFC3629] data within a text
		 * message).
		 */
		public static final CloseReason INCOINSITENT = new CloseReason(1007);

		/**
		 * 1008 indicates that an endpoint is terminating the connection because
		 * it has received a message that violates its policy. This is a generic
		 * status code that can be returned when there is no other more suitable
		 * status code (e.g., 1003 or 1009) or if there is a need to hide
		 * specific details about the policy.
		 */
		public static final CloseReason POLICY_VIOLATION = new CloseReason(1008);

		/**
		 * 1009 indicates that an endpoint is terminating the connection because
		 * it has received a message that is too big for it to process.
		 */
		public static final CloseReason TOO_BIG = new CloseReason(1009);

		/**
		 * 1010 indicates that an endpoint (client) is terminating the
		 * connection because it has expected the server to negotiate one or
		 * more extension, but the server didn't return them in the response
		 * message of the WebSocket handshake. The list of extensions that are
		 * needed SHOULD appear in the /reason/ part of the Close frame. Note
		 * that this status code is not used by the server, because it can fail
		 * the WebSocket handshake instead.
		 */
		public static final CloseReason REQUIRED_EXTENSION = new CloseReason(1010);

		/**
		 * 1011 indicates that a server is terminating the connection because it
		 * encountered an unexpected condition that prevented it from fulfilling
		 * the request.
		 */
		public static final CloseReason SERVER_ERROR = new CloseReason(1011);

		/**
		 * 1015 is a reserved value and MUST NOT be set as a status code in a
		 * Close control frame by an endpoint. It is designated for use in
		 * applications expecting a status code to indicate that the connection
		 * was closed due to a failure to perform a TLS handshake (e.g., the
		 * server certificate can't be verified).
		 */
		public static final CloseReason TLS_HANDSHAKE_FAILURE = new CloseReason(1015);

		private int code;
		private String reason;

		/**
		 * Creates a reason for closure with the given status code.
		 */
		public CloseReason(int code) {
			this(code, null);
		}

		/**
		 * Creates a reason for closure with the given status code and reason.
		 */
		public CloseReason(int code, String reason) {
			this.code = code;
			this.reason = reason;
		}

		/**
		 * Returns the status code.
		 */
		public int code() {
			return code;
		}

		/**
		 * Returns the reason.
		 */
		public String reason() {
			return reason;
		}

		/**
		 * Creates a reason for closure with new reason.
		 */
		public CloseReason newReason(String reason) {
			return new CloseReason(code, reason);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + code;
			result = prime * result
					+ ((reason == null) ? 0 : reason.hashCode());
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
			CloseReason other = (CloseReason) obj;
			if (code != other.code)
				return false;
			if (reason == null) {
				if (other.reason != null)
					return false;
			} else if (!reason.equals(other.reason))
				return false;
			return true;
		}

	}

}
