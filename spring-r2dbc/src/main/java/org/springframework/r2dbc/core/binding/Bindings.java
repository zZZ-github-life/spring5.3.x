/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.r2dbc.core.binding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.function.Consumer;

import io.r2dbc.spi.Statement;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * 值对象，表示使用 {@link BindMarkers} 为 {@link Statement} 提供的值绑定
 * 和 {@code null} 绑定。绑定通常是不可变的。
 *
 * @author Mark Paluch
 * @since 5.3
 */
public class Bindings implements Iterable<Bindings.Binding> {

	private static final Bindings EMPTY = new Bindings();

	private final Map<BindMarker, Binding> bindings;


	/**
	 * 创建空的 {@link Bindings}。
	 */
	public Bindings() {
		this.bindings = Collections.emptyMap();
	}

	/**
	 * 根据给定的集合创建 {@link Bindings}。
	 * @param bindings a collection of {@link Binding} objects
	 */
	public Bindings(Collection<Binding> bindings) {
		Assert.notNull(bindings, "Collection must not be null");
		Map<BindMarker, Binding> mapping = CollectionUtils.newLinkedHashMap(bindings.size());
		bindings.forEach(binding -> mapping.put(binding.getBindMarker(), binding));
		this.bindings = mapping;
	}

	Bindings(Map<BindMarker, Binding> bindings) {
		this.bindings = bindings;
	}


	protected Map<BindMarker, Binding> getBindings() {
		return this.bindings;
	}

	/**
	 * 将此绑定与另一个 {@link Bindings} 对象合并，
	 * 并创建一个新的合并后的 {@link Bindings} 对象。
	 * @param other the object to merge with
	 * @return a newly merged {@link Bindings} object
	 */
	public Bindings and(Bindings other) {
		return merge(this, other);
	}

	/**
	 * 将绑定应用到 {@link BindTarget}。
	 * @param bindTarget the target to apply bindings to
	 */
	public void apply(BindTarget bindTarget) {
		Assert.notNull(bindTarget, "BindTarget must not be null");
		this.bindings.forEach((marker, binding) -> binding.apply(bindTarget));
	}

	/**
	 * 对此 {@link Bindings} 的每个绑定执行给定的操作，直到所有绑定
	 * 都被处理完毕或该操作抛出异常。操作按迭代顺序执行
	 * （如果指定了迭代顺序）。操作抛出的异常会向调用方传播。
	 * @param action the action to be performed for each {@link Binding}
	 */
	@Override
	public void forEach(Consumer<? super Binding> action) {
		this.bindings.forEach((marker, binding) -> action.accept(binding));
	}

	@Override
	public Iterator<Binding> iterator() {
		return this.bindings.values().iterator();
	}

	@Override
	public Spliterator<Binding> spliterator() {
		return this.bindings.values().spliterator();
	}


	/**
	 * 返回一个空的 {@link Bindings} 对象。
	 */
	public static Bindings empty() {
		return EMPTY;
	}

	/**
	 * 将此绑定与另一个 {@link Bindings} 对象合并，
	 * 并创建一个新的合并后的 {@link Bindings} 对象。
	 * @param left the left object to merge with
	 * @param right the right object to merge with
	 * @return a newly merged {@link Bindings} object
	 */
	public static Bindings merge(Bindings left, Bindings right) {
		Assert.notNull(left, "Left side Bindings must not be null");
		Assert.notNull(right, "Right side Bindings must not be null");
		List<Binding> result = new ArrayList<>(left.getBindings().size() + right.getBindings().size());
		result.addAll(left.getBindings().values());
		result.addAll(right.getBindings().values());
		return new Bindings(result);
	}


	/**
	 * 值对象的基类，表示值绑定或 {@code NULL} 绑定。
	 */
	public abstract static class Binding {

		private final BindMarker marker;

		protected Binding(BindMarker marker) {
			this.marker = marker;
		}

		/**
		 * 返回关联的 {@link BindMarker}。
		 */
		public BindMarker getBindMarker() {
			return this.marker;
		}

		/**
		 * 返回该绑定是否关联了值。
		 * @return {@code true} if there is a value present,
		 * otherwise {@code false} for a {@code NULL} binding
		 */
		public abstract boolean hasValue();

		/**
		 * 返回该绑定是否为空。
		 * @return {@code true} if this is a {@code NULL} binding
		 */
		public boolean isNull() {
			return !hasValue();
		}

		/**
		 * 返回绑定值。
		 * @return the value of this binding
		 * (can be {@code null} if this is a {@code NULL} binding)
		 */
		@Nullable
		public abstract Object getValue();

		/**
		 * 将绑定应用到 {@link BindTarget}。
		 * @param bindTarget the target to apply bindings to
		 */
		public abstract void apply(BindTarget bindTarget);
	}


	/**
	 * 值绑定。
	 */
	static class ValueBinding extends Binding {

		private final Object value;

		ValueBinding(BindMarker marker, Object value) {
			super(marker);
			this.value = value;
		}

		@Override
		public boolean hasValue() {
			return true;
		}

		@Override
		@NonNull
		public Object getValue() {
			return this.value;
		}

		@Override
		public void apply(BindTarget bindTarget) {
			getBindMarker().bind(bindTarget, getValue());
		}
	}


	/**
	 * {@code NULL} 绑定。
	 */
	static class NullBinding extends Binding {

		private final Class<?> valueType;

		NullBinding(BindMarker marker, Class<?> valueType) {
			super(marker);
			this.valueType = valueType;
		}

		@Override
		public boolean hasValue() {
			return false;
		}

		@Override
		@Nullable
		public Object getValue() {
			return null;
		}

		public Class<?> getValueType() {
			return this.valueType;
		}

		@Override
		public void apply(BindTarget bindTarget) {
			getBindMarker().bindNull(bindTarget, getValueType());
		}
	}

}
