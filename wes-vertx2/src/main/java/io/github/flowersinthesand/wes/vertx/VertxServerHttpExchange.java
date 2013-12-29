package io.github.flowersinthesand.wes.vertx;

import io.github.flowersinthesand.wes.Data;
import io.github.flowersinthesand.wes.http.AbstractServerHttpExchange;
import io.github.flowersinthesand.wes.http.ServerHttpExchange;
import io.github.flowersinthesand.wes.http.StatusCode;

import java.util.List;
import java.util.Set;

import org.vertx.java.core.Handler;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

public class VertxServerHttpExchange extends AbstractServerHttpExchange {

	private final HttpServerRequest request;

	public VertxServerHttpExchange(HttpServerRequest request) {
		this.request = request;
		request.dataHandler(new Handler<Buffer>() {
			@Override
			public void handle(Buffer buffer) {
				chunkActions.fire(new Data(buffer.toString()));
			}
		})
		.bodyHandler(new Handler<Buffer>() {
			@Override
			public void handle(Buffer buffer) {
				bodyActions.fire(new Data(buffer.toString()));
			}
		});
		request.response().setChunked(true).closeHandler(new VoidHandler() {
			@Override
			protected void handle() {
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
		return request.headers().names();
	}

	@Override
	public List<String> requestHeaders(String name) {
		return request.headers().getAll(name);
	}

	@Override
	public String requestHeader(String name) {
		return request.headers().get(name);
	}

	@Override
	public ServerHttpExchange setResponseHeader(String name, String value) {
		request.response().putHeader(name, value);
		return this;
	}

	@Override
	public ServerHttpExchange setResponseHeader(String name, Iterable<String> value) {
		request.response().putHeader(name, value);
		return this;
	}

	@Override
	protected void doWrite(String data) {
		request.response().write(data);
	}

	@Override
	protected void doClose() {
		request.response().end();
		request.response().close();
	}

	@Override
	public ServerHttpExchange setStatus(StatusCode status) {
		request.response().setStatusCode(status.code()).setStatusMessage(status.reason());
		return this;
	}

	@Override
	public <T> T unwrap(Class<T> clazz) {
		return HttpServerRequest.class.isAssignableFrom(clazz) ? clazz.cast(request) : null;
	}

}
