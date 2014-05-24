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

import java.util.List;
import java.util.Set;

/**
 * Represents a server-side HTTP request-response exchange.
 * <p/>
 * Implementations are not thread-safe and decide whether and which event is
 * fired in asynchronous manner.
 *
 * @author Donghwan Kim
 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616.html">RFC2616 -
 * Hypertext Transfer Protocol -- HTTP/1.1</a>
 */
public interface ServerHttpExchange extends Wrapper {

    /**
     * The request URI used to connect.
     */
    String uri();

    /**
     * The name of the request method.
     */
    String method();

    /**
     * The names of the request headers. HTTP header is not case-sensitive but
     * {@link Set} is case-sensitive. When iterating the set unlike getting the
     * header value, you should make it lower-case or upper-case and use it.
     */
    Set<String> requestHeaderNames();

    /**
     * Returns the first request header associated with the given name.
     */
    String requestHeader(String name);

    /**
     * Returns the request headers associated with the given name or empty list
     * if no header is found.
     */
    List<String> requestHeaders(String name);

    /**
     * Attaches an action to be called with the whole request body where the
     * request ends. If the body is quite big, it may drain memory quickly.
     */
    ServerHttpExchange bodyAction(Action<Data> action);

    /**
     * Sets a response header.
     */
    ServerHttpExchange setResponseHeader(String name, String value);

    /**
     * Sets response headers.
     */
    ServerHttpExchange setResponseHeader(String name, Iterable<String> value);

    /**
     * Writes a text to the response body.
     */
    ServerHttpExchange write(String data);

    /**
     * Writes a byte body to the response body.
     */
    ServerHttpExchange write(byte[] data, int offset, int length);

    /**
     * Closes the response. Each exchange must be finished with this method when
     * done. This method has no side effect if called more than once.
     */
    ServerHttpExchange close();

    /**
     * Writes a string to the response body and close the response.
     */
    ServerHttpExchange close(String data);

    /**
     * Sets the HTTP status for the response.
     */
    ServerHttpExchange setStatus(HttpStatus status);

    /**
     * Attaches an action to be called on response close.
     */
    ServerHttpExchange closeAction(Action<Void> action);

}
