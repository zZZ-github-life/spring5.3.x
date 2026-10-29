/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.dao.support;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * AOP Alliance MethodInterceptor，基于给定的 PersistenceExceptionTranslator
 * 提供持久化异常翻译功能。
 *
 * <p>委托给给定的 {@link PersistenceExceptionTranslator}，将抛出的
 * RuntimeException 翻译为 Spring 的 DataAccessException 层次结构
 * （如果合适的话）。如果所涉及的 RuntimeException 在目标方法上已声明，
 * 则始终原样传播（不进行翻译）。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see PersistenceExceptionTranslator
 */
public class PersistenceExceptionTranslationInterceptor
		implements MethodInterceptor, BeanFactoryAware, InitializingBean {

	@Nullable
	private volatile PersistenceExceptionTranslator persistenceExceptionTranslator;

	private boolean alwaysTranslate = false;

	@Nullable
	private ListableBeanFactory beanFactory;


	/**
	 * 创建新的 PersistenceExceptionTranslationInterceptor。
	 * 之后需要使用 PersistenceExceptionTranslator 进行配置。
	 * @see #setPersistenceExceptionTranslator
	 */
	public PersistenceExceptionTranslationInterceptor() {
	}

	/**
	 * 为给定的 PersistenceExceptionTranslator
	 * 创建新的 PersistenceExceptionTranslationInterceptor。
	 * @param pet the PersistenceExceptionTranslator to use
	 */
	public PersistenceExceptionTranslationInterceptor(PersistenceExceptionTranslator pet) {
		Assert.notNull(pet, "PersistenceExceptionTranslator must not be null");
		this.persistenceExceptionTranslator = pet;
	}

	/**
	 * 创建新的 PersistenceExceptionTranslationInterceptor，
	 * 自动检测给定 BeanFactory 中的 PersistenceExceptionTranslator。
	 * @param beanFactory the ListableBeanFactory to obtaining all
	 * PersistenceExceptionTranslators from
	 */
	public PersistenceExceptionTranslationInterceptor(ListableBeanFactory beanFactory) {
		Assert.notNull(beanFactory, "ListableBeanFactory must not be null");
		this.beanFactory = beanFactory;
	}


	/**
	 * 指定要使用的 PersistenceExceptionTranslator。
	 * <p>默认值是自动检测包含的 BeanFactory 中所有的
	 * PersistenceExceptionTranslator，并以链式方式使用它们。
	 * @see #detectPersistenceExceptionTranslators
	 */
	public void setPersistenceExceptionTranslator(PersistenceExceptionTranslator pet) {
		this.persistenceExceptionTranslator = pet;
	}

	/**
	 * 指定是否始终翻译异常（"true"），还是当原始方法签名的异常声明
	 * 允许抛出原始异常时直接抛出原始异常（"false"）。
	 * <p>默认值为 "false"。将此标志切换为 "true" 以始终翻译适用的异常，
	 * 而不考虑原始方法签名。
	 * <p>请注意，原始方法不必声明特定的异常。任何基类都可以，
	 * 甚至是 {@code throws Exception}：只要原始方法确实显式声明了兼容的异常，
	 * 原始异常就会被重新抛出。如果你希望在任何情况下都避免抛出原始异常，
	 * 请将此标志切换为 "true"。
	 */
	public void setAlwaysTranslate(boolean alwaysTranslate) {
		this.alwaysTranslate = alwaysTranslate;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (this.persistenceExceptionTranslator == null) {
			// No explicit exception translator specified - perform autodetection.
			if (!(beanFactory instanceof ListableBeanFactory)) {
				throw new IllegalArgumentException(
						"Cannot use PersistenceExceptionTranslator autodetection without ListableBeanFactory");
			}
			this.beanFactory = (ListableBeanFactory) beanFactory;
		}
	}

	@Override
	public void afterPropertiesSet() {
		if (this.persistenceExceptionTranslator == null && this.beanFactory == null) {
			throw new IllegalArgumentException("Property 'persistenceExceptionTranslator' is required");
		}
	}


	@Override
	@Nullable
	public Object invoke(MethodInvocation mi) throws Throwable {
		try {
			return mi.proceed();
		}
		catch (RuntimeException ex) {
			// Let it throw raw if the type of the exception is on the throws clause of the method.
			if (!this.alwaysTranslate && ReflectionUtils.declaresException(mi.getMethod(), ex.getClass())) {
				throw ex;
			}
			else {
				PersistenceExceptionTranslator translator = this.persistenceExceptionTranslator;
				if (translator == null) {
					Assert.state(this.beanFactory != null,
							"Cannot use PersistenceExceptionTranslator autodetection without ListableBeanFactory");
					translator = detectPersistenceExceptionTranslators(this.beanFactory);
					this.persistenceExceptionTranslator = translator;
				}
				throw DataAccessUtils.translateIfNecessary(ex, translator);
			}
		}
	}

	/**
	 * 检测给定 BeanFactory 中所有的 PersistenceExceptionTranslator。
	 * @param bf the ListableBeanFactory to obtain PersistenceExceptionTranslators from
	 * @return a chained PersistenceExceptionTranslator, combining all
	 * PersistenceExceptionTranslators found in the given bean factory
	 * @see ChainedPersistenceExceptionTranslator
	 */
	protected PersistenceExceptionTranslator detectPersistenceExceptionTranslators(ListableBeanFactory bf) {
		// Find all translators, being careful not to activate FactoryBeans.
		ChainedPersistenceExceptionTranslator cpet = new ChainedPersistenceExceptionTranslator();
		bf.getBeanProvider(PersistenceExceptionTranslator.class, false).orderedStream().forEach(cpet::addDelegate);
		return cpet;
	}

}
