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

package org.springframework.messaging.simp.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 用于自定义 {@link org.springframework.messaging.MessageChannel} 配置的注册类。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class ChannelRegistration {

	@Nullable
	private TaskExecutorRegistration registration;

	private final List<ChannelInterceptor> interceptors = new ArrayList<>();


	/**
	 * 配置支持此消息通道的线程池。
	 */
	public TaskExecutorRegistration taskExecutor() {
		return taskExecutor(null);
	}

	/**
	 * 使用自定义的 ThreadPoolTaskExecutor 配置支持此消息通道的线程池。
	 * @param taskExecutor 要使用的执行器（或 {@code null} 表示使用默认执行器）
	 */
	public TaskExecutorRegistration taskExecutor(@Nullable ThreadPoolTaskExecutor taskExecutor) {
		if (this.registration == null) {
			this.registration = (taskExecutor != null ? new TaskExecutorRegistration(taskExecutor) :
					new TaskExecutorRegistration());
		}
		return this.registration;
	}

	/**
	 * 为消息通道配置给定的拦截器，将它们添加到通道当前的拦截器列表中。
	 * @since 4.3.12
	 */
	public ChannelRegistration interceptors(ChannelInterceptor... interceptors) {
		this.interceptors.addAll(Arrays.asList(interceptors));
		return this;
	}

	/**
	 * 为消息通道配置拦截器。
	 * @deprecated as of 4.3.12, in favor of {@link #interceptors(ChannelInterceptor...)}
	 */
	@Deprecated
	public ChannelRegistration setInterceptors(@Nullable ChannelInterceptor... interceptors) {
		if (interceptors != null) {
			this.interceptors.addAll(Arrays.asList(interceptors));
		}
		return this;
	}


	protected boolean hasTaskExecutor() {
		return (this.registration != null);
	}

	protected boolean hasInterceptors() {
		return !this.interceptors.isEmpty();
	}

	protected List<ChannelInterceptor> getInterceptors() {
		return this.interceptors;
	}

}
