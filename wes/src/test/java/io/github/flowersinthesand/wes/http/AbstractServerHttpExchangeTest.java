package io.github.flowersinthesand.wes.http;

import static org.junit.Assert.assertEquals;
import io.github.flowersinthesand.wes.Action;
import io.github.flowersinthesand.wes.Actions;
import io.github.flowersinthesand.wes.Data;
import io.github.flowersinthesand.wes.VoidAction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class AbstractServerHttpExchangeTest {

	@Test
	public void requestHeader() {

		EmptyServerHttpExchange http = new EmptyServerHttpExchange() {
			Map<String, List<String>> headers = new LinkedHashMap<>();

			{
				headers.put("A", Arrays.asList("A0", "A1"));
				headers.put("B", Arrays.asList("B0"));
				headers.put("C", Arrays.asList(""));
				headers.put("D", Arrays.asList("D0", "D1"));
				headers.put("E", new ArrayList<String>());
			}

			@Override
			public Set<String> requestHeaderNames() {
				return headers.keySet();
			}

			@Override
			public List<String> requestHeaders(String name) {
				return headers.get(name);
			}
		};

		StringBuilder output = new StringBuilder();
		for (String headerName : http.requestHeaderNames()) {
			output.append(http.requestHeader(headerName));
		}
		output.append(http.requestHeader("F"));
		assertEquals(output.toString(), "A0B0D0nullnull");
	}

	@Test
	public void bodyAction() {
		EmptyServerHttpExchange http = new EmptyServerHttpExchange();
		final StringBuilder output = new StringBuilder("A");
		Action<Data> out = new Action<Data>() {
			@Override
			public void on(Data data) {
				output.append(data.as(String.class));
			}
		};
		http.bodyAction(out);
		output.append("B");
		http.bodyActions.fire(new Data("BODY"));
		http.bodyAction(out);
		assertEquals(output.toString(), "ABBODYBODY");
	}
	
	@Test
	public void closeAction() {
		EmptyServerHttpExchange http = new EmptyServerHttpExchange();
		final StringBuilder output = new StringBuilder("A");
		http.closeAction(new VoidAction() {
			@Override
			public void on() {
				output.append("C");
			}
		});
		output.append("B");
		http.closeActions.fire();
		output.append("D");
		http.closeAction(new VoidAction() {
			@Override
			public void on() {
				output.append("E");
			}
		});
		assertEquals(output.toString(), "ABCDE");
	}

	static class EmptyServerHttpExchange extends AbstractServerHttpExchange {

		public Actions<Void> getCloseActions() {
			return closeActions;
		}

		public Actions<Data> getBodyActions() {
			return bodyActions;
		}

		@Override
		public String uri() {
			return null;
		}

		@Override
		public String method() {
			return null;
		}

		@Override
		public Set<String> requestHeaderNames() {
			return null;
		}

		@Override
		public List<String> requestHeaders(String name) {
			return null;
		}
		
		@Override
		protected void readBody() {
		}
		
		@Override
		public ServerHttpExchange setResponseHeader(String name, String value) {
			return null;
		}

		@Override
		protected void doWrite(String data) {
		}

		@Override
		protected void doClose() {
		}

		@Override
		public ServerHttpExchange setStatus(StatusCode status) {
			return null;
		}

		@Override
		public <T> T unwrap(Class<T> clazz) {
			return null;
		}

	}

}
