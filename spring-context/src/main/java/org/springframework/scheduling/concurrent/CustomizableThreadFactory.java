/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.scheduling.concurrent;

import java.util.concurrent.ThreadFactory;

import org.springframework.util.CustomizableThreadCreator;

/**
 * {@link java.util.concurrent.ThreadFactory} 接口的实现，允许自定义创建的线程（名称、优先级等）。
 *
 * <p>有关可用配置选项的详细信息，请参见基类
 * {@link org.springframework.util.CustomizableThreadCreator}。
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see #setThreadNamePrefix
 * @see #setThreadPriority
 */
@SuppressWarnings("serial")
public class CustomizableThreadFactory extends CustomizableThreadCreator implements ThreadFactory {

	/**
	 * 使用默认线程名前缀创建一个新的 CustomizableThreadFactory。
	 */
	public CustomizableThreadFactory() {
		super();
	}

	/**
	 * 使用给定的线程名前缀创建一个新的 CustomizableThreadFactory。
	 * @param threadNamePrefix 用于新建线程名称的前缀
	 */
	public CustomizableThreadFactory(String threadNamePrefix) {
		super(threadNamePrefix);
	}


	@Override
	public Thread newThread(Runnable runnable) {
		return createThread(runnable);
	}

}
