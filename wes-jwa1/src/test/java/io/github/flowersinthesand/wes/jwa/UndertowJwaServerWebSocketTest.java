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
package io.github.flowersinthesand.wes.jwa;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import io.github.flowersinthesand.wes.Action;
import io.github.flowersinthesand.wes.ServerWebSocket;
import io.github.flowersinthesand.wes.test.ServerWebSocketTestTemplate;
import io.undertow.Undertow;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;

import javax.servlet.ServletException;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpointConfig;

import org.junit.Test;

public class UndertowJwaServerWebSocketTest extends ServerWebSocketTestTemplate {

	Undertow server;

	@Override
	protected void startServer() throws ServletException {
		ServerEndpointConfig config = new JwaBridge("/test").websocketAction(new Action<ServerWebSocket>() {
			@Override
			public void on(ServerWebSocket ws) {
				performer.serverAction().on(ws);
			}
		})
		.config();
		
		DeploymentInfo builder = Servlets.deployment()
			.setClassLoader(UndertowJwaServerWebSocketTest.class.getClassLoader())
			.setContextPath("/")
			.addServletContextAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME,  new WebSocketDeploymentInfo().addEndpoint(config))
			.setDeploymentName("test.war");
		
		DeploymentManager manager = Servlets.defaultContainer().addDeployment(builder);
        manager.deploy();
        
		server = Undertow.builder()
			.addHttpListener(port, "localhost")
			.setHandler(manager.start())
			.build();
		server.start();
	}
	
	@Test
	public void unwrap() {
		performer.serverAction(new Action<ServerWebSocket>() {
			@Override
			public void on(ServerWebSocket ws) {
				assertThat(ws.unwrap(Session.class), instanceOf(Session.class));
				performer.start();
			}
		})
		.connect();
	}

	@Override
	protected void stopServer() {
		server.stop();
	}

}
