package io.github.flowersinthesand.wes.atmosphere;

import java.io.IOException;
import java.io.PrintWriter;

import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.websocket.WebSocketEventListenerAdapter;

import io.github.flowersinthesand.wes.Data;
import io.github.flowersinthesand.wes.websocket.AbstractServerWebSocket;
import io.github.flowersinthesand.wes.websocket.CloseReason;

public class AtmosphereServerWebSocket extends AbstractServerWebSocket {

	private final AtmosphereResource resource;

	public AtmosphereServerWebSocket(AtmosphereResource resource) {
		this.resource = resource;
		resource.addEventListener(new WebSocketEventListenerAdapter() {
			@SuppressWarnings("rawtypes")
			@Override
			public void onMessage(WebSocketEvent event) {
				messageActions.fire(new Data(event.message().toString()));
			}

			@SuppressWarnings("rawtypes")
			@Override
			public void onClose(WebSocketEvent event) {
				closeActions.fire();
			}

			@SuppressWarnings("rawtypes")
			@Override
			public void onDisconnect(WebSocketEvent event) {
				closeActions.fire();
			}

			@Override
			public void onThrowable(AtmosphereResourceEvent event) {
				errorActions.fire(event.throwable());
			}
		});
	}

	@Override
	public String uri() {
		return resource.getRequest().getRequestURI();
	}
	
	@Override
	protected void doSend(String data) {
		try {
			PrintWriter writer = resource.getResponse().getWriter();
			writer.print(data);
			writer.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void doClose(CloseReason reason) {
		resource.resume();
		try {
			resource.close();
		} catch (IOException e) {}
	}

	@Override
	public <T> T unwrap(Class<T> clazz) {
		return AtmosphereResource.class.isAssignableFrom(clazz) ? clazz.cast(resource) : null;
	}

}
