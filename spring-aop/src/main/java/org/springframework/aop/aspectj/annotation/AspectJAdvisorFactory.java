/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.aop.aspectj.annotation;

import java.lang.reflect.Method;
import java.util.List;

import org.aopalliance.aop.Advice;

import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.framework.AopConfigException;
import org.springframework.lang.Nullable;

/**
 * 工厂接口，这些工厂可以从使用 AspectJ 注解语法的类创建 Spring AOP Advisor。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see AspectMetadata
 * @see org.aspectj.lang.reflect.AjTypeSystem
 */
public interface AspectJAdvisorFactory {

	/**
	 * 判断给定的类是否为切面，判断依据来自 AspectJ 的 {@link org.aspectj.lang.reflect.AjTypeSystem}。
	 * <p>如果假定的切面无效（例如具体切面类的扩展），将直接返回 {@code false}。对于 Spring AOP 无法处
	 * 理的某些切面（例如使用不受支持的实例化模型的切面），将返回 true。如有必要，可使用
	 * {@link #validate} 方法来处理这些情况。
	 * @param clazz the supposed annotation-style AspectJ class
	 * @return whether this class is recognized by AspectJ as an aspect class
	 */
	boolean isAspect(Class<?> clazz);

	/**
	 * 给定的类是否为有效的 AspectJ 切面类？
	 * @param aspectClass the supposed AspectJ annotation-style class to validate
	 * @throws AopConfigException if the class is an invalid aspect
	 * (which can never be legal)
	 * @throws NotAnAtAspectException if the class is not an aspect at all
	 * (which may or may not be legal, depending on the context)
	 */
	void validate(Class<?> aspectClass) throws AopConfigException;

	/**
	 * 为指定的切面实例上所有带注解的 At-AspectJ 方法构建 Spring AOP Advisor。
	 * @param aspectInstanceFactory the aspect instance factory
	 * (not the aspect instance itself in order to avoid eager instantiation)
	 * @return a list of advisors for this class
	 */
	List<Advisor> getAdvisors(MetadataAwareAspectInstanceFactory aspectInstanceFactory);

	/**
	 * 为给定的 AspectJ 通知方法构建 Spring AOP Advisor。
	 * @param candidateAdviceMethod the candidate advice method
	 * @param aspectInstanceFactory the aspect instance factory
	 * @param declarationOrder the declaration order within the aspect
	 * @param aspectName the name of the aspect
	 * @return {@code null} if the method is not an AspectJ advice method
	 * or if it is a pointcut that will be used by other advice but will not
	 * create a Spring advice in its own right
	 */
	@Nullable
	Advisor getAdvisor(Method candidateAdviceMethod, MetadataAwareAspectInstanceFactory aspectInstanceFactory,
			int declarationOrder, String aspectName);

	/**
	 * 为给定的 AspectJ 通知方法构建 Spring AOP Advice。
	 * @param candidateAdviceMethod the candidate advice method
	 * @param expressionPointcut the AspectJ expression pointcut
	 * @param aspectInstanceFactory the aspect instance factory
	 * @param declarationOrder the declaration order within the aspect
	 * @param aspectName the name of the aspect
	 * @return {@code null} if the method is not an AspectJ advice method
	 * or if it is a pointcut that will be used by other advice but will not
	 * create a Spring advice in its own right
	 * @see org.springframework.aop.aspectj.AspectJAroundAdvice
	 * @see org.springframework.aop.aspectj.AspectJMethodBeforeAdvice
	 * @see org.springframework.aop.aspectj.AspectJAfterAdvice
	 * @see org.springframework.aop.aspectj.AspectJAfterReturningAdvice
	 * @see org.springframework.aop.aspectj.AspectJAfterThrowingAdvice
	 */
	@Nullable
	Advice getAdvice(Method candidateAdviceMethod, AspectJExpressionPointcut expressionPointcut,
			MetadataAwareAspectInstanceFactory aspectInstanceFactory, int declarationOrder, String aspectName);

}
