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

package org.springframework.aop.framework.autoproxy;

import org.springframework.aop.TargetSource;
import org.springframework.lang.Nullable;

/**
 * 实现类可以为特定 Bean 创建特殊的目标源，例如池化目标源。例如，它们可以根据目标类上的属性（如池化
 * 属性）来做出选择。
 *
 * <p>AbstractAutoProxyCreator 可以支持多个 TargetSourceCreator，它们将按顺序应用。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@FunctionalInterface
public interface TargetSourceCreator {

	/**
	 * 为给定 Bean 创建特殊的目标源（如果有）。
	 * @param beanClass the class of the bean to create a TargetSource for
	 * @param beanName the name of the bean
	 * @return a special TargetSource or {@code null} if this TargetSourceCreator isn't
	 * interested in the particular bean
	 */
	@Nullable
	TargetSource getTargetSource(Class<?> beanClass, String beanName);

}
