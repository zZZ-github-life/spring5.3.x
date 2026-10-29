/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.beans.factory;

/**
 * 当 Bean 不是工厂，但用户试图通过给定 Bean 名称获取该工厂时抛出的异常。Bean 是否为工厂取决于它是
 * 否实现 FactoryBean 接口。
 *
 * @author Rod Johnson
 * @since 10.03.2003
 * @see org.springframework.beans.factory.FactoryBean
 */
@SuppressWarnings("serial")
public class BeanIsNotAFactoryException extends BeanNotOfRequiredTypeException {

	/**
	 * 创建一个新的 BeanIsNotAFactoryException。
	 * @param name the name of the bean requested
	 * @param actualType the actual type returned, which did not match
	 * the expected type
	 */
	public BeanIsNotAFactoryException(String name, Class<?> actualType) {
		super(name, FactoryBean.class, actualType);
	}

}
