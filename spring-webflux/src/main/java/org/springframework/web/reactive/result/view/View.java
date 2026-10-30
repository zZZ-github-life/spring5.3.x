/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.reactive.result.view;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Mono;

import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.server.ServerWebExchange;

/**
 * 将 {@link HandlerResult} 渲染到 HTTP 响应的契约。
 *
 * <p>与 {@link org.springframework.core.codec.Encoder Encoder}（它是单例并编码给定类型的任何对象）不同，{@code View} 通常按名称选择，并使用 {@link ViewResolver} 解析，例如可以将名称匹配到 HTML 模板。此外，{@code View} 可以基于模型中包含的多个属性进行渲染。
 *
 * <p>{@code View} 还可以选择从模型中选择一个属性，并使用任何现有的 {@code Encoder} 渲染替代媒体类型。
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface View {

	/**
	 * 包含请求的 {@link org.springframework.web.reactive.BindingContext BindingContext} 的交换属性名称，可用于为模型中的对象创建 {@link org.springframework.validation.BindingResult BindingResult} 实例。
	 * <p>注意：此属性不是必需的，可能不存在。
	 * @since 5.1.8
	 */
	String BINDING_CONTEXT_ATTRIBUTE = View.class.getName() + ".bindingContext";


	/**
	 * 返回此 View 支持的媒体类型列表，或空列表。
	 */
	default List<MediaType> getSupportedMediaTypes() {
		return Collections.emptyList();
	}

	/**
	 * 此 View 是否通过执行重定向进行渲染。
	 */
	default boolean isRedirectView() {
		return false;
	}

	/**
	 * 基于给定 {@link HandlerResult} 渲染视图。实现可以访问并使用模型，或仅使用其中的特定属性。
	 * @param model 以名称字符串为键、对应模型对象为值的 Map
	 * （在模型为空时 Map 也可以为 {@code null}）
	 * @param contentType 选择用于渲染的内容类型，
	 * 它应匹配 {@link #getSupportedMediaTypes() supported media types}。
	 * @param exchange 当前交换
	 * @return 表示渲染何时成功以及是否成功的 {@code Mono}
	 */
	Mono<Void> render(@Nullable Map<String, ?> model, @Nullable MediaType contentType, ServerWebExchange exchange);

}
