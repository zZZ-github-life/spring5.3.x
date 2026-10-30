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

package org.springframework.transaction.interceptor;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 组合式{@link TransactionAttributeSource}实现, 遍历给定的{@link TransactionAttributeSource}实例数组。
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public class CompositeTransactionAttributeSource implements TransactionAttributeSource, Serializable {

	private final TransactionAttributeSource[] transactionAttributeSources;


	/**
	 * 为给定的源创建一个新的CompositeTransactionAttributeSource。
	 * @param transactionAttributeSources 要组合的 TransactionAttributeSource 实例
	 */
	public CompositeTransactionAttributeSource(TransactionAttributeSource... transactionAttributeSources) {
		Assert.notNull(transactionAttributeSources, "TransactionAttributeSource array must not be null");
		this.transactionAttributeSources = transactionAttributeSources;
	}

	/**
	 * 返回此CompositeTransactionAttributeSource组合的TransactionAttributeSource实例。
	 */
	public final TransactionAttributeSource[] getTransactionAttributeSources() {
		return this.transactionAttributeSources;
	}


	@Override
	public boolean isCandidateClass(Class<?> targetClass) {
		for (TransactionAttributeSource source : this.transactionAttributeSources) {
			if (source.isCandidateClass(targetClass)) {
				return true;
			}
		}
		return false;
	}

	@Override
	@Nullable
	public TransactionAttribute getTransactionAttribute(Method method, @Nullable Class<?> targetClass) {
		for (TransactionAttributeSource source : this.transactionAttributeSources) {
			TransactionAttribute attr = source.getTransactionAttribute(method, targetClass);
			if (attr != null) {
				return attr;
			}
		}
		return null;
	}

}
