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

package org.springframework.scheduling.annotation;

import java.util.concurrent.Executor;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.lang.Nullable;

/**
 * 由标注了 @{@link EnableAsync} 的 @
 * {@link org.springframework.context.annotation.Configuration Configuration} 类实现的接口，这些类
 * 希望自定义处理异步方法调用时使用的 {@link Executor} 实例，或用于处理返回类型为 {@code void} 的异
 * 步方法所抛异常的 {@link AsyncUncaughtExceptionHandler} 实例。
 *
 * <p>用法示例请参见 @{@link EnableAsync}。
 *
 * @author Chris Beams
 * @author Stephane Nicoll
 * @since 3.1
 * @see AbstractAsyncConfiguration
 * @see EnableAsync
 */
public interface AsyncConfigurer {

	/**
	 * 处理异步方法调用时要使用的 {@link Executor} 实例。
	 */
	@Nullable
	default Executor getAsyncExecutor() {
		return null;
	}

	/**
	 * 当返回类型为 {@code void} 的异步方法执行期间抛出异常时要使用的
	 * {@link AsyncUncaughtExceptionHandler} 实例。
	 */
	@Nullable
	default AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return null;
	}

}
