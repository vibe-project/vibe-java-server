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
package io.github.flowersinthesand.wes;

import static org.junit.Assert.assertEquals;

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
		protected void readBody() {}

		@Override
		public void doSetResponseHeader(String name, String value) {}

		@Override
		protected void doWrite(String data) {}

		@Override
		protected void doClose() {}

		@Override
		protected void doSetStatus(StatusCode status) {}

		@Override
		public <T> T unwrap(Class<T> clazz) {
			return null;
		}

	}

}
