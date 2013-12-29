package io.github.flowersinthesand.wes.websocket;

import static org.junit.Assert.assertEquals;
import io.github.flowersinthesand.wes.Action;
import io.github.flowersinthesand.wes.Actions;
import io.github.flowersinthesand.wes.Data;

import org.junit.Test;

public class AbstractServerWebSocketTest {

	@Test
	public void stateTransition() {
		EmptyServerWebSocket ws = new EmptyServerWebSocket();
		assertEquals(ws.state(), State.OPEN);
		ws.close();
		assertEquals(ws.state(), State.CLOSING);
		ws.closeActions.fire();
		assertEquals(ws.state(), State.CLOSED);
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
	
	@Test
	public void messageAction() {
		EmptyServerWebSocket ws = new EmptyServerWebSocket();
		final StringBuilder output = new StringBuilder("A");
		Action<Data> out = new Action<Data>() {
			@Override
			public void on(Data data) {
				output.append(data.as(String.class));
			}
		};
		ws.messageActions.fire(new Data("X"));
		ws.messageAction(out);
		ws.messageActions.fire(new Data("B")).fire(new Data("C"));
		ws.messageAction(out);
		assertEquals(output.toString(), "ABC");
	}

	static class EmptyServerWebSocket extends AbstractServerWebSocket {

		public Actions<Data> getMessageActions() {
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
		public <T> T unwrap(Class<T> clazz) {
			return null;
		}

	}

}
