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

package org.springframework.web.reactive.result.view;

import java.util.Collection;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.ui.Model;

/**
 * HTML 渲染的公共 API。在 Spring WebFlux 控制器中支持作为返回值。可与 Spring MVC 控制器中 {@code ModelAndView} 作为返回值的使用相类比。
 *
 * <p>控制器通常返回 {@link String} 视图名称，并依赖也可以注入控制器方法的“隐式”模型。或者控制器可以返回模型属性，并依赖基于请求路径选择的默认视图名称。
 *
 * <p>{@code Rendering} 可用于将视图名称与模型属性组合、设置 HTTP 状态或头，以及用于重定向场景的其他更高级选项。
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface Rendering {

	/**
	 * 返回选定的 {@link String} 视图名称或 {@link View} 对象。
	 */
	@Nullable
	Object view();

	/**
	 * 返回要添加到模型的属性。
	 */
	Map<String, Object> modelAttributes();

	/**
	 * 返回要设置到响应的 HTTP 状态。
	 */
	@Nullable
	HttpStatus status();

	/**
	 * 返回要添加到响应的头。
	 */
	HttpHeaders headers();


	/**
	 * 基于给定视图名称创建用于响应渲染的新构建器。
	 * @param name 要解析为 {@link View} 的视图名称
	 * @return 构建器
	 */
	static Builder<?> view(String name) {
		return new DefaultRenderingBuilder(name);
	}

	/**
	 * 创建用于通过 {@link RedirectView} 重定向的新构建器。
	 * @param url 重定向 URL
	 * @return 构建器
	 */
	static RedirectBuilder redirectTo(String url) {
		return new DefaultRenderingBuilder(new RedirectView(url));
	}


	/**
	 * 定义 {@link Rendering} 的构建器。
	 * @param <B> 对构建器类型的自引用
	 */
	interface Builder<B extends Builder<B>> {

		/**
		 * 使用提供的名称添加给定模型属性。
		 * @see Model#addAttribute(String, Object)
		 */
		B modelAttribute(String name, Object value);

		/**
		 * 使用{@link org.springframework.core.Conventions#getVariableName 生成的名称}向模型添加属性。
		 * @see Model#addAttribute(Object)
		 */
		B modelAttribute(Object value);

		/**
		 * 使用{@link org.springframework.core.Conventions#getVariableName 生成的名称}将所有给定属性添加到模型。
		 * @see Model#addAllAttributes(Collection)
		 */
		B modelAttributes(Object... values);

		/**
		 * 将给定属性添加到模型。
		 * @see Model#addAllAttributes(Map)
		 */
		B model(Map<String, ?> map);

		/**
		 * 指定用于响应的状态。
		 */
		B status(HttpStatus status);

		/**
		 * 指定要添加到响应的头。
		 */
		B header(String headerName, String... headerValues);

		/**
		 * 指定要添加到响应的多个头。
		 */
		B headers(HttpHeaders headers);

		/**
		 * 构建 {@link Rendering} 实例。
		 */
		Rendering build();
	}


	/**
	 * 扩展 {@link Builder}，为重定向场景提供额外选项。
	 */
	interface RedirectBuilder extends Builder<RedirectBuilder> {

		/**
		 * 是否应在提供的重定向 URL 前加上应用程序上下文路径（如果有）。
		 * <p>默认设置为 {@code true}。
		 * @see RedirectView#setContextRelative(boolean)
		 */
		RedirectBuilder contextRelative(boolean contextRelative);

		/**
		 * 是否将当前 URL 的查询字符串追加到目标重定向 URL。
		 * <p>默认设置为 {@code false}。
		 * @see RedirectView#setPropagateQuery(boolean)
		 */
		RedirectBuilder propagateQuery(boolean propagate);
	}

}
