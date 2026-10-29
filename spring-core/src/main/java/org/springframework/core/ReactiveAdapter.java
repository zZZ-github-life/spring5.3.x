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

package org.springframework.core;

import java.util.function.Function;

import org.reactivestreams.Publisher;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 用于在 Reactive Streams {@link Publisher} 与异步/响应式类型（如 {@code CompletableFuture}、
 * RxJava {@code Observable} 等）之间进行适配的适配器。
 *
 * <p>适配器通常通过 {@link ReactiveAdapterRegistry} 获取。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ReactiveAdapter {

	private final ReactiveTypeDescriptor descriptor;

	private final Function<Object, Publisher<?>> toPublisherFunction;

	private final Function<Publisher<?>, Object> fromPublisherFunction;


	/**
	 * 用于创建适配器的构造方法，提供将目标响应式或异步类型与 Reactive Streams Publisher 相互转换的函数。
	 *
	 * @param descriptor the reactive type descriptor
	 * @param toPublisherFunction adapter to a Publisher
	 * @param fromPublisherFunction adapter from a Publisher
	 */
	public ReactiveAdapter(ReactiveTypeDescriptor descriptor,
			Function<Object, Publisher<?>> toPublisherFunction,
			Function<Publisher<?>, Object> fromPublisherFunction) {

		Assert.notNull(descriptor, "'descriptor' is required");
		Assert.notNull(toPublisherFunction, "'toPublisherFunction' is required");
		Assert.notNull(fromPublisherFunction, "'fromPublisherFunction' is required");

		this.descriptor = descriptor;
		this.toPublisherFunction = toPublisherFunction;
		this.fromPublisherFunction = fromPublisherFunction;
	}


	/**
	 * 返回该适配器所针对的响应式类型的描述符。
	 */
	public ReactiveTypeDescriptor getDescriptor() {
		return this.descriptor;
	}

	/**
	 * {@code getDescriptor().getReactiveType()} 的快捷方式。
	 */
	public Class<?> getReactiveType() {
		return getDescriptor().getReactiveType();
	}

	/**
	 * {@code getDescriptor().isMultiValue()} 的快捷方式。
	 */
	public boolean isMultiValue() {
		return getDescriptor().isMultiValue();
	}

	/**
	 * {@code getDescriptor().isNoValue()} 的快捷方式。
	 */
	public boolean isNoValue() {
		return getDescriptor().isNoValue();
	}

	/**
	 * {@code getDescriptor().supportsEmpty()} 的快捷方式。
	 */
	public boolean supportsEmpty() {
		return getDescriptor().supportsEmpty();
	}


	/**
	 * 将给定实例适配为 Reactive Streams {@code Publisher}。
	 * @param source the source object to adapt from; if the given object is
	 * {@code null}, {@link ReactiveTypeDescriptor#getEmptyValue()} is used.
	 * @return the Publisher representing the adaptation
	 */
	@SuppressWarnings("unchecked")
	public <T> Publisher<T> toPublisher(@Nullable Object source) {
		if (source == null) {
			source = getDescriptor().getEmptyValue();
		}
		return (Publisher<T>) this.toPublisherFunction.apply(source);
	}

	/**
	 * 从给定的 Reactive Streams Publisher 进行适配。
	 * @param publisher the publisher to adapt from
	 * @return the reactive type instance representing the adapted publisher
	 */
	public Object fromPublisher(Publisher<?> publisher) {
		return this.fromPublisherFunction.apply(publisher);
	}

}
