/*
 * Copyright 2002-2017 the original author or authors.
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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.springframework.context.MessageSource;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.support.JstlUtils;
import org.springframework.web.servlet.support.RequestContext;

/**
 * {@link InternalResourceView} 针对 JSTL 页面（即使用 JSP 标准标签库
 * 的 JSP 页面）的专用化实现。
 *
 * <p>公开 JSTL 特有的请求属性，为 JSTL 的格式化标签和消息标签
 * 指定区域设置和资源束，
 * 使用 Spring 的区域设置和 {@link org.springframework.context.MessageSource}。
 *
 * <p>从 DispatcherServlet 上下文定义的角度来看，
 * 与 {@link InternalResourceViewResolver} 配合使用的典型用法如下：
 *
 * <pre class="code">
 * &lt;bean id="viewResolver" class="org.springframework.web.servlet.view.InternalResourceViewResolver"&gt;
 *   &lt;property name="viewClass" value="org.springframework.web.servlet.view.JstlView"/&gt;
 *   &lt;property name="prefix" value="/WEB-INF/jsp/"/&gt;
 *   &lt;property name="suffix" value=".jsp"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="messageSource" class="org.springframework.context.support.ResourceBundleMessageSource"&gt;
 *   &lt;property name="basename" value="messages"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * 处理器返回的每个视图名称都将被转换为 JSP 资源
 * （例如："myView" &rarr; "/WEB-INF/jsp/myView.jsp"），
 * 使用此视图类以启用显式的 JSTL 支持。
 *
 * <p>所指定的 MessageSource 从类路径中的 "messages.properties" 等文件
 * 加载消息。这些消息将自动作为 JSTL 本地化上下文公开给视图，
 * JSTL fmt 标签（message 等）将使用它。
 * 考虑使用 Spring 的 ReloadableResourceBundleMessageSource 代替
 * 标准的 ResourceBundleMessageSource 以获得更强的能力。
 * 当然，任何其他 Spring 组件都可以共享同一个 MessageSource。
 *
 * <p>将其作为单独的类，主要是为了避免 {@link InternalResourceView} 本身
 * 对 JSTL 产生依赖。直到 J2EE 1.4，JSTL 才成为标准 J2EE 的一部分，
 * 因此我们不能假定类路径上一定存在 JSTL API jar。
 *
 * <p>提示：将 {@link #setExposeContextBeansAsAttributes} 标志设为 "true"，
 * 即可使应用上下文中的所有 Spring bean 可在 JSTL 表达式中访问
 * （例如在 {@code c:out} 值表达式中）。
 * 这也会使所有此类 bean 可在 JSP 2.0 页面的普通 {@code ${...}}
 * 表达式中访问。
 *
 * @author Juergen Hoeller
 * @since 27.02.2003
 * @see org.springframework.web.servlet.support.JstlUtils#exposeLocalizationContext
 * @see InternalResourceViewResolver
 * @see org.springframework.context.support.ResourceBundleMessageSource
 * @see org.springframework.context.support.ReloadableResourceBundleMessageSource
 */
public class JstlView extends InternalResourceView {

	@Nullable
	private MessageSource messageSource;


	/**
	 * 作为 bean 使用的构造函数。
	 * @see #setUrl
	 */
	public JstlView() {
	}

	/**
	 * 使用给定 URL 创建一个新的 JstlView。
	 * @param url 要转发到的 URL
	 */
	public JstlView(String url) {
		super(url);
	}

	/**
	 * 使用给定 URL 创建一个新的 JstlView。
	 * @param url 要转发到的 URL
	 * @param messageSource 要暴露给 JSTL 标签的 MessageSource
	 * （将被包装为感知 JSTL 的 MessageSource，它能识别 JSTL 的
	 * {@code javax.servlet.jsp.jstl.fmt.localizationContext} context-param）
	 * @see JstlUtils#getJstlAwareMessageSource
	 */
	public JstlView(String url, MessageSource messageSource) {
		this(url);
		this.messageSource = messageSource;
	}


	/**
	 * 使用能识别 JSTL 的 MessageSource 包装 MessageSource，
	 * 该包装器了解 JSTL 的 {@code javax.servlet.jsp.jstl.fmt.localizationContext}
	 * 上下文参数。
	 * @see JstlUtils#getJstlAwareMessageSource
	 */
	@Override
	protected void initServletContext(ServletContext servletContext) {
		if (this.messageSource != null) {
			this.messageSource = JstlUtils.getJstlAwareMessageSource(servletContext, this.messageSource);
		}
		super.initServletContext(servletContext);
	}

	/**
	 * 为 Spring 的区域设置和 MessageSource 公开一个 JSTL LocalizationContext。
	 * @see JstlUtils#exposeLocalizationContext
	 */
	@Override
	protected void exposeHelpers(HttpServletRequest request) throws Exception {
		if (this.messageSource != null) {
			JstlUtils.exposeLocalizationContext(request, this.messageSource);
		}
		else {
			JstlUtils.exposeLocalizationContext(new RequestContext(request, getServletContext()));
		}
	}

}
