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
package org.atmosphere.vibe.transport;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;

import org.atmosphere.vibe.platform.action.Action;
import org.atmosphere.vibe.platform.action.Actions;
import org.atmosphere.vibe.platform.action.ConcurrentActions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for {@link ServerTransport}.
 *
 * @author Donghwan Kim
 */
public abstract class BaseServerTransport implements ServerTransport {

    protected Actions<String> textActions = new ConcurrentActions<>();
    protected Actions<ByteBuffer> binaryActions = new ConcurrentActions<>();
    protected Actions<Throwable> errorActions = new ConcurrentActions<Throwable>()
    .add(new Action<Throwable>() {
        @Override
        public void on(Throwable throwable) {
            logger.trace("{} has received a throwable {}", BaseServerTransport.this, throwable);
        }
    });
    protected Actions<Void> closeActions = new ConcurrentActions<Void>(new Actions.Options().once(true).memory(true))
    .add(new Action<Void>() {
        @Override
        public void on(Void _) {
            logger.trace("{} has been closed", BaseServerTransport.this);
            stateRef.set(State.CLOSED);
            textActions.disable();
            errorActions.disable();
        }
    });
    private final Logger logger = LoggerFactory.getLogger(BaseServerTransport.class);
    private AtomicReference<State> stateRef = new AtomicReference<BaseServerTransport.State>(State.OPEN);

    @Override
    public ServerTransport ontext(Action<String> action) {
        textActions.add(action);
        return this;
    }

    @Override
    public ServerTransport onbinary(Action<ByteBuffer> action) {
        binaryActions.add(action);
        return this;
    }

    @Override
    public BaseServerTransport send(String data) {
        logger.trace("{} sends a text message {}", this, data);
        if (stateRef.get() == State.OPEN) {
            doSend(data);
        } else {
            errorActions.fire(new RuntimeException("notopened"));
        }
        return this;
    }

    @Override
    public ServerTransport send(ByteBuffer data) {
        logger.trace("{} sends a binary message {}", this, data);
        if (stateRef.get() == State.OPEN) {
            doSend(data);
        } else {
            errorActions.fire(new RuntimeException("notopened"));
        }
        return this;
    }

    protected abstract void doSend(String data);

    protected abstract void doSend(ByteBuffer data);

    @Override
    public ServerTransport onerror(Action<Throwable> action) {
        errorActions.add(action);
        return this;
    }

    @Override
    public ServerTransport onclose(Action<Void> action) {
        closeActions.add(action);
        return this;
    }

    @Override
    public void close() {
        logger.trace("{} has started to close the connection", this);
        State state = stateRef.get();
        if (state != State.CLOSING && state != State.CLOSED) {
            if (stateRef.compareAndSet(state, State.CLOSING)) {
                doClose();
            }
        }
    }

    protected abstract void doClose();

    /**
     * Represents the state of the connection.
     */
    static enum State {
        OPEN, CLOSING, CLOSED
    }

}