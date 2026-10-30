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

package org.springframework.core.task;

import java.util.concurrent.Executor;

/**
 * 简单的任务执行器接口，抽象了 {@link Runnable} 的执行。
 *
 * <p>实现可以使用各种不同的执行策略，例如：同步、异步、使用线程池等。
 *
 * <p>等效于 JDK 1.5 的 {@link java.util.concurrent.Executor} 接口；在 Spring 3.0 中现在继承它，以
 * 便客户端可以声明对 Executor 的依赖并接收任何 TaskExecutor 实现。此接口与标准 Executor 接口保持分
 * 离，主要是为了在 Spring 2.x 中向后兼容 JDK 1.4。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see java.util.concurrent.Executor
 */
@FunctionalInterface
public interface TaskExecutor extends Executor {

	/**
	 * 执行给定的 {@code task}。
	 * <p>如果实现使用异步执行策略，该调用可能立即返回；在同步执行的情况下则可能阻塞。
	 * @param task 要执行的 {@code Runnable}（永不为 {@code null}）
	 * @throws TaskRejectedException 如果给定的任务未被接受
	 */
	@Override
	void execute(Runnable task);

}
