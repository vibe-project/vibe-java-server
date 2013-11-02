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
import io.github.flowersinthesand.wes.Actions;
import io.github.flowersinthesand.wes.SimpleActions;
import io.github.flowersinthesand.wes.VoidAction;
import io.github.flowersinthesand.wes.util.GenericTypeResolver;

import java.nio.ByteBuffer;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for {@link ServerWebSocket}.
 * 
 * @author Donghwan Kim
 */
public abstract class AbstractServerWebSocket implements ServerWebSocket {
	
	protected Actions<Void> openActions = new SimpleActions<>(new Actions.Options().once(true).memory(true));
	protected Actions<Object> messageActions = new SimpleActions<>();
	protected Actions<Throwable> errorActions = new SimpleActions<>(new Actions.Options().once(true).memory(true));
	protected Actions<CloseReason> closeActions = new SimpleActions<>(new Actions.Options().once(true).memory(true));

	private final Logger logger = LoggerFactory.getLogger(AbstractServerWebSocket.class);
	private String id = UUID.randomUUID().toString();
	private State state = State.CONNECTING;
	private Class<?> messageType;
	private Actions<String> textMessageActions = new SimpleActions<>();
	private Actions<ByteBuffer> binaryMessageActions = new SimpleActions<>();
	
	public AbstractServerWebSocket() {
		openActions.add(new VoidAction() {
			@Override
			public void on() {
				logger.trace("{} has been opened", AbstractServerWebSocket.this);
				state = State.OPEN;
			}
		});
		messageActions.add(new Action<Object>() {
			@Override
			public void on(Object message) {
				logger.trace("{} has received a message [{}]", AbstractServerWebSocket.this, message);
				Class<?> type = message.getClass();
				validateMessageType(type);
				
				if (String.class.isAssignableFrom(type)) {
					textMessageActions.fire((String) message);
				} else if (ByteBuffer.class.isAssignableFrom(type)) {
					binaryMessageActions.fire((ByteBuffer) message);
				}
			}
		});
		errorActions.add(new Action<Throwable>() {
			@Override
			public void on(Throwable throwable) {
				logger.trace("{} has received a throwable [{}]", AbstractServerWebSocket.this, throwable);
				if (state != State.CLOSING && state != State.CLOSED) {
					close(CloseReason.SERVER_ERROR);
				}
			}
		});
		closeActions.add(new Action<CloseReason>() {
			@Override
			public void on(CloseReason reason) {
				logger.trace("{} has been closed due to the reason [{}]", AbstractServerWebSocket.this, reason);
				state = State.CLOSED;
				openActions.disable();
				messageActions.disable();
				textMessageActions.disable();
				binaryMessageActions.disable();
			}
		});
	}
	
	public String id() {
		return id;
	}

	@Override
	public State state() {
		return state;
	}

	@Override
	public ServerWebSocket close() {
		return close(CloseReason.NORMAL);
	}

	@Override
	public ServerWebSocket close(CloseReason reason) {
		logger.trace("{} has started to close the connection with the reason [{}]", this, reason);
		state = State.CLOSING;
		doClose(reason);
		return this;
	}

	protected abstract void doClose(CloseReason reason);

	@Override
	public ServerWebSocket send(String data) {
		logger.trace("{} sends a text message [{}]", this, data);
		doSend(data);
		return this;
	}

	protected abstract void doSend(String data);

	@Override
	public ServerWebSocket send(ByteBuffer data) {
		logger.trace("{} sends a binary message [{}]", this, data);
		doSend(data);
		return this;
	}

	protected abstract void doSend(ByteBuffer data);

	@Override
	public ServerWebSocket openAction(Action<Void> action) {
		openActions.add(action);
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public ServerWebSocket messageAction(Action<?> action) {
		Class<?> type = GenericTypeResolver.resolveTypeArgument(action.getClass(), Action.class);
		validateMessageType(type);
		if (messageType == null) {
			messageType = type;
		}
		
		if (String.class.isAssignableFrom(type)) {
			textMessageActions.add((Action<String>) action);
		} else if (ByteBuffer.class.isAssignableFrom(type)) {
			binaryMessageActions.add((Action<ByteBuffer>) action);
		}
		return this;
	}
	
	protected Class<?> messageType() {
		return messageType;
	}
	
	private void validateMessageType(Class<?> type) {
		if (!String.class.isAssignableFrom(type) && !ByteBuffer.class.isAssignableFrom(type)) {
			throw new IllegalArgumentException("Unsupported message type [" + type + "]");
		}
		if (messageType != null && messageType != type) {
			throw new IllegalArgumentException("This WebSocket's message type is already set to [" + messageType + "] not [" + type + "]");
		}
	}

	@Override
	public ServerWebSocket errorAction(Action<Throwable> action) {
		errorActions.add(action);
		return this;
	}

	@Override
	public ServerWebSocket closeAction(Action<CloseReason> action) {
		closeActions.add(action);
		return this;
	}

	@Override
	public String toString() {
		return "ServerWebSocket [id=" + id + ", state=" + state + ", messageType=" + messageType + "]";
	}

}
