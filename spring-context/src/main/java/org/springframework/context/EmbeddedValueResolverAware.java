/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.context;

import org.springframework.beans.factory.Aware;
import org.springframework.util.StringValueResolver;

/**
 * 由任何希望获知用于解析嵌入式定义值的 {@code StringValueResolver} 的对象实现的接口。
 *
 * <p>这是通过 {@code ApplicationContextAware}/{@code BeanFactoryAware} 接口引入完整
 * ConfigurableBeanFactory 依赖的一种替代方案。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 3.0.3
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#resolveEmbeddedValue(String)
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#getBeanExpressionResolver()
 * @see org.springframework.beans.factory.config.EmbeddedValueResolver
 */
public interface EmbeddedValueResolverAware extends Aware {

	/**
	 * 设置用于解析嵌入式定义值的 StringValueResolver。
	 */
	void setEmbeddedValueResolver(StringValueResolver resolver);

}
