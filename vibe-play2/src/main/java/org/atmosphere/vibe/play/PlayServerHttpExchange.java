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
package org.atmosphere.vibe.play;

import org.atmosphere.vibe.AbstractServerHttpExchange;
import org.atmosphere.vibe.Data;
import org.atmosphere.vibe.HttpStatus;
import org.atmosphere.vibe.ServerHttpExchange;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import play.libs.F.Callback0;
import play.libs.F.Function0;
import play.libs.F.Promise;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Result;
import play.mvc.Results;
import play.mvc.Results.Chunks;
import play.mvc.Results.StringChunks;

/**
 * {@link ServerHttpExchange} for Play 2.
 *
 * @author Donghwan Kim
 */
public class PlayServerHttpExchange extends AbstractServerHttpExchange {

    private final Request request;
    private final Response response;
    private boolean aborted;
    private CountDownLatch written = new CountDownLatch(1);
    private List<String> buffer = new ArrayList<>();
    private HttpStatus status = HttpStatus.OK;
    private Chunks.Out<String> out;

    public PlayServerHttpExchange(Request request, Response response) {
        this.request = request;
        this.response = response;
    }

    public Promise<Result> result() {
        return Promise.promise(new Function0<Result>() {
            @Override
            public Result apply() throws Throwable {
                // Block the current thread until the first call of write or close
                // Is there any other solution?
                written.await();
                // Because ServerHttpExchange is not thread-safe
                synchronized (PlayServerHttpExchange.this) {
                    return Results.status(status.code(), new StringChunks() {
                        @Override
                        public void onReady(Chunks.Out<String> out) {
                            // With the same reason as above
                            synchronized (PlayServerHttpExchange.this) {
                                PlayServerHttpExchange.this.out = out;
                                out.onDisconnected(new Callback0() {
                                    @Override
                                    public void invoke() throws Throwable {
                                        closeActions.fire();
                                    }
                                });
                                for (String data : buffer) {
                                    out.write(data);
                                }
                                if (aborted) {
                                    out.close();
                                }
                            }
                        }
                    });
                }
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
        return request.headers().keySet();
    }

    @Override
    public List<String> requestHeaders(String name) {
        for (String h : request.headers().keySet()) {
            if (name.toLowerCase().equals(h.toLowerCase())) {
                return Arrays.asList(request.headers().get(h));
            }
        }
        return Collections.<String> emptyList();
    }

    @Override
    protected void readBody() {
        // Play can't read body asynchronously
        bodyActions.fire(new Data(request.body().asText()));
    }
    
    private void throwIfWritten() {
        if (written.getCount() == 0) {
            throw new IllegalStateException("Response has already been written");
        }
    }

    @Override
    public void doSetStatus(HttpStatus status) {
        throwIfWritten();
        this.status = status;
    }

    @Override
    public void doSetResponseHeader(String name, String value) {
        throwIfWritten();
        // 'content-type' doesn't work. It must be 'Content-Type'. 
        // TODO file an issue to Play
        if (name.equalsIgnoreCase(Response.CONTENT_TYPE)) {
            name = Response.CONTENT_TYPE;
        }
        response.setHeader(name, value);
    }

    @Override
    protected void doWrite(byte[] data, int offset, int length) {
        // TODO: https://github.com/Atmosphere/vibe/issues/23
        // TODO: We need the char encoding
        try {
            doWrite(new String(data, offset, length, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doWrite(String data) {
        if (out == null) {
            written.countDown();
            buffer.add(data);
        } else {
            out.write(data);
        }
    }

    @Override
    protected void doClose() {
        if (out == null) {
            written.countDown();
            aborted = true;
        } else {
            out.close();
        }
    }

    /**
     * {@link Request} and {@link Response} are available.
     */
    @Override
    public <T> T unwrap(Class<T> clazz) {
        return Request.class.isAssignableFrom(clazz) ?
                clazz.cast(request) :
                Response.class.isAssignableFrom(clazz) ?
                        clazz.cast(response) :
                        null;
    }

}
