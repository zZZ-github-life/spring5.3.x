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

package org.springframework.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * 指示某个方法（或类中的所有方法）触发
 * {@link org.springframework.cache.Cache#evict(Object) 缓存逐出}操作的注解。
 *
 * <p>此注解可用作<em>元注解</em>，以创建带属性覆盖的自定义<em>组合注解</em>。
 *
 * @author Costin Leau
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 3.1
 * @see CacheConfig
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface CacheEvict {

	/**
	 * {@link #cacheNames} 的别名。
	 */
	@AliasFor("cacheNames")
	String[] value() default {};

	/**
	 * 用于缓存逐出操作的缓存名称。
	 *
	 * <p>名称可用于确定目标缓存（或缓存集合），匹配限定符值或特定 Bean 定义的 Bean 名称。
	 * @since 4.2
	 * @see #value
	 * @see CacheConfig#cacheNames
	 */
	@AliasFor("value")
	String[] cacheNames() default {};

	/**
	 * 用于动态计算键的 Spring 表达式语言 (SpEL) 表达式。
	 *
	 * <p>默认值为 {@code ""}，表示所有方法参数都被视为键，除非已设置自定义的 {@link #keyGenerator}。
	 *
	 * <p>SpEL 表达式在提供以下元数据的专用上下文中求值：
	 * <ul>
	 * <li>{@code #result} 用于引用方法调用的结果；只有在 {@link #beforeInvocation()} 为 {@code false} 时才能使用。对于受支持的包装类型（例如 {@code Optional}），{@code #result} 引用实际对象，而不是包装器</li>
	 * <li>{@code #root.method}、{@code #root.target} 和 {@code #root.caches}，分别用于引用 {@link java.lang.reflect.Method 方法}、目标对象和受影响的缓存。</li>
	 * <li>还提供方法名（{@code #root.methodName}）和目标类（{@code #root.targetClass}）的快捷方式。
	 * <li>方法参数可以通过索引访问。例如，第二个参数可以通过 {@code #root.args[1]}、{@code #p1} 或 {@code #a1} 访问。如果参数名信息可用，也可以通过参数名访问参数。</li>
	 * </ul>
	 */
	String key() default "";

	/**
	 * 要使用的自定义 {@link org.springframework.cache.interceptor.KeyGenerator} 的 Bean 名称。
	 *
	 * <p>与 {@link #key} 属性互斥。
	 * @see CacheConfig#keyGenerator
	 */
	String keyGenerator() default "";

	/**
	 * 用于在尚未设置时创建默认 {@link org.springframework.cache.interceptor.CacheResolver} 的自定义
	 * {@link org.springframework.cache.CacheManager} 的 Bean 名称。
	 *
	 * <p>与 {@link #cacheResolver} 属性互斥。
	 * @see org.springframework.cache.interceptor.SimpleCacheResolver
	 * @see CacheConfig#cacheManager
	 */
	String cacheManager() default "";

	/**
	 * 要使用的自定义 {@link org.springframework.cache.interceptor.CacheResolver} 的 Bean 名称。
	 * @see CacheConfig#cacheResolver
	 */
	String cacheResolver() default "";

	/**
	 * 用于使缓存逐出操作带条件的 Spring 表达式语言 (SpEL) 表达式。如果条件求值为 {@code true}，则逐出
	 * 该缓存。
	 *
	 * <p>默认值为 {@code ""}，表示始终执行缓存逐出。
	 *
	 * <p>SpEL 表达式在提供以下元数据的专用上下文中求值：
	 * <ul>
	 * <li>{@code #root.method}、{@code #root.target} 和 {@code #root.caches}，分别用于引用 {@link java.lang.reflect.Method 方法}、目标对象和受影响的缓存。</li>
	 * <li>还提供方法名（{@code #root.methodName}）和目标类（{@code #root.targetClass}）的快捷方式。
	 * <li>方法参数可以通过索引访问。例如，第二个参数可以通过 {@code #root.args[1]}、{@code #p1} 或 {@code #a1} 访问。如果参数名信息可用，也可以通过参数名访问参数。</li>
	 * </ul>
	 */
	String condition() default "";

	/**
	 * 是否移除缓存中的所有条目。
	 *
	 * <p>默认情况下，仅移除关联键下的值。
	 *
	 * <p>请注意，不允许将此参数设置为 {@code true} 并同时指定 {@link #key}。
	 */
	boolean allEntries() default false;

	/**
	 * 逐出是否应在调用方法之前发生。
	 *
	 * <p>将此属性设置为 {@code true} 会使逐出发生，而不受方法结果影响（即无论是否抛出异常）。
	 *
	 * <p>默认值为 {@code false}，表示缓存逐出操作将在被通知方法成功调用<em>之后</em>发生（即仅在调用未
	 * 抛出异常时）。
	 */
	boolean beforeInvocation() default false;

}
