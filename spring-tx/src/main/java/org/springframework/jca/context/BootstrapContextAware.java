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

package org.springframework.jca.context;

import javax.resource.spi.BootstrapContext;

import org.springframework.beans.factory.Aware;

/**
 * 任何希望被通知其运行所在的 BootstrapContext
 * （通常由 {@link ResourceAdapterApplicationContext} 确定）
 * 的对象都应实现此接口。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 2.5
 * @see javax.resource.spi.BootstrapContext
 */
public interface BootstrapContextAware extends Aware {

	/**
	 * 设置此对象运行的 BootstrapContext。
	 * <p>在常规 bean 属性填充之后调用，但在初始化回调（如 InitializingBean 的
	 * {@code afterPropertiesSet} 或自定义 init 方法）之前调用。
	 * 在 ApplicationContextAware 的 {@code setApplicationContext} 之后调用。
	 * @param bootstrapContext 此对象要使用的 BootstrapContext 对象
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext
	 */
	void setBootstrapContext(BootstrapContext bootstrapContext);

}
