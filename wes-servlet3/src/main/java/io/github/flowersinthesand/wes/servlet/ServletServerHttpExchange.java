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
package io.github.flowersinthesand.wes.servlet;

import io.github.flowersinthesand.wes.AbstractServerHttpExchange;
import io.github.flowersinthesand.wes.Data;
import io.github.flowersinthesand.wes.HttpStatus;
import io.github.flowersinthesand.wes.ServerHttpExchange;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * {@link ServerHttpExchange} for Servlet 3.
 * 
 * @author Donghwan Kim
 */
public class ServletServerHttpExchange extends AbstractServerHttpExchange {

	private final HttpServletRequest request;
	private final HttpServletResponse response;

	public ServletServerHttpExchange(HttpServletRequest request, HttpServletResponse response) {
		this.request = request;
		this.response = response;
		AsyncContext async = request.startAsync();
		async.setTimeout(0);
		async.addListener(new AsyncListener() {
			@Override
			public void onStartAsync(AsyncEvent event) throws IOException {}
			
			@Override
			public void onTimeout(AsyncEvent event) throws IOException {
				closeActions.fire();
			}
			
			@Override
			public void onError(AsyncEvent event) throws IOException {
				closeActions.fire();
			}
			
			@Override
			public void onComplete(AsyncEvent event) throws IOException {
				closeActions.fire();
			}
		});
	}

	@Override
	public String uri() {
		String uri = request.getRequestURI();
		if (request.getQueryString() != null) {
			uri += "?" + request.getQueryString();
		}
		return uri;
	}

	@Override
	public String method() {
		return request.getMethod();
	}

	@Override
	public Set<String> requestHeaderNames() {
		Set<String> headerNames = new LinkedHashSet<>();
		Enumeration<String> enumeration = request.getHeaderNames();
		while (enumeration.hasMoreElements()) {
			headerNames.add(enumeration.nextElement());
		}
		return headerNames;
	}

	@Override
	public List<String> requestHeaders(String name) {
		return Collections.list(request.getHeaders(name));
	}

	@Override
	protected void readBody() {
		final ServletInputStream input;
		try {
			input = request.getInputStream();
		} catch (IOException e) {
			throw new RuntimeException();
		}
		// HTTP 1.1 says that the default charset is ISO-8859-1 
		// http://www.w3.org/International/O-HTTP-charset#charset
		String charsetName = request.getCharacterEncoding();
		final Charset charset = Charset.forName(charsetName == null ? "ISO-8859-1" : charsetName);
		final StringBuilder body = new StringBuilder();
		
		if (request.getServletContext().getMinorVersion() > 0) {
			// 3.1+ asynchronous
			input.setReadListener(new ReadListener() {
				@Override
				public void onDataAvailable() throws IOException {
					int bytesRead = -1;
					byte buffer[] = new byte[4096];
					while (input.isReady() && (bytesRead = input.read(buffer)) != -1) {
						String data = new String(buffer, 0, bytesRead, charset);
						body.append(data);
					}
				}

				@Override
				public void onAllDataRead() throws IOException {
					bodyActions.fire(new Data(body.toString()));
				}

				@Override
				public void onError(Throwable t) {
					throw new RuntimeException(t);
				}
			});
		} else {
			// 3.0 synchronous
			try {
				int bytesRead = -1;
				byte buffer[] = new byte[4096];
				while ((bytesRead = input.read(buffer)) != -1) {
					String data = new String(buffer, 0, bytesRead, charset);
					body.append(data);
				}
				bodyActions.fire(new Data(body.toString()));
			} catch (IOException e) {
				throw new RuntimeException();
			}
		}
	}

	@Override
	protected void doSetResponseHeader(String name, String value) {
		response.setHeader(name, value);
	}

	@Override
	protected void doSetStatus(HttpStatus status) {
		response.setStatus(status.code());
	}

	@Override
	protected void doWrite(String data) {
		try {
			PrintWriter writer = response.getWriter();
			writer.print(data);
			writer.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void doClose() {
		request.getAsyncContext().complete();
	}

	/**
	 * {@link HttpServletRequest} and {@link HttpServletResponse} are available.
	 */
	@Override
	public <T> T unwrap(Class<T> clazz) {
		return HttpServletRequest.class.isAssignableFrom(clazz) ? 
			clazz.cast(request) : 
			HttpServletResponse.class.isAssignableFrom(clazz) ? 
				clazz.cast(response) : 
				null;
	}

}
