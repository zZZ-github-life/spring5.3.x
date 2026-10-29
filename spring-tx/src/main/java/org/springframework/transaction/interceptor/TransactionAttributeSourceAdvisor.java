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

package org.springframework.transaction.interceptor;

import org.aopalliance.aop.Advice;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 由{@link TransactionAttributeSource}驱动的Advisor, 仅对事务性方法包含{@link TransactionInterceptor}。
 *
 * <p>由于AOP框架缓存通知计算, 这通常比直接让TransactionInterceptor运行并自行发现没有工作要做要快。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #setTransactionInterceptor
 * @see TransactionProxyFactoryBean
 */
@SuppressWarnings("serial")
public class TransactionAttributeSourceAdvisor extends AbstractPointcutAdvisor {

	@Nullable
	private TransactionInterceptor transactionInterceptor;

	private final TransactionAttributeSourcePointcut pointcut = new TransactionAttributeSourcePointcut() {
		@Override
		@Nullable
		protected TransactionAttributeSource getTransactionAttributeSource() {
			return (transactionInterceptor != null ? transactionInterceptor.getTransactionAttributeSource() : null);
		}
	};


	/**
	 * 创建一个新的TransactionAttributeSourceAdvisor。
	 */
	public TransactionAttributeSourceAdvisor() {
	}

	/**
	 * 创建一个新的TransactionAttributeSourceAdvisor。
	 * @param interceptor the transaction interceptor to use for this advisor
	 */
	public TransactionAttributeSourceAdvisor(TransactionInterceptor interceptor) {
		setTransactionInterceptor(interceptor);
	}


	/**
	 * 设置此Advisor使用的事务拦截器。
	 */
	public void setTransactionInterceptor(TransactionInterceptor interceptor) {
		this.transactionInterceptor = interceptor;
	}

	/**
	 * 设置此切点使用的{@link ClassFilter}。
	 * 默认值为{@link ClassFilter#TRUE}。
	 */
	public void setClassFilter(ClassFilter classFilter) {
		this.pointcut.setClassFilter(classFilter);
	}


	@Override
	public Advice getAdvice() {
		Assert.state(this.transactionInterceptor != null, "No TransactionInterceptor set");
		return this.transactionInterceptor;
	}

	@Override
	public Pointcut getPointcut() {
		return this.pointcut;
	}

}
