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

package org.springframework.cache.jcache.interceptor;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheMethodDetails;

import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;

/**
 * 基于键进行操作的 {@link JCacheOperation} 基类。
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @param <A> the annotation type
 */
abstract class AbstractJCacheKeyOperation<A extends Annotation> extends AbstractJCacheOperation<A> {

	private final KeyGenerator keyGenerator;

	private final List<CacheParameterDetail> keyParameterDetails;


	/**
	 * 创建一个新实例。
	 * @param methodDetails the {@link CacheMethodDetails} related to the cached method
	 * @param cacheResolver the cache resolver to resolve regular caches
	 * @param keyGenerator the key generator to compute cache keys
	 */
	protected AbstractJCacheKeyOperation(CacheMethodDetails<A> methodDetails,
			CacheResolver cacheResolver, KeyGenerator keyGenerator) {

		super(methodDetails, cacheResolver);
		this.keyGenerator = keyGenerator;
		this.keyParameterDetails = initializeKeyParameterDetails(this.allParameterDetails);
	}


	/**
	 * 返回用于计算缓存键的 {@link KeyGenerator}。
	 */
	public KeyGenerator getKeyGenerator() {
		return this.keyGenerator;
	}

	/**
	 * 返回用于计算键的参数所对应的 {@link CacheInvocationParameter}。
	 * <p>按照规范，如果部分方法参数带有 {@link javax.cache.annotation.CacheKey} 注解，则只有这些参数应
	 * 参与构成键。如果没有任何参数带该注解，则除带 {@link javax.cache.annotation.CacheValue} 注解的参
	 * 数外，其余所有参数都应参与构成键。
	 * <p>方法参数必须与相关方法调用的签名一致
	 * @param values the parameters value for a particular invocation
	 * @return the {@link CacheInvocationParameter} instances for the parameters to be
	 * used to compute the key
	 */
	public CacheInvocationParameter[] getKeyParameters(Object... values) {
		List<CacheInvocationParameter> result = new ArrayList<>();
		for (CacheParameterDetail keyParameterDetail : this.keyParameterDetails) {
			int parameterPosition = keyParameterDetail.getParameterPosition();
			if (parameterPosition >= values.length) {
				throw new IllegalStateException("Values mismatch, key parameter at position "
						+ parameterPosition + " cannot be matched against " + values.length + " value(s)");
			}
			result.add(keyParameterDetail.toCacheInvocationParameter(values[parameterPosition]));
		}
		return result.toArray(new CacheInvocationParameter[0]);
	}


	private static List<CacheParameterDetail> initializeKeyParameterDetails(List<CacheParameterDetail> allParameters) {
		List<CacheParameterDetail> all = new ArrayList<>();
		List<CacheParameterDetail> annotated = new ArrayList<>();
		for (CacheParameterDetail allParameter : allParameters) {
			if (!allParameter.isValue()) {
				all.add(allParameter);
			}
			if (allParameter.isKey()) {
				annotated.add(allParameter);
			}
		}
		return (annotated.isEmpty() ? all : annotated);
	}

}
