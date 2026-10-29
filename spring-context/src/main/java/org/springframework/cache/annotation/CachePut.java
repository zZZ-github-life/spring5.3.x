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
 * {@link org.springframework.cache.Cache#put(Object, Object) 缓存放入}操作的注解。
 *
 * <p>与 {@link Cacheable @Cacheable} 注解不同，此注解不会导致跳过被通知的方法。相反，它总是会调用
 * 该方法，并在 {@link #condition()} 和 {@link #unless()} 表达式相应匹配时将结果存储到关联的缓存中。
 * 请注意，Java8 的 {@code Optional} 返回类型会被自动处理，如果存在内容，则会将其存储到缓存中。
 *
 * <p>此注解可用作<em>元注解</em>，以创建带属性覆盖的自定义<em>组合注解</em>。
 *
 * @author Costin Leau
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 3.1
 * @see CacheConfig
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface CachePut {

	/**
	 * {@link #cacheNames} 的别名。
	 */
	@AliasFor("cacheNames")
	String[] value() default {};

	/**
	 * 用于缓存放入操作的缓存名称。
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
	 * <li>{@code #result} 用于引用方法调用的结果。对于受支持的包装类型（例如 {@code Optional}），{@code #result} 引用实际对象，而不是包装器</li>
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
	 * 用于使缓存放入操作带条件的 Spring 表达式语言 (SpEL) 表达式。如果条件求值为 {@code true}，则更新
	 * 缓存。
	 *
	 * <p>由于放入操作的性质，此表达式在调用方法之后求值，因此可以引用 {@code result}。
	 *
	 * <p>默认值为 {@code ""}，表示始终缓存方法结果。
	 *
	 * <p>SpEL 表达式在提供以下元数据的专用上下文中求值：
	 * <ul>
	 * <li>{@code #result} 用于引用方法调用的结果。对于受支持的包装类型（例如 {@code Optional}），{@code #result} 引用实际对象，而不是包装器</li>
	 * <li>{@code #root.method}、{@code #root.target} 和 {@code #root.caches}，分别用于引用 {@link java.lang.reflect.Method 方法}、目标对象和受影响的缓存。</li>
	 * <li>还提供方法名（{@code #root.methodName}）和目标类（{@code #root.targetClass}）的快捷方式。
	 * <li>方法参数可以通过索引访问。例如，第二个参数可以通过 {@code #root.args[1]}、{@code #p1} 或 {@code #a1} 访问。如果参数名信息可用，也可以通过参数名访问参数。</li>
	 * </ul>
	 */
	String condition() default "";

	/**
	 * 用于否决缓存放入操作的 Spring 表达式语言 (SpEL) 表达式。如果条件求值为 {@code true}，则否决更新
	 * 缓存。
	 *
	 * <p>默认值为 {@code ""}，表示从不否决缓存。
	 *
	 * <p>SpEL 表达式在提供以下元数据的专用上下文中求值：
	 * <ul>
	 * <li>{@code #result} 用于引用方法调用的结果。对于受支持的包装类型（例如 {@code Optional}），{@code #result} 引用实际对象，而不是包装器</li>
	 * <li>{@code #root.method}、{@code #root.target} 和 {@code #root.caches}，分别用于引用 {@link java.lang.reflect.Method 方法}、目标对象和受影响的缓存。</li>
	 * <li>还提供方法名（{@code #root.methodName}）和目标类（{@code #root.targetClass}）的快捷方式。
	 * <li>方法参数可以通过索引访问。例如，第二个参数可以通过 {@code #root.args[1]}、{@code #p1} 或 {@code #a1} 访问。如果参数名信息可用，也可以通过参数名访问参数。</li>
	 * </ul>
	 * @since 3.2
	 */
	String unless() default "";

}
