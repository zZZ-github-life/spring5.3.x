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

import org.springframework.beans.FatalBeanException;

/**
 * 如果 Bean 尚未完全初始化（例如因为它涉及循环引用），则由 FactoryBean 的 {@code getObject()} 方法
 * 抛出的异常。
 *
 * <p>注意：与 FactoryBean 的循环引用无法像普通 Bean 那样通过急切缓存单例实例来解决。原因是<i>每个
 * </i> FactoryBean 都需要完全初始化后才能返回所创建的 Bean，而只有<i>特定</i>的普通 Bean 需要被初
 * 始化——也就是说，仅当协作 Bean 在初始化时实际调用它们，而不仅仅是保存引用时。
 *
 * @author Juergen Hoeller
 * @since 30.10.2003
 * @see FactoryBean#getObject()
 */
@SuppressWarnings("serial")
public class FactoryBeanNotInitializedException extends FatalBeanException {

	/**
	 * 使用默认消息创建一个新的 FactoryBeanNotInitializedException。
	 */
	public FactoryBeanNotInitializedException() {
		super("FactoryBean is not fully initialized yet");
	}

	/**
	 * 使用给定消息创建一个新的 FactoryBeanNotInitializedException。
	 * @param msg 详细消息
	 */
	public FactoryBeanNotInitializedException(String msg) {
		super(msg);
	}

}
