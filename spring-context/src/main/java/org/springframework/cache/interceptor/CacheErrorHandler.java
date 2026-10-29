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

package org.springframework.cache.interceptor;

import org.springframework.cache.Cache;
import org.springframework.lang.Nullable;

/**
 * 处理缓存相关错误的策略。大多数情况下，缓存提供者抛出的任何异常都应直接抛回给客户端，但在某些情况
 * 下，基础设施可能需要以不同方式处理缓存提供者异常。
 *
 * <p>通常，无法使用给定 id 从缓存中检索对象可以透明地作为缓存未命中处理，而不抛出此类异常。
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
public interface CacheErrorHandler {

	/**
	 * 处理缓存提供者在检索具有指定 {@code key} 的项时抛出的给定运行时异常，可能会将其作为致命异常重新
	 * 抛出。
	 * @param exception the exception thrown by the cache provider
	 * @param cache the cache
	 * @param key the key used to get the item
	 * @see Cache#get(Object)
	 */
	void handleCacheGetError(RuntimeException exception, Cache cache, Object key);

	/**
	 * 处理缓存提供者在更新具有指定 {@code key} 和 {@code value} 的项时抛出的给定运行时异常，可能会将其
	 * 作为致命异常重新抛出。
	 * @param exception the exception thrown by the cache provider
	 * @param cache the cache
	 * @param key the key used to update the item
	 * @param value the value to associate with the key
	 * @see Cache#put(Object, Object)
	 */
	void handleCachePutError(RuntimeException exception, Cache cache, Object key, @Nullable Object value);

	/**
	 * 处理缓存提供者在清除具有指定 {@code key} 的项时抛出的给定运行时异常，可能会将其作为致命异常重新
	 * 抛出。
	 * @param exception the exception thrown by the cache provider
	 * @param cache the cache
	 * @param key the key used to clear the item
	 */
	void handleCacheEvictError(RuntimeException exception, Cache cache, Object key);

	/**
	 * 处理缓存提供者在清除指定的 {@link Cache} 时抛出的给定运行时异常，可能会将其作为致命异常重新抛出。
	 *
	 * @param exception the exception thrown by the cache provider
	 * @param cache the cache to clear
	 */
	void handleCacheClearError(RuntimeException exception, Cache cache);

}
