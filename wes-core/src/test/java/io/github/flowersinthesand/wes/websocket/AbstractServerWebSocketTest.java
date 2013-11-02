package io.github.flowersinthesand.wes.websocket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import io.github.flowersinthesand.wes.Action;
import io.github.flowersinthesand.wes.Actions;
import io.github.flowersinthesand.wes.VoidAction;

import java.nio.ByteBuffer;

import org.junit.Test;

public class AbstractServerWebSocketTest {

	@Test
	public void stateTransition() {
		EmptyServerWebSocket ws = new EmptyServerWebSocket();
		assertEquals(ws.state(), State.CONNECTING);
		ws.openActions.fire();
		assertEquals(ws.state(), State.OPEN);
		ws.close();
		assertEquals(ws.state(), State.CLOSING);
		ws.closeActions.fire();
		assertEquals(ws.state(), State.CLOSED);
	}

	@Test
	public void openAction() {
		EmptyServerWebSocket ws = new EmptyServerWebSocket();
		final StringBuilder output = new StringBuilder("A");
		ws.openAction(new VoidAction() {
			@Override
			public void on() {
				output.append("C");
			}
		});
		output.append("B");
		ws.openActions.fire();
		output.append("D");
		ws.openAction(new VoidAction() {
			@Override
			public void on() {
				output.append("E");
			}
		});
		assertEquals(output.toString(), "ABCDE");
	}

	@Test
	public void closeAction() {
		EmptyServerWebSocket ws = new EmptyServerWebSocket();
		final StringBuilder output = new StringBuilder("A");
		ws.closeAction(new Action<CloseReason>() {
			@Override
			public void on(CloseReason reason) {
				output.append("C");
			}
		});
		output.append("B");
		ws.closeActions.fire();
		output.append("D");
		ws.closeAction(new Action<CloseReason>() {
			@Override
			public void on(CloseReason reason) {
				output.append("E");
			}
		});
		assertEquals(output.toString(), "ABCDE");
	}

	@Test
	public void errorAction() {
		EmptyServerWebSocket ws = new EmptyServerWebSocket() {
			@Override
			protected void doClose(CloseReason reason) {
				closeActions.fire(reason);
			}
		};
		final StringBuilder output = new StringBuilder("A");
		ws.errorAction(new Action<Throwable>() {
			@Override
			public void on(Throwable throwable) {
				output.append("C");
			}
		});
		output.append("B");
		ws.errorActions.fire();
		output.append("D");
		ws.errorAction(new Action<Throwable>() {
			@Override
			public void on(Throwable throwable) {
				output.append("E");
			}
		});
		ws.closeAction(new Action<CloseReason>() {
			@Override
			public void on(CloseReason reason) {
				assertEquals(reason, CloseReason.SERVER_ERROR);
				output.append("F");
			}
		});
		assertEquals(output.toString(), "ABCDEF");
	}

	@Test(expected = IllegalStateException.class)
	public void closeAndOpenAction() {
		EmptyServerWebSocket ws = new EmptyServerWebSocket();
		ws.closeActions.fire();
		ws.openAction(new VoidAction() {
			@Override
			public void on() {
				assertFalse(true);
			}
		});
		assertFalse(true);
	}

	@Test(expected = IllegalStateException.class)
	public void closeAndMessageAction() {
		EmptyServerWebSocket ws = new EmptyServerWebSocket();
		ws.closeActions.fire();
		ws.messageAction(new Action<String>() {
			@Override
			public void on(String object) {
				assertFalse(true);
			}
		});
		assertFalse(true);
	}

	@Test
	public void messageAction() {
		EmptyServerWebSocket ws = new EmptyServerWebSocket();
		final StringBuilder output = new StringBuilder("A");
		ws.messageActions.fire("X");
		assertNull(ws.messageType());
		ws.messageAction(new Action<String>() {
			@Override
			public void on(String object) {
				output.append(object);
			}
		});
		assertEquals(ws.messageType(), String.class);
		ws.messageActions.fire("B").fire("C");
		assertEquals(output.toString(), "ABC");
	}

	@Test(expected = IllegalArgumentException.class)
	public void illegalMessageAction() {
		EmptyServerWebSocket ws = new EmptyServerWebSocket();
		ws.messageAction(new VoidAction() {
			@Override
			public void on() {
				assertFalse(true);
			}
		});
		assertFalse(true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void illegalMessageFire() {
		EmptyServerWebSocket ws = new EmptyServerWebSocket();
		ws.messageActions.fire(1);
		assertFalse(true);
	}

	static class EmptyServerWebSocket extends AbstractServerWebSocket {

		public Actions<Void> getOpenActions() {
			return openActions;
		}

		public Actions<Object> getMessageActions() {
			return messageActions;
		}

		public Actions<Throwable> getErrorActions() {
			return errorActions;
		}

		public Actions<CloseReason> getCloseActions() {
			return closeActions;
		}

		@Override
		public String uri() {
			return null;
		}

		@Override
		protected void doClose(CloseReason reason) {
		}

		@Override
		protected void doSend(String data) {

		}

		@Override
		protected void doSend(ByteBuffer data) {

		}

		@Override
		public <T> T unwrap(Class<T> clazz) {
			return null;
		}

	}

}
