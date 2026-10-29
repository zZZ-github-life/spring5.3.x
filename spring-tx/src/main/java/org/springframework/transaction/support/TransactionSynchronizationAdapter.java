/*
 * Copyright 2002-2020 the original author or authors.
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

import org.springframework.core.Ordered;

/**
 * 简单的 {@link TransactionSynchronization} 适配器，包含空的方法实现，便于仅重写单个方法。<p>还实现了 {@link Ordered} 接口，以便以声明方式控制同步的执行顺序。默认的 {@link #getOrder() order} 为 {@link Ordered#LOWEST_PRECEDENCE}，表示较晚执行；返回较小的值可更早执行。
 * @author Juergen Hoeller
 * @since 22.01.2004
 * @deprecated as of 5.3, in favor of the default methods on the
 * {@link TransactionSynchronization} interface
 */
@Deprecated
public abstract class TransactionSynchronizationAdapter implements TransactionSynchronization, Ordered {

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	@Override
	public void suspend() {
	}

	@Override
	public void resume() {
	}

	@Override
	public void flush() {
	}

	@Override
	public void beforeCommit(boolean readOnly) {
	}

	@Override
	public void beforeCompletion() {
	}

	@Override
	public void afterCommit() {
	}

	@Override
	public void afterCompletion(int status) {
	}

}
