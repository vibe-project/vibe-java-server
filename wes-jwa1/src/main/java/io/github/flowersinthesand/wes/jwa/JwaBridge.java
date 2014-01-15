package io.github.flowersinthesand.wes.jwa;

import io.github.flowersinthesand.wes.Action;
import io.github.flowersinthesand.wes.Actions;
import io.github.flowersinthesand.wes.ServerWebSocket;
import io.github.flowersinthesand.wes.SimpleActions;

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
			JwaServerWebSocket ws = new JwaServerWebSocket(session);
			sessions.put(session.getId(), ws);
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
