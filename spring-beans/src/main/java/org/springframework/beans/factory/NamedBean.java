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

package org.springframework.beans.factory;

/**
 * {@link BeanNameAware} 的对应物。返回对象的 Bean 名称。
 *
 * <p>可以引入此接口，以避免在用于 Spring IoC 和 Spring AOP 的对象中对 Bean 名称产生脆弱的依赖。
 *
 * @author Rod Johnson
 * @since 2.0
 * @see BeanNameAware
 */
public interface NamedBean {

	/**
	 * 返回此 Bean 在 Spring Bean 工厂中的名称（如果已知）。
	 */
	String getBeanName();

}
