/*
 * Copyright 2002-2015 the original author or authors.
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

/**
 * {@code BootstrapContext} 封装了<em>Spring
 * TestContext Framework</em> 进行引导时所处的上下文。
 *
 * @author Sam Brannen
 * @since 4.1
 * @see BootstrapWith
 * @see TestContextBootstrapper
 */
public interface BootstrapContext {

	/**
	 * 获取此引导上下文的 {@linkplain Class 测试类}。
	 * @return the test class (never {@code null})
	 */
	Class<?> getTestClass();

	/**
	 * 获取用于与 {@code ContextCache} 透明
	 * 交互的 {@link CacheAwareContextLoaderDelegate}。
	 * @return the context loader delegate (never {@code null})
	 */
	CacheAwareContextLoaderDelegate getCacheAwareContextLoaderDelegate();

}
