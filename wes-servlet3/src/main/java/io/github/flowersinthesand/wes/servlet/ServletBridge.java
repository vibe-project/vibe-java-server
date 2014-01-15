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

import io.github.flowersinthesand.wes.Action;
import io.github.flowersinthesand.wes.Actions;
import io.github.flowersinthesand.wes.ServerHttpExchange;
import io.github.flowersinthesand.wes.SimpleActions;

import java.util.UUID;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Convenient class to install Servlet bridge.
 * 
 * @author Donghwan Kim
 */
public class ServletBridge {

	private Actions<ServerHttpExchange> httpActions = new SimpleActions<>();

	public ServletBridge(ServletContext context, String path) {
		@SuppressWarnings("serial")
		Servlet servlet = new HttpServlet() {
			@Override
			protected void service(HttpServletRequest req, HttpServletResponse resp) {
				httpActions.fire(new ServletServerHttpExchange(req, resp));
			}
		};
		ServletRegistration.Dynamic reg = context.addServlet("wes#" + UUID.randomUUID(), servlet);
		reg.setAsyncSupported(true);
		reg.addMapping(path);
	}

	/**
	 * Adds an action to be called on HTTP request with
	 * {@link ServerHttpExchange}.
	 */
	public ServletBridge httpAction(Action<ServerHttpExchange> action) {
		httpActions.add(action);
		return this;
	}

}
