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

package org.springframework.util.concurrent;

import java.util.concurrent.ExecutionException;

import org.springframework.lang.Nullable;

/**
 * 抽象类，将参数类型为 S 的 {@link ListenableFuture} 适配为参数类型为 T 的
 * {@code ListenableFuture}。所有方法都委托给被适配对象，其中 {@link #get()}、
 * {@link #get(long, java.util.concurrent.TimeUnit)} 和
 * {@link ListenableFutureCallback#onSuccess(Object)} 会对被适配对象的结果调用
 * {@link #adapt(Object)}。
 *
 * @author Arjen Poutsma
 * @since 4.0
 * @param <T> 该 {@code Future} 的类型
 * @param <S> 被适配对象的 {@code Future} 的类型
 */
public abstract class ListenableFutureAdapter<T, S> extends FutureAdapter<T, S> implements ListenableFuture<T> {

	/**
	 * 使用给定的被适配对象构造一个新的 {@code ListenableFutureAdapter}。
	 * @param adaptee 要适配到的 future
	 */
	protected ListenableFutureAdapter(ListenableFuture<S> adaptee) {
		super(adaptee);
	}


	@Override
	public void addCallback(final ListenableFutureCallback<? super T> callback) {
		addCallback(callback, callback);
	}

	@Override
	public void addCallback(final SuccessCallback<? super T> successCallback, final FailureCallback failureCallback) {
		ListenableFuture<S> listenableAdaptee = (ListenableFuture<S>) getAdaptee();
		listenableAdaptee.addCallback(new ListenableFutureCallback<S>() {
			@Override
			public void onSuccess(@Nullable S result) {
				T adapted = null;
				if (result != null) {
					try {
						adapted = adaptInternal(result);
					}
					catch (ExecutionException ex) {
						Throwable cause = ex.getCause();
						onFailure(cause != null ? cause : ex);
						return;
					}
					catch (Throwable ex) {
						onFailure(ex);
						return;
					}
				}
				successCallback.onSuccess(adapted);
			}
			@Override
			public void onFailure(Throwable ex) {
				failureCallback.onFailure(ex);
			}
		});
	}

}
