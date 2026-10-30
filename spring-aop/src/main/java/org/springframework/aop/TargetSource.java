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

package org.springframework.aop;

import org.springframework.lang.Nullable;

/**
 * {@code TargetSource} 用于获取 AOP 调用的当前“目标”，如果没有环绕通知选择自行结束拦截器链，则该目
 * 标将通过反射被调用。
 *
 * <p>如果 {@code TargetSource} 是“静态”的，它将始终返回同一个目标，从而允许 AOP 框架进行优化。动态
 * 目标源可以支持池化、热交换等。
 *
 * <p>应用开发者通常不需要直接使用 {@code TargetSources}：这是一个 AOP 框架接口。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public interface TargetSource extends TargetClassAware {

	/**
	 * 返回此 {@link TargetSource} 所返回目标的类型。<p>可以返回 {@code null}，不过
	 * {@code TargetSource} 的某些用法可能只需使用预先确定的目标类即可。
	 * @return 该 {@link TargetSource} 返回的目标的类型
	 */
	@Override
	@Nullable
	Class<?> getTargetClass();

	/**
	 * 对 {@link #getTarget()} 的所有调用都会返回同一个对象吗？<p>在这种情况下，无需调用
	 * {@link #releaseTarget(Object)}，并且 AOP 框架可以缓存 {@link #getTarget()} 的返回值。
	 * @return 如果目标是不可变的，则返回 {@code true}
	 * @see #getTarget
	 */
	boolean isStatic();

	/**
	 * 返回一个目标实例。在 AOP 框架调用 AOP 方法调用的“目标”之前立即调用。
	 * @return 包含连接点的目标对象，如果没有实际的目标实例
	 * 则为 {@code null}
	 * @throws Exception 如果无法解析目标对象
	 */
	@Nullable
	Object getTarget() throws Exception;

	/**
	 * 释放从 {@link #getTarget()} 方法获得的给定目标对象（如果有）。
	 * @param target 通过调用 {@link #getTarget()} 获得的对象
	 * @throws Exception 如果无法释放该对象
	 */
	void releaseTarget(Object target) throws Exception;

}
