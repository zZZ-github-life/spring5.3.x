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

package org.springframework.expression;

/**
 * 可以将 Bean 解析器注册到求值上下文中，它会在处理 Bean 引用时生效：即 {@code @myBeanName} 和
 * {@code &myBeanName} 表达式。{@code &} 变体语法允许在相关情况下访问工厂 Bean。
 *
 * @author Andy Clement
 * @since 3.0.3
 */
public interface BeanResolver {

	/**
	 * 按给定名称查找 Bean，并返回与之对应的实例。若要尝试访问工厂 Bean，名称需要带有 {@code &} 前缀。
	 * @param context 当前的求值上下文
	 * @param beanName 要查找的 Bean 名称
	 * @return 表示该 Bean 的对象
	 * @throws AccessException 如果解析该 Bean 时出现意外问题
	 */
	Object resolve(EvaluationContext context, String beanName) throws AccessException;

}
