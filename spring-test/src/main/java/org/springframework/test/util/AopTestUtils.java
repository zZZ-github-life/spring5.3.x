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

package org.springframework.test.util;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.util.Assert;

/**
 * {@code AopTestUtils} 是一组与 AOP 相关的实用方法，用于单元和集成测试场景。
 *
 * <p>有关 Spring 核心 AOP 工具，请参见 {@link org.springframework.aop.support.AopUtils AopUtils} 和 {@link org.springframework.aop.framework.AopProxyUtils AopProxyUtils}。
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 4.2
 * @see org.springframework.aop.support.AopUtils
 * @see org.springframework.aop.framework.AopProxyUtils
 * @see ReflectionTestUtils
 */
public abstract class AopTestUtils {

	/**
	 * 获取所提供 {@code candidate} 对象的<em>目标</em>对象。
	 * <p>如果所提供的 {@code candidate} 是 Spring {@linkplain AopUtils#isAopProxy 代理}，则将返回该代理的目标；否则，将<em>原样</em>返回 {@code candidate}。
	 * @param <T> the type of the target object
	 * @param candidate the instance to check (potentially a Spring AOP proxy;
	 * never {@code null})
	 * @return the target object or the {@code candidate} (never {@code null})
	 * @throws IllegalStateException if an error occurs while unwrapping a proxy
	 * @see Advised#getTargetSource()
	 * @see #getUltimateTargetObject
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getTargetObject(Object candidate) {
		Assert.notNull(candidate, "Candidate must not be null");
		try {
			if (AopUtils.isAopProxy(candidate) && candidate instanceof Advised) {
				Object target = ((Advised) candidate).getTargetSource().getTarget();
				if (target != null) {
					return (T) target;
				}
			}
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Failed to unwrap proxied object", ex);
		}
		return (T) candidate;
	}

	/**
	 * 获取所提供 {@code candidate} 对象的最终<em>目标</em>对象，不仅解包顶层代理，还解包任意数量的嵌套代理。
	 * <p>如果所提供的 {@code candidate} 是 Spring {@linkplain AopUtils#isAopProxy 代理}，则将返回所有嵌套代理的最终目标；否则，将<em>原样</em>返回 {@code candidate}。
	 * <p>注意：如果顶层代理或某个嵌套代理不是由{@linkplain org.springframework.aop.TargetSource#isStatic() 静态}{@link org.springframework.aop.TargetSource TargetSource} 支持的，则调用此实用方法可能导致不良行为，例如无限递归并导致 {@link StackOverflowError}。
	 * @param <T> the type of the target object
	 * @param candidate the instance to check (potentially a Spring AOP proxy;
	 * never {@code null})
	 * @return the target object or the {@code candidate} (never {@code null})
	 * @throws IllegalStateException if an error occurs while unwrapping a proxy
	 * @see Advised#getTargetSource()
	 * @see org.springframework.aop.TargetSource#isStatic()
	 * @see org.springframework.aop.framework.AopProxyUtils#ultimateTargetClass
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getUltimateTargetObject(Object candidate) {
		Assert.notNull(candidate, "Candidate must not be null");
		try {
			if (AopUtils.isAopProxy(candidate) && candidate instanceof Advised) {
				Object target = ((Advised) candidate).getTargetSource().getTarget();
				if (target != null) {
					return (T) getUltimateTargetObject(target);
				}
			}
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Failed to unwrap proxied object", ex);
		}
		return (T) candidate;
	}

}
