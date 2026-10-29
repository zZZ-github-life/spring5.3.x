/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.servlet;

import java.util.Locale;

import org.springframework.lang.Nullable;

/**
 * 由能够按名称解析视图的对象实现的接口。
 *
 * <p>视图状态在应用运行期间不会发生变化，因此实现类可以自由地缓存视图。
 *
 * <p>鼓励实现类支持国际化，即本地化的视图解析。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.web.servlet.view.InternalResourceViewResolver
 * @see org.springframework.web.servlet.view.ContentNegotiatingViewResolver
 * @see org.springframework.web.servlet.view.BeanNameViewResolver
 */
public interface ViewResolver {

	/**
	 * 按名称解析给定的视图。
	 * <p>注意：为了支持 ViewResolver 链式调用，如果其中未定义具有给定名称的视图，
	 * ViewResolver 应返回 {@code null}。
	 * 但这并非强制要求：某些 ViewResolver 总是会尝试构建具有给定名称的 View 对象，
	 * 无法返回 {@code null}（而是在 View 创建失败时抛出异常）。
	 * @param viewName name of the view to resolve
	 * @param locale the Locale in which to resolve the view.
	 * ViewResolvers that support internationalization should respect this.
	 * @return the View object, or {@code null} if not found
	 * (optional, to allow for ViewResolver chaining)
	 * @throws Exception if the view cannot be resolved
	 * (typically in case of problems creating an actual View object)
	 */
	@Nullable
	View resolveViewName(String viewName, Locale locale) throws Exception;

}
