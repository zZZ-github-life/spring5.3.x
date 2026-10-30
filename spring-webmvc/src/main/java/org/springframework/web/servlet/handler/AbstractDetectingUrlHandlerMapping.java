/*
 * Copyright 2002-2021 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.util.ObjectUtils;

/**
 * {@link org.springframework.web.servlet.HandlerMapping} 接口的抽象实现，
 * 通过内省应用程序上下文中所有已定义的 bean，
 * 检测处理器 bean 的 URL 映射。
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see #determineUrlsForHandler
 */
public abstract class AbstractDetectingUrlHandlerMapping extends AbstractUrlHandlerMapping {

	private boolean detectHandlersInAncestorContexts = false;


	/**
	 * 设置是否在祖先 ApplicationContext 中检测处理器 bean。
	 * <p>默认值为 "false"：只检测当前 ApplicationContext 中的处理器 bean，
	 * 即仅在此 HandlerMapping 本身所定义的上下文中检测
	 * （通常是当前 DispatcherServlet 的上下文）。
	 * <p>开启此标志还可以检测祖先上下文
	 * （通常是 Spring 根 WebApplicationContext）中的处理器 bean。
	 */
	public void setDetectHandlersInAncestorContexts(boolean detectHandlersInAncestorContexts) {
		this.detectHandlersInAncestorContexts = detectHandlersInAncestorContexts;
	}


	/**
	 * 除父类的初始化之外，还会调用 {@link #detectHandlers()} 方法。
	 */
	@Override
	public void initApplicationContext() throws ApplicationContextException {
		super.initApplicationContext();
		detectHandlers();
	}

	/**
	 * 注册在当前 ApplicationContext 中找到的所有处理器。
	 * <p>处理器的实际 URL 确定工作由具体的
	 * {@link #determineUrlsForHandler(String)} 实现负责。对于
	 * 无法确定此类 URL 的 bean，根本不会被视为处理器。
	 * @throws org.springframework.beans.BeansException 如果无法注册该处理器
	 * @see #determineUrlsForHandler(String)
	 */
	protected void detectHandlers() throws BeansException {
		ApplicationContext applicationContext = obtainApplicationContext();
		String[] beanNames = (this.detectHandlersInAncestorContexts ?
				BeanFactoryUtils.beanNamesForTypeIncludingAncestors(applicationContext, Object.class) :
				applicationContext.getBeanNamesForType(Object.class));

		// Take any bean name that we can determine URLs for.
		for (String beanName : beanNames) {
			String[] urls = determineUrlsForHandler(beanName);
			if (!ObjectUtils.isEmpty(urls)) {
				// URL paths found: Let's consider it a handler.
				registerHandler(urls, beanName);
			}
		}

		if (mappingsLogger.isDebugEnabled()) {
			mappingsLogger.debug(formatMappingName() + " " + getHandlerMap());
		}
		else if ((logger.isDebugEnabled() && !getHandlerMap().isEmpty()) || logger.isTraceEnabled()) {
			logger.debug("Detected " + getHandlerMap().size() + " mappings in " + formatMappingName());
		}
	}


	/**
	 * 确定给定处理器 bean 的 URL。
	 * @param beanName 候选 Bean 的名称
	 * @return 为该 Bean 确定的 URL；如果没有，则为空数组
	 */
	protected abstract String[] determineUrlsForHandler(String beanName);

}
