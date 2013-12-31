package io.github.flowersinthesand.wes.atmosphere;

import io.github.flowersinthesand.wes.Data;
import io.github.flowersinthesand.wes.http.AbstractServerHttpExchange;
import io.github.flowersinthesand.wes.http.ServerHttpExchange;
import io.github.flowersinthesand.wes.http.StatusCode;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.AtmosphereResourceEventListenerAdapter;

public class AtmosphereServerHttpExchange extends AbstractServerHttpExchange {

	private final AtmosphereResource resource;

	public AtmosphereServerHttpExchange(AtmosphereResource resource) {
		this.resource = resource.suspend();
		try {
			final ServletInputStream input = resource.getRequest().getInputStream();
			// Since Servlet 3.1
			input.setReadListener(new ReadListener() {
				List<String> chunks = new ArrayList<>();
				@Override
				public void onDataAvailable() throws IOException {
					int bytesRead = -1;
					byte buffer[] = new byte[4096];
					while (input.isReady() && (bytesRead = input.read(buffer)) != -1) {
						String data = new String(buffer, 0, bytesRead);
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
		resource.addEventListener(new AtmosphereResourceEventListenerAdapter() {
			@Override
			public void onResume(AtmosphereResourceEvent event) {
				closeActions.fire();
			}

			@Override
			public void onDisconnect(AtmosphereResourceEvent event) {
				closeActions.fire();
			}

			@Override
			public void onClose(AtmosphereResourceEvent event) {
				closeActions.fire();
			}
		});
	}

	@Override
	public String uri() {
		return resource.getRequest().getRequestURI();
	}

	@Override
	public String method() {
		return resource.getRequest().getMethod();
	}

	@Override
	public Set<String> requestHeaderNames() {
		Set<String> headerNames = new LinkedHashSet<>();
		Enumeration<String> enumeration = resource.getRequest().getHeaderNames();
		while (enumeration.hasMoreElements()) {
			headerNames.add(enumeration.nextElement());
		}
		return headerNames;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<String> requestHeaders(String name) {
		return Collections.list(resource.getRequest().getHeaders(name));
	}

	@Override
	public ServerHttpExchange setResponseHeader(String name, String value) {
		resource.getResponse().setHeader(name, value);
		return this;
	}

	@Override
	public ServerHttpExchange setResponseHeader(String name, Iterable<String> value) {
		for (String v : value) {
			resource.getResponse().addHeader(name, v);
		}
		return this;
	}

	@Override
	public ServerHttpExchange setStatus(StatusCode status) {
		resource.getResponse().setStatus(status.code(), status.reason());
		return this;
	}

	@Override
	protected void doWrite(String data) {
		try {
			PrintWriter writer = resource.getResponse().getWriter();
			writer.print(data);
			writer.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void doClose() {
		resource.resume();
		try {
			resource.close();
		} catch (IOException e) {}
	}

	@Override
	public <T> T unwrap(Class<T> clazz) {
		return AtmosphereResource.class.isAssignableFrom(clazz) ? clazz.cast(resource) : null;
	}

}
