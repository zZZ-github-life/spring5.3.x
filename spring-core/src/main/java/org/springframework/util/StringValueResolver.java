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

package org.springframework.util;

import org.springframework.lang.Nullable;

/**
 * 用于解析 String 值的简单策略接口。
 * 由 {@link org.springframework.beans.factory.config.ConfigurableBeanFactory} 使用。
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#resolveAliases
 * @see org.springframework.beans.factory.config.BeanDefinitionVisitor#BeanDefinitionVisitor(StringValueResolver)
 * @see org.springframework.beans.factory.config.PropertyPlaceholderConfigurer
 */
@FunctionalInterface
public interface StringValueResolver {

	/**
	 * 解析给定的 String 值，例如解析占位符。
	 * @param strVal 原始 String 值（永不为 {@code null}）
	 * @return 解析后的 String 值（解析为 null 值时可能为 {@code null}），
	 * 也可能是原始 String 值本身（在没有占位符需要解析
	 * 或忽略无法解析的占位符时）
	 * @throws IllegalArgumentException String 值无法解析时
	 */
	@Nullable
	String resolveStringValue(String strVal);

}
