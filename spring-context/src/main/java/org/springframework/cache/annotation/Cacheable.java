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
import java.util.concurrent.Callable;

import org.springframework.core.annotation.AliasFor;

/**
 * 指示调用某个方法（或类中的所有方法）的结果可以被缓存的注解。
 *
 * <p>每次调用被通知的方法时，都会应用缓存行为，检查该方法是否已针对给定参数被调用过。合理的默认做
 * 法是直接使用方法参数计算键，但也可以通过 {@link #key} 属性提供 SpEL 表达式，或使用自定义的
 * {@link org.springframework.cache.interceptor.KeyGenerator} 实现替换默认实现（参见
 * {@link #keyGenerator}）。
 *
 * <p>如果在缓存中未找到所计算键对应的值，则将调用目标方法，并将返回的值存储到关联的缓存中。请注意，
 * {@link java.util.Optional} 返回类型会被自动解包。如果 {@code Optional} 值
 * {@linkplain java.util.Optional#isPresent() 存在}，则将其存储到关联的缓存中。如果
 * {@code Optional} 值不存在，则会将 {@code null} 存储到关联的缓存中。
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
public @interface Cacheable {

	/**
	 * {@link #cacheNames} 的别名。
	 */
	@AliasFor("cacheNames")
	String[] value() default {};

	/**
	 * 存储方法调用结果的缓存名称。
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
	 * <p>默认值为 {@code ""}，表示所有方法参数都被视为键，除非已配置自定义的 {@link #keyGenerator}。
	 *
	 * <p>SpEL 表达式在提供以下元数据的专用上下文中求值：
	 * <ul>
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
	 * 用于使方法缓存带条件的 Spring 表达式语言 (SpEL) 表达式。如果条件求值为 {@code true}，则缓存结果。
	 *
	 *
	 * <p>默认值为 {@code ""}，表示始终缓存方法结果。
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
	 * 用于否决方法缓存的 Spring 表达式语言 (SpEL) 表达式。如果条件求值为 {@code true}，则否决缓存结果。
	 *
	 *
	 * <p>与 {@link #condition} 不同，此表达式在调用方法之后求值，因此可以引用 {@code result}。
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

	/**
	 * 如果多个线程尝试为同一个键加载值，则同步底层方法的调用。此同步会导致一些限制：
	 * <ol>
	 * <li>不支持 {@link #unless()}</li>
	 * <li>只能指定一个缓存</li>
	 * <li>不能与其他任何缓存相关操作组合</li>
	 * </ol>
	 * 这实际上只是一个提示，你所使用的实际缓存提供者可能不支持以同步方式处理它。有关实际语义的更多详细
	 * 信息，请查阅提供者文档。
	 * @since 4.3
	 * @see org.springframework.cache.Cache#get(Object, Callable)
	 */
	boolean sync() default false;

}
