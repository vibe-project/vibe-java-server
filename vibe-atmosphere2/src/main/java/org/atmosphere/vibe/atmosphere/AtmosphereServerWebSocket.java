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
package org.atmosphere.vibe.atmosphere;

import org.atmosphere.vibe.AbstractServerWebSocket;
import org.atmosphere.vibe.Data;
import org.atmosphere.vibe.ServerWebSocket;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.websocket.WebSocketEventListenerAdapter;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;

/**
 * {@link ServerWebSocket} for Atmosphere 2.
 *
 * @author Donghwan Kim
 */
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
        String uri = resource.getRequest().getRequestURI();
        if (resource.getRequest().getQueryString() != null) {
            uri += "?" + resource.getRequest().getQueryString();
        }
        return uri;
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
    protected void doClose() {
        resource.resume();
        try {
            resource.close();
        } catch (IOException e) {
        }
    }

    @Override
    protected void doSend(ByteBuffer byteBuffer) {
        try {
            byte[] b = byteBuffer.array();
            resource.getResponse().getOutputStream().write(b, 0, b.length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@link AtmosphereResource} is available.
     */
    @Override
    public <T> T unwrap(Class<T> clazz) {
        return AtmosphereResource.class.isAssignableFrom(clazz) ? clazz.cast(resource) : null;
    }

}
