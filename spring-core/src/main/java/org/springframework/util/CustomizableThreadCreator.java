/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.util;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.lang.Nullable;

/**
 * 用于创建新 {@link Thread} 实例的简单可定制辅助类。提供各种 Bean 属性：线程名称前缀、线程优先级等。
 *
 *
 * <p>用作线程工厂（例如
 * {@link org.springframework.scheduling.concurrent.CustomizableThreadFactory}）的基类。
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see org.springframework.scheduling.concurrent.CustomizableThreadFactory
 */
@SuppressWarnings("serial")
public class CustomizableThreadCreator implements Serializable {

	private String threadNamePrefix;

	private int threadPriority = Thread.NORM_PRIORITY;

	private boolean daemon = false;

	@Nullable
	private ThreadGroup threadGroup;

	private final AtomicInteger threadCount = new AtomicInteger();


	/**
	 * 使用默认线程名称前缀创建一个新的 CustomizableThreadCreator。
	 */
	public CustomizableThreadCreator() {
		this.threadNamePrefix = getDefaultThreadNamePrefix();
	}

	/**
	 * 使用给定的线程名称前缀创建一个新的 CustomizableThreadCreator。
	 * @param threadNamePrefix 用于新创建线程名称的前缀
	 */
	public CustomizableThreadCreator(@Nullable String threadNamePrefix) {
		this.threadNamePrefix = (threadNamePrefix != null ? threadNamePrefix : getDefaultThreadNamePrefix());
	}


	/**
	 * 指定新建线程名称所使用的前缀。默认值为 "SimpleAsyncTaskExecutor-"。
	 */
	public void setThreadNamePrefix(@Nullable String threadNamePrefix) {
		this.threadNamePrefix = (threadNamePrefix != null ? threadNamePrefix : getDefaultThreadNamePrefix());
	}

	/**
	 * 返回新建线程名称所使用的线程名称前缀。
	 */
	public String getThreadNamePrefix() {
		return this.threadNamePrefix;
	}

	/**
	 * 设置此工厂创建的线程的优先级。默认值为 5。
	 * @see java.lang.Thread#NORM_PRIORITY
	 */
	public void setThreadPriority(int threadPriority) {
		this.threadPriority = threadPriority;
	}

	/**
	 * 返回此工厂创建的线程的优先级。
	 */
	public int getThreadPriority() {
		return this.threadPriority;
	}

	/**
	 * 设置此工厂是否应创建守护线程，即仅当应用本身运行期间才执行。
	 *
	 * <p>默认值为 "false"：具体工厂通常支持显式取消。因此，如果应用关闭，Runnable 默认会完成其执行。
	 *
	 * <p>如果希望在应用本身关闭时立即关闭仍在积极执行 {@link Runnable} 的线程，请指定 "true"。
	 * @see java.lang.Thread#setDaemon
	 */
	public void setDaemon(boolean daemon) {
		this.daemon = daemon;
	}

	/**
	 * 返回此工厂是否应创建守护线程。
	 */
	public boolean isDaemon() {
		return this.daemon;
	}

	/**
	 * 指定应在其中创建线程的线程组的名称。
	 * @see #setThreadGroup
	 */
	public void setThreadGroupName(String name) {
		this.threadGroup = new ThreadGroup(name);
	}

	/**
	 * 指定应在其中创建线程的线程组。
	 * @see #setThreadGroupName
	 */
	public void setThreadGroup(@Nullable ThreadGroup threadGroup) {
		this.threadGroup = threadGroup;
	}

	/**
	 * 返回应在其中创建线程的线程组（默认组则为 {@code null}）。
	 */
	@Nullable
	public ThreadGroup getThreadGroup() {
		return this.threadGroup;
	}


	/**
	 * 用于创建新 {@link Thread} 的模板方法。
	 *
	 * <p>默认实现为给定的 {@link Runnable} 创建一个新 Thread，并应用适当的线程名称。
	 * @param runnable 要执行的 Runnable
	 * @see #nextThreadName()
	 */
	public Thread createThread(Runnable runnable) {
		Thread thread = new Thread(getThreadGroup(), runnable, nextThreadName());
		thread.setPriority(getThreadPriority());
		thread.setDaemon(isDaemon());
		return thread;
	}

	/**
	 * 返回新建 {@link Thread} 所使用的线程名称。
	 *
	 * <p>默认实现返回指定的线程名称前缀并附加递增的线程计数：例如 "SimpleAsyncTaskExecutor-0"。
	 * @see #getThreadNamePrefix()
	 */
	protected String nextThreadName() {
		return getThreadNamePrefix() + this.threadCount.incrementAndGet();
	}

	/**
	 * 为此工厂构建默认的线程名称前缀。
	 * @return 默认的线程名前缀（永不为 {@code null}）
	 */
	protected String getDefaultThreadNamePrefix() {
		return ClassUtils.getShortName(getClass()) + "-";
	}

}
