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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.atmosphere.vibe.platform.action.Action;
import org.atmosphere.vibe.platform.action.VoidAction;
import org.atmosphere.vibe.platform.http.ServerHttpExchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Represents a server-side HTTP Long Polling transport.
 * 
 * @author Donghwan Kim
 */
public class HttpLongpollServerTransport extends BaseHttpServerTransport {

    private AtomicReference<ServerHttpExchange> httpRef = new AtomicReference<>();
    private AtomicBoolean aborted = new AtomicBoolean();
    private AtomicBoolean completed = new AtomicBoolean();
    private AtomicBoolean written = new AtomicBoolean();
    private AtomicReference<Timer> closeTimer = new AtomicReference<>();
    private AtomicInteger msgId = new AtomicInteger();
    private Map<String, String> cache = new ConcurrentHashMap<>();
    private ObjectMapper mapper = new ObjectMapper();
    private String jsonpCallback;

    public HttpLongpollServerTransport(ServerHttpExchange http) {
        super(http);
        refresh(http);
        if ("true".equals(params.get("jsonp"))) {
            jsonpCallback = params.get("callback");
        }
        Map<String, String> query = new LinkedHashMap<String, String>();
        query.put("id", id);
        httpRef.getAndSet(null).end(formatMessage("?" + formatQuery(query)));
    }

    public void refresh(ServerHttpExchange http) {
        final Map<String, String> parameters = parseQuery(http.uri());
        http.finishAction(new VoidAction() {
            @Override
            public void on() {
                completed.set(true);
                if (parameters.get("when").equals("poll") && !written.get()) {
                    closeActions.fire();
                } else {
                    Timer timer = new Timer(true);
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            closeActions.fire();
                        }
                    }, 2000);
                    closeTimer.set(timer);
                }
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
        .setHeader("content-type", "text/" + (jsonpCallback != null ? "javascript" : "plain") + "; charset=utf-8");
        httpRef.set(http);
        if (parameters.get("when").equals("poll")) {
            completed.set(false);
            written.set(false);
            Timer timer = closeTimer.getAndSet(null);
            if (timer != null) {
                timer.cancel();
            }
            if (aborted.get()) {
                http.end();
                return;
            }
            cache.remove(parameters.get("lastMsgId"));
            for (String item : cache.values()) {
                send(item, true);
                break;
            }
        }
    }

    @Override
    public void doSend(String data) {
        send(data, false);
    }
    
    private void send(String data, boolean noCache) {
        if (!noCache) {
            String id = "" + msgId.incrementAndGet();
            data = id + "|" + data;
            cache.put(id, data);
        }
        ServerHttpExchange http = httpRef.getAndSet(null);
        if (http != null && !completed.get()) {
            written.set(true);
            http.end(formatMessage(data));
        }
    }
    
    private String formatMessage(String data) {
        if (jsonpCallback != null) {
            try {
                return jsonpCallback + "(" + mapper.writeValueAsString(data) + ");";
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        } else {
            return data;
        }
    }

    @Override
    public void doClose() {
        aborted.set(true);
        ServerHttpExchange http = httpRef.getAndSet(null);
        if (http != null && !completed.get()) {
            http.end();
        }
    }

}