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
 * 在引用当前正在创建中的 Bean 时抛出的异常。通常发生在构造器自动装配匹配到当前正在构造的 Bean 时。
 *
 * @author Juergen Hoeller
 * @since 1.1
 */
@SuppressWarnings("serial")
public class BeanCurrentlyInCreationException extends BeanCreationException {

	/**
	 * 创建新的 BeanCurrentlyInCreationException，并带有指示循环引用的默认错误消息。
	 * @param beanName the name of the bean requested
	 */
	public BeanCurrentlyInCreationException(String beanName) {
		super(beanName,
				"Requested bean is currently in creation: Is there an unresolvable circular reference?");
	}

	/**
	 * 创建新的 BeanCurrentlyInCreationException。
	 * @param beanName the name of the bean requested
	 * @param msg the detail message
	 */
	public BeanCurrentlyInCreationException(String beanName, String msg) {
		super(beanName, msg);
	}

}
