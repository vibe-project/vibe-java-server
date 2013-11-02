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