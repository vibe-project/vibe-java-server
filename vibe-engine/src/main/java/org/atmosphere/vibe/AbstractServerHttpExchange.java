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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;

/**
 * Abstract base class for {@link ServerHttpExchange}.
 *
 * @author Donghwan Kim
 */
public abstract class AbstractServerHttpExchange implements ServerHttpExchange {

    private boolean closed;
    private boolean readBody;
    protected Actions<Data> bodyActions = new SimpleActions<>(new Actions.Options().once(true).memory(true));
    protected Actions<Void> closeActions = new SimpleActions<>(new Actions.Options().once(true).memory(true));

    private final Logger logger = LoggerFactory.getLogger(AbstractServerHttpExchange.class);

    public AbstractServerHttpExchange() {
        closeActions.add(new VoidAction() {
            @Override
            public void on() {
                logger.trace("{} has been closed", AbstractServerHttpExchange.this);
            }
        });
    }

    @Override
    public String requestHeader(String name) {
        List<String> headers = requestHeaders(name);
        return headers != null && headers.size() > 0 ? headers.get(0) : null;
    }

    @Override
    public ServerHttpExchange bodyAction(Action<Data> action) {
        if (!readBody) {
            readBody();
            readBody = true;
        }
        bodyActions.add(action);
        return this;
    }

    protected abstract void readBody();

    @Override
    public final ServerHttpExchange setResponseHeader(String name, Iterable<String> value) {
        // See http://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2
        Iterator<String> iterator = value.iterator();
        StringBuilder builder = new StringBuilder(iterator.next());
        while (iterator.hasNext()) {
            builder.append(", ").append(iterator.next());
        }
        return setResponseHeader(name, builder.toString());
    }

    @Override
    public ServerHttpExchange setResponseHeader(String name, String value) {
        logger.trace("{} sets a response header {} to {}", this, name, value);
        doSetResponseHeader(name, value);
        return this;
    }

    protected abstract void doSetResponseHeader(String name, String value);

    @Override
    public ServerHttpExchange write(String data) {
        logger.trace("{} sends a text chunk {}", this, data);
        doWrite(data);
        return this;
    }

    @Override
    public ServerHttpExchange write(byte[] data, int offset, int length) {
        logger.trace("{} sends a text chunk {}", this, data);
        doWrite(data, offset, length);
        return this;
    }

    protected abstract void doWrite(byte[] data, int offset, int length);

    protected abstract void doWrite(String data);

    @Override
    public ServerHttpExchange close() {
        logger.trace("{} has started to close the connection", this);
        if (!closed) {
            closed = true;
            doClose();
        }
        return this;
    }

    protected abstract void doClose();

    @Override
    public ServerHttpExchange close(String data) {
        return write(data).close();
    }

    @Override
    public ServerHttpExchange setStatus(HttpStatus status) {
        logger.trace("{} sets a response status, {}", this, status);
        doSetStatus(status);
        return this;
    }

    protected abstract void doSetStatus(HttpStatus status);

    @Override
    public ServerHttpExchange closeAction(Action<Void> action) {
        closeActions.add(action);
        return this;
    }

}
