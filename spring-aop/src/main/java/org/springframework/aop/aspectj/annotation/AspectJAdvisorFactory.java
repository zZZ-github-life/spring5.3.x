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
	 * @param clazz 假定的注解风格 AspectJ 类
	 * @return 该类是否被 AspectJ 识别为切面类
	 */
	boolean isAspect(Class<?> clazz);

	/**
	 * 给定的类是否为有效的 AspectJ 切面类？
	 * @param aspectClass 要校验的假定 AspectJ 注解风格类
	 * @throws AopConfigException 如果该类是无效的切面（这永远不合法）
	 * @throws NotAnAtAspectException 如果该类根本不是一个切面（是否合法取决于上下文）
	 */
	void validate(Class<?> aspectClass) throws AopConfigException;

	/**
	 * 为指定的切面实例上所有带注解的 At-AspectJ 方法构建 Spring AOP Advisor。
	 * @param aspectInstanceFactory 切面实例工厂（不是切面实例本身，以避免急切实例化）
	 * @return 该类的通知器列表
	 */
	List<Advisor> getAdvisors(MetadataAwareAspectInstanceFactory aspectInstanceFactory);

	/**
	 * 为给定的 AspectJ 通知方法构建 Spring AOP Advisor。
	 * @param candidateAdviceMethod 候选通知方法
	 * @param aspectInstanceFactory 切面实例工厂
	 * @param declarationOrder 在切面内的声明顺序
	 * @param aspectName 切面的名称
	 * @return 如果该方法不是 AspectJ 通知方法，或者它是一个将被其他通知使用
	 * 但本身不会创建 Spring 通知的切点，则返回 {@code null}
	 */
	@Nullable
	Advisor getAdvisor(Method candidateAdviceMethod, MetadataAwareAspectInstanceFactory aspectInstanceFactory,
			int declarationOrder, String aspectName);

	/**
	 * 为给定的 AspectJ 通知方法构建 Spring AOP Advice。
	 * @param candidateAdviceMethod 候选通知方法
	 * @param expressionPointcut AspectJ 表达式切点
	 * @param aspectInstanceFactory 切面实例工厂
	 * @param declarationOrder 在切面内的声明顺序
	 * @param aspectName 切面的名称
	 * @return 如果该方法不是 AspectJ 通知方法，或者它是一个将被其他通知使用
	 * 但本身不会创建 Spring 通知的切点，则返回 {@code null}
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
