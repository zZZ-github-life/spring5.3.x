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

package org.springframework.web.servlet.view;

import java.util.Locale;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

/**
 * {@link org.springframework.web.servlet.ViewResolver} 的一个简单实现，
 * 它将视图名称解释为当前应用上下文中的 bean 名称，
 * 即通常位于执行中的 {@code DispatcherServlet} 的 XML 文件
 * 或相应的配置类中。
 *
 * <p>注意：此 {@code ViewResolver} 实现了 {@link Ordered} 接口，
 * 以便灵活参与 {@code ViewResolver} 链。例如，可以通过此 {@code ViewResolver}
 * 定义一些特殊视图（将其 "order" 值设为 0），
 * 而所有其余视图则可由 {@link UrlBasedViewResolver}
 * 解析。
 *
 * @author Juergen Hoeller
 * @since 18.06.2003
 * @see UrlBasedViewResolver
 */
public class BeanNameViewResolver extends WebApplicationObjectSupport implements ViewResolver, Ordered {

	private int order = Ordered.LOWEST_PRECEDENCE;  // default: same as non-Ordered


	/**
	 * 指定此 ViewResolver bean 的顺序值。
	 * <p>默认值为 {@code Ordered.LOWEST_PRECEDENCE}，表示无序。
	 * @see org.springframework.core.Ordered#getOrder()
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}


	@Override
	@Nullable
	public View resolveViewName(String viewName, Locale locale) throws BeansException {
		ApplicationContext context = obtainApplicationContext();
		if (!context.containsBean(viewName)) {
			// Allow for ViewResolver chaining...
			return null;
		}
		if (!context.isTypeMatch(viewName, View.class)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Found bean named '" + viewName + "' but it does not implement View");
			}
			// Since we're looking into the general ApplicationContext here,
			// let's accept this as a non-match and allow for chaining as well...
			return null;
		}
		return context.getBean(viewName, View.class);
	}

}
