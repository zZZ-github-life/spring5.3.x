/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.handler;

import org.springframework.web.socket.WebSocketHandler;

/**
 * 用于向 WebSocketHandler 应用装饰器的工厂。
 *
 * <p>应通过继承
 * {@link org.springframework.web.socket.handler.WebSocketHandlerDecorator
 * WebSocketHandlerDecorator} 来完成装饰，以便任何代码都能在必要时
 * 遍历装饰器链和/或解包原始处理器。
 *
 * @author Rossen Stoyanchev
 * @since 4.1.2
 */
public interface WebSocketHandlerDecoratorFactory {

	/**
	 * 装饰给定的 WebSocketHandler。
	 * @param handler the handler to be decorated.
	 * @return the same handler or the handler wrapped with a subclass of
	 * {@code WebSocketHandlerDecorator}.
	 */
	WebSocketHandler decorate(WebSocketHandler handler);

}
