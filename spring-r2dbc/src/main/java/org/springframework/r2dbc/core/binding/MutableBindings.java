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

package org.springframework.r2dbc.core.binding;

import java.util.LinkedHashMap;

import io.r2dbc.spi.Statement;

import org.springframework.util.Assert;

/**
 * {@link Bindings} 的可变扩展，用于使用 {@link BindMarkers} 为 {@link Statement}
 * 提供值绑定和 {@code null} 绑定。
 *
 * @author Mark Paluch
 * @since 5.3
 */
public class MutableBindings extends Bindings {

	private final BindMarkers markers;


	/**
	 * 创建新的 {@link MutableBindings}。
	 * @param markers the {@link BindMarkers} to wrap
	 */
	public MutableBindings(BindMarkers markers) {
		super(new LinkedHashMap<>());
		Assert.notNull(markers, "BindMarkers must not be null");
		this.markers = markers;
	}


	/**
	 * 获取下一个 {@link BindMarker}。
	 * 递增 {@link BindMarkers} 的状态
	 * @return the next {@link BindMarker}
	 */
	public BindMarker nextMarker() {
		return this.markers.next();
	}

	/**
	 * 获取带名称 {@code hint} 的下一个 {@link BindMarker}。
	 * 递增 {@link BindMarkers} 的状态。
	 * @param hint name hint
	 * @return the next {@link BindMarker}
	 */
	public BindMarker nextMarker(String hint) {
		return this.markers.next(hint);
	}

	/**
	 * 将值绑定到 {@link BindMarker}。
	 * @param marker the {@link BindMarker} to bind to
	 * @param value the value to bind
	 */
	public MutableBindings bind(BindMarker marker, Object value) {
		Assert.notNull(marker, "BindMarker must not be null");
		Assert.notNull(value, "Value must not be null");
		getBindings().put(marker, new ValueBinding(marker, value));
		return this;
	}

	/**
	 * 绑定一个值并返回相关的 {@link BindMarker}。
	 * 递增 {@link BindMarkers} 的状态。
	 * @param value the value to bind
	 */
	public BindMarker bind(Object value) {
		Assert.notNull(value, "Value must not be null");
		BindMarker marker = nextMarker();
		getBindings().put(marker, new ValueBinding(marker, value));
		return marker;
	}

	/**
	 * 将 {@code NULL} 值绑定到 {@link BindMarker}。
	 * @param marker the {@link BindMarker} to bind to
	 * @param valueType the value type
	 */
	public MutableBindings bindNull(BindMarker marker, Class<?> valueType) {
		Assert.notNull(marker, "BindMarker must not be null");
		Assert.notNull(valueType, "Value type must not be null");
		getBindings().put(marker, new NullBinding(marker, valueType));
		return this;
	}

	/**
	 * 绑定一个 {@code NULL} 值并返回相关的 {@link BindMarker}。
	 * 递增 {@link BindMarkers} 的状态。
	 * @param valueType the value type
	 */
	public BindMarker bindNull(Class<?> valueType) {
		Assert.notNull(valueType, "Value type must not be null");
		BindMarker marker = nextMarker();
		getBindings().put(marker, new NullBinding(marker, valueType));
		return marker;
	}

}
