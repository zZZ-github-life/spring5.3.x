/*
 * Copyright 2002-2021 the original author or authors.
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

package org.aopalliance.intercept;

import java.lang.reflect.AccessibleObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 此接口表示一个通用的运行时连接点（在 AOP 术语中）。
 *
 * <p>运行时连接点是在静态连接点（即程序中的一个位置）上发生的<i>事件</i>。
 * 例如，调用是方法（静态连接点）上的运行时连接点。
 * 可以使用 {@link #getStaticPart()} 方法以通用方式获取给定连接点的静态部分。
 *
 * <p>在拦截框架的上下文中，运行时连接点是对可访问对象（方法、构造器、字段）的访问的具体化，即连接
 * 点的静态部分。它被传递给安装在静态连接点上的拦截器。
 *
 * @author Rod Johnson
 * @see Interceptor
 */
public interface Joinpoint {

	/**
	 * 继续执行链中的下一个拦截器。
	 * <p>此方法的实现和语义取决于实际的连接点类型（参见子接口）。
	 * @return 参见子接口中的 proceed 定义
	 * @throws Throwable 如果连接点抛出异常
	 */
	@Nullable
	Object proceed() throws Throwable;

	/**
	 * 返回持有当前连接点静态部分的对象。
	 * <p>例如，对于调用而言，就是目标对象。
	 * @return 该对象（如果可访问对象是静态的，则可以为 null）
	 */
	@Nullable
	Object getThis();

	/**
	 * 返回此连接点的静态部分。
	 * <p>静态部分是安装了拦截器链的可访问对象。
	 */
	@Nonnull
	AccessibleObject getStaticPart();

}
