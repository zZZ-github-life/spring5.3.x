/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.dao.annotation;

import java.lang.annotation.Annotation;

import org.aopalliance.aop.Advice;

import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.dao.support.PersistenceExceptionTranslationInterceptor;
import org.springframework.dao.support.PersistenceExceptionTranslator;

/**
 * Spring AOP 异常翻译切面，用于在 Repository 或 DAO 层使用。
 * 基于给定的 PersistenceExceptionTranslator，将原生持久化异常
 * 翻译为 Spring 的 DataAccessException 层次结构。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.dao.DataAccessException
 * @see org.springframework.dao.support.PersistenceExceptionTranslator
 */
@SuppressWarnings("serial")
public class PersistenceExceptionTranslationAdvisor extends AbstractPointcutAdvisor {

	private final PersistenceExceptionTranslationInterceptor advice;

	private final AnnotationMatchingPointcut pointcut;


	/**
	 * 创建新的 PersistenceExceptionTranslationAdvisor。
	 * @param persistenceExceptionTranslator 要使用的 PersistenceExceptionTranslator
	 * @param repositoryAnnotationType 要检查的注解类型
	 */
	public PersistenceExceptionTranslationAdvisor(
			PersistenceExceptionTranslator persistenceExceptionTranslator,
			Class<? extends Annotation> repositoryAnnotationType) {

		this.advice = new PersistenceExceptionTranslationInterceptor(persistenceExceptionTranslator);
		this.pointcut = new AnnotationMatchingPointcut(repositoryAnnotationType, true);
	}

	/**
	 * 创建新的 PersistenceExceptionTranslationAdvisor。
	 * @param beanFactory 用于从中获取所有 PersistenceExceptionTranslator 的 ListableBeanFactory
	 * @param repositoryAnnotationType 要检查的注解类型
	 */
	PersistenceExceptionTranslationAdvisor(
			ListableBeanFactory beanFactory, Class<? extends Annotation> repositoryAnnotationType) {

		this.advice = new PersistenceExceptionTranslationInterceptor(beanFactory);
		this.pointcut = new AnnotationMatchingPointcut(repositoryAnnotationType, true);
	}


	@Override
	public Advice getAdvice() {
		return this.advice;
	}

	@Override
	public Pointcut getPointcut() {
		return this.pointcut;
	}

}
