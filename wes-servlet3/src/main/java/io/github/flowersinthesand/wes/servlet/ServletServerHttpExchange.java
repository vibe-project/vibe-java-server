package io.github.flowersinthesand.wes.servlet;

import io.github.flowersinthesand.wes.AbstractServerHttpExchange;
import io.github.flowersinthesand.wes.Data;
import io.github.flowersinthesand.wes.HttpStatus;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
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
		try {
			final ServletInputStream input = request.getInputStream();
			final String charset = request.getCharacterEncoding() == null ?
				// HTTP 1.1 says that the default charset is ISO-8859-1 
				// http://www.w3.org/International/O-HTTP-charset#charset
				"ISO-8859-1" : 
				request.getCharacterEncoding();
			
			input.setReadListener(new ReadListener() {
				List<String> chunks = new ArrayList<>();
				@Override
				public void onDataAvailable() throws IOException {
					int bytesRead = -1;
					byte buffer[] = new byte[4096];
					while (input.isReady() && (bytesRead = input.read(buffer)) != -1) {
						String data = new String(buffer, 0, bytesRead, charset);
						chunks.add(data);
					}
				}

				@Override
				public void onAllDataRead() throws IOException {
					StringBuilder body = new StringBuilder();
					for (String chunk : chunks) {
						body.append(chunk);
					}
					bodyActions.fire(new Data(body.toString()));
				}

				@Override
				public void onError(Throwable t) {}
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
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

	@Override
	public <T> T unwrap(Class<T> clazz) {
		return HttpServletRequest.class.isAssignableFrom(clazz) ? 
			clazz.cast(request) : 
			HttpServletResponse.class.isAssignableFrom(clazz) ? 
				clazz.cast(response) : 
				null;
	}

}
