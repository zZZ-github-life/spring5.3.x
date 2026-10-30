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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 抽象类，将参数类型为 S 的 {@link Future} 适配为参数类型为 T 的 {@code Future}。所有方法都委托给
 * 被适配对象，其中 {@link #get()} 和 {@link #get(long, TimeUnit)} 会对被适配对象的结果调用
 * {@link #adapt(Object)}。
 *
 * @author Arjen Poutsma
 * @since 4.0
 * @param <T> 该 {@code Future} 的类型
 * @param <S> 被适配对象的 {@code Future} 的类型
 */
public abstract class FutureAdapter<T, S> implements Future<T> {

	private final Future<S> adaptee;

	@Nullable
	private Object result;

	private State state = State.NEW;

	private final Object mutex = new Object();


	/**
	 * 使用给定的被适配对象构造一个新的 {@code FutureAdapter}。
	 * @param adaptee 要委托的 future
	 */
	protected FutureAdapter(Future<S> adaptee) {
		Assert.notNull(adaptee, "Delegate must not be null");
		this.adaptee = adaptee;
	}


	/**
	 * 返回被适配对象。
	 */
	protected Future<S> getAdaptee() {
		return this.adaptee;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return this.adaptee.cancel(mayInterruptIfRunning);
	}

	@Override
	public boolean isCancelled() {
		return this.adaptee.isCancelled();
	}

	@Override
	public boolean isDone() {
		return this.adaptee.isDone();
	}

	@Override
	@Nullable
	public T get() throws InterruptedException, ExecutionException {
		return adaptInternal(this.adaptee.get());
	}

	@Override
	@Nullable
	public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return adaptInternal(this.adaptee.get(timeout, unit));
	}

	@SuppressWarnings("unchecked")
	@Nullable
	final T adaptInternal(S adapteeResult) throws ExecutionException {
		synchronized (this.mutex) {
			switch (this.state) {
				case SUCCESS:
					return (T) this.result;
				case FAILURE:
					Assert.state(this.result instanceof ExecutionException, "Failure without exception");
					throw (ExecutionException) this.result;
				case NEW:
					try {
						T adapted = adapt(adapteeResult);
						this.result = adapted;
						this.state = State.SUCCESS;
						return adapted;
					}
					catch (ExecutionException ex) {
						this.result = ex;
						this.state = State.FAILURE;
						throw ex;
					}
					catch (Throwable ex) {
						ExecutionException execEx = new ExecutionException(ex);
						this.result = execEx;
						this.state = State.FAILURE;
						throw execEx;
					}
				default:
					throw new IllegalStateException();
			}
		}
	}

	/**
	 * 将给定被适配对象的结果适配为 T。
	 * @return 适配后的结果
	 */
	@Nullable
	protected abstract T adapt(S adapteeResult) throws ExecutionException;


	private enum State {NEW, SUCCESS, FAILURE}

}
