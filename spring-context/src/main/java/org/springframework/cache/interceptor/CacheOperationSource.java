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

package org.springframework.cache.interceptor;

import java.lang.reflect.Method;
import java.util.Collection;

import org.springframework.lang.Nullable;

/**
 * 由 {@link CacheInterceptor} 使用的接口。实现类知道如何获取缓存操作属性，
 * 无论这些属性来自配置、源码级别的元数据属性，还是来自其他位置。
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 3.1
 */
public interface CacheOperationSource {

	/**
	 * 确定给定类是否为本 {@code CacheOperationSource} 的元数据格式下缓存操作的候选者。
	 * <p>如果此方法返回 {@code false}，则不会遍历给定类上的方法进行 {@link #getCacheOperations} 内省。
	 * 因此，返回 {@code false} 是对不受影响的类的优化，而 {@code true} 仅表示
	 * 需要针对给定类上的每个方法单独进行完整内省。
	 * @param targetClass the class to introspect
	 * @return {@code false} if the class is known to have no cache operation
	 * metadata at class or method level; {@code true} otherwise. The default
	 * implementation returns {@code true}, leading to regular introspection.
	 * @since 5.2
	 */
	default boolean isCandidateClass(Class<?> targetClass) {
		return true;
	}

	/**
	 * 返回此方法的缓存操作集合，
	 * 如果该方法不包含 <em>可缓存</em> 注解，则返回 {@code null}。
	 * @param method the method to introspect
	 * @param targetClass the target class (may be {@code null}, in which case
	 * the declaring class of the method must be used)
	 * @return all cache operations for this method, or {@code null} if none found
	 */
	@Nullable
	Collection<CacheOperation> getCacheOperations(Method method, @Nullable Class<?> targetClass);

}
