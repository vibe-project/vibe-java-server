/*
 * Copyright 2013-2014 Donghwan Kim
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
package io.github.flowersinthesand.wes.play;

import io.github.flowersinthesand.wes.AbstractServerHttpExchange;
import io.github.flowersinthesand.wes.Data;
import io.github.flowersinthesand.wes.HttpStatus;
import io.github.flowersinthesand.wes.ServerHttpExchange;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import play.libs.F.Callback0;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Results.Chunks;

/**
 * {@link ServerHttpExchange} for Play 2.
 * 
 * <h3>Quirks</h3>
 * <pre><code>{@literal @}BodyParser.Of(BodyParser.TolerantText.class)
public static Result http() {
  final Request request = request();
  final Response response = response();
  // <strong>Response header is settable here</strong>
  // <strong>Status is set in return</strong>
  return ok(new Chunks&lt;String&gt;(JavaResults.writeString(Codec.utf_8())) {
    {@literal @}Override
    public void onReady(Chunks.Out&lt;String&gt; out) {
      // <strong>Response header is not settable from here on</strong>
      new PlayServerHttpExchange(request, response, out);
    }
  });
}</code></pre>
 * <ul>
 * <li><code>setStatus</code> doesn't work because it have to be specified in return.</li>
 * <li><code>setResponseHeader</code> doesn't work because PlayServerHttpExchange is created after onReady.</li>
 * <li>Request body is read in a synchronous manner.</li>
 * </ul>
 * 
 * @author Donghwan Kim
 */
public class PlayServerHttpExchange extends AbstractServerHttpExchange {

	private final Request request;
	private final Response response;
	private final Chunks.Out<String> out;

	public PlayServerHttpExchange(Request request, Response response, Chunks.Out<String> out) {
		this.request = request;
		this.response = response;
		this.out = out;
		out.onDisconnected(new Callback0() {
			@Override
			public void invoke() throws Throwable {
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
		return request.headers().keySet();
	}

	@Override
	public List<String> requestHeaders(String name) {
		return request.headers().containsKey(name) ? 
			Arrays.asList(request.headers().get(name)) : Collections.<String> emptyList();
	}
	
	@Override
	protected void readBody() {
		// Play can't read body asynchronously
		bodyActions.fire(new Data(request.body().asText()));
	}

	@Override
	public void doSetResponseHeader(String name, String value) {
		response.setHeader(name, value);
	}
	
	@Override
	public void doSetStatus(HttpStatus status) {
		// TODO Is it better to throw an unsupported operation exception?
	}

	@Override
	protected void doWrite(String data) {
		out.write(data);
	}

	@Override
	protected void doClose() {
		out.close();
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
