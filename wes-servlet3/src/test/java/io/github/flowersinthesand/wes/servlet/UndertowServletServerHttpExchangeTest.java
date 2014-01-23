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
import io.undertow.Undertow;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

public class UndertowServletServerHttpExchangeTest extends ServerHttpExchangeTestTemplate {
	
	Undertow server;

	@Override
	protected void startServer() throws Exception {
		DeploymentInfo builder = Servlets.deployment()
			.setClassLoader(UndertowServletServerHttpExchangeTest.class.getClassLoader())
			.setContextPath("/")
			.addServletContextAttribute("performer", performer)
			.addListener(Servlets.listener(UndertowServletContextListener.class))
			.setDeploymentName("test.war");
		
		DeploymentManager manager = Servlets.defaultContainer().addDeployment(builder);
        manager.deploy();
        
		server = Undertow.builder()
			.addHttpListener(port, "localhost")
			.setHandler(manager.start())
			.build();
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
	
	public static class UndertowServletContextListener implements ServletContextListener {

		@Override
		public void contextInitialized(ServletContextEvent event) {
			final ServletContext servletContext = event.getServletContext();
			new ServletBridge(servletContext, "/test").httpAction(new Action<ServerHttpExchange>() {
				@Override
				public void on(ServerHttpExchange http) {
					Performer performer = (Performer) servletContext.getAttribute("performer"); 
					performer.serverAction().on(http);
				}
			});
		}

		@Override
		public void contextDestroyed(ServletContextEvent event) {}
		
	}

}
