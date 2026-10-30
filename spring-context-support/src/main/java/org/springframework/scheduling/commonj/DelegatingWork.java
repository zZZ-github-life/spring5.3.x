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

package org.springframework.scheduling.commonj;

import commonj.work.Work;

import org.springframework.scheduling.SchedulingAwareRunnable;
import org.springframework.util.Assert;

/**
 * 简单的 Work 适配器，委托给指定的 Runnable。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @deprecated as of 5.1, in favor of EE 7's
 * {@link org.springframework.scheduling.concurrent.DefaultManagedTaskExecutor}
 */
@Deprecated
public class DelegatingWork implements Work {

	private final Runnable delegate;


	/**
	 * 创建一个新的 DelegatingWork。
	 * @param delegate 要委托给的 Runnable 实现
	 * （可以是 SchedulingAwareRunnable 以获得扩展支持）
	 * @see org.springframework.scheduling.SchedulingAwareRunnable
	 * @see #isDaemon()
	 */
	public DelegatingWork(Runnable delegate) {
		Assert.notNull(delegate, "Delegate must not be null");
		this.delegate = delegate;
	}

	/**
	 * 返回被包装的 Runnable 实现。
	 */
	public final Runnable getDelegate() {
		return this.delegate;
	}


	/**
	 * 将执行委托给底层的 Runnable。
	 */
	@Override
	public void run() {
		this.delegate.run();
	}

	/**
	 * 此实现委托给
	 * {@link org.springframework.scheduling.SchedulingAwareRunnable#isLongLived()}，
	 * 如果可用的话。
	 */
	@Override
	public boolean isDaemon() {
		return (this.delegate instanceof SchedulingAwareRunnable &&
				((SchedulingAwareRunnable) this.delegate).isLongLived());
	}

	/**
	 * 此实现为空，因为我们期望 Runnable
	 * 基于某个特定的关闭信号来终止。
	 */
	@Override
	public void release() {
	}

}
