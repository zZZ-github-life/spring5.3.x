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

package org.springframework.transaction.reactive;

import reactor.core.publisher.Mono;

/**
 * {@link TransactionSynchronization}的实现，管理通过
 * {@link TransactionSynchronizationManager}绑定的资源对象。
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 * @since 5.2
 * @param <O> 资源持有者类型
 * @param <K> 资源键类型
 */
public abstract class ReactiveResourceSynchronization<O, K> implements TransactionSynchronization {

	private final O resourceObject;

	private final K resourceKey;

	private final TransactionSynchronizationManager synchronizationManager;

	private volatile boolean holderActive = true;


	/**
	 * 为给定的持有者创建一个新的ReactiveResourceSynchronization。
	 * @param resourceObject the resource object to manage
	 * @param resourceKey the key to bind the resource object for
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @see TransactionSynchronizationManager#bindResource
	 */
	public ReactiveResourceSynchronization(
			O resourceObject, K resourceKey, TransactionSynchronizationManager synchronizationManager) {

		this.resourceObject = resourceObject;
		this.resourceKey = resourceKey;
		this.synchronizationManager = synchronizationManager;
	}


	@Override
	public Mono<Void> suspend() {
		if (this.holderActive) {
			this.synchronizationManager.unbindResource(this.resourceKey);
		}
		return Mono.empty();
	}

	@Override
	public Mono<Void> resume() {
		if (this.holderActive) {
			this.synchronizationManager.bindResource(this.resourceKey, this.resourceObject);
		}
		return Mono.empty();
	}

	@Override
	public Mono<Void> beforeCommit(boolean readOnly) {
		return Mono.empty();
	}

	@Override
	public Mono<Void> beforeCompletion() {
		if (shouldUnbindAtCompletion()) {
			this.synchronizationManager.unbindResource(this.resourceKey);
			this.holderActive = false;
			if (shouldReleaseBeforeCompletion()) {
				return releaseResource(this.resourceObject, this.resourceKey);
			}
		}
		return Mono.empty();
	}

	@Override
	public Mono<Void> afterCommit() {
		if (!shouldReleaseBeforeCompletion()) {
			return processResourceAfterCommit(this.resourceObject);
		}
		return Mono.empty();
	}

	@Override
	public Mono<Void> afterCompletion(int status) {
		return Mono.defer(() -> {
			Mono<Void> sync = Mono.empty();
			if (shouldUnbindAtCompletion()) {
				boolean releaseNecessary = false;
				if (this.holderActive) {
					// The thread-bound resource holder might not be available anymore,
					// since afterCompletion might get called from a different thread.
					this.holderActive = false;
					this.synchronizationManager.unbindResourceIfPossible(this.resourceKey);
					releaseNecessary = true;
				}
				else {
					releaseNecessary = shouldReleaseAfterCompletion(this.resourceObject);
				}
				if (releaseNecessary) {
					sync = releaseResource(this.resourceObject, this.resourceKey);
				}
			}
			else {
				// Probably a pre-bound resource...
				sync = cleanupResource(this.resourceObject, this.resourceKey, (status == STATUS_COMMITTED));
			}
			return sync;
		});
	}


	/**
	 * 返回此持有者是否应在完成时解绑
	 * （或者更倾向于在事务结束后保持绑定到线程）。
	 * <p>默认实现返回{@code true}。
	 */
	protected boolean shouldUnbindAtCompletion() {
		return true;
	}

	/**
	 * 返回此持有者的资源是否应在事务完成之前释放
	 * （{@code true}）还是在事务完成之后释放（{@code false}）。
	 * <p>请注意，仅当资源从线程解绑时才会被释放
	 * （{@link #shouldUnbindAtCompletion()}）。
	 * <p>默认实现返回{@code true}。
	 * @see #releaseResource
	 */
	protected boolean shouldReleaseBeforeCompletion() {
		return true;
	}

	/**
	 * 返回此持有者的资源是否应在事务完成之后释放（{@code true}）。
	 * <p>默认实现返回{@code !shouldReleaseBeforeCompletion()}，
	 * 如果在完成之前没有尝试释放，则在完成之后释放。
	 * @see #releaseResource
	 */
	protected boolean shouldReleaseAfterCompletion(O resourceHolder) {
		return !shouldReleaseBeforeCompletion();
	}

	/**
	 * 给定资源持有者的提交后回调。
	 * 仅在资源尚未被释放时调用
	 * （{@link #shouldReleaseBeforeCompletion()}）。
	 * @param resourceHolder the resource holder to process
	 */
	protected Mono<Void> processResourceAfterCommit(O resourceHolder) {
		return Mono.empty();
	}

	/**
	 * 释放给定的资源（在其从线程解绑之后）。
	 * @param resourceHolder the resource holder to process
	 * @param resourceKey the key that the resource object was bound for
	 */
	protected Mono<Void> releaseResource(O resourceHolder, K resourceKey) {
		return Mono.empty();
	}

	/**
	 * 对给定的资源执行清理操作（资源保持绑定到线程）。
	 * @param resourceHolder the resource holder to process
	 * @param resourceKey the key that the resource object was bound for
	 * @param committed whether the transaction has committed ({@code true})
	 * or rolled back ({@code false})
	 */
	protected Mono<Void> cleanupResource(O resourceHolder, K resourceKey, boolean committed) {
		return Mono.empty();
	}

}
