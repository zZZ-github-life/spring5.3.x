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

package org.springframework.beans.factory.wiring;

import org.springframework.lang.Nullable;

/**
 * 由能够根据新实例化的 Bean 对象解析 Bean 名称信息的对象实现的策略接口。对此接口上
 * {@link #resolveWiringInfo} 方法的调用将由相关具体切面中的 AspectJ 切点驱动。
 *
 * <p>元数据解析策略可以插拔。一个良好的默认实现是 {@link ClassNameBeanWiringInfoResolver}，它使用
 * 完全限定类名作为 Bean 名称。
 *
 * @author Rod Johnson
 * @since 2.0
 * @see BeanWiringInfo
 * @see ClassNameBeanWiringInfoResolver
 * @see org.springframework.beans.factory.annotation.AnnotationBeanWiringInfoResolver
 */
public interface BeanWiringInfoResolver {

	/**
	 * 解析给定 Bean 实例的 BeanWiringInfo。
	 * @param beanInstance 要为其解析信息的 Bean 实例
	 * @return BeanWiringInfo，如果未找到则为 {@code null}
	 */
	@Nullable
	BeanWiringInfo resolveWiringInfo(Object beanInstance);

}
