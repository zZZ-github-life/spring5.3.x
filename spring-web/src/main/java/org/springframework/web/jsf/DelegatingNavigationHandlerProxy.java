/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.jsf;

import javax.faces.application.NavigationHandler;
import javax.faces.context.FacesContext;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.web.context.WebApplicationContext;

/**
 * JSF NavigationHandler 实现，委托给从 Spring 根 WebApplicationContext 获取的
 * NavigationHandler Bean。
 *
 * <p>在 {@code faces-config.xml} 文件中按如下方式配置此处理器代理：
 *
 * <pre class="code">
 * &lt;application&gt;
 *   ...
 *   &lt;navigation-handler&gt;
 * 	   org.springframework.web.jsf.DelegatingNavigationHandlerProxy
 *   &lt;/navigation-handler&gt;
 *   ...
 * &lt;/application&gt;</pre>
 *
 * 默认情况下，将在 Spring ApplicationContext 中按 Bean 名称
 * "jsfNavigationHandler" 查找 NavigationHandler。最简单的情况下，
 * 这只是一个普通的 Spring Bean 定义，如下所示。不过，可以将 Spring 的全部
 * Bean 配置能力应用于此类 Bean，特别是各种形式的依赖注入。
 *
 * <pre class="code">
 * &lt;bean name="jsfNavigationHandler" class="mypackage.MyNavigationHandler"&gt;
 *   &lt;property name="myProperty" ref="myOtherBean"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * 目标 NavigationHandler Bean 通常会扩展标准的 JSF NavigationHandler 类。
 * 不过请注意，在这种场景下<i>不</i>支持装饰原始 NavigationHandler
 * （JSF 提供程序的默认处理器），因为我们无法以标准 JSF 方式
 * （即作为构造函数参数）注入原始处理器。
 *
 * <p>如需<b>装饰原始 NavigationHandler</b>，请确保目标 Bean 扩展 Spring 的
 * <b>DecoratingNavigationHandler</b> 类。这样就可以将原始处理器作为方法参数传入，
 * 此代理会自动识别。请注意，DecoratingNavigationHandler 子类同样可以作为标准的
 * JSF NavigationHandler 正常工作！
 *
 * <p>可以对此代理进行子类化，以更改用于查找导航处理器的 Bean 名称、
 * 更改获取目标处理器的策略，或更改访问 ApplicationContext 的策略
 * （通常通过
 * {@link FacesContextUtils#getWebApplicationContext(FacesContext)} 获取）。
 *
 * @author Juergen Hoeller
 * @author Colin Sampaleanu
 * @since 1.2.7
 * @see DecoratingNavigationHandler
 */
public class DelegatingNavigationHandlerProxy extends NavigationHandler {

	/**
	 * Spring 应用上下文中目标 Bean 的默认名称：
	 * "jsfNavigationHandler"。
	 */
	public static final String DEFAULT_TARGET_BEAN_NAME = "jsfNavigationHandler";

	@Nullable
	private NavigationHandler originalNavigationHandler;


	/**
	 * 创建一个新的 DelegatingNavigationHandlerProxy。
	 */
	public DelegatingNavigationHandlerProxy() {
	}

	/**
	 * 创建一个新的 DelegatingNavigationHandlerProxy。
	 * @param originalNavigationHandler 原始的 NavigationHandler
	 */
	public DelegatingNavigationHandlerProxy(NavigationHandler originalNavigationHandler) {
		this.originalNavigationHandler = originalNavigationHandler;
	}


	/**
	 * 通过委托给 Spring 应用上下文中的目标 Bean，处理由指定参数隐含的导航请求。
	 * <p>目标 Bean 需要扩展 JSF NavigationHandler 类。
	 * 如果它扩展了 Spring 的 DecoratingNavigationHandler，则将使用以原始
	 * NavigationHandler 为参数的重载 {@code handleNavigation} 方法。
	 * 否则，将调用标准的 {@code handleNavigation} 方法。
	 */
	@Override
	public void handleNavigation(FacesContext facesContext, String fromAction, String outcome) {
		NavigationHandler handler = getDelegate(facesContext);
		if (handler instanceof DecoratingNavigationHandler) {
			((DecoratingNavigationHandler) handler).handleNavigation(
					facesContext, fromAction, outcome, this.originalNavigationHandler);
		}
		else {
			handler.handleNavigation(facesContext, fromAction, outcome);
		}
	}

	/**
	 * 返回要委托的目标 NavigationHandler。
	 * <p>默认情况下，每次调用都会从 Spring 根 WebApplicationContext 中获取
	 * 名称为 "jsfNavigationHandler" 的 Bean。
	 * @param facesContext 当前的 JSF 上下文
	 * @return 要委托的目标 NavigationHandler
	 * @see #getTargetBeanName
	 * @see #getBeanFactory
	 */
	protected NavigationHandler getDelegate(FacesContext facesContext) {
		String targetBeanName = getTargetBeanName(facesContext);
		return getBeanFactory(facesContext).getBean(targetBeanName, NavigationHandler.class);
	}

	/**
	 * 返回 BeanFactory 中目标 NavigationHandler Bean 的名称。
	 * 默认为 "jsfNavigationHandler"。
	 * @param facesContext 当前的 JSF 上下文
	 * @return 目标 Bean 的名称
	 */
	protected String getTargetBeanName(FacesContext facesContext) {
		return DEFAULT_TARGET_BEAN_NAME;
	}

	/**
	 * 获取用于委托 Bean 名称解析的 Spring BeanFactory。
	 * <p>默认实现委托给 {@code getWebApplicationContext}。
	 * 可以重写此方法以提供一个任意的 BeanFactory 引用用于解析；
	 * 通常这将是一个完整的 Spring ApplicationContext。
	 * @param facesContext 当前的 JSF 上下文
	 * @return Spring BeanFactory（永不为 {@code null}）
	 * @see #getWebApplicationContext
	 */
	protected BeanFactory getBeanFactory(FacesContext facesContext) {
		return getWebApplicationContext(facesContext);
	}

	/**
	 * 获取用于委托 Bean 名称解析的 Web 应用上下文。
	 * <p>默认实现委托给 FacesContextUtils。
	 * @param facesContext 当前的 JSF 上下文
	 * @return Spring Web 应用上下文（永不为 {@code null}）
	 * @see FacesContextUtils#getRequiredWebApplicationContext
	 */
	protected WebApplicationContext getWebApplicationContext(FacesContext facesContext) {
		return FacesContextUtils.getRequiredWebApplicationContext(facesContext);
	}

}
