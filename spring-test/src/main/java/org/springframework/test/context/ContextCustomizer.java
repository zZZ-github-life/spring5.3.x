/*
 * Copyright 2002-2016 the original author or authors.
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

import org.springframework.context.ConfigurableApplicationContext;

/**
 * 用于自定义由 <em>Spring
 * TestContext Framework</em> 创建和管理的 {@link ConfigurableApplicationContext
 * 应用程序上下文}的策略接口。
 *
 * <p>自定义器由 {@link ContextCustomizerFactory} 实现创建。
 *
 * <p>实现必须正确实现 {@code equals} 和 {@code hashCode} 方法，
 * 因为自定义器构成 {@link MergedContextConfiguration} 的一部分，
 * 而后者用作缓存键。
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 4.3
 * @see ContextCustomizerFactory
 * @see org.springframework.test.context.support.AbstractContextLoader#customizeContext
 */
@FunctionalInterface
public interface ContextCustomizer {

	/**
	 * 在 Bean 定义加载到上下文<em>之后</em>但在
	 * 上下文刷新<em>之前</em>自定义提供的 {@code ConfigurableApplicationContext}。
	 * @param context 要定制的上下文
	 * @param mergedConfig 合并后的上下文配置
	 */
	void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig);

}
