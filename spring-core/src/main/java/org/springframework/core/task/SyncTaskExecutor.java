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

package org.springframework.core.task;

import java.io.Serializable;

import org.springframework.util.Assert;

/**
 * 在调用线程中<i>同步</i>执行每个任务的 {@link TaskExecutor} 实现。
 *
 * <p>主要用于测试场景。
 *
 * <p>在调用线程中执行确实具有参与其线程上下文的优势，例如线程上下文类加载器或线程当前的事务关联。
 * 话虽如此，在许多情况下，异步执行会更可取：对于此类场景，请改用异步的 {@code TaskExecutor}。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see SimpleAsyncTaskExecutor
 */
@SuppressWarnings("serial")
public class SyncTaskExecutor implements TaskExecutor, Serializable {

	/**
	 * 通过直接调用给定 {@code task} 的 {@link Runnable#run() run()} 方法，同步执行该任务。
	 * @throws IllegalArgumentException if the given {@code task} is {@code null}
	 */
	@Override
	public void execute(Runnable task) {
		Assert.notNull(task, "Runnable must not be null");
		task.run();
	}

}
