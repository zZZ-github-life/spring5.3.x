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

package org.springframework.test.context.web;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Conventions;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * {@code TestExecutionListener}，为 <em>Spring TestContext Framework</em> 加载的 {@link WebApplicationContext WebApplicationContexts} 提供 mock Servlet API 支持。
 * <p>具体而言，{@code ServletTestExecutionListener} 在{@linkplain #prepareTestInstance(TestContext) 测试实例准备}期间和{@linkplain #beforeTestMethod(TestContext) 每个测试方法之前}，通过 Spring Web 的 {@link RequestContextHolder} 设置线程本地状态，并基于 {@code WebApplicationContext} 中存在的 {@link MockServletContext} 创建 {@link MockHttpServletRequest}、{@link MockHttpServletResponse} 和 {@link ServletWebRequest}。此监听器还确保 {@code MockHttpServletResponse} 和 {@code ServletWebRequest} 可以注入到测试实例中，并且在测试完成后，此监听器会{@linkplain #afterTestMethod(TestContext) 清理}线程本地状态。
 * <p>请注意，{@code ServletTestExecutionListener} 默认启用，但如果{@linkplain TestContext#getTestClass() 测试类}未标注 {@link WebAppConfiguration @WebAppConfiguration}，则通常不执行任何操作。有关详情，请参见此类中各个方法的 javadoc。
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 3.2
 */
public class ServletTestExecutionListener extends AbstractTestExecutionListener {

	/**
	 * 用于 {@link TestContext} 属性的属性名，指示 {@code ServletTestExecutionListener} 是否应在 {@link #afterTestMethod(TestContext)} 中{@linkplain RequestContextHolder#resetRequestAttributes() 重置} Spring Web 的 {@code RequestContextHolder}。
	 * <p>允许的值包括 {@link Boolean#TRUE} 和 {@link Boolean#FALSE}。
	 */
	public static final String RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE = Conventions.getQualifiedAttributeName(
			ServletTestExecutionListener.class, "resetRequestContextHolder");

	/**
	 * 用于 {@link TestContext} 属性的属性名，指示 {@code ServletTestExecutionListener} 已填充 Spring Web 的 {@code RequestContextHolder}。
	 * <p>允许的值包括 {@link Boolean#TRUE} 和 {@link Boolean#FALSE}。
	 */
	public static final String POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE = Conventions.getQualifiedAttributeName(
			ServletTestExecutionListener.class, "populatedRequestContextHolder");

	/**
	 * 用于请求属性的属性名，指示 Spring Web 的 {@link RequestContextHolder} 中 {@link RequestAttributes} 内存储的 {@link MockHttpServletRequest} 是由 TestContext 框架创建的。
	 * <p>允许的值包括 {@link Boolean#TRUE} 和 {@link Boolean#FALSE}。
	 * @since 4.2
	 */
	public static final String CREATED_BY_THE_TESTCONTEXT_FRAMEWORK = Conventions.getQualifiedAttributeName(
			ServletTestExecutionListener.class, "createdByTheTestContextFramework");

	/**
	 * 用于 {@link TestContext} 属性的属性名，指示应激活 {@code ServletTestExecutionListener}。当未设置为 {@code true} 时，在{@linkplain TestContext#getTestClass() 测试类}标注了 {@link WebAppConfiguration @WebAppConfiguration} 的情况下会激活。
	 * <p>允许的值包括 {@link Boolean#TRUE} 和 {@link Boolean#FALSE}。
	 * @since 4.3
	 */
	public static final String ACTIVATE_LISTENER = Conventions.getQualifiedAttributeName(
			ServletTestExecutionListener.class, "activateListener");


	private static final Log logger = LogFactory.getLog(ServletTestExecutionListener.class);


	/**
	 * 返回 {@code 1000}。
	 */
	@Override
	public final int getOrder() {
		return 1000;
	}

	/**
	 * 在<em>测试实例准备</em>回调阶段通过 Spring Web 的 {@link RequestContextHolder} 设置线程本地状态，但仅当{@linkplain TestContext#getTestClass() 测试类}标注了 {@link WebAppConfiguration @WebAppConfiguration} 时才执行。
	 * @see TestExecutionListener#prepareTestInstance(TestContext)
	 * @see #setUpRequestContextIfNecessary(TestContext)
	 */
	@Override
	public void prepareTestInstance(TestContext testContext) throws Exception {
		setUpRequestContextIfNecessary(testContext);
	}

	/**
	 * 在每个测试方法之前通过 Spring Web 的 {@link RequestContextHolder} 设置线程本地状态，但仅当{@linkplain TestContext#getTestClass() 测试类}标注了 {@link WebAppConfiguration @WebAppConfiguration} 时才执行。
	 * @see TestExecutionListener#beforeTestMethod(TestContext)
	 * @see #setUpRequestContextIfNecessary(TestContext)
	 */
	@Override
	public void beforeTestMethod(TestContext testContext) throws Exception {
		setUpRequestContextIfNecessary(testContext);
	}

	/**
	 * 如果所提供的 {@code TestContext} 中的 {@link #RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE} 值为 {@link Boolean#TRUE}，则此方法将 (1) 通过{@linkplain RequestContextHolder#resetRequestAttributes() 重置} Spring Web 的 {@code RequestContextHolder} 在每个测试方法之后清理线程本地状态，并 (2) 通过将测试上下文中的 {@link DependencyInjectionTestExecutionListener#REINJECT_DEPENDENCIES_ATTRIBUTE} 设置为 {@code true}，确保为后续测试将新的 mock 对象注入到测试实例中。
	 * <p>无论其值如何，{@link #RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE} 和 {@link #POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE} 随后都将从测试上下文中移除。
	 * @see TestExecutionListener#afterTestMethod(TestContext)
	 */
	@Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		if (Boolean.TRUE.equals(testContext.getAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE))) {
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Resetting RequestContextHolder for test context %s.", testContext));
			}
			RequestContextHolder.resetRequestAttributes();
			testContext.setAttribute(DependencyInjectionTestExecutionListener.REINJECT_DEPENDENCIES_ATTRIBUTE,
				Boolean.TRUE);
		}
		testContext.removeAttribute(POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE);
		testContext.removeAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE);
	}

	private boolean isActivated(TestContext testContext) {
		return (Boolean.TRUE.equals(testContext.getAttribute(ACTIVATE_LISTENER)) ||
				AnnotatedElementUtils.hasAnnotation(testContext.getTestClass(), WebAppConfiguration.class));
	}

	private boolean alreadyPopulatedRequestContextHolder(TestContext testContext) {
		return Boolean.TRUE.equals(testContext.getAttribute(POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE));
	}

	private void setUpRequestContextIfNecessary(TestContext testContext) {
		if (!isActivated(testContext) || alreadyPopulatedRequestContextHolder(testContext)) {
			return;
		}

		ApplicationContext context = testContext.getApplicationContext();

		if (context instanceof WebApplicationContext) {
			WebApplicationContext wac = (WebApplicationContext) context;
			ServletContext servletContext = wac.getServletContext();
			Assert.state(servletContext instanceof MockServletContext, () -> String.format(
						"The WebApplicationContext for test context %s must be configured with a MockServletContext.",
						testContext));

			if (logger.isDebugEnabled()) {
				logger.debug(String.format(
						"Setting up MockHttpServletRequest, MockHttpServletResponse, ServletWebRequest, and RequestContextHolder for test context %s.",
						testContext));
			}

			MockServletContext mockServletContext = (MockServletContext) servletContext;
			MockHttpServletRequest request = new MockHttpServletRequest(mockServletContext);
			request.setAttribute(CREATED_BY_THE_TESTCONTEXT_FRAMEWORK, Boolean.TRUE);
			MockHttpServletResponse response = new MockHttpServletResponse();
			ServletWebRequest servletWebRequest = new ServletWebRequest(request, response);

			RequestContextHolder.setRequestAttributes(servletWebRequest);
			testContext.setAttribute(POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);
			testContext.setAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);

			if (wac instanceof ConfigurableApplicationContext) {
				@SuppressWarnings("resource")
				ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) wac;
				ConfigurableListableBeanFactory bf = configurableApplicationContext.getBeanFactory();
				bf.registerResolvableDependency(MockHttpServletResponse.class, response);
				bf.registerResolvableDependency(ServletWebRequest.class, servletWebRequest);
			}
		}
	}

}
