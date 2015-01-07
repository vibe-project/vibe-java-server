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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.atmosphere.vibe.platform.http.ServerHttpExchange;
import org.atmosphere.vibe.transport.BaseServerTransport;

/**
 * Base class for HTTP transport.
 * 
 * @author Donghwan Kim
 */
public abstract class BaseHttpServerTransport extends BaseServerTransport {

    protected final String id = UUID.randomUUID().toString();
    protected final ServerHttpExchange http;
    protected final Map<String, String> params;

    public BaseHttpServerTransport(ServerHttpExchange http) {
        this.params = parseQuery(http.uri());
        this.http = http;
    }
    
    public String id() {
        return id;
    }

    @Override
    public String uri() {
        return http.uri();
    }

    public void handleText(String text) {
        textActions.fire(text);
    }

    /**
     * {@link ServerHttpExchange} is available.
     */
    @Override
    public <T> T unwrap(Class<T> clazz) {
        return ServerHttpExchange.class.isAssignableFrom(clazz) ? clazz.cast(http) : null;
    }

    /**
     * For internal use only.
     */
    // TODO find a URI manipulation library
    public static Map<String, String> parseQuery(String uri) {
        Map<String, String> map = new LinkedHashMap<>();
        String query = URI.create(uri).getQuery();
        if (query == null || query.equals("")) {
            return Collections.unmodifiableMap(map);
        }
        String[] params = query.split("&");
        for (String param : params) {
            try {
                String[] pair = param.split("=", 2);
                String name = URLDecoder.decode(pair[0], "UTF-8");
                if (name.equals("")) {
                    continue;
                }
                map.put(name, pair.length > 1 ? URLDecoder.decode(pair[1], "UTF-8") : "");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * For internal use only.
     */
    public static String formatQuery(Map<String, String> params) {
        StringBuilder query = new StringBuilder();
        for (Entry<String, String> entry : params.entrySet()) {
            try {
                query.append(URLEncoder.encode(entry.getKey(), "UTF-8")).append("=").append(URLEncoder.encode(entry.getValue(), "UTF-8"))
                .append("&");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        return query.deleteCharAt(query.length() - 1).toString();
    }

}