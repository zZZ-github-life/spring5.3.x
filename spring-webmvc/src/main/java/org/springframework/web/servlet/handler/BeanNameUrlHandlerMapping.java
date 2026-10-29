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

package org.springframework.web.servlet.handler;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.web.servlet.HandlerMapping} 接口的实现，
 * 将 URL 映射到名称以斜杠（"/"）开头的 bean，
 * 类似于 Struts 将 URL 映射到 action 名称的方式。
 *
 * <p>这是
 * {@link org.springframework.web.servlet.DispatcherServlet} 默认使用的实现，
 * 与 {@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping} 一起使用。
 * 或者，{@link SimpleUrlHandlerMapping} 允许以声明方式自定义
 * 处理器映射。
 *
 * <p>映射关系是从 URL 到 bean 名称。因此，传入的 URL "/foo" 会映射
 * 到名为 "/foo" 的处理器，或者在多个映射指向单个处理器的情况下
 * 映射到 "/foo /foo2"。
 *
 * <p>支持直接匹配（给定 "/test" -&gt; 已注册的 "/test"）和 "*"
 * 匹配（给定 "/test" -&gt; 已注册的 "/t*"）。请注意，默认情况下，
 * 会在当前 Servlet 映射内进行映射（如果适用）；
 * 详情请参阅 {@link #setAlwaysUseFullPath "alwaysUseFullPath"} 属性。
 * 有关模式选项的详情，请参阅
 * {@link org.springframework.util.AntPathMatcher} 的 javadoc。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see SimpleUrlHandlerMapping
 */
public class BeanNameUrlHandlerMapping extends AbstractDetectingUrlHandlerMapping {

	/**
	 * 检查给定 bean 的名称和别名中是否有以 "/" 开头的 URL。
	 */
	@Override
	protected String[] determineUrlsForHandler(String beanName) {
		List<String> urls = new ArrayList<>();
		if (beanName.startsWith("/")) {
			urls.add(beanName);
		}
		String[] aliases = obtainApplicationContext().getAliases(beanName);
		for (String alias : aliases) {
			if (alias.startsWith("/")) {
				urls.add(alias);
			}
		}
		return StringUtils.toStringArray(urls);
	}

}
