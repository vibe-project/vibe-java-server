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
package io.github.flowersinthesand.wes.atmosphere;

import io.github.flowersinthesand.wes.AbstractServerHttpExchange;
import io.github.flowersinthesand.wes.Data;
import io.github.flowersinthesand.wes.ServerHttpExchange;
import io.github.flowersinthesand.wes.HttpStatus;

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

/**
 * {@link ServerHttpExchange} for Atmosphere 2.
 * 
 * @author Donghwan Kim
 */
public class AtmosphereServerHttpExchange extends AbstractServerHttpExchange {

	private final AtmosphereResource resource;

	public AtmosphereServerHttpExchange(AtmosphereResource resource) {
		this.resource = resource.suspend();
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
		String uri = resource.getRequest().getRequestURI();
		if (resource.getRequest().getQueryString() != null) {
			uri += "?" + resource.getRequest().getQueryString();
		}
		return uri;
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
	protected void readBody() {
		try {
			final ServletInputStream input = resource.getRequest().getInputStream();
			final String charset = resource.getRequest().getCharacterEncoding() == null ?
				// HTTP 1.1 says that the default charset is ISO-8859-1 
				// http://www.w3.org/International/O-HTTP-charset#charset
				"ISO-8859-1" : 
				resource.getRequest().getCharacterEncoding();
			
			// Supported as of Servlet 3.1
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
	public void doSetResponseHeader(String name, String value) {
		resource.getResponse().setHeader(name, value);
	}

	@Override
	public void doSetStatus(HttpStatus status) {
		resource.getResponse().setStatus(status.code());
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
