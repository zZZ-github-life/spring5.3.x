/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.cache.interceptor;

import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cache.Cache;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 记录错误消息的 {@link CacheErrorHandler} 实现。
 *
 * <p>当应忽略底层缓存错误时可以使用。
 *
 * @author Adam Ostrožlík
 * @author Stephane Nicoll
 * @author Vedran Pavic
 * @author Sam Brannen
 * @since 5.3.16
 */
public class LoggingCacheErrorHandler implements CacheErrorHandler {

	private final Log logger;

	private final boolean logStackTraces;


	/**
	 * 创建一个 {@code LoggingCacheErrorHandler}，使用默认日志类别且不记录堆栈跟踪。
	 *
	 * <p>默认日志类别为
	 * "{@code org.springframework.cache.interceptor.LoggingCacheErrorHandler}"。
	 */
	public LoggingCacheErrorHandler() {
		this(false);
	}

	/**
	 * 创建一个 {@code LoggingCacheErrorHandler}，使用默认日志类别和提供的
	 * {@code logStackTraces} 标志。
	 *
	 * <p>默认日志类别为
	 * "{@code org.springframework.cache.interceptor.LoggingCacheErrorHandler}"。
	 * @param logStackTraces whether to log stack traces
	 * @since 5.3.22
	 */
	public LoggingCacheErrorHandler(boolean logStackTraces) {
		this(LogFactory.getLog(LoggingCacheErrorHandler.class), logStackTraces);
	}

	/**
	 * 创建一个 {@code LoggingCacheErrorHandler}，使用提供的
	 * {@link Log logger} 和 {@code logStackTraces} 标志。
	 * @param logger the logger to use
	 * @param logStackTraces whether to log stack traces
	 */
	public LoggingCacheErrorHandler(Log logger, boolean logStackTraces) {
		Assert.notNull(logger, "'logger' must not be null");
		this.logger = logger;
		this.logStackTraces = logStackTraces;
	}

	/**
	 * 创建一个 {@code LoggingCacheErrorHandler}，使用提供的
	 * {@code loggerName} 和 {@code logStackTraces} 标志。
	 * @param loggerName the name of the logger to use. The name will be passed
	 * to the underlying logger implementation through Commons Logging, getting
	 * interpreted as log category according to the logger's configuration.
	 * @param logStackTraces whether to log stack traces
	 * @since 5.3.24
	 * @see org.apache.commons.logging.LogFactory#getLog(String)
	 * @see java.util.logging.Logger#getLogger(String)
	 */
	public LoggingCacheErrorHandler(String loggerName, boolean logStackTraces) {
		Assert.notNull(loggerName, "'loggerName' must not be null");
		this.logger = LogFactory.getLog(loggerName);
		this.logStackTraces = logStackTraces;
	}


	@Override
	public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
		logCacheError(
				() -> String.format("Cache '%s' failed to get entry with key '%s'", cache.getName(), key),
				exception);
	}

	@Override
	public void handleCachePutError(RuntimeException exception, Cache cache, Object key, @Nullable Object value) {
		logCacheError(
				() -> String.format("Cache '%s' failed to put entry with key '%s'", cache.getName(), key),
				exception);
	}

	@Override
	public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
		logCacheError(
				() -> String.format("Cache '%s' failed to evict entry with key '%s'", cache.getName(), key),
				exception);
	}

	@Override
	public void handleCacheClearError(RuntimeException exception, Cache cache) {
		logCacheError(
				() -> String.format("Cache '%s' failed to clear entries", cache.getName()),
				exception);
	}


	/**
	 * 获取此 {@code LoggingCacheErrorHandler} 的日志记录器。
	 * @return the logger
	 * @since 5.3.22
	 */
	protected final Log getLogger() {
		return logger;
	}

	/**
	 * 获取此 {@code LoggingCacheErrorHandler} 的 {@code logStackTraces} 标志。
	 * @return {@code true} if this {@code LoggingCacheErrorHandler} logs stack traces
	 * @since 5.3.22
	 */
	protected final boolean isLogStackTraces() {
		return this.logStackTraces;
	}

	/**
	 * 在给定的 supplier 中记录缓存错误消息。
	 * <p>如果 {@link #isLogStackTraces()} 为 {@code true}，则给定的
	 * {@code exception} 也会被记录。
	 * <p>默认实现将该消息记录为警告。
	 * @param messageSupplier the message supplier
	 * @param exception the exception thrown by the cache provider
	 * @since 5.3.22
	 * @see #isLogStackTraces()
	 * @see #getLogger()
	 */
	protected void logCacheError(Supplier<String> messageSupplier, RuntimeException exception) {
		if (getLogger().isWarnEnabled()) {
			if (isLogStackTraces()) {
				getLogger().warn(messageSupplier.get(), exception);
			}
			else {
				getLogger().warn(messageSupplier.get());
			}
		}
	}

}
