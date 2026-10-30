/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.aop.support;

import java.io.Serializable;

import org.aopalliance.aop.Advice;

import org.springframework.aop.Pointcut;
import org.springframework.lang.Nullable;

/**
 * 便捷的由 Pointcut 驱动的 Advisor 实现。
 *
 * <p>这是最常用的 Advisor 实现。它可用于任何切点和通知类型，但引入（introduction）除外。通常无需为
 * 此类创建子类，也无需实现自定义的 Advisor。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #setPointcut
 * @see #setAdvice
 */
@SuppressWarnings("serial")
public class DefaultPointcutAdvisor extends AbstractGenericPointcutAdvisor implements Serializable {

	private Pointcut pointcut = Pointcut.TRUE;


	/**
	 * 创建一个空的 DefaultPointcutAdvisor。<p>在使用 setter 方法之前必须设置通知。通常也会设置切点，但
	 * 其默认值为 {@code Pointcut.TRUE}。
	 */
	public DefaultPointcutAdvisor() {
	}

	/**
	 * 创建一个匹配所有方法的 DefaultPointcutAdvisor。<p>将使用 {@code Pointcut.TRUE} 作为切点。
	 * @param advice 要使用的通知
	 */
	public DefaultPointcutAdvisor(Advice advice) {
		this(Pointcut.TRUE, advice);
	}

	/**
	 * 创建一个 DefaultPointcutAdvisor，并指定切点和通知。
	 * @param pointcut 指向该通知的 Pointcut
	 * @param advice Pointcut 匹配时运行的通知
	 */
	public DefaultPointcutAdvisor(Pointcut pointcut, Advice advice) {
		this.pointcut = pointcut;
		setAdvice(advice);
	}


	/**
	 * 指定针对该通知的切点。<p>默认值为 {@code Pointcut.TRUE}。
	 * @see #setAdvice
	 */
	public void setPointcut(@Nullable Pointcut pointcut) {
		this.pointcut = (pointcut != null ? pointcut : Pointcut.TRUE);
	}

	@Override
	public Pointcut getPointcut() {
		return this.pointcut;
	}


	@Override
	public String toString() {
		return getClass().getName() + ": pointcut [" + getPointcut() + "]; advice [" + getAdvice() + "]";
	}

}
