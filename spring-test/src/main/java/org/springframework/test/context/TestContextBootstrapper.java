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

package org.springframework.test.context;

import java.util.List;

/**
 * {@code TestContextBootstrapper} 定义用于引导
 * <em>Spring TestContext Framework</em> 的 SPI。
 *
 * <p>{@link TestContextManager} 使用 {@code TestContextBootstrapper}
 * 为当前测试{@linkplain #getTestExecutionListeners 获取 TestExecutionListeners}，
 * 并{@linkplain #buildTestContext 构建其管理的 TestContext}。
 *
 * <h3>配置</h3>
 *
 * <p>可以通过 {@link BootstrapWith @BootstrapWith}（直接或作为元注解）
 * 为测试类（或测试类层次结构）配置自定义引导策略。
 *
 * <p>如果未通过 {@code @BootstrapWith} 显式配置引导器，
 * 将根据是否存在
 * {@link org.springframework.test.context.web.WebAppConfiguration @WebAppConfiguration}，
 * 使用 {@link org.springframework.test.context.support.DefaultTestContextBootstrapper
 * DefaultTestContextBootstrapper} 或
 * {@link org.springframework.test.context.web.WebTestContextBootstrapper
 * WebTestContextBootstrapper}。
 *
 * <h3>实现说明</h3>
 *
 * <p>具体实现必须提供 {@code public} 无参构造函数。
 *
 * <p><strong>警告</strong>：此 SPI 未来可能会更改
 * 以适应新需求。因此，强烈建议实现者
 * <strong>不要</strong>直接实现此接口，而是<em>扩展</em>
 * {@link org.springframework.test.context.support.AbstractTestContextBootstrapper
 * AbstractTestContextBootstrapper} 或其具体子类之一。
 *
 * @author Sam Brannen
 * @since 4.1
 * @see BootstrapWith
 * @see BootstrapContext
 */
public interface TestContextBootstrapper {

	/**
	 * 设置此引导器要使用的 {@link BootstrapContext}。
	 */
	void setBootstrapContext(BootstrapContext bootstrapContext);

	/**
	 * 获取与此引导器关联的 {@link BootstrapContext}。
	 */
	BootstrapContext getBootstrapContext();

	/**
	 * 为此引导器关联的 {@link BootstrapContext}
	 * 构建 {@link TestContext}。
	 * @return 新的 {@link TestContext}，永不为 {@code null}
	 * @since 4.2
	 * @see #buildMergedContextConfiguration()
	 */
	TestContext buildTestContext();

	/**
	 * 为此引导器关联的 {@link BootstrapContext} 中的测试类构建
	 * {@linkplain MergedContextConfiguration 合并上下文配置}。
	 * <p>实现构建合并配置时必须考虑以下内容：
	 * <ul>
	 * <li>通过 {@link ContextHierarchy @ContextHierarchy} 和 {@link ContextConfiguration @ContextConfiguration} 声明的上下文层次结构</li>
	 * <li>通过 {@link ActiveProfiles @ActiveProfiles} 声明的活动 Bean 定义配置</li>
	 * <li>通过 {@link ContextConfiguration#initializers} 声明的{@linkplain org.springframework.context.ApplicationContextInitializer 上下文初始化器}</li>
	 * <li>通过 {@link TestPropertySource @TestPropertySource} 声明的测试属性源</li>
	 * </ul>
	 * <p>有关所需语义的详细信息，请参阅上述注解的 Javadoc。
	 * <p>请注意，{@link #buildTestContext()} 的实现通常在构建 {@code TestContext} 时应
	 * 委托给此方法。
	 * <p>在确定为给定测试类使用哪个 {@link ContextLoader} 时，
	 * 应使用以下算法：
	 * <ol>
	 * <li>如果已通过 {@link ContextConfiguration#loader} 显式声明
	 * {@code ContextLoader} 类，则使用它。</li>
	 * <li>否则，具体实现可以自由确定默认使用哪个
	 * {@code ContextLoader} 类。</li>
	 * </ol>
	 * @return 合并后的上下文配置，永不为 {@code null}
	 * @see #buildTestContext()
	 */
	MergedContextConfiguration buildMergedContextConfiguration();

	/**
	 * 为此引导器关联的 {@link BootstrapContext} 中的测试类获取新实例化的
	 * {@link TestExecutionListener TestExecutionListener} 列表。
	 * <p>如果 {@code BootstrapContext} 中的测试类上未<em>存在</em>
	 * {@link TestExecutionListeners @TestExecutionListeners}，
	 * 则应返回<em>默认</em>监听器。此外，默认
	 * 监听器必须使用
	 * {@link org.springframework.core.annotation.AnnotationAwareOrderComparator
	 * AnnotationAwareOrderComparator} 进行排序。
	 * <p>具体实现可以自由确定默认监听器集合的组成。
	 * 不过，默认情况下，Spring TestContext
	 * Framework 将使用
	 * {@link org.springframework.core.io.support.SpringFactoriesLoader SpringFactoriesLoader}
	 * 机制查找类路径上所有 {@code META-INF/spring.factories} 文件中配置的
	 * 所有 {@code TestExecutionListener} 类名。
	 * <p>必须考虑 {@link TestExecutionListeners @TestExecutionListeners} 的
	 * {@link TestExecutionListeners#inheritListeners() inheritListeners}
	 * 标志。具体而言，如果 {@code inheritListeners}
	 * 标志设置为 {@code true}，则必须将为给定测试类声明的监听器
	 * 追加到超类或封闭类中声明的监听器列表的末尾。
	 * @return {@code TestExecutionListener} 实例列表
	 */
	List<TestExecutionListener> getTestExecutionListeners();

}
