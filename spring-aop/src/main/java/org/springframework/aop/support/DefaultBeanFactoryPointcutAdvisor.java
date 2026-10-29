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

import org.springframework.aop.Pointcut;
import org.springframework.lang.Nullable;

/**
 * 基于 BeanFactory 的具体 PointcutAdvisor，允许将任意 Advice 配置为对 BeanFactory 中 Advice Bean
 * 的引用，也允许通过 Bean 属性配置 Pointcut。
 *
 * <p>指定通知 Bean 的名称而不是通知对象本身（如果在 BeanFactory 中运行）可在初始化时提高松耦合程度，
 * 从而在切点实际匹配之前不初始化通知对象。
 *
 * @author Juergen Hoeller
 * @since 2.0.2
 * @see #setPointcut
 * @see #setAdviceBeanName
 */
@SuppressWarnings("serial")
public class DefaultBeanFactoryPointcutAdvisor extends AbstractBeanFactoryPointcutAdvisor {

	private Pointcut pointcut = Pointcut.TRUE;


	/**
	 * 指定针对该通知的切点。<p>默认值为 {@code Pointcut.TRUE}。
	 * @see #setAdviceBeanName
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
		return getClass().getName() + ": pointcut [" + getPointcut() + "]; advice bean '" + getAdviceBeanName() + "'";
	}

}
