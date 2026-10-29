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
 * 在 Bean 创建当前不被允许（例如在 Bean 工厂的关闭阶段）却仍请求 Bean 的情况下抛出的异常。
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public class BeanCreationNotAllowedException extends BeanCreationException {

	/**
	 * 创建新的 BeanCreationNotAllowedException。
	 * @param beanName the name of the bean requested
	 * @param msg the detail message
	 */
	public BeanCreationNotAllowedException(String beanName, String msg) {
		super(beanName, msg);
	}

}
