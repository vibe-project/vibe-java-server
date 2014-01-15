package io.github.flowersinthesand.wes.jwa;

import io.github.flowersinthesand.wes.AbstractServerWebSocket;
import io.github.flowersinthesand.wes.Actions;
import io.github.flowersinthesand.wes.Data;

import java.io.IOException;
import java.net.URI;

import javax.websocket.MessageHandler;
import javax.websocket.Session;

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

	@Override
	public <T> T unwrap(Class<T> clazz) {
		return Session.class.isAssignableFrom(clazz) ? clazz.cast(session) : null;
	}

}
