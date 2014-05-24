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
package org.atmosphere.vibe.vertx;

import io.netty.buffer.Unpooled;
import org.atmosphere.vibe.AbstractServerHttpExchange;
import org.atmosphere.vibe.Data;
import org.atmosphere.vibe.HttpStatus;
import org.atmosphere.vibe.ServerHttpExchange;
import org.vertx.java.core.Handler;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

import java.util.List;
import java.util.Set;

/**
 * {@link ServerHttpExchange} for Vert.x 2.
 *
 * @author Donghwan Kim
 */
public class VertxServerHttpExchange extends AbstractServerHttpExchange {

    private final HttpServerRequest request;

    public VertxServerHttpExchange(HttpServerRequest request) {
        this.request = request;
        request.response().setChunked(true).closeHandler(new VoidHandler() {
            @Override
            protected void handle() {
                closeActions.fire();
            }
        });
    }

    @Override
    public String uri() {
        return request.uri();
    }

    @Override
    public String method() {
        return request.method();
    }

    @Override
    public Set<String> requestHeaderNames() {
        return request.headers().names();
    }

    @Override
    public List<String> requestHeaders(String name) {
        return request.headers().getAll(name);
    }

    @Override
    protected void readBody() {
        request.bodyHandler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer body) {
                bodyActions.fire(new Data(body.toString()));
            }
        });
    }

    @Override
    public void doSetResponseHeader(String name, String value) {
        request.response().putHeader(name, value);
    }

    @Override
    protected void doWrite(byte[] data, int offset, int length) {
        request.response().write(new Buffer(Unpooled.wrappedBuffer(data, offset, length)));
    }

    @Override
    public void doSetStatus(HttpStatus status) {
        request.response().setStatusCode(status.code()).setStatusMessage(status.reason());
    }

    @Override
    protected void doWrite(String data) {
        request.response().write(data);
    }

    @Override
    protected void doClose() {
        request.response().end();
        request.response().close();
    }

    /**
     * {@link HttpServerRequest} is available.
     */
    @Override
    public <T> T unwrap(Class<T> clazz) {
        return HttpServerRequest.class.isAssignableFrom(clazz) ? clazz.cast(request) : null;
    }

}
