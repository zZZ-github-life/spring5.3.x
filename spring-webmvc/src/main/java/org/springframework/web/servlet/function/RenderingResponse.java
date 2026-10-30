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

package org.springframework.web.servlet.function;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

import javax.servlet.http.Cookie;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

/**
 * {@link ServerResponse} 的渲染特定子类型，暴露模型和模板数据。
 *
 * @author Arjen Poutsma
 * @since 5.2
 */
public interface RenderingResponse extends ServerResponse {

	/**
	 * 返回要渲染的模板名称。
	 */
	String name();

	/**
	 * 返回不可修改的模型映射。
	 */
	Map<String, Object> model();


	// Builder

	/**
	 * 使用给定响应的模板名称、状态码、标头和模型创建构建器。
	 * @param other 要从中复制值的响应
	 * @return 创建的构建器
	 */
	static Builder from(RenderingResponse other) {
		return new DefaultRenderingResponseBuilder(other);
	}

	/**
	 * 使用给定模板名称创建构建器。
	 * @param name 要渲染的模板名称
	 * @return 创建的构建器
	 */
	static Builder create(String name) {
		return new DefaultRenderingResponseBuilder(name);
	}


	/**
	 * 定义 {@code RenderingResponse} 的构建器。
	 */
	interface Builder {

		/**
		 * 使用
		 * {@linkplain org.springframework.core.Conventions#getVariableName 生成的名称}
		 * 将提供的属性添加到模型中。
		 * <p><em>注意：使用此方法时，空的 {@link Collection Collections}
		 * 不会被添加到模型中，因为无法正确确定真正的约定名称。
		 * 视图代码应检查 {@code null}，而不是空集合。</em>
		 * @param attribute 模型属性值（永不为 {@code null}）
		 */
		Builder modelAttribute(Object attribute);

		/**
		 * 在提供的名称下添加提供的属性值。
		 * @param name 模型属性的名称（永不为 {@code null}）
		 * @param value 模型属性值（可以为 {@code null}）
		 */
		Builder modelAttribute(String name, @Nullable Object value);

		/**
		 * 将提供的数组中的所有属性复制到模型中，并为每个元素生成属性名称。
		 * @see #modelAttribute(Object)
		 */
		Builder modelAttributes(Object... attributes);

		/**
		 * 将提供的 {@code Collection} 中的所有属性复制到模型中，并为每个元素生成属性名称。
		 * @see #modelAttribute(Object)
		 */
		Builder modelAttributes(Collection<?> attributes);

		/**
		 * 将提供的 {@code Map} 中的所有属性复制到模型中。
		 * @see #modelAttribute(String, Object)
		 */
		Builder modelAttributes(Map<String, ?> attributes);

		/**
		 * 在给定名称下添加给定的标头值。
		 * @param headerName 头名称
		 * @param headerValues 头值
		 * @return 此构建器
		 * @see HttpHeaders#add(String, String)
		 */
		Builder header(String headerName, String... headerValues);

		/**
		 * 使用给定消费者操作此响应的标头。提供给消费者的标头是"实时"的，
		 * 因此消费者可以用它来{@linkplain HttpHeaders#set(String, String) 覆盖}现有标头值、
		 * {@linkplain HttpHeaders#remove(Object) 移除}值，或使用任何其他
		 * {@link HttpHeaders} 方法。
		 * @param headersConsumer 消费 {@code HttpHeaders} 的函数
		 * @return 此构建器
		 */
		Builder headers(Consumer<HttpHeaders> headersConsumer);

		/**
		 * 设置 HTTP 状态。
		 * @param status 响应状态
		 * @return 此构建器
		 */
		Builder status(HttpStatus status);

		/**
		 * 设置 HTTP 状态。
		 * @param status 响应状态
		 * @return 此构建器
		 */
		Builder status(int status);

		/**
		 * 将给定 cookie 添加到响应中。
		 * @param cookie 要添加的 Cookie
		 * @return 此构建器
		 */
		Builder cookie(Cookie cookie);

		/**
		 * 使用给定消费者操作此响应的 cookie。提供给消费者的 cookie 是"实时"的，
		 * 因此消费者可以用它来{@linkplain MultiValueMap#set(Object, Object) 覆盖}现有 cookie、
		 * {@linkplain MultiValueMap#remove(Object) 移除}cookie，或使用任何其他
		 * {@link MultiValueMap} 方法。
		 * @param cookiesConsumer 消费 Cookie 的函数
		 * @return 此构建器
		 */
		Builder cookies(Consumer<MultiValueMap<String, Cookie>> cookiesConsumer);

		/**
		 * 构建响应。
		 * @return 构建好的响应
		 */
		RenderingResponse build();
	}

}
