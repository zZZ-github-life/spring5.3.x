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

package org.springframework.core;

import java.util.function.Supplier;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 描述响应式类型的语义，包括对 {@link #isMultiValue()}、{@link #isNoValue()} 和
 * {@link #supportsEmpty()} 的布尔检查。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public final class ReactiveTypeDescriptor {

	private final Class<?> reactiveType;

	private final boolean multiValue;

	private final boolean noValue;

	@Nullable
	private final Supplier<?> emptyValueSupplier;

	private final boolean deferred;


	private ReactiveTypeDescriptor(Class<?> reactiveType, boolean multiValue, boolean noValue,
			@Nullable Supplier<?> emptySupplier) {

		this(reactiveType, multiValue, noValue, emptySupplier, true);
	}

	private ReactiveTypeDescriptor(Class<?> reactiveType, boolean multiValue, boolean noValue,
			@Nullable Supplier<?> emptySupplier, boolean deferred) {

		Assert.notNull(reactiveType, "'reactiveType' must not be null");
		this.reactiveType = reactiveType;
		this.multiValue = multiValue;
		this.noValue = noValue;
		this.emptyValueSupplier = emptySupplier;
		this.deferred = deferred;
	}


	/**
	 * 返回此描述符对应的响应式类型。
	 */
	public Class<?> getReactiveType() {
		return this.reactiveType;
	}

	/**
	 * 如果该响应式类型可以产生多于 1 个值，因而非常适合适配为 {@code Flux}，则返回 {@code true}。返回
	 * {@code false} 意味着该响应式类型最多只能产生 1 个值，因而非常适合适配为 {@code Mono}。
	 */
	public boolean isMultiValue() {
		return this.multiValue;
	}

	/**
	 * 如果该响应式类型不产生任何值，仅提供完成和错误信号，则返回 {@code true}。
	 */
	public boolean isNoValue() {
		return this.noValue;
	}

	/**
	 * 如果该响应式类型可以在没有值的情况下完成，则返回 {@code true}。
	 */
	public boolean supportsEmpty() {
		return (this.emptyValueSupplier != null);
	}

	/**
	 * 返回底层响应式或异步类型的空值实例。使用此类型意味着 {@link #supportsEmpty()} 为 {@code true}。
	 */
	public Object getEmptyValue() {
		Assert.state(this.emptyValueSupplier != null, "Empty values not supported");
		return this.emptyValueSupplier.get();
	}

	/**
	 * 底层操作是否是延迟的、需要显式启动（例如通过订阅或类似方式），还是在消费者无法控制的情况下被触发。
	 *
	 * @since 5.2.7
	 */
	public boolean isDeferred() {
		return this.deferred;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		return this.reactiveType.equals(((ReactiveTypeDescriptor) other).reactiveType);
	}

	@Override
	public int hashCode() {
		return this.reactiveType.hashCode();
	}


	/**
	 * 可产生 0..N 个值的响应式类型的描述符。
	 * @param type 响应式类型
	 * @param emptySupplier 响应式类型的空值实例的供应器
	 */
	public static ReactiveTypeDescriptor multiValue(Class<?> type, Supplier<?> emptySupplier) {
		return new ReactiveTypeDescriptor(type, true, false, emptySupplier);
	}

	/**
	 * 可产生 0..1 个值的响应式类型的描述符。
	 * @param type 响应式类型
	 * @param emptySupplier 响应式类型的空值实例的供应器
	 */
	public static ReactiveTypeDescriptor singleOptionalValue(Class<?> type, Supplier<?> emptySupplier) {
		return new ReactiveTypeDescriptor(type, false, false, emptySupplier);
	}

	/**
	 * 必须产生 1 个值才能完成的响应式类型的描述符。
	 * @param type 响应式类型
	 */
	public static ReactiveTypeDescriptor singleRequiredValue(Class<?> type) {
		return new ReactiveTypeDescriptor(type, false, false, null);
	}

	/**
	 * 不产生任何值的响应式类型的描述符。
	 * @param type 响应式类型
	 * @param emptySupplier 响应式类型的空值实例的供应器
	 */
	public static ReactiveTypeDescriptor noValue(Class<?> type, Supplier<?> emptySupplier) {
		return new ReactiveTypeDescriptor(type, false, true, emptySupplier);
	}

	/**
	 * 与 {@link #singleOptionalValue(Class, Supplier)} 相同，但用于非延迟的异步类型，例如
	 * {@link java.util.concurrent.CompletableFuture}。
	 * @param type 响应式类型
	 * @param emptySupplier 响应式类型的空值实例的供应器
	 * @since 5.2.7
	 */
	public static ReactiveTypeDescriptor nonDeferredAsyncValue(Class<?> type, Supplier<?> emptySupplier) {
		return new ReactiveTypeDescriptor(type, false, false, emptySupplier, false);
	}

}
