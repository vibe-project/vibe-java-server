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
package org.atmosphere.vibe.transport.http;

import java.nio.CharBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

import org.atmosphere.vibe.platform.action.Action;
import org.atmosphere.vibe.platform.action.VoidAction;
import org.atmosphere.vibe.platform.http.ServerHttpExchange;

/**
 * Represents a server-side HTTP Streaming transport.
 * 
 * @author Donghwan Kim
 */
public class HttpStreamServerTransport extends BaseHttpServerTransport {

    private final static String text2KB = CharBuffer.allocate(2048).toString().replace('\0', ' ');
    
    public HttpStreamServerTransport(ServerHttpExchange http) {
        super(http);
        Map<String, String> query = new LinkedHashMap<String, String>();
        query.put("id", id);
        http.finishAction(new VoidAction() {
            @Override
            public void on() {
                closeActions.fire();
            }
        })
        .errorAction(new Action<Throwable>() {
            @Override
            public void on(Throwable throwable) {
                errorActions.fire(throwable);
            }
        })
        .closeAction(new VoidAction() {
            @Override
            public void on() {
                closeActions.fire();
            }
        })
        .setHeader("content-type", "text/" + ("true".equals(params.get("sse")) ? "event-stream" : "plain") + "; charset=utf-8")
        .write(text2KB + "\ndata: ?" + formatQuery(query) + "\n\n");
    }

    @Override
    public synchronized void doSend(String data) {
        String payload = "";
        for (String line : data.split("\r\n|\r|\n")) {
            payload += "data: " + line + "\n";
        }
        payload += "\n";
        http.write(payload);
    }

    @Override
    public synchronized void doClose() {
        http.end();
    }

}