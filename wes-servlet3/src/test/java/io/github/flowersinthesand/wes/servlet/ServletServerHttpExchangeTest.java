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
package io.github.flowersinthesand.wes.servlet;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import io.github.flowersinthesand.wes.Action;
import io.github.flowersinthesand.wes.ServerHttpExchange;
import io.github.flowersinthesand.wes.test.ServerHttpExchangeTestTemplate;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.Ignore;
import org.junit.Test;

public class ServletServerHttpExchangeTest extends ServerHttpExchangeTestTemplate {

	Server server;

	@Override
	protected void startServer() throws Exception {
		server = new Server();
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(port);
		server.addConnector(connector);
		
		// Servlet
		ServletHandler handler = new ServletHandler();
		server.setHandler(handler);
		@SuppressWarnings("serial")
	 	Servlet servlet = new HttpServlet() {
			@Override
			protected void service(HttpServletRequest req, HttpServletResponse res) {
				performer.serverAction().on(new ServletServerHttpExchange(req, res));
			}
		};
		ServletHolder holder = new ServletHolder(servlet);
		holder.setAsyncSupported(true);
		handler.addServletWithMapping(holder, "/test");
		
		server.start();
	}

	@Override
	protected void stopServer() throws Exception {
		server.stop();
	}

	@Test
	public void unwrap() {
		performer.serverAction(new Action<ServerHttpExchange>() {
			@Override
			public void on(ServerHttpExchange http) {
				assertThat(http.unwrap(HttpServletRequest.class), instanceOf(HttpServletRequest.class));
				assertThat(http.unwrap(HttpServletResponse.class), instanceOf(HttpServletResponse.class));
				performer.start();
			}
		})
		.send();
	}
	
	@Override
	@Test
	@Ignore
	public void closeAction_by_client() {}

}
