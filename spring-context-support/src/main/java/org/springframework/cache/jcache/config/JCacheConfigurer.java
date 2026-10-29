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

package org.springframework.cache.jcache.config;

import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.lang.Nullable;

/**
 * JSR-107 实现的 {@link CachingConfigurer} 扩展。<p>由标注了 {@link org.springframework.cache.annotation.EnableCaching} 并希望或需要显式指定如何为注解驱动的缓存管理解析异常缓存的类实现。<p>一般示例和上下文请参见 {@link org.springframework.cache.annotation.EnableCaching}；详细说明请参见 {@link #exceptionCacheResolver()}。
 * @author Stephane Nicoll
 * @since 4.1
 * @see CachingConfigurer
 * @see org.springframework.cache.annotation.EnableCaching
 */
public interface JCacheConfigurer extends CachingConfigurer {

	/**
	 * 返回用于为注解驱动的缓存管理解析异常缓存的 {@link CacheResolver} bean。
	 * 实现必须显式声明 {@link org.springframework.context.annotation.Bean @Bean}，例如
	 * <pre class="code">
	 * &#064;Configuration
	 * &#064;EnableCaching
	 * public class AppConfig extends JCacheConfigurerSupport {
	 *     &#064;Bean // important!
	 *     &#064;Override
	 *     public CacheResolver exceptionCacheResolver() {
	 *         // configure and return CacheResolver instance
	 *     }
	 *     // ...
	 * }
	 * </pre>
	 * 更完整的示例请参见 {@link org.springframework.cache.annotation.EnableCaching}。
	 */
	@Nullable
	default CacheResolver exceptionCacheResolver() {
		return null;
	}

}
