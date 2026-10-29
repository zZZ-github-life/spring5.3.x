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

package org.springframework.test.context.junit4;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.runner.RunWith;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.event.ApplicationEventsTestExecutionListener;
import org.springframework.test.context.event.EventPublishingTestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.web.ServletTestExecutionListener;

/**
 * 抽象基测试类，在 <strong>JUnit 4</strong> 环境中将 <em>Spring TestContext Framework</em> 与显式的 {@link ApplicationContext} 测试支持集成在一起。
 * <p>具体子类通常应声明类级 {@link ContextConfiguration @ContextConfiguration} 注解，以配置 {@linkplain ApplicationContext 应用程序上下文}的{@linkplain ContextConfiguration#locations() 资源位置}或{@linkplain ContextConfiguration#classes() 组件类}。<em>如果测试不需要加载应用程序上下文，可以选择省略 {@link ContextConfiguration @ContextConfiguration} 声明，并手动配置适当的 {@link org.springframework.test.context.TestExecutionListener TestExecutionListeners}。</em>
 * <p>此类显式注册以下 {@code TestExecutionListener} 实现。如果希望切换到<em>默认</em>监听器集合，请参阅 {@link TestExecutionListeners @TestExecutionListeners} 的类级 Javadoc 以了解详情。
 * <ul>
 * <li>{@link org.springframework.test.context.web.ServletTestExecutionListener}
 * <li>{@link org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener}
 * <li>{@link org.springframework.test.context.event.ApplicationEventsTestExecutionListener}
 * <li>{@link org.springframework.test.context.support.DependencyInjectionTestExecutionListener}
 * <li>{@link org.springframework.test.context.support.DirtiesContextTestExecutionListener}
 * <li>{@link org.springframework.test.context.event.EventPublishingTestExecutionListener}
 * </ul>
 * <p>此类仅作为便于扩展的基类。
 * <ul>
 * <li>如果不希望测试类绑定到 Spring 特定的类层次结构，可以使用 {@link SpringRunner}、{@link ContextConfiguration @ContextConfiguration}、{@link TestExecutionListeners @TestExecutionListeners} 等配置自己的自定义测试类。</li>
 * <li>如果希望扩展此类并使用 {@link SpringRunner} 以外的运行器，可以使用 {@link org.springframework.test.context.junit4.rules.SpringClassRule SpringClassRule} 和 {@link org.springframework.test.context.junit4.rules.SpringMethodRule SpringMethodRule}，并通过 {@link RunWith @RunWith(...)} 指定所选的运行器。</li>
 * </ul>
 * <p><strong>注意：</strong>此类要求 JUnit 4.12 或更高版本。
 *
 * @author Sam Brannen
 * @since 2.5
 * @see ContextConfiguration
 * @see TestContext
 * @see TestContextManager
 * @see TestExecutionListeners
 * @see ServletTestExecutionListener
 * @see DirtiesContextBeforeModesTestExecutionListener
 * @see ApplicationEventsTestExecutionListener
 * @see DependencyInjectionTestExecutionListener
 * @see DirtiesContextTestExecutionListener
 * @see EventPublishingTestExecutionListener
 * @see AbstractTransactionalJUnit4SpringContextTests
 * @see org.springframework.test.context.testng.AbstractTestNGSpringContextTests
 */
@RunWith(SpringRunner.class)
@TestExecutionListeners({ ServletTestExecutionListener.class, DirtiesContextBeforeModesTestExecutionListener.class,
	ApplicationEventsTestExecutionListener.class, DependencyInjectionTestExecutionListener.class,
	DirtiesContextTestExecutionListener.class, EventPublishingTestExecutionListener.class })
public abstract class AbstractJUnit4SpringContextTests implements ApplicationContextAware {

	/**
	 * 可供子类使用的 Logger。
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * 通过 {@link #setApplicationContext(ApplicationContext)} 注入此测试实例的 {@link ApplicationContext}。
	 */
	@Nullable
	protected ApplicationContext applicationContext;


	/**
	 * 设置此测试实例要使用的 {@link ApplicationContext}，通过 {@link ApplicationContextAware} 语义提供。
	 * @param applicationContext the ApplicationContext that this test runs in
	 */
	@Override
	public final void setApplicationContext(final ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

}
