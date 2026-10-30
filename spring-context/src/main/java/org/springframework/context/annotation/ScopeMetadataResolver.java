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

package org.springframework.context.annotation;

import org.springframework.beans.factory.config.BeanDefinition;

/**
 * 用于解析 Bean 定义作用域的策略接口。
 *
 * @author Mark Fisher
 * @since 2.5
 * @see org.springframework.context.annotation.Scope
 */
@FunctionalInterface
public interface ScopeMetadataResolver {

	/**
	 * 解析适用于所提供 Bean {@code definition} 的 {@link ScopeMetadata}。
	 *
	 * <p>实现当然可以使用任何自己喜欢的策略来确定作用域元数据，但一些立即浮现在脑海中的实现可能是使用
	 * 所提供 {@code definition} 的 {@link BeanDefinition#getBeanClassName() 类}上存在的源代码级注解，
	 * 或使用所提供 {@code definition} 的 {@link BeanDefinition#attributeNames()} 中存在的元数据。
	 * @param definition 目标 Bean 定义
	 * @return 相关的作用域元数据；永不为 {@code null}
	 */
	ScopeMetadata resolveScopeMetadata(BeanDefinition definition);

}
