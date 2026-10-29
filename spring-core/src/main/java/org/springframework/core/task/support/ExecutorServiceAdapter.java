/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.core.task.support;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.core.task.TaskExecutor;
import org.springframework.util.Assert;

/**
 * 接收 Spring {@link org.springframework.core.task.TaskExecutor} 并为其暴露完整
 * {@code java.util.concurrent.ExecutorService} 的适配器。
 *
 * <p>这主要用于适配通过 {@code java.util.concurrent.ExecutorService} API 通信的客户端组件。它还可
 * 以作为本地 Spring {@code TaskExecutor} 后端与 Java EE 7 环境中通过 JNDI 定位的
 * {@code ManagedExecutorService} 之间的共同基础。
 *
 * <p><b>注意：</b>此 ExecutorService 适配器<em>不</em>支持
 * {@code java.util.concurrent.ExecutorService} API 中的生命周期方法（“shutdown()”等），这类似于
 * Java EE 7 环境中服务器范围的 {@code ManagedExecutorService}。生命周期始终由后端池管理，此适配器
 * 仅作为该目标池的只访问代理。
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see java.util.concurrent.ExecutorService
 */
public class ExecutorServiceAdapter extends AbstractExecutorService {

	private final TaskExecutor taskExecutor;


	/**
	 * 使用给定的目标执行器创建一个新的 ExecutorServiceAdapter。
	 * @param taskExecutor the target executor to delegate to
	 */
	public ExecutorServiceAdapter(TaskExecutor taskExecutor) {
		Assert.notNull(taskExecutor, "TaskExecutor must not be null");
		this.taskExecutor = taskExecutor;
	}


	@Override
	public void execute(Runnable task) {
		this.taskExecutor.execute(task);
	}

	@Override
	public void shutdown() {
		throw new IllegalStateException(
				"Manual shutdown not supported - ExecutorServiceAdapter is dependent on an external lifecycle");
	}

	@Override
	public List<Runnable> shutdownNow() {
		throw new IllegalStateException(
				"Manual shutdown not supported - ExecutorServiceAdapter is dependent on an external lifecycle");
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		throw new IllegalStateException(
				"Manual shutdown not supported - ExecutorServiceAdapter is dependent on an external lifecycle");
	}

	@Override
	public boolean isShutdown() {
		return false;
	}

	@Override
	public boolean isTerminated() {
		return false;
	}

	// @Override on JDK 19
	public void close() {
		// no-op in order to avoid container-triggered shutdown call which would lead to exception logging
	}

}
