/*
 * Copyright 2002-2015 the original author or authors.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @CacheConfig} 提供了一种在类级别共享通用缓存相关设置的机制。
 *
 * <p>当此注解出现在给定类上时，它会为该类中定义的任何缓存操作提供一组默认设置。
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 4.1
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheConfig {

	/**
	 * 用于对被注解类中定义的缓存操作进行缓存的默认缓存名称。
	 *
	 * <p>如果在操作级别未设置，则使用这些名称代替默认值。
	 *
	 * <p>可用于确定目标缓存（或缓存集合），匹配限定符值或特定 Bean 定义的 Bean 名称。
	 */
	String[] cacheNames() default {};

	/**
	 * 用于该类的默认 {@link org.springframework.cache.interceptor.KeyGenerator} 的 Bean 名称。
	 *
	 * <p>如果在操作级别未设置，则使用此项代替默认值。
	 *
	 * <p>键生成器与自定义键的使用互斥。当为操作定义了此类键时，此键生成器的值将被忽略。
	 */
	String keyGenerator() default "";

	/**
	 * 用于在尚未设置时创建默认 {@link org.springframework.cache.interceptor.CacheResolver} 的自定义
	 * {@link org.springframework.cache.CacheManager} 的 Bean 名称。
	 *
	 * <p>如果在操作级别未设置解析器和缓存管理器，并且未通过 {@link #cacheResolver} 设置缓存解析器，则
	 * 使用此项代替默认值。
	 * @see org.springframework.cache.interceptor.SimpleCacheResolver
	 */
	String cacheManager() default "";

	/**
	 * 要使用的自定义 {@link org.springframework.cache.interceptor.CacheResolver} 的 Bean 名称。
	 *
	 * <p>如果在操作级别未设置解析器和缓存管理器，则使用此项代替默认值。
	 */
	String cacheResolver() default "";

}
