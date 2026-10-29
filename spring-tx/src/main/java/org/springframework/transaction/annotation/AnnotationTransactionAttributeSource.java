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

import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.lang.Nullable;
import org.springframework.transaction.interceptor.AbstractFallbackTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link org.springframework.transaction.interceptor.TransactionAttributeSource}接口的实现,
 * 用于处理JDK 1.5+注解格式的事务元数据。
 *
 * <p>该类读取Spring的JDK 1.5+ {@link Transactional}注解, 并将对应的事务属性暴露给
 * Spring的事务基础设施。同时支持JTA 1.2的{@link javax.transaction.Transactional}和
 * EJB3的{@link javax.ejb.TransactionAttribute}注解(如果存在)。
 * 该类也可作为自定义TransactionAttributeSource的基类,
 * 或通过{@link TransactionAnnotationParser}策略进行定制。
 *
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @since 1.2
 * @see Transactional
 * @see TransactionAnnotationParser
 * @see SpringTransactionAnnotationParser
 * @see Ejb3TransactionAnnotationParser
 * @see org.springframework.transaction.interceptor.TransactionInterceptor#setTransactionAttributeSource
 * @see org.springframework.transaction.interceptor.TransactionProxyFactoryBean#setTransactionAttributeSource
 */
@SuppressWarnings("serial")
public class AnnotationTransactionAttributeSource extends AbstractFallbackTransactionAttributeSource
		implements Serializable {

	private static final boolean jta12Present;

	private static final boolean ejb3Present;

	static {
		ClassLoader classLoader = AnnotationTransactionAttributeSource.class.getClassLoader();
		jta12Present = ClassUtils.isPresent("javax.transaction.Transactional", classLoader);
		ejb3Present = ClassUtils.isPresent("javax.ejb.TransactionAttribute", classLoader);
	}

	private final boolean publicMethodsOnly;

	private final Set<TransactionAnnotationParser> annotationParsers;


	/**
	 * 创建默认的AnnotationTransactionAttributeSource, 支持带有
	 * {@code Transactional}注解或EJB3 {@link javax.ejb.TransactionAttribute}注解的公共方法。
	 */
	public AnnotationTransactionAttributeSource() {
		this(true);
	}

	/**
	 * 创建自定义AnnotationTransactionAttributeSource, 支持带有
	 * {@code Transactional}注解或EJB3 {@link javax.ejb.TransactionAttribute}注解的公共方法。
	 * @param publicMethodsOnly whether to support public methods that carry
	 * the {@code Transactional} annotation only (typically for use
	 * with proxy-based AOP), or protected/private methods as well
	 * (typically used with AspectJ class weaving)
	 */
	public AnnotationTransactionAttributeSource(boolean publicMethodsOnly) {
		this.publicMethodsOnly = publicMethodsOnly;
		if (jta12Present || ejb3Present) {
			this.annotationParsers = new LinkedHashSet<>(4);
			this.annotationParsers.add(new SpringTransactionAnnotationParser());
			if (jta12Present) {
				this.annotationParsers.add(new JtaTransactionAnnotationParser());
			}
			if (ejb3Present) {
				this.annotationParsers.add(new Ejb3TransactionAnnotationParser());
			}
		}
		else {
			this.annotationParsers = Collections.singleton(new SpringTransactionAnnotationParser());
		}
	}

	/**
	 * 创建自定义AnnotationTransactionAttributeSource。
	 * @param annotationParser the TransactionAnnotationParser to use
	 */
	public AnnotationTransactionAttributeSource(TransactionAnnotationParser annotationParser) {
		this.publicMethodsOnly = true;
		Assert.notNull(annotationParser, "TransactionAnnotationParser must not be null");
		this.annotationParsers = Collections.singleton(annotationParser);
	}

	/**
	 * 创建自定义AnnotationTransactionAttributeSource。
	 * @param annotationParsers the TransactionAnnotationParsers to use
	 */
	public AnnotationTransactionAttributeSource(TransactionAnnotationParser... annotationParsers) {
		this.publicMethodsOnly = true;
		Assert.notEmpty(annotationParsers, "At least one TransactionAnnotationParser needs to be specified");
		this.annotationParsers = new LinkedHashSet<>(Arrays.asList(annotationParsers));
	}

	/**
	 * 创建自定义AnnotationTransactionAttributeSource。
	 * @param annotationParsers the TransactionAnnotationParsers to use
	 */
	public AnnotationTransactionAttributeSource(Set<TransactionAnnotationParser> annotationParsers) {
		this.publicMethodsOnly = true;
		Assert.notEmpty(annotationParsers, "At least one TransactionAnnotationParser needs to be specified");
		this.annotationParsers = annotationParsers;
	}


	@Override
	public boolean isCandidateClass(Class<?> targetClass) {
		for (TransactionAnnotationParser parser : this.annotationParsers) {
			if (parser.isCandidateClass(targetClass)) {
				return true;
			}
		}
		return false;
	}

	@Override
	@Nullable
	protected TransactionAttribute findTransactionAttribute(Class<?> clazz) {
		return determineTransactionAttribute(clazz);
	}

	@Override
	@Nullable
	protected TransactionAttribute findTransactionAttribute(Method method) {
		return determineTransactionAttribute(method);
	}

	/**
	 * 确定给定方法或类的事务属性。
	 * <p>该实现委托给已配置的{@link TransactionAnnotationParser TransactionAnnotationParsers},
	 * 将已知注解解析为Spring的元数据属性类。如果不是事务性的, 则返回{@code null}。
	 * <p>可以被子类重写以支持携带事务元数据的自定义注解。
	 * @param element the annotated method or class
	 * @return the configured transaction attribute, or {@code null} if none was found
	 */
	@Nullable
	protected TransactionAttribute determineTransactionAttribute(AnnotatedElement element) {
		for (TransactionAnnotationParser parser : this.annotationParsers) {
			TransactionAttribute attr = parser.parseTransactionAnnotation(element);
			if (attr != null) {
				return attr;
			}
		}
		return null;
	}

	/**
	 * 默认情况下, 只有公共方法可以被设置为事务性的。
	 */
	@Override
	protected boolean allowPublicMethodsOnly() {
		return this.publicMethodsOnly;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AnnotationTransactionAttributeSource)) {
			return false;
		}
		AnnotationTransactionAttributeSource otherTas = (AnnotationTransactionAttributeSource) other;
		return (this.annotationParsers.equals(otherTas.annotationParsers) &&
				this.publicMethodsOnly == otherTas.publicMethodsOnly);
	}

	@Override
	public int hashCode() {
		return this.annotationParsers.hashCode();
	}

}
