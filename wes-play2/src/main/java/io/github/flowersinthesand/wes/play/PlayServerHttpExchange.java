package io.github.flowersinthesand.wes.play;

import io.github.flowersinthesand.wes.Data;
import io.github.flowersinthesand.wes.http.AbstractServerHttpExchange;
import io.github.flowersinthesand.wes.http.ServerHttpExchange;
import io.github.flowersinthesand.wes.http.StatusCode;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import play.libs.F.Callback0;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Results.Chunks;

public class PlayServerHttpExchange extends AbstractServerHttpExchange {

	private final Request request;
	private final Response response;
	private final Chunks.Out<String> out;

	public PlayServerHttpExchange(Request request, Response response, Chunks.Out<String> out) {
		this.request = request;
		this.response = response;
		this.out = out;
		// Play 2 can't read body in chunk and read body asynchronously
		bodyActions.fire(new Data(request.body().asText()));
		out.onDisconnected(new Callback0() {
			@Override
			public void invoke() throws Throwable {
				closeActions.fire();
			}
		});
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
			Arrays.asList(request.headers().get(name)) : 
			null;
	}

	@Override
	public ServerHttpExchange setResponseHeader(String name, String value) {
		response.setHeader(name, value);
		return this;
	}

	@Override
	public ServerHttpExchange setResponseHeader(String name, Iterable<String> value) {
		setResponseHeader(name, StringUtils.join(value, ","));
		return this;
	}

	@Override
	public ServerHttpExchange setStatus(StatusCode status) {
		// TODO Is it better to throw an unsupported operation exception?
		return this;
	}

	@Override
	public String uri() {
		return request.uri();
	}

	@Override
	protected void doClose() {
		out.close();
	}

	@Override
	protected void doWrite(String data) {
		out.write(data);
	}
	
	@Override
	public <T> T unwrap(Class<T> clazz) {
		return Request.class.isAssignableFrom(clazz) ? 
			clazz.cast(request) : 
			Response.class.isAssignableFrom(clazz) ? 
				clazz.cast(response) : 
				null;
	}

}
