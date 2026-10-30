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

package org.springframework.transaction.jta;

import java.util.List;

import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

/**
 * JTA Synchronization的适配器, 在外层JTA事务完成后调用Spring {@link TransactionSynchronization}
 * 对象的{@code afterCommit} / {@code afterCompletion}回调。
 * 在参与现有(非Spring)JTA事务时应用。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see TransactionSynchronization#afterCommit
 * @see TransactionSynchronization#afterCompletion
 */
public class JtaAfterCompletionSynchronization implements Synchronization {

	private final List<TransactionSynchronization> synchronizations;


	/**
	 * 为给定的同步对象创建一个新的JtaAfterCompletionSynchronization。
	 * @param synchronizations TransactionSynchronization 对象的 List
	 * @see org.springframework.transaction.support.TransactionSynchronization
	 */
	public JtaAfterCompletionSynchronization(List<TransactionSynchronization> synchronizations) {
		this.synchronizations = synchronizations;
	}


	@Override
	public void beforeCompletion() {
	}

	@Override
	public void afterCompletion(int status) {
		switch (status) {
			case Status.STATUS_COMMITTED:
				try {
					TransactionSynchronizationUtils.invokeAfterCommit(this.synchronizations);
				}
				finally {
					TransactionSynchronizationUtils.invokeAfterCompletion(
							this.synchronizations, TransactionSynchronization.STATUS_COMMITTED);
				}
				break;
			case Status.STATUS_ROLLEDBACK:
				TransactionSynchronizationUtils.invokeAfterCompletion(
						this.synchronizations, TransactionSynchronization.STATUS_ROLLED_BACK);
				break;
			default:
				TransactionSynchronizationUtils.invokeAfterCompletion(
						this.synchronizations, TransactionSynchronization.STATUS_UNKNOWN);
		}
	}
}
