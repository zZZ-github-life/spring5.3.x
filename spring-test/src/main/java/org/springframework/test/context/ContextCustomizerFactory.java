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

import org.springframework.lang.Nullable;

/**
 * 用于创建 {@link ContextCustomizer ContextCustomizer} 的工厂。
 *
 * <p>工厂在 {@link ContextLoader ContextLoader} 处理完
 * 上下文配置属性之后、但在创建
 * {@link MergedContextConfiguration} 之前被调用。
 *
 * <p>默认情况下，Spring TestContext Framework 将使用
 * {@link org.springframework.core.io.support.SpringFactoriesLoader SpringFactoriesLoader}
 * 机制来加载类路径上所有 {@code META-INF/spring.factories}
 * 文件中配置的工厂。
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 4.3
 */
@FunctionalInterface
public interface ContextCustomizerFactory {

	/**
	 * 创建应在 {@link org.springframework.context.ConfigurableApplicationContext ConfigurableApplicationContext}
	 * 刷新之前用于自定义它的 {@link ContextCustomizer}。
	 * @param testClass the test class
	 * @param configAttributes the list of context configuration attributes for
	 * the test class, ordered <em>bottom-up</em> (i.e., as if we were traversing
	 * up the class hierarchy or enclosing class hierarchy); never {@code null} or empty
	 * @return a {@link ContextCustomizer} or {@code null} if no customizer should
	 * be used
	 */
	@Nullable
	ContextCustomizer createContextCustomizer(Class<?> testClass, List<ContextConfigurationAttributes> configAttributes);

}
