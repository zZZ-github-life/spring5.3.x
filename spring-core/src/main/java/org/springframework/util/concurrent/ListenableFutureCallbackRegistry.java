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

package org.springframework.util.concurrent;

import java.util.ArrayDeque;
import java.util.Queue;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 用于 {@link ListenableFuture} 实现的辅助类，维护成功和失败回调的队列并协助通知它们。
 *
 * <p>受 {@code com.google.common.util.concurrent.ExecutionList} 启发。
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 4.0
 * @param <T> 回调结果类型
 */
public class ListenableFutureCallbackRegistry<T> {

	private final Queue<SuccessCallback<? super T>> successCallbacks = new ArrayDeque<>(1);

	private final Queue<FailureCallback> failureCallbacks = new ArrayDeque<>(1);

	private State state = State.NEW;

	@Nullable
	private Object result;

	private final Object mutex = new Object();


	/**
	 * 将给定的回调添加到此注册表。
	 * @param callback 要添加的回调
	 */
	public void addCallback(ListenableFutureCallback<? super T> callback) {
		Assert.notNull(callback, "'callback' must not be null");
		synchronized (this.mutex) {
			switch (this.state) {
				case NEW:
					this.successCallbacks.add(callback);
					this.failureCallbacks.add(callback);
					break;
				case SUCCESS:
					notifySuccess(callback);
					break;
				case FAILURE:
					notifyFailure(callback);
					break;
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void notifySuccess(SuccessCallback<? super T> callback) {
		try {
			callback.onSuccess((T) this.result);
		}
		catch (Throwable ex) {
			// Ignore
		}
	}

	private void notifyFailure(FailureCallback callback) {
		Assert.state(this.result instanceof Throwable, "No Throwable result for failure state");
		try {
			callback.onFailure((Throwable) this.result);
		}
		catch (Throwable ex) {
			// Ignore
		}
	}

	/**
	 * 将给定的成功回调添加到此注册表。
	 * @param callback 要添加的成功回调
	 * @since 4.1
	 */
	public void addSuccessCallback(SuccessCallback<? super T> callback) {
		Assert.notNull(callback, "'callback' must not be null");
		synchronized (this.mutex) {
			switch (this.state) {
				case NEW:
					this.successCallbacks.add(callback);
					break;
				case SUCCESS:
					notifySuccess(callback);
					break;
			}
		}
	}

	/**
	 * 将给定的失败回调添加到此注册表。
	 * @param callback 要添加的失败回调
	 * @since 4.1
	 */
	public void addFailureCallback(FailureCallback callback) {
		Assert.notNull(callback, "'callback' must not be null");
		synchronized (this.mutex) {
			switch (this.state) {
				case NEW:
					this.failureCallbacks.add(callback);
					break;
				case FAILURE:
					notifyFailure(callback);
					break;
			}
		}
	}

	/**
	 * 使用给定结果对所有已添加的回调触发 {@link ListenableFutureCallback#onSuccess(Object)} 调用。
	 * @param result 用于触发回调的结果
	 */
	public void success(@Nullable T result) {
		synchronized (this.mutex) {
			this.state = State.SUCCESS;
			this.result = result;
			SuccessCallback<? super T> callback;
			while ((callback = this.successCallbacks.poll()) != null) {
				notifySuccess(callback);
			}
		}
	}

	/**
	 * 使用给定的 {@code Throwable} 对所有已添加的回调触发
	 * {@link ListenableFutureCallback#onFailure(Throwable)} 调用。
	 * @param ex 用于触发回调的异常
	 */
	public void failure(Throwable ex) {
		synchronized (this.mutex) {
			this.state = State.FAILURE;
			this.result = ex;
			FailureCallback callback;
			while ((callback = this.failureCallbacks.poll()) != null) {
				notifyFailure(callback);
			}
		}
	}


	private enum State {NEW, SUCCESS, FAILURE}

}
