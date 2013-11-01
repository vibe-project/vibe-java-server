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
package io.github.flowersinthesand.wes;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;

/**
 * Represents a client-side HTTP request-response exchange.
 * 
 * @author Donghwan Kim
 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616.html">RFC2616 -
 *      Hypertext Transfer Protocol -- HTTP/1.1</a>
 */
public interface HttpClientExchange extends Wrapper {

	/**
	 * The request URI used to connect.
	 */
	String uri();

	/**
	 * The name of the request method.
	 */
	String method();

	/**
	 * Sets a request header.
	 */
	HttpClientExchange requestHeader(String name, String value);

	/**
	 * Sets request headers.
	 */
	HttpClientExchange requestHeader(String name, Iterable<String> value);

	/**
	 * Writes a string to the request body.
	 */
	HttpClientExchange write(String data);

	/**
	 * Writes a binary to the request body.
	 */
	HttpClientExchange write(ByteBuffer data);

	/**
	 * Closes the request.
	 */
	HttpClientExchange close();

	/**
	 * Writes a string to the request body and close the request.
	 */
	HttpClientExchange close(String data);

	/**
	 * Writes a binary to the request body and close the request.
	 */
	HttpClientExchange close(ByteBuffer data);

	/**
	 * Attaches an action to be called on request close.
	 */
	HttpClientExchange closeAction(Action<Void> action);

	/**
	 * Returns the HTTP status of the response
	 */
	HttpStatus status();

	/**
	 * The names of the response headers.
	 */
	Set<String> responseHeaderNames();

	/**
	 * Returns the first response header associated with the given name.
	 */
	String responseHeader(String name);

	/**
	 * Returns the response headers associated with the given name.
	 */
	List<String> responseHeaders(String name);

	/**
	 * Attaches an action to be called with the response chunk. The allowed
	 * chunk type is {@link String} for text chunks and {@link ByteBuffer} for
	 * binary chunks.
	 */
	HttpClientExchange chunkAction(Action<?> action);

	/**
	 * Attaches an action to be called with the whole response body where the
	 * request ends. The allowed body type is {@link String} for text body and
	 * {@link ByteBuffer} for binary body.
	 */
	HttpClientExchange bodyAction(Action<?> action);

}
