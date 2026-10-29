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

package org.springframework.transaction.interceptor;

import java.io.Serializable;
import java.util.Collection;

import org.springframework.lang.Nullable;
import org.springframework.transaction.support.DelegatingTransactionDefinition;

/**
 * {@link TransactionAttribute}实现, 将所有调用委托给给定的目标{@link TransactionAttribute}实例。
 * 抽象类, 旨在被子类化, 子类重写不应简单委托到目标实例的特定方法。
 *
 * @author Juergen Hoeller
 * @author Mark Paluch
 * @since 1.2
 */
@SuppressWarnings("serial")
public abstract class DelegatingTransactionAttribute extends DelegatingTransactionDefinition
		implements TransactionAttribute, Serializable {

	private final TransactionAttribute targetAttribute;


	/**
	 * 为给定的目标属性创建DelegatingTransactionAttribute。
	 * @param targetAttribute the target TransactionAttribute to delegate to
	 */
	public DelegatingTransactionAttribute(TransactionAttribute targetAttribute) {
		super(targetAttribute);
		this.targetAttribute = targetAttribute;
	}


	@Override
	@Nullable
	public String getQualifier() {
		return this.targetAttribute.getQualifier();
	}

	@Override
	public Collection<String> getLabels() {
		return this.targetAttribute.getLabels();
	}

	@Override
	public boolean rollbackOn(Throwable ex) {
		return this.targetAttribute.rollbackOn(ex);
	}

}
