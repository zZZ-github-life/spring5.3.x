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

package org.springframework.cache.annotation;

import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.lang.Nullable;

/**
 * 由带有 @{@link EnableCaching} 注解的 @
 * {@link org.springframework.context.annotation.Configuration Configuration} 类实现的接口，这些类
 * 希望或需要为注解驱动的缓存管理显式指定缓存如何解析以及键如何生成。
 *
 * <p>一般示例和上下文请参见 @{@link EnableCaching}；详细说明请参见 {@link #cacheManager()}、
 * {@link #cacheResolver()} 和 {@link #keyGenerator()}。
 *
 * @author Chris Beams
 * @author Stephane Nicoll
 * @since 3.1
 * @see EnableCaching
 */
public interface CachingConfigurer {

	/**
	 * 返回用于注解驱动缓存管理的缓存管理器 Bean。将使用此缓存管理器在后台初始化一个默认的
	 * {@link CacheResolver}。如需更细粒度地管理缓存解析，请考虑直接设置 {@link CacheResolver}。
	 *
	 * <p>实现必须显式声明 {@link org.springframework.context.annotation.Bean @Bean}，例如
	 * <pre class="code">
	 * &#064;Configuration
	 * &#064;EnableCaching
	 * public class AppConfig extends CachingConfigurerSupport {
	 *     &#064;Bean // important!
	 *     &#064;Override
	 *     public CacheManager cacheManager() {
	 *         // configure and return CacheManager instance
	 *     }
	 *     // ...
	 * }
	 * </pre>
	 * 更完整的示例请参见 @{@link EnableCaching}。
	 */
	@Nullable
	default CacheManager cacheManager() {
		return null;
	}

	/**
	 * 返回用于为注解驱动的缓存管理解析常规缓存的 {@link CacheResolver} Bean。这是指定要使用的
	 * {@link CacheManager} 的一种替代且更强大的选项。
	 *
	 * <p>如果同时设置了 {@link #cacheManager()} 和 {@code #cacheResolver()}，则忽略缓存管理器。
	 *
	 * <p>实现必须显式声明 {@link org.springframework.context.annotation.Bean @Bean}，例如
	 * <pre class="code">
	 * &#064;Configuration
	 * &#064;EnableCaching
	 * public class AppConfig extends CachingConfigurerSupport {
	 *     &#064;Bean // important!
	 *     &#064;Override
	 *     public CacheResolver cacheResolver() {
	 *         // configure and return CacheResolver instance
	 *     }
	 *     // ...
	 * }
	 * </pre>
	 * 更完整的示例请参见 {@link EnableCaching}。
	 */
	@Nullable
	default CacheResolver cacheResolver() {
		return null;
	}

	/**
	 * 返回用于注解驱动缓存管理的键生成器 Bean。实现必须显式声明
	 * {@link org.springframework.context.annotation.Bean @Bean}，例如
	 * <pre class="code">
	 * &#064;Configuration
	 * &#064;EnableCaching
	 * public class AppConfig extends CachingConfigurerSupport {
	 *     &#064;Bean // important!
	 *     &#064;Override
	 *     public KeyGenerator keyGenerator() {
	 *         // configure and return KeyGenerator instance
	 *     }
	 *     // ...
	 * }
	 * </pre>
	 * 更完整的示例请参见 @{@link EnableCaching}。
	 */
	@Nullable
	default KeyGenerator keyGenerator() {
		return null;
	}

	/**
	 * 返回用于处理缓存相关错误的 {@link CacheErrorHandler}。
	 *
	 * <p>默认情况下，使用 {@link org.springframework.cache.interceptor.SimpleCacheErrorHandler}，并简
	 * 单地将异常抛回给客户端。
	 *
	 * <p>实现必须显式声明 {@link org.springframework.context.annotation.Bean @Bean}，例如
	 * <pre class="code">
	 * &#064;Configuration
	 * &#064;EnableCaching
	 * public class AppConfig extends CachingConfigurerSupport {
	 *     &#064;Bean // important!
	 *     &#064;Override
	 *     public CacheErrorHandler errorHandler() {
	 *         // configure and return CacheErrorHandler instance
	 *     }
	 *     // ...
	 * }
	 * </pre>
	 * 更完整的示例请参见 @{@link EnableCaching}。
	 */
	@Nullable
	default CacheErrorHandler errorHandler() {
		return null;
	}

}
