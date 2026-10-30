/*
 * Copyright 2002-2019 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.Collection;

import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.lang.Nullable;

/**
 * 用于解析已知缓存注解类型的策略接口。{@link AnnotationCacheOperationSource} 委托给此类解析器，以
 * 支持特定的注解类型，例如 Spring 自身的 {@link Cacheable}、{@link CachePut} 和 {@link CacheEvict}。
 *
 *
 * @author Costin Leau
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 3.1
 * @see AnnotationCacheOperationSource
 * @see SpringCacheAnnotationParser
 */
public interface CacheAnnotationParser {

	/**
	 * 确定给定类是否为此 {@code CacheAnnotationParser} 的注解格式的缓存操作候选者。
	 *
	 * <p>如果此方法返回 {@code false}，则不会针对 {@code #parseCacheAnnotations} 内省而遍历给定类上的
	 * 方法。因此，对于不受影响的类，返回 {@code false} 是一种优化；而 {@code true} 仅表示需要针对给定
	 * 类上的每个方法对该类进行完整内省。
	 * @param targetClass 要内省的类
	 * @return 如果已知该类在类或方法级别没有缓存操作注解，则返回 {@code false}；
	 * 否则返回 {@code true}。默认实现返回 {@code true}，从而执行常规内省。
	 * @since 5.2
	 */
	default boolean isCandidateClass(Class<?> targetClass) {
		return true;
	}

	/**
	 * 基于此解析器所理解的注解类型，解析给定类的缓存定义。
	 *
	 * <p>这实质上将一个已知的缓存注解解析为 Spring 的元数据属性类。如果该类不可缓存，则返回
	 * {@code null}。
	 * @param type 带注解的类
	 * @return 配置的缓存操作；如果未找到任何缓存操作，则为 {@code null}
	 * @see AnnotationCacheOperationSource#findCacheOperations(Class)
	 */
	@Nullable
	Collection<CacheOperation> parseCacheAnnotations(Class<?> type);

	/**
	 * 基于此解析器所理解的注解类型，解析给定方法的缓存定义。
	 *
	 * <p>这实质上将一个已知的缓存注解解析为 Spring 的元数据属性类。如果该方法不可缓存，则返回
	 * {@code null}。
	 * @param method 带注解的方法
	 * @return 配置的缓存操作；如果未找到任何缓存操作，则为 {@code null}
	 * @see AnnotationCacheOperationSource#findCacheOperations(Method)
	 */
	@Nullable
	Collection<CacheOperation> parseCacheAnnotations(Method method);

}
