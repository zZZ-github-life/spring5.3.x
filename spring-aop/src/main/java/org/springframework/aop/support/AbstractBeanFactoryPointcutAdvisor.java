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

package org.springframework.aop.support;

import java.io.IOException;
import java.io.ObjectInputStream;

import org.aopalliance.aop.Advice;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 基于 BeanFactory 的抽象 PointcutAdvisor，允许将任意 Advice 配置为对 BeanFactory 中 Advice Bean
 * 的引用。
 *
 * <p>与直接指定通知对象本身相比，指定通知 Bean 的名称（如果在 BeanFactory 中运行）可提高初始化时的
 * 松耦合程度，从而在切点实际匹配之前不初始化通知对象。
 *
 * @author Juergen Hoeller
 * @since 2.0.2
 * @see #setAdviceBeanName
 * @see DefaultBeanFactoryPointcutAdvisor
 */
@SuppressWarnings("serial")
public abstract class AbstractBeanFactoryPointcutAdvisor extends AbstractPointcutAdvisor implements BeanFactoryAware {

	@Nullable
	private String adviceBeanName;

	@Nullable
	private BeanFactory beanFactory;

	@Nullable
	private transient volatile Advice advice;

	private transient volatile Object adviceMonitor = new Object();


	/**
	 * 指定此通知器应引用的通知 Bean 的名称。
	 * <p>在首次访问此通知器的通知时将获取指定 Bean 的实例。此通知器最多只会获取一个通知 Bean 实例，并
	 * 在通知器的整个生命周期内缓存该实例。
	 * @see #getAdvice()
	 */
	public void setAdviceBeanName(@Nullable String adviceBeanName) {
		this.adviceBeanName = adviceBeanName;
	}

	/**
	 * 返回此通知器所引用的通知 Bean 的名称（如果有）。
	 */
	@Nullable
	public String getAdviceBeanName() {
		return this.adviceBeanName;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		resetAdviceMonitor();
	}

	private void resetAdviceMonitor() {
		if (this.beanFactory instanceof ConfigurableBeanFactory) {
			this.adviceMonitor = ((ConfigurableBeanFactory) this.beanFactory).getSingletonMutex();
		}
		else {
			this.adviceMonitor = new Object();
		}
	}

	/**
	 * 直接指定目标通知的特定实例，避免在 {@link #getAdvice()} 中进行延迟解析。
	 * @since 3.1
	 */
	public void setAdvice(Advice advice) {
		synchronized (this.adviceMonitor) {
			this.advice = advice;
		}
	}

	@Override
	public Advice getAdvice() {
		Advice advice = this.advice;
		if (advice != null) {
			return advice;
		}

		Assert.state(this.adviceBeanName != null, "'adviceBeanName' must be specified");
		Assert.state(this.beanFactory != null, "BeanFactory must be set to resolve 'adviceBeanName'");

		if (this.beanFactory.isSingleton(this.adviceBeanName)) {
			// Rely on singleton semantics provided by the factory.
			advice = this.beanFactory.getBean(this.adviceBeanName, Advice.class);
			this.advice = advice;
			return advice;
		}
		else {
			// No singleton guarantees from the factory -> let's lock locally but
			// reuse the factory's singleton lock, just in case a lazy dependency
			// of our advice bean happens to trigger the singleton lock implicitly...
			synchronized (this.adviceMonitor) {
				advice = this.advice;
				if (advice == null) {
					advice = this.beanFactory.getBean(this.adviceBeanName, Advice.class);
					this.advice = advice;
				}
				return advice;
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getName());
		sb.append(": advice ");
		if (this.adviceBeanName != null) {
			sb.append("bean '").append(this.adviceBeanName).append('\'');
		}
		else {
			sb.append(this.advice);
		}
		return sb.toString();
	}


	//---------------------------------------------------------------------
	// Serialization support
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// Rely on default serialization, just initialize state after deserialization.
		ois.defaultReadObject();

		// Initialize transient fields.
		resetAdviceMonitor();
	}

}
