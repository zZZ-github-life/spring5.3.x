/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.messaging.simp.config;

import org.springframework.lang.Nullable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;

/**
 * 用于自定义 {@link ThreadPoolTaskExecutor} 属性的注册类。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 */
public class TaskExecutorRegistration {

	private final ThreadPoolTaskExecutor taskExecutor;

	@Nullable
	private Integer corePoolSize;

	@Nullable
	private Integer maxPoolSize;

	@Nullable
	private Integer keepAliveSeconds;

	@Nullable
	private Integer queueCapacity;


	/**
	 * 为默认的 {@link ThreadPoolTaskExecutor} 创建一个新的 {@code TaskExecutorRegistration}。
	 */
	public TaskExecutorRegistration() {
		this.taskExecutor = new ThreadPoolTaskExecutor();
		this.taskExecutor.setCorePoolSize(Runtime.getRuntime().availableProcessors() * 2);
		this.taskExecutor.setAllowCoreThreadTimeOut(true);
	}

	/**
	 * 为给定的 {@link ThreadPoolTaskExecutor} 创建一个新的 {@code TaskExecutorRegistration}。
	 * @param taskExecutor the executor to use
	 */
	public TaskExecutorRegistration(ThreadPoolTaskExecutor taskExecutor) {
		Assert.notNull(taskExecutor, "ThreadPoolTaskExecutor must not be null");
		this.taskExecutor = taskExecutor;
	}


	/**
	 * 设置 ThreadPoolExecutor 的核心池大小。
	 * <p><strong>注意：</strong>当配置了无界的 {@link #queueCapacity(int) queueCapacity}
	 * （默认情况）时，核心池大小实际上就是最大池大小。这本质上就是
	 * {@link java.util.concurrent.ThreadPoolExecutor ThreadPoolExecutor} 中
	 * 所述的 "Unbounded queues" 策略。使用该策略时，
	 * {@link #maxPoolSize(int) maxPoolSize} 会被忽略。
	 * <p>默认情况下此项设置为
	 * {@link Runtime#availableProcessors()} 值的两倍。对于任务不会频繁阻塞的应用程序，
	 * 该数值应接近或等于可用 CPU/核心数。
	 */
	public TaskExecutorRegistration corePoolSize(int corePoolSize) {
		this.corePoolSize = corePoolSize;
		return this;
	}

	/**
	 * 设置 ThreadPoolExecutor 的最大池大小。
	 * <p><strong>注意：</strong>当配置了无界的
	 * {@link #queueCapacity(int) queueCapacity}（默认情况）时，最大池大小实际上
	 * 会被忽略。有关更多详细信息，请参阅
	 * {@link java.util.concurrent.ThreadPoolExecutor ThreadPoolExecutor} 中的
	 * "Unbounded queues" 策略。
	 * <p>默认情况下此项设置为 {@code Integer.MAX_VALUE}。
	 */
	public TaskExecutorRegistration maxPoolSize(int maxPoolSize) {
		this.maxPoolSize = maxPoolSize;
		return this;
	}

	/**
	 * 设置线程在被终止之前可以保持空闲的时间限制。
	 * 如果当前池中的线程数超过核心线程数，那么在等待这段时间且未处理任务后，
	 * 多余的线程将被终止。此设置会覆盖构造函数中设置的任何值。
	 * <p>默认情况下此项设置为 60。
	 */
	public TaskExecutorRegistration keepAliveSeconds(int keepAliveSeconds) {
		this.keepAliveSeconds = keepAliveSeconds;
		return this;
	}

	/**
	 * 设置 ThreadPoolExecutor 的队列容量。
	 * <p><strong>注意：</strong>当配置了无界的 {@code queueCapacity}
	 * （默认情况）时，核心池大小实际上就是最大池大小。这本质上就是
	 * {@link java.util.concurrent.ThreadPoolExecutor ThreadPoolExecutor} 中
	 * 所述的 "Unbounded queues" 策略。使用该策略时，
	 * {@link #maxPoolSize(int) maxPoolSize} 会被忽略。
	 * <p>默认情况下此项设置为 {@code Integer.MAX_VALUE}。
	 */
	public TaskExecutorRegistration queueCapacity(int queueCapacity) {
		this.queueCapacity = queueCapacity;
		return this;
	}


	protected ThreadPoolTaskExecutor getTaskExecutor() {
		if (this.corePoolSize != null) {
			this.taskExecutor.setCorePoolSize(this.corePoolSize);
		}
		if (this.maxPoolSize != null) {
			this.taskExecutor.setMaxPoolSize(this.maxPoolSize);
		}
		if (this.keepAliveSeconds != null) {
			this.taskExecutor.setKeepAliveSeconds(this.keepAliveSeconds);
		}
		if (this.queueCapacity != null) {
			this.taskExecutor.setQueueCapacity(this.queueCapacity);
		}
		return this.taskExecutor;
	}

}
