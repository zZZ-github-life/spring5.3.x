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

package org.springframework.aop.scope;

import org.springframework.aop.RawTargetAccess;

/**
 * 用于作用域对象的 AOP 引入接口。
 *
 * <p>由 {@link ScopedProxyFactoryBean} 创建的对象可以转换为该接口，从而能够访问原始目标对象并以编
 * 程方式移除目标对象。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see ScopedProxyFactoryBean
 */
public interface ScopedObject extends RawTargetAccess {

	/**
	 * 返回此作用域对象代理背后的当前目标对象，即其原始形式（与存储在目标作用域中的形式一致）。
	 * <p>例如，可将原始目标对象传递给无法处理作用域代理对象的持久化提供者。
	 * @return 该作用域对象代理背后的当前目标对象
	 */
	Object getTargetObject();

	/**
	 * 从目标作用域中移除此对象，例如从后备会话中移除。
	 * <p>请注意，此后不应再对该作用域对象进行任何调用（至少在当前线程内，即使用目标作用域中完全相同的
	 * 目标对象时）。
	 */
	void removeFromScope();

}
