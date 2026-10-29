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

package org.springframework.transaction.interceptor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.support.AopUtils;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.MethodClassKey;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringValueResolver;

/**
 * {@link TransactionAttributeSource}的抽象实现, 缓存方法的属性并实现回退策略:
 * 1. 特定目标方法; 2. 目标类; 3. 声明方法; 4. 声明类/接口。
 *
 * <p>如果目标方法没有关联属性, 则默认使用目标类的事务属性。与目标方法关联的任何事务属性
 * 都会完全覆盖类事务属性。如果在目标类上未找到, 将检查被调用方法所通过的接口(在使用JDK代理的情况下)。
 *
 * <p>此实现在首次使用后按方法缓存属性。如果需要允许动态更改事务属性(这非常少见),
 * 可以使缓存可配置。缓存是必要的, 因为评估回滚规则的开销较大。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 1.1
 */
public abstract class AbstractFallbackTransactionAttributeSource
		implements TransactionAttributeSource, EmbeddedValueResolverAware {

	/**
	 * 缓存中保留的规范值, 表示此方法未找到事务属性, 无需再次查找。
	 */
	@SuppressWarnings("serial")
	private static final TransactionAttribute NULL_TRANSACTION_ATTRIBUTE = new DefaultTransactionAttribute() {
		@Override
		public String toString() {
			return "null";
		}
	};


	/**
	 * 子类可用的日志记录器。
	 * <p>由于此基类未标记为Serializable, 序列化后将重新创建日志记录器 - 前提是具体子类是Serializable的。
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private transient StringValueResolver embeddedValueResolver;

	/**
	 * TransactionAttribute的缓存, 以特定目标类上的方法为键。
	 * <p>由于此基类未标记为Serializable, 序列化后将重新创建缓存 - 前提是具体子类是Serializable的。
	 */
	private final Map<Object, TransactionAttribute> attributeCache = new ConcurrentHashMap<>(1024);


	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.embeddedValueResolver = resolver;
	}


	/**
	 * 确定此方法调用的事务属性。
	 * <p>如果未找到方法属性, 则默认使用类的事务属性。
	 * @param method the method for the current invocation (never {@code null})
	 * @param targetClass the target class for this invocation (may be {@code null})
	 * @return a TransactionAttribute for this method, or {@code null} if the method
	 * is not transactional
	 */
	@Override
	@Nullable
	public TransactionAttribute getTransactionAttribute(Method method, @Nullable Class<?> targetClass) {
		if (method.getDeclaringClass() == Object.class) {
			return null;
		}

		// First, see if we have a cached value.
		Object cacheKey = getCacheKey(method, targetClass);
		TransactionAttribute cached = this.attributeCache.get(cacheKey);
		if (cached != null) {
			// Value will either be canonical value indicating there is no transaction attribute,
			// or an actual transaction attribute.
			if (cached == NULL_TRANSACTION_ATTRIBUTE) {
				return null;
			}
			else {
				return cached;
			}
		}
		else {
			// We need to work it out.
			TransactionAttribute txAttr = computeTransactionAttribute(method, targetClass);
			// Put it in the cache.
			if (txAttr == null) {
				this.attributeCache.put(cacheKey, NULL_TRANSACTION_ATTRIBUTE);
			}
			else {
				String methodIdentification = ClassUtils.getQualifiedMethodName(method, targetClass);
				if (txAttr instanceof DefaultTransactionAttribute) {
					DefaultTransactionAttribute dta = (DefaultTransactionAttribute) txAttr;
					dta.setDescriptor(methodIdentification);
					dta.resolveAttributeStrings(this.embeddedValueResolver);
				}
				if (logger.isTraceEnabled()) {
					logger.trace("Adding transactional method '" + methodIdentification + "' with attribute: " + txAttr);
				}
				this.attributeCache.put(cacheKey, txAttr);
			}
			return txAttr;
		}
	}

	/**
	 * 确定给定方法和目标类的缓存键。
	 * <p>不能为重载方法生成相同的键。必须为同一方法的不同实例生成相同的键。
	 * @param method the method (never {@code null})
	 * @param targetClass the target class (may be {@code null})
	 * @return the cache key (never {@code null})
	 */
	protected Object getCacheKey(Method method, @Nullable Class<?> targetClass) {
		return new MethodClassKey(method, targetClass);
	}

	/**
	 * 与{@link #getTransactionAttribute}签名相同, 但不缓存结果。
	 * {@link #getTransactionAttribute}实际上是此方法的缓存装饰器。
	 * <p>从4.1.8开始, 此方法可以被子类重写。
	 * @since 4.1.8
	 * @see #getTransactionAttribute
	 */
	@Nullable
	protected TransactionAttribute computeTransactionAttribute(Method method, @Nullable Class<?> targetClass) {
		// Don't allow non-public methods, as configured.
		if (allowPublicMethodsOnly() && !Modifier.isPublic(method.getModifiers())) {
			return null;
		}

		// The method may be on an interface, but we need attributes from the target class.
		// If the target class is null, the method will be unchanged.
		Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);

		// First try is the method in the target class.
		TransactionAttribute txAttr = findTransactionAttribute(specificMethod);
		if (txAttr != null) {
			return txAttr;
		}

		// Second try is the transaction attribute on the target class.
		txAttr = findTransactionAttribute(specificMethod.getDeclaringClass());
		if (txAttr != null && ClassUtils.isUserLevelMethod(method)) {
			return txAttr;
		}

		if (specificMethod != method) {
			// Fallback is to look at the original method.
			txAttr = findTransactionAttribute(method);
			if (txAttr != null) {
				return txAttr;
			}
			// Last fallback is the class of the original method.
			txAttr = findTransactionAttribute(method.getDeclaringClass());
			if (txAttr != null && ClassUtils.isUserLevelMethod(method)) {
				return txAttr;
			}
		}

		return null;
	}


	/**
	 * 子类需要实现此方法以返回给定类的事务属性(如果有的话)。
	 * @param clazz the class to retrieve the attribute for
	 * @return all transaction attribute associated with this class, or {@code null} if none
	 */
	@Nullable
	protected abstract TransactionAttribute findTransactionAttribute(Class<?> clazz);

	/**
	 * 子类需要实现此方法以返回给定方法的事务属性(如果有的话)。
	 * @param method the method to retrieve the attribute for
	 * @return all transaction attribute associated with this method, or {@code null} if none
	 */
	@Nullable
	protected abstract TransactionAttribute findTransactionAttribute(Method method);

	/**
	 * 是否只允许公共方法具有事务语义?
	 * <p>默认实现返回{@code false}。
	 */
	protected boolean allowPublicMethodsOnly() {
		return false;
	}

}
