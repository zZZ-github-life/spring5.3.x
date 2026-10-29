/*
 * Copyright 2002-2022 the original author or authors.
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

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 一个 {@link ListenableFuture}，其值可通过 {@link #set(Object)} 或
 * {@link #setException(Throwable)} 设置。它也可能被取消。
 *
 * <p>灵感来自 {@code com.google.common.util.concurrent.SettableFuture}。
 *
 * @author Mattias Severson
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.1
 * @param <T> the result type returned by this Future's {@code get} method
 */
public class SettableListenableFuture<T> implements ListenableFuture<T> {

	private static final Callable<Object> DUMMY_CALLABLE = () -> {
		throw new IllegalStateException("Should never be called");
	};


	private final SettableTask<T> settableTask = new SettableTask<>();


	/**
	 * 设置此 future 的值。如果值设置成功，此方法返回 {@code true}；如果该 future 已被设置或已取消，则
	 * 返回 {@code false}。
	 * @param value the value that will be set
	 * @return {@code true} if the value was successfully set, else {@code false}
	 */
	public boolean set(@Nullable T value) {
		return this.settableTask.setResultValue(value);
	}

	/**
	 * 设置此 future 的异常。如果异常设置成功，此方法返回 {@code true}；如果该 future 已被设置或已取消，
	 * 则返回 {@code false}。
	 * @param exception the value that will be set
	 * @return {@code true} if the exception was successfully set, else {@code false}
	 */
	public boolean setException(Throwable exception) {
		Assert.notNull(exception, "Exception must not be null");
		return this.settableTask.setExceptionResult(exception);
	}


	@Override
	public void addCallback(ListenableFutureCallback<? super T> callback) {
		this.settableTask.addCallback(callback);
	}

	@Override
	public void addCallback(SuccessCallback<? super T> successCallback, FailureCallback failureCallback) {
		this.settableTask.addCallback(successCallback, failureCallback);
	}

	@Override
	public CompletableFuture<T> completable() {
		return this.settableTask.completable();
	}


	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		boolean cancelled = this.settableTask.cancel(mayInterruptIfRunning);
		if (cancelled && mayInterruptIfRunning) {
			interruptTask();
		}
		return cancelled;
	}

	@Override
	public boolean isCancelled() {
		return this.settableTask.isCancelled();
	}

	@Override
	public boolean isDone() {
		return this.settableTask.isDone();
	}

	/**
	 * 获取值。
	 * <p>如果已通过 {@link #set(Object)} 设置值，此方法返回该值；如果已通过
	 * {@link #setException(Throwable)} 设置异常，则抛出
	 * {@link java.util.concurrent.ExecutionException}；如果该 future 已被取消，则抛出
	 * {@link java.util.concurrent.CancellationException}。
	 * @return the value associated with this future
	 */
	@Nullable
	@Override
	public T get() throws InterruptedException, ExecutionException {
		return this.settableTask.get();
	}

	/**
	 * 获取值。
	 * <p>如果已通过 {@link #set(Object)} 设置值，此方法返回该值；如果已通过
	 * {@link #setException(Throwable)} 设置异常，则抛出
	 * {@link java.util.concurrent.ExecutionException}；如果该 future 已被取消，则抛出
	 * {@link java.util.concurrent.CancellationException}。
	 * @param timeout the maximum time to wait
	 * @param unit the unit of the timeout argument
	 * @return the value associated with this future
	 */
	@Nullable
	@Override
	public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return this.settableTask.get(timeout, unit);
	}

	/**
	 * 子类可以重写此方法以实现对 future 计算的打断。成功调用 {@link #cancel(boolean) cancel(true)} 时
	 * 会自动调用此方法。
	 * <p>默认实现为空。
	 */
	protected void interruptTask() {
	}


	private static class SettableTask<T> extends ListenableFutureTask<T> {

		@Nullable
		private volatile Thread completingThread;

		@SuppressWarnings("unchecked")
		public SettableTask() {
			super((Callable<T>) DUMMY_CALLABLE);
		}

		public boolean setResultValue(@Nullable T value) {
			set(value);
			return checkCompletingThread();
		}

		public boolean setExceptionResult(Throwable exception) {
			setException(exception);
			return checkCompletingThread();
		}

		@Override
		protected void done() {
			if (!isCancelled()) {
				// Implicitly invoked by set/setException: store current thread for
				// determining whether the given result has actually triggered completion
				// (since FutureTask.set/setException unfortunately don't expose that)
				this.completingThread = Thread.currentThread();
			}
			super.done();
		}

		private boolean checkCompletingThread() {
			boolean check = (this.completingThread == Thread.currentThread());
			if (check) {
				this.completingThread = null;  // only first match actually counts
			}
			return check;
		}
	}

}
