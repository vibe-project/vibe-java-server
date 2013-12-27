package io.github.flowersinthesand.wes.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import io.github.flowersinthesand.wes.Action;
import io.github.flowersinthesand.wes.Actions;
import io.github.flowersinthesand.wes.VoidAction;

import java.nio.ByteBuffer;
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
	public void chunkAction() {
		EmptyServerHttpExchange http = new EmptyServerHttpExchange();
		final StringBuilder output = new StringBuilder("A");
		Action<String> out = new Action<String>() {
			@Override
			public void on(String object) {
				output.append(object);
			}
		};
		http.chunkActions.fire("X");
		assertNull(http.bodyType());
		http.chunkAction(out);
		assertEquals(http.bodyType(), String.class);
		output.append("B");
		http.chunkActions.fire("C").fire("D");
		http.chunkAction(out);
		assertEquals(output.toString(), "ABCD");
	}

	@Test(expected = IllegalArgumentException.class)
	public void illegalChunkAction() {
		EmptyServerHttpExchange http = new EmptyServerHttpExchange();
		http.chunkAction(new VoidAction() {
			@Override
			public void on() {
				assertFalse(true);
			}
		});
		assertFalse(true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void illegalChunkActionWithNewChunkType() {
		EmptyServerHttpExchange http = new EmptyServerHttpExchange();
		http.chunkAction(new Action<String>() {
			@Override
			public void on(String object) {
				assertFalse(true);
			}
		});
		http.chunkAction(new Action<ByteBuffer>() {
			@Override
			public void on(ByteBuffer object) {
				assertFalse(true);
			}
		});
		assertFalse(true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void illegalChunkFire() {
		EmptyServerHttpExchange http = new EmptyServerHttpExchange();
		http.chunkActions.fire(1);
		assertFalse(true);
	}

	@Test
	public void bodyAction() {
		EmptyServerHttpExchange http = new EmptyServerHttpExchange();
		final StringBuilder output = new StringBuilder("A");
		Action<String> out = new Action<String>() {
			@Override
			public void on(String object) {
				output.append(object);
			}
		};
		assertNull(http.bodyType());
		http.bodyAction(out);
		assertEquals(http.bodyType(), String.class);
		output.append("B");
		http.bodyActions.fire("BODY");
		http.bodyAction(out);
		assertEquals(output.toString(), "ABBODYBODY");
	}

	@Test(expected = IllegalStateException.class)
	public void bodyAndChunkAction() {
		EmptyServerHttpExchange http = new EmptyServerHttpExchange();
		http.bodyActions.fire("BODY");
		http.chunkAction(new Action<String>() {
			@Override
			public void on(String object) {
				assertFalse(true);
			}
		});
		assertFalse(true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void illegalBodyAction() {
		EmptyServerHttpExchange http = new EmptyServerHttpExchange();
		http.bodyAction(new VoidAction() {
			@Override
			public void on() {
				assertFalse(true);
			}
		});
		assertFalse(true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void illegalBodyActionWithNewChunkType() {
		EmptyServerHttpExchange http = new EmptyServerHttpExchange();
		http.chunkAction(new Action<String>() {
			@Override
			public void on(String object) {
				assertFalse(true);
			}
		});
		http.chunkAction(new Action<ByteBuffer>() {
			@Override
			public void on(ByteBuffer object) {
				assertFalse(true);
			}
		});
		assertFalse(true);
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

	@Test
	public void status() {
		final StringBuilder output = new StringBuilder();
		new EmptyServerHttpExchange() {
			@Override
			public ServerHttpExchange setStatus(StatusCode status) {
				assertEquals(status, StatusCode.OK);
				output.append("A");
				return this;
			}
		};
		output.append("B");
		assertEquals(output.toString(), "AB");
	}

	static class EmptyServerHttpExchange extends AbstractServerHttpExchange {

		public Actions<Void> getCloseActions() {
			return closeActions;
		}

		public Actions<Object> getChunkActions() {
			return chunkActions;
		}

		public Actions<Object> getBodyActions() {
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
		public ServerHttpExchange setResponseHeader(String name, Iterable<String> value) {
			return null;
		}
		@Override
		public ServerHttpExchange setResponseHeader(String name, String value) {
			return null;
		}

		@Override
		protected void doWrite(String data) {
		}

		@Override
		protected void doWrite(ByteBuffer data) {
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
