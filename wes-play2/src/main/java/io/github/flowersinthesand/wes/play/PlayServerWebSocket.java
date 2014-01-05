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
package io.github.flowersinthesand.wes.play;

import io.github.flowersinthesand.wes.AbstractServerWebSocket;
import io.github.flowersinthesand.wes.Data;
import io.github.flowersinthesand.wes.ServerWebSocket;
import play.libs.F.Callback;
import play.libs.F.Callback0;
import play.mvc.Http.Request;
import play.mvc.WebSocket;
import play.mvc.WebSocket.In;
import play.mvc.WebSocket.Out;

/**
 * {@link ServerWebSocket} for Play 2.
 * 
 * @author Donghwan Kim
 */
public class PlayServerWebSocket extends AbstractServerWebSocket {

	private final Request request;
	private final WebSocket.Out<String> out;

	public PlayServerWebSocket(Request request, In<String> in, Out<String> out) {
		this.request = request;
		this.out = out;
		in.onMessage(new Callback<String>() {
			@Override
			public void invoke(String message) throws Throwable {
				messageActions.fire(new Data(message));
			}
		});
		in.onClose(new Callback0() {
			@Override
			public void invoke() throws Throwable {
				closeActions.fire();
			}
		});
	}

	@Override
	public String uri() {
		return request.uri();
	}

	@Override
	protected void doClose() {
		out.close();
	}

	@Override
	protected void doSend(String data) {
		out.write(data);
	}
	
	@Override
	public <T> T unwrap(Class<T> clazz) {
		return Request.class.isAssignableFrom(clazz) ? 
			clazz.cast(request) : 
			Out.class.isAssignableFrom(clazz) ? 
				clazz.cast(out) : 
				null;
	}

}
