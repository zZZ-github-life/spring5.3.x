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

package org.springframework.cache.interceptor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.support.AopUtils;
import org.springframework.core.MethodClassKey;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * {@link CacheOperation} 的抽象实现，它为方法缓存属性并实现回退策略：1. 具体目标方法；2. 目标类；
 * 3. 声明方法；4. 声明类/接口。
 *
 * <p>如果目标方法没有关联任何缓存属性，则默认使用目标类的缓存属性。与目标方法关联的任何缓存属性都
 * 会完全覆盖类缓存属性。如果在目标类上找不到，则会检查调用该方法时所经由的接口（在 JDK 代理的情况
 * 下）。
 *
 * <p>此实现在属性首次使用后按方法缓存属性。如果确实希望允许动态更改可缓存属性（这极不可能），可以
 * 将缓存设为可配置。
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 3.1
 */
public abstract class AbstractFallbackCacheOperationSource implements CacheOperationSource {

	/**
	 * 保存在缓存中的规范值，用于指示未找到此方法的缓存属性，并且无需再次查找。
	 */
	private static final Collection<CacheOperation> NULL_CACHING_ATTRIBUTE = Collections.emptyList();


	/**
	 * 可供子类使用的 Logger。
	 * <p>由于此基类未标记为 Serializable，因此序列化后将重新创建 logger——前提是具体子类为 Serializable。
	 *
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * CacheOperations 的缓存，以特定目标类上的方法为键。
	 * <p>由于此基类未标记为 Serializable，因此序列化后将重新创建该缓存——前提是具体子类为 Serializable。
	 *
	 */
	private final Map<Object, Collection<CacheOperation>> attributeCache = new ConcurrentHashMap<>(1024);


	/**
	 * 确定此方法调用的缓存属性。
	 * <p>如果未找到方法属性，则默认为类的缓存属性。
	 * @param method 当前调用的方法（永不为 {@code null}）
	 * @param targetClass 本次调用的目标类（可能为 {@code null}）
	 * @return 此方法的 {@link CacheOperation}；如果该方法不可缓存，则为 {@code null}
	 */
	@Override
	@Nullable
	public Collection<CacheOperation> getCacheOperations(Method method, @Nullable Class<?> targetClass) {
		if (method.getDeclaringClass() == Object.class) {
			return null;
		}

		Object cacheKey = getCacheKey(method, targetClass);
		Collection<CacheOperation> cached = this.attributeCache.get(cacheKey);

		if (cached != null) {
			return (cached != NULL_CACHING_ATTRIBUTE ? cached : null);
		}
		else {
			Collection<CacheOperation> cacheOps = computeCacheOperations(method, targetClass);
			if (cacheOps != null) {
				if (logger.isTraceEnabled()) {
					logger.trace("Adding cacheable method '" + method.getName() + "' with attribute: " + cacheOps);
				}
				this.attributeCache.put(cacheKey, cacheOps);
			}
			else {
				this.attributeCache.put(cacheKey, NULL_CACHING_ATTRIBUTE);
			}
			return cacheOps;
		}
	}

	/**
	 * 为给定方法和目标类确定缓存键。
	 * <p>对于重载方法不得产生相同的键。对于同一方法的不同实例必须产生相同的键。
	 * @param method 方法（永不为 {@code null}）
	 * @param targetClass 目标类（可能为 {@code null}）
	 * @return 缓存键（永不为 {@code null}）
	 */
	protected Object getCacheKey(Method method, @Nullable Class<?> targetClass) {
		return new MethodClassKey(method, targetClass);
	}

	@Nullable
	private Collection<CacheOperation> computeCacheOperations(Method method, @Nullable Class<?> targetClass) {
		// Don't allow non-public methods, as configured.
		if (allowPublicMethodsOnly() && !Modifier.isPublic(method.getModifiers())) {
			return null;
		}

		// The method may be on an interface, but we need attributes from the target class.
		// If the target class is null, the method will be unchanged.
		Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);

		// First try is the method in the target class.
		Collection<CacheOperation> opDef = findCacheOperations(specificMethod);
		if (opDef != null) {
			return opDef;
		}

		// Second try is the caching operation on the target class.
		opDef = findCacheOperations(specificMethod.getDeclaringClass());
		if (opDef != null && ClassUtils.isUserLevelMethod(method)) {
			return opDef;
		}

		if (specificMethod != method) {
			// Fallback is to look at the original method.
			opDef = findCacheOperations(method);
			if (opDef != null) {
				return opDef;
			}
			// Last fallback is the class of the original method.
			opDef = findCacheOperations(method.getDeclaringClass());
			if (opDef != null && ClassUtils.isUserLevelMethod(method)) {
				return opDef;
			}
		}

		return null;
	}


	/**
	 * 子类需要实现此方法，以返回给定类的缓存属性（如果有）。
	 * @param clazz 要为其获取属性的类
	 * @return 与此类关联的所有缓存属性；如果没有，则为 {@code null}
	 */
	@Nullable
	protected abstract Collection<CacheOperation> findCacheOperations(Class<?> clazz);

	/**
	 * 子类需要实现此方法，以返回给定方法的缓存属性（如果有）。
	 * @param method 要为其获取属性的方法
	 * @return 与此方法关联的所有缓存属性；如果没有，则为 {@code null}
	 */
	@Nullable
	protected abstract Collection<CacheOperation> findCacheOperations(Method method);

	/**
	 * 是否只允许 public 方法具有缓存语义？
	 * <p>默认实现返回 {@code false}。
	 */
	protected boolean allowPublicMethodsOnly() {
		return false;
	}

}
