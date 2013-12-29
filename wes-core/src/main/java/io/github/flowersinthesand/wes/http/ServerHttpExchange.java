/*
 * Copyright 2013 Donghwan Kim
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
package io.github.flowersinthesand.wes.http;

import io.github.flowersinthesand.wes.Action;
import io.github.flowersinthesand.wes.Data;
import io.github.flowersinthesand.wes.Wrapper;

import java.util.List;
import java.util.Set;

/**
 * Represents a server-side HTTP request-response exchange.
 * 
 * @author Donghwan Kim
 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616.html">RFC2616 -
 *      Hypertext Transfer Protocol -- HTTP/1.1</a>
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
	 * The names of the request headers.
	 */
	Set<String> requestHeaderNames();

	/**
	 * Returns the first request header associated with the given name.
	 */
	String requestHeader(String name);

	/**
	 * Returns the request headers associated with the given name.
	 */
	List<String> requestHeaders(String name);

	/**
	 * Attaches an action to be called with the request chunk. The allowed chunk
	 * type is {@link String} for text chunks.
	 */
	ServerHttpExchange chunkAction(Action<Data> action);

	/**
	 * Attaches an action to be called with the whole request body where the
	 * request ends. The allowed body type is {@link String} for text body.
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
	 * Writes a string to the response body.
	 */
	ServerHttpExchange write(String data);

	/**
	 * Closes the response. Each exchange should be finished with this method when done. 
	 */
	ServerHttpExchange close();

	/**
	 * Writes a string to the response body and close the response.
	 */
	ServerHttpExchange close(String data);

	/**
	 * Sets the HTTP status for the response. By default, {@link StatusCode#OK}
	 * is set.
	 */
	ServerHttpExchange setStatus(StatusCode status);

	/**
	 * Attaches an action to be called on response close.
	 */
	ServerHttpExchange closeAction(Action<Void> action);

}
