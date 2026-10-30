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
	 * @param logStackTraces 是否记录堆栈跟踪
	 * @since 5.3.22
	 */
	public LoggingCacheErrorHandler(boolean logStackTraces) {
		this(LogFactory.getLog(LoggingCacheErrorHandler.class), logStackTraces);
	}

	/**
	 * 创建一个 {@code LoggingCacheErrorHandler}，使用提供的
	 * {@link Log logger} 和 {@code logStackTraces} 标志。
	 * @param logger 要使用的 logger
	 * @param logStackTraces 是否记录堆栈跟踪
	 */
	public LoggingCacheErrorHandler(Log logger, boolean logStackTraces) {
		Assert.notNull(logger, "'logger' must not be null");
		this.logger = logger;
		this.logStackTraces = logStackTraces;
	}

	/**
	 * 创建一个 {@code LoggingCacheErrorHandler}，使用提供的
	 * {@code loggerName} 和 {@code logStackTraces} 标志。
	 * @param loggerName 要使用的 logger 名称。该名称将通过 Commons Logging 传递给底层 logger 实现，
	 * 并根据 logger 的配置被解释为日志类别。
	 * @param logStackTraces 是否记录堆栈跟踪
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
	 * @return 该 logger
	 * @since 5.3.22
	 */
	protected final Log getLogger() {
		return logger;
	}

	/**
	 * 获取此 {@code LoggingCacheErrorHandler} 的 {@code logStackTraces} 标志。
	 * @return 如果此 {@code LoggingCacheErrorHandler} 记录堆栈跟踪，则返回 {@code true}
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
	 * @param messageSupplier 消息 supplier
	 * @param exception 缓存提供者抛出的异常
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
