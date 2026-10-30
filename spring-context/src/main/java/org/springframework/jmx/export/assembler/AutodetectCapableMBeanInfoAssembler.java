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

package org.springframework.jmx.export.assembler;

/**
 * 扩展 {@code MBeanInfoAssembler} 以添加自动检测逻辑。{@code MBeanExporter} 为这些接口的实现提供了
 * 在注册过程中包含额外 Bean 的机会。
 *
 * <p>决定包含哪些 Bean 的具体机制留给实现类。
 *
 * @author Rob Harrop
 * @since 1.2
 * @see org.springframework.jmx.export.MBeanExporter
 */
public interface AutodetectCapableMBeanInfoAssembler extends MBeanInfoAssembler {

	/**
	 * 指示某个特定 Bean 是否应被包含在注册过程中（如果它未在 {@code MBeanExporter} 的 {@code beans} 映
	 * 射中指定）。
	 * @param beanClass Bean 的类（可能是代理类）
	 * @param beanName Bean 在 Bean 工厂中的名称
	 */
	boolean includeBean(Class<?> beanClass, String beanName);

}
