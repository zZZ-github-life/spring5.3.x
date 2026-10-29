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

package org.springframework.transaction.annotation;

import java.lang.reflect.AnnotatedElement;

import org.springframework.lang.Nullable;
import org.springframework.transaction.interceptor.TransactionAttribute;

/**
 * 用于解析已知事务注解类型的策略接口。
 * {@link AnnotationTransactionAttributeSource}将此类解析器委托用于支持特定的注解类型,
 * 如Spring自己的{@link Transactional}、JTA 1.2的{@link javax.transaction.Transactional}
 * 或EJB3的{@link javax.ejb.TransactionAttribute}。
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see AnnotationTransactionAttributeSource
 * @see SpringTransactionAnnotationParser
 * @see Ejb3TransactionAnnotationParser
 * @see JtaTransactionAnnotationParser
 */
public interface TransactionAnnotationParser {

	/**
	 * 确定给定类是否是该{@code TransactionAnnotationParser}注解格式的事务属性候选类。
	 * <p>如果此方法返回{@code false}, 则不会遍历给定类上的方法以进行
	 * {@code #parseTransactionAnnotation}的内省。因此, 返回{@code false}是对不受影响类的优化,
	 * 而{@code true}仅意味着需要对该给定类上的每个方法进行完整的内省。
	 * @param targetClass the class to introspect
	 * @return {@code false} if the class is known to have no transaction
	 * annotations at class or method level; {@code true} otherwise. The default
	 * implementation returns {@code true}, leading to regular introspection.
	 * @since 5.2
	 */
	default boolean isCandidateClass(Class<?> targetClass) {
		return true;
	}

	/**
	 * 基于此解析器理解的注解类型, 解析给定方法或类的事务属性。
	 * <p>这本质上是将已知的事务注解解析为Spring的元数据属性类。
	 * 如果方法/类不是事务性的, 则返回{@code null}。
	 * @param element the annotated method or class
	 * @return the configured transaction attribute, or {@code null} if none found
	 * @see AnnotationTransactionAttributeSource#determineTransactionAttribute
	 */
	@Nullable
	TransactionAttribute parseTransactionAnnotation(AnnotatedElement element);

}
