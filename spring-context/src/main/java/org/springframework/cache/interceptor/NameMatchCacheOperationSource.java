/*
 * Copyright 2002-2018 the original author or authors.
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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PatternMatchUtils;

/**
 * 简单的 {@link CacheOperationSource} 实现，允许按注册的名称匹配属性。
 *
 * @author Costin Leau
 * @since 3.1
 */
@SuppressWarnings("serial")
public class NameMatchCacheOperationSource implements CacheOperationSource, Serializable {

	/**
	 * 可供子类使用的日志记录器。
	 *
	 * <p>为获得最佳序列化效果而设为静态。
	 */
	protected static final Log logger = LogFactory.getLog(NameMatchCacheOperationSource.class);


	/** 键为方法名；值为 TransactionAttributes。 */
	private Map<String, Collection<CacheOperation>> nameMap = new LinkedHashMap<>();


	/**
	 * 设置名称/属性映射，由方法名（例如 "myMethod"）和 CacheOperation 实例
	 * （或要转换为 CacheOperation 实例的 String）组成。
	 * @see CacheOperation
	 */
	public void setNameMap(Map<String, Collection<CacheOperation>> nameMap) {
		nameMap.forEach(this::addCacheMethod);
	}

	/**
	 * 为可缓存的方法添加属性。
	 *
	 * <p>方法名可以是精确匹配，也可以是 "xxx*"、
	 * "*xxx" 或 "*xxx*" 模式，用于匹配多个方法。
	 * @param methodName 方法的名称
	 * @param ops 与此方法关联的操作
	 */
	public void addCacheMethod(String methodName, Collection<CacheOperation> ops) {
		if (logger.isDebugEnabled()) {
			logger.debug("Adding method [" + methodName + "] with cache operations [" + ops + "]");
		}
		this.nameMap.put(methodName, ops);
	}

	@Override
	@Nullable
	public Collection<CacheOperation> getCacheOperations(Method method, @Nullable Class<?> targetClass) {
		// look for direct name match
		String methodName = method.getName();
		Collection<CacheOperation> ops = this.nameMap.get(methodName);

		if (ops == null) {
			// Look for most specific name match.
			String bestNameMatch = null;
			for (String mappedName : this.nameMap.keySet()) {
				if (isMatch(methodName, mappedName)
						&& (bestNameMatch == null || bestNameMatch.length() <= mappedName.length())) {
					ops = this.nameMap.get(mappedName);
					bestNameMatch = mappedName;
				}
			}
		}

		return ops;
	}

	/**
	 * 返回给定的方法名是否与映射的名称匹配。
	 *
	 * <p>默认实现检查 "xxx*"、"*xxx" 和 "*xxx*" 匹配，
	 * 以及直接相等。可以在子类中重写。
	 * @param methodName 类中方法的名称
	 * @param mappedName 描述符中的名称
	 * @return 如果名称匹配
	 * @see org.springframework.util.PatternMatchUtils#simpleMatch(String, String)
	 */
	protected boolean isMatch(String methodName, String mappedName) {
		return PatternMatchUtils.simpleMatch(mappedName, methodName);
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof NameMatchCacheOperationSource)) {
			return false;
		}
		NameMatchCacheOperationSource otherTas = (NameMatchCacheOperationSource) other;
		return ObjectUtils.nullSafeEquals(this.nameMap, otherTas.nameMap);
	}

	@Override
	public int hashCode() {
		return NameMatchCacheOperationSource.class.hashCode();
	}

	@Override
	public String toString() {
		return getClass().getName() + ": " + this.nameMap;
	}
}
