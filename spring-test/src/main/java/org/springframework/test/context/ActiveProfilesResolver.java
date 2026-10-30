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

/**
 * 用于以编程方式解析在加载测试类的
 * {@link org.springframework.context.ApplicationContext ApplicationContext}
 * 时应使用哪些<em>活动 Bean 定义配置</em>的策略接口。
 *
 * <p>可以通过 {@code @ActiveProfiles} 的
 * {@link ActiveProfiles#resolver resolver} 属性注册自定义的 {@code ActiveProfilesResolver}。
 *
 * <p>具体实现必须提供 {@code public} 无参构造函数。
 *
 * @author Sam Brannen
 * @author Michail Nikolaev
 * @since 4.0
 * @see ActiveProfiles
 */
@FunctionalInterface
public interface ActiveProfilesResolver {

	/**
	 * 解析为给定 {@linkplain Class 测试类}加载
	 * {@code ApplicationContext} 时要使用的<em>Bean 定义配置</em>。
	 * @param testClass 应为其解析 profile 的测试类；永不为 {@code null}
	 * @return 加载 {@code ApplicationContext} 时要使用的
	 * Bean 定义 profile 列表；永不为 {@code null}
	 * @see ActiveProfiles#resolver
	 * @see ActiveProfiles#inheritProfiles
	 */
	String[] resolve(Class<?> testClass);

}
