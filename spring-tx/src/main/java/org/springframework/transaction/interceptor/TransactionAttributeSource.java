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

package org.springframework.transaction.interceptor;

import java.lang.reflect.Method;

import org.springframework.lang.Nullable;

/**
 * {@link TransactionInterceptor}用于元数据检索的策略接口。
 *
 * <p>实现类知道如何获取事务属性, 无论是从配置、源码级别的元数据属性(如注解)还是其他任何地方。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 15.04.2003
 * @see TransactionInterceptor#setTransactionAttributeSource
 * @see TransactionProxyFactoryBean#setTransactionAttributeSource
 * @see org.springframework.transaction.annotation.AnnotationTransactionAttributeSource
 */
public interface TransactionAttributeSource {

	/**
	 * 确定给定类是否是此{@code TransactionAttributeSource}元数据格式的事务属性候选类。
	 * <p>如果此方法返回{@code false}, 则不会遍历给定类上的方法进行{@link #getTransactionAttribute}内省。
	 * 因此, 返回{@code false}是对不受影响类的优化, 而{@code true}仅意味着需要对该给定类上的每个方法进行完整的内省。
	 * @param targetClass the class to introspect
	 * @return {@code false} if the class is known to have no transaction
	 * attributes at class or method level; {@code true} otherwise. The default
	 * implementation returns {@code true}, leading to regular introspection.
	 * @since 5.2
	 */
	default boolean isCandidateClass(Class<?> targetClass) {
		return true;
	}

	/**
	 * 返回给定方法的事务属性, 如果方法是非事务性的则返回{@code null}。
	 * @param method the method to introspect
	 * @param targetClass the target class (may be {@code null},
	 * in which case the declaring class of the method must be used)
	 * @return the matching transaction attribute, or {@code null} if none found
	 */
	@Nullable
	TransactionAttribute getTransactionAttribute(Method method, @Nullable Class<?> targetClass);

}
