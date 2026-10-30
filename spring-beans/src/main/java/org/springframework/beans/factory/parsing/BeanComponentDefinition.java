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

package org.springframework.beans.factory.parsing;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.lang.Nullable;

/**
 * 基于标准 BeanDefinition 的 ComponentDefinition，暴露给定的 Bean 定义以及该 Bean 的内部 Bean 定义
 * 和 Bean 引用。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public class BeanComponentDefinition extends BeanDefinitionHolder implements ComponentDefinition {

	private BeanDefinition[] innerBeanDefinitions;

	private BeanReference[] beanReferences;


	/**
	 * 为给定的 Bean 创建一个新的 BeanComponentDefinition。
	 * @param beanDefinition 该 BeanDefinition
	 * @param beanName 该 Bean 的名称
	 */
	public BeanComponentDefinition(BeanDefinition beanDefinition, String beanName) {
		this(new BeanDefinitionHolder(beanDefinition, beanName));
	}

	/**
	 * 为给定的 Bean 创建一个新的 BeanComponentDefinition。
	 * @param beanDefinition 该 BeanDefinition
	 * @param beanName 该 Bean 的名称
	 * @param aliases 该 Bean 的别名；若没有则为 {@code null}
	 */
	public BeanComponentDefinition(BeanDefinition beanDefinition, String beanName, @Nullable String[] aliases) {
		this(new BeanDefinitionHolder(beanDefinition, beanName, aliases));
	}

	/**
	 * 为给定的 Bean 创建一个新的 BeanComponentDefinition。
	 * @param beanDefinitionHolder 封装 Bean 定义及 Bean 名称的 BeanDefinitionHolder
	 */
	public BeanComponentDefinition(BeanDefinitionHolder beanDefinitionHolder) {
		super(beanDefinitionHolder);

		List<BeanDefinition> innerBeans = new ArrayList<>();
		List<BeanReference> references = new ArrayList<>();
		PropertyValues propertyValues = beanDefinitionHolder.getBeanDefinition().getPropertyValues();
		for (PropertyValue propertyValue : propertyValues.getPropertyValues()) {
			Object value = propertyValue.getValue();
			if (value instanceof BeanDefinitionHolder) {
				innerBeans.add(((BeanDefinitionHolder) value).getBeanDefinition());
			}
			else if (value instanceof BeanDefinition) {
				innerBeans.add((BeanDefinition) value);
			}
			else if (value instanceof BeanReference) {
				references.add((BeanReference) value);
			}
		}
		this.innerBeanDefinitions = innerBeans.toArray(new BeanDefinition[0]);
		this.beanReferences = references.toArray(new BeanReference[0]);
	}


	@Override
	public String getName() {
		return getBeanName();
	}

	@Override
	public String getDescription() {
		return getShortDescription();
	}

	@Override
	public BeanDefinition[] getBeanDefinitions() {
		return new BeanDefinition[] {getBeanDefinition()};
	}

	@Override
	public BeanDefinition[] getInnerBeanDefinitions() {
		return this.innerBeanDefinitions;
	}

	@Override
	public BeanReference[] getBeanReferences() {
		return this.beanReferences;
	}


	/**
	 * 此实现返回该 ComponentDefinition 的描述。
	 * @see #getDescription()
	 */
	@Override
	public String toString() {
		return getDescription();
	}

	/**
	 * 除了父类的相等性要求之外，此实现还要求另一个对象也是 BeanComponentDefinition 类型。
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof BeanComponentDefinition && super.equals(other)));
	}

}
