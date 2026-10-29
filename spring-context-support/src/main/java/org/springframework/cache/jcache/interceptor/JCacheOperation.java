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

package org.springframework.cache.jcache.interceptor;

import java.lang.annotation.Annotation;

import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheMethodDetails;

import org.springframework.cache.interceptor.BasicOperation;
import org.springframework.cache.interceptor.CacheResolver;

/**
 * 通过接口契约对 JSR-107 缓存操作的基础进行建模。
 *
 * <p>缓存操作可以静态缓存，因为它不包含特定缓存调用的任何运行时操作。
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @param <A> the type of the JSR-107 annotation
 */
public interface JCacheOperation<A extends Annotation> extends BasicOperation, CacheMethodDetails<A> {

	/**
	 * 返回用于解析此操作所使用的缓存的 {@link CacheResolver} 实例。
	 */
	CacheResolver getCacheResolver();

	/**
	 * 基于指定的方法参数返回 {@link CacheInvocationParameter} 实例。
	 * <p>方法参数必须匹配相关方法调用的签名
	 * @param values the parameters value for a particular invocation
	 */
	CacheInvocationParameter[] getAllParameters(Object... values);

}
