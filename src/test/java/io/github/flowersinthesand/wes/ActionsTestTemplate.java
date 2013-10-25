/*
 * Copyright 2013 Donghwan Kim
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import io.github.flowersinthesand.wes.Actions.Options;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public abstract class ActionsTestTemplate {

	@Test
	public void options() {
		Actions.Options options = new Actions.Options().unique(true);
		Actions<Void> actions = createActions(options);
		final AtomicInteger count = new AtomicInteger();
		VoidAction action = new VoidAction() {
			@Override
			public void on() {
				assertTrue("called once", count.getAndIncrement() == 0);
			}
		};
		options.unique(false);
		actions.add(action).add(action).fire();
	}

	@Test
	public void add() {
		Actions<Void> actions = createActions();
		final StringBuilder output = new StringBuilder();
		VoidAction out = new VoidAction() {
			@Override
			public void on() {
				output.append("A");
			}
		};
		actions.add(out).add(out).fire();
		assertEquals(output.toString(), "AA");

		actions = createActions(new Actions.Options().unique(true));
		output.setLength(0);
		actions.add(out).add(out).fire();
		assertEquals(output.toString(), "A");

		actions = createActions(new Actions.Options().memory(true));
		output.setLength(0);
		actions.add(out).fire();
		assertEquals(output.toString(), "A");
		actions.add(out);
		assertEquals(output.toString(), "AA");
	}

	@Test
	public void fire() {
		Actions<String> actions = createActions();
		final StringBuilder output = new StringBuilder();
		Action<String> out = new Action<String>() {
			@Override
			public void on(String string) {
				output.append(string);
			}
		};

		actions.add(out);
		assertFalse(actions.fired());
		assertEquals("", output.toString());
		actions.fire("H");
		assertTrue(actions.fired());
		assertEquals("H", output.toString());
		actions.fire("H");
		assertTrue(actions.fired());
		assertEquals("HH", output.toString());

		actions = createActions();
		output.setLength(0);
		actions.add(out).add(new Action<String>() {
			@Override
			public void on(String string) {
				output.append("R");
			}
		}).fire("F").fire("E");
		assertEquals("FRER", output.toString());

		actions = createActions(new Actions.Options().memory(true));
		output.setLength(0);
		actions.add(out).fire("F");
		assertEquals("F", output.toString());
		actions.add(out);
		assertEquals("FF", output.toString());

		output.setLength(0);
		actions.fire("E");
		assertEquals("EE", output.toString());
		actions.add(out);
		assertEquals("EEE", output.toString());
	}

	@Test(expected = IllegalStateException.class)
	public void fireAndFireWithOnce() {
		Actions<String> actions = createActions(new Actions.Options()
				.once(true));
		actions.fire().fire();
	}

	@Test(expected = IllegalStateException.class)
	public void disable() {
		Actions<Void> actions = createActions();
		actions.add(new VoidAction() {
			@Override
			public void on() {
			}
		});
		assertFalse(actions.disabled());
		actions.disable();
		assertTrue(actions.disabled());
		actions.disable();
	}

	@Test(expected = IllegalStateException.class)
	public void disableAndAdd() {
		Actions<Void> actions = createActions();
		actions.disable().add(new VoidAction() {
			@Override
			public void on() {
			}
		});
	}

	@Test(expected = IllegalStateException.class)
	public void disableAndFire() {
		Actions<Void> actions = createActions();
		actions.disable().fire();
	}

	@Test
	public void empty() {
		Actions<Void> actions = createActions();
		actions.add(new VoidAction() {
			@Override
			public void on() {
				assertTrue(false);
			}
		}).add(new VoidAction() {

			@Override
			public void on() {
				assertFalse(true);
			}
		}).empty().fire();
	}

	@Test
	public void remove() {
		Actions<Void> actions = createActions();
		VoidAction action = new VoidAction() {
			@Override
			public void on() {
				assertTrue(false);
			}
		};
		actions.add(action).add(action).add(new VoidAction() {
			@Override
			public void on() {
				assertTrue(true);
			}
		}).remove(action).fire();
	}

	@Test
	public void has() {
		final Actions<Void> actions = createActions();
		final VoidAction actionA = new VoidAction() {
			@Override
			public void on() {
			}
		};
		VoidAction actionB = new VoidAction() {
			@Override
			public void on() {
			}
		};

		actions.add(actionA);
		assertTrue(actions.has());
		assertTrue(actions.has(actionA));
		assertFalse(actions.has(actionB));
	}

	protected abstract <T> Actions<T> createActions();

	protected abstract <T> Actions<T> createActions(Options options);

}
