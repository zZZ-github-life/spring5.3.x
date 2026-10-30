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

package org.springframework.web.reactive;

import java.util.Collections;
import java.util.Map;

import reactor.core.publisher.Mono;

import org.springframework.lang.Nullable;
import org.springframework.ui.Model;
import org.springframework.validation.support.BindingAwareConcurrentModel;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.bind.support.WebExchangeDataBinder;
import org.springframework.web.server.ServerErrorException;
import org.springframework.web.server.ServerWebExchange;

/**
 * 用于协助将请求数据绑定到对象上，并提供对带有控制器专属属性的共享 {@link Model} 的访问的上下文。
 *
 * <p>提供用于为特定目标对象（命令对象）创建 {@link WebExchangeDataBinder} 的方法，以便对其应用数据绑定和校验；也可以在没有目标对象的情况下创建，用于对请求值进行简单类型转换。
 *
 * <p>请求默认模型的容器。
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 5.0
 */
public class BindingContext {

	@Nullable
	private final WebBindingInitializer initializer;

	private final Model model = new BindingAwareConcurrentModel();


	/**
	 * 创建一个新的 {@code BindingContext}。
	 */
	public BindingContext() {
		this(null);
	}

	/**
	 * 使用给定的初始化器创建一个新的 {@code BindingContext}。
	 * @param initializer 要应用的绑定初始化器（可以为 {@code null}）
	 */
	public BindingContext(@Nullable WebBindingInitializer initializer) {
		this.initializer = initializer;
	}


	/**
	 * 返回默认模型。
	 */
	public Model getModel() {
		return this.model;
	}


	/**
	 * 创建 {@link WebExchangeDataBinder}，用于对目标命令对象应用数据绑定和校验。
	 * @param exchange 当前的交换
	 * @param target 要为其创建数据绑定器的对象
	 * @param name 目标对象的名称
	 * @return 创建的数据绑定器
	 * @throws ServerErrorException 如果 {@code @InitBinder} 方法调用失败
	 */
	public WebExchangeDataBinder createDataBinder(ServerWebExchange exchange, @Nullable Object target, String name) {
		WebExchangeDataBinder dataBinder = new ExtendedWebExchangeDataBinder(target, name);
		if (this.initializer != null) {
			this.initializer.initBinder(dataBinder);
		}
		return initDataBinder(dataBinder, exchange);
	}

	/**
	 * 为给定的 exchange 初始化数据绑定器实例。
	 * @throws ServerErrorException 如果 {@code @InitBinder} 方法调用失败
	 */
	protected WebExchangeDataBinder initDataBinder(WebExchangeDataBinder binder, ServerWebExchange exchange) {
		return binder;
	}

	/**
	 * 创建一个不带目标对象的 {@link WebExchangeDataBinder}，用于将请求值转换为简单类型。
	 * @param exchange 当前的交换
	 * @param name 目标对象的名称
	 * @return 创建的数据绑定器
	 * @throws ServerErrorException 如果 {@code @InitBinder} 方法调用失败
	 */
	public WebExchangeDataBinder createDataBinder(ServerWebExchange exchange, String name) {
		return createDataBinder(exchange, null, name);
	}


	/**
	 * {@link WebExchangeDataBinder} 的扩展变体，增加了路径变量支持。
	 */
	private static class ExtendedWebExchangeDataBinder extends WebExchangeDataBinder {

		public ExtendedWebExchangeDataBinder(@Nullable Object target, String objectName) {
			super(target, objectName);
		}

		@Override
		public Mono<Map<String, Object>> getValuesToBind(ServerWebExchange exchange) {
			return super.getValuesToBind(exchange).doOnNext(map ->
					map.putAll(exchange.<Map<String, String>>getAttributeOrDefault(
							HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Collections.emptyMap())));
		}
	}

}
