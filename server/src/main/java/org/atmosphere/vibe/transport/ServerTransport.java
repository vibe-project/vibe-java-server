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

import java.net.URI;

import org.atmosphere.vibe.platform.action.Action;

/**
 * Represents a server-side full duplex message channel ensuring there is no
 * message loss and no idle connection.
 * <p>
 * Implementations are thread safe.
 * 
 * @author Donghwan Kim
 */
public interface ServerTransport {

    /**
     * A URI used to connect. To work with URI parts, use {@link URI} or
     * something like that.
     */
    // TODO should we recover original URI?
    String uri();

    /**
     * Executed if there was any error on the connection. You don't need to
     * close it explicitly.
     */
    ServerTransport errorAction(Action<Throwable> action);

    /**
     * Attaches an action for the text message.
     */
    ServerTransport textAction(Action<String> action);

    /**
     * Sends a text message through the connection.
     */
    ServerTransport send(String data);

    /**
     * Attaches an action for the close event. After this event, the instance
     * shouldn't be used and all the other events will be disabled.
     */
    ServerTransport closeAction(Action<Void> action);

    /**
     * Closes the connection. This method has no side effect if called more than
     * once.
     */
    void close();

    /**
     * Returns the provider-specific component.
     */
    <T> T unwrap(Class<T> clazz);

}