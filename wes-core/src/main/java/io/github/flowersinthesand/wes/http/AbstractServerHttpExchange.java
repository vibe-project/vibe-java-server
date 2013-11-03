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
package io.github.flowersinthesand.wes.http;

import io.github.flowersinthesand.wes.Action;
import io.github.flowersinthesand.wes.Actions;
import io.github.flowersinthesand.wes.SimpleActions;
import io.github.flowersinthesand.wes.VoidAction;
import io.github.flowersinthesand.wes.util.GenericTypeResolver;
import io.github.flowersinthesand.wes.websocket.ServerWebSocket;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for {@link ServerWebSocket}.
 * 
 * @author Donghwan Kim
 */
public abstract class AbstractServerHttpExchange implements ServerHttpExchange {

	protected Actions<Void> closeActions = new SimpleActions<>(new Actions.Options().once(true).memory(true));
	protected Actions<Object> chunkActions = new SimpleActions<>();
	protected Actions<Object> bodyActions = new SimpleActions<>(new Actions.Options().once(true).memory(true));

	private final Logger logger = LoggerFactory.getLogger(AbstractServerHttpExchange.class);
	private String id = UUID.randomUUID().toString();
	private StatusCode status = StatusCode.OK;
	private Class<?> bodyType;
	private Actions<String> textChunkActions = new SimpleActions<>();
	private Actions<ByteBuffer> binaryChunkActions = new SimpleActions<>();
	private Actions<String> textBodyActions = new SimpleActions<>(new Actions.Options().once(true).memory(true));
	private Actions<ByteBuffer> binaryBodyActions = new SimpleActions<>(new Actions.Options().once(true).memory(true));
	
	public AbstractServerHttpExchange() {
		chunkActions.add(new Action<Object>() {
			@Override
			public void on(Object chunk) {
				logger.trace("{} has received a chunk [{}]", AbstractServerHttpExchange.this, chunk);
				Class<?> type = chunk.getClass();
				validateBodyType(type);
				
				if (String.class.isAssignableFrom(type)) {
					textChunkActions.fire((String) chunk);
				} else if (ByteBuffer.class.isAssignableFrom(type)) {
					binaryChunkActions.fire((ByteBuffer) chunk);
				}
			}
		});
		bodyActions.add(new Action<Object>() {
			@Override
			public void on(Object body) {
				logger.trace("{} has received a body [{}]", AbstractServerHttpExchange.this, body);
				chunkActions.disable();
				textChunkActions.disable();
				binaryChunkActions.disable();
				
				Class<?> type = body.getClass();
				validateBodyType(type);
				
				if (String.class.isAssignableFrom(type)) {
					textBodyActions.fire((String) body);
				} else if (ByteBuffer.class.isAssignableFrom(type)) {
					binaryBodyActions.fire((ByteBuffer) body);
				}
			}
		});
		closeActions.add(new VoidAction() {
			@Override
			public void on() {
				logger.trace("{} has been closed", AbstractServerHttpExchange.this);
			}
		});
		setStatus(status);
	}

	public String id() {
		return id;
	}

	@Override
	public String requestHeader(String name) {
		List<String> headers = requestHeaders(name);
		return headers != null && headers.size() > 0 ? headers.get(0) : null;
	}

	@Override
	public ServerHttpExchange responseHeader(String name, String value) {
		setResponseHeader(name, value);
		return this;
	}

	protected abstract void setResponseHeader(String name, String value);

	@Override
	public ServerHttpExchange responseHeader(String name, Iterable<String> value) {
		setResponseHeader(name, value);
		return this;
	}

	protected abstract void setResponseHeader(String name, Iterable<String> value);

	public Class<?> bodyType() {
		return bodyType;
	}

	@SuppressWarnings("unchecked")
	@Override
	public ServerHttpExchange chunkAction(Action<?> action) {
		Class<?> type = GenericTypeResolver.resolveTypeArgument(action.getClass(), Action.class);
		validateBodyType(type);
		if (bodyType == null) {
			bodyType = type;
		}
		
		if (String.class.isAssignableFrom(type)) {
			textChunkActions.add((Action<String>) action);
		} else if (ByteBuffer.class.isAssignableFrom(type)) {
			binaryChunkActions.add((Action<ByteBuffer>) action);
		}
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public ServerHttpExchange bodyAction(Action<?> action) {
		Class<?> type = GenericTypeResolver.resolveTypeArgument(action.getClass(), Action.class);
		validateBodyType(type);
		if (bodyType == null) {
			bodyType = type;
		}
		
		if (String.class.isAssignableFrom(type)) {
			textBodyActions.add((Action<String>) action);
		} else if (ByteBuffer.class.isAssignableFrom(type)) {
			binaryBodyActions.add((Action<ByteBuffer>) action);
		}
		return this;
	}
	
	private void validateBodyType(Class<?> type) {
		if (!String.class.isAssignableFrom(type) && !ByteBuffer.class.isAssignableFrom(type)) {
			throw new IllegalArgumentException("Unsupported body type [" + type + "]");
		}
		if (bodyType != null && bodyType != type) {
			throw new IllegalArgumentException("This HttpExchange's body type is already set to [" + bodyType + "] not [" + type + "]");
		}
	}

	@Override
	public ServerHttpExchange write(String data) {
		logger.trace("{} sends a text chunk [{}]", this, data);
		doWrite(data);
		return this;
	}

	protected abstract void doWrite(String data);

	@Override
	public ServerHttpExchange write(ByteBuffer data) {
		logger.trace("{} sends a binary chunk [{}]", this, data);
		doWrite(data);
		return this;
	}

	protected abstract void doWrite(ByteBuffer data);

	@Override
	public ServerHttpExchange close() {
		logger.trace("{} has started to close the connection", this);
		doClose();
		return this;
	}

	protected abstract void doClose();

	@Override
	public ServerHttpExchange close(String data) {
		return write(data).close();
	}

	@Override
	public ServerHttpExchange close(ByteBuffer data) {
		return write(data).close();
	}

	@Override
	public ServerHttpExchange status(StatusCode status) {
		this.status = status;
		setStatus(status);
		return this;
	}

	protected abstract void setStatus(StatusCode status);

	@Override
	public ServerHttpExchange closeAction(Action<Void> action) {
		closeActions.add(action);
		return this;
	}

	@Override
	public String toString() {
		return "ServerHttpExchange [id=" + id + ", status=" + status + ", bodyType=" + bodyType + "]";
	}

}
