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

package org.springframework.transaction.support;

import java.io.Serializable;

import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.util.Assert;

/**
 * 将所有调用委托给给定目标 {@link TransactionDefinition} 实例的 {@link TransactionDefinition} 实现。由于设计为供子类扩展，因此是抽象类；子类可覆盖不应简单委托给目标实例的特定方法。
 * @author Juergen Hoeller
 * @since 3.0
 */
@SuppressWarnings("serial")
public abstract class DelegatingTransactionDefinition implements TransactionDefinition, Serializable {

	private final TransactionDefinition targetDefinition;


	/**
	 * 为给定目标属性创建一个 DelegatingTransactionAttribute。
	 * @param targetDefinition 要委派到的目标 TransactionAttribute
	 */
	public DelegatingTransactionDefinition(TransactionDefinition targetDefinition) {
		Assert.notNull(targetDefinition, "Target definition must not be null");
		this.targetDefinition = targetDefinition;
	}


	@Override
	public int getPropagationBehavior() {
		return this.targetDefinition.getPropagationBehavior();
	}

	@Override
	public int getIsolationLevel() {
		return this.targetDefinition.getIsolationLevel();
	}

	@Override
	public int getTimeout() {
		return this.targetDefinition.getTimeout();
	}

	@Override
	public boolean isReadOnly() {
		return this.targetDefinition.isReadOnly();
	}

	@Override
	@Nullable
	public String getName() {
		return this.targetDefinition.getName();
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return this.targetDefinition.equals(other);
	}

	@Override
	public int hashCode() {
		return this.targetDefinition.hashCode();
	}

	@Override
	public String toString() {
		return this.targetDefinition.toString();
	}

}
