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

package org.springframework.cache.support;

import org.springframework.cache.Cache;
import org.springframework.lang.Nullable;

/**
 * 用于 {@link Cache} 实现的通用基类，这些实现需要先将 {@code null} 值（以及可能的其他此类特殊值）
 * 适配后再传递给底层存储。
 *
 * <p>如果配置为支持 {@code null} 值（由 {@link #isAllowNullValues()} 指示），则会透明地将给定的
 * {@code null} 用户值替换为内部的 {@link NullValue#INSTANCE}。
 *
 * @author Juergen Hoeller
 * @since 4.2.2
 */
public abstract class AbstractValueAdaptingCache implements Cache {

	private final boolean allowNullValues;


	/**
	 * 使用给定设置创建一个 {@code AbstractValueAdaptingCache}。
	 * @param allowNullValues whether to allow for {@code null} values
	 */
	protected AbstractValueAdaptingCache(boolean allowNullValues) {
		this.allowNullValues = allowNullValues;
	}


	/**
	 * 返回此缓存是否允许 {@code null} 值。
	 */
	public final boolean isAllowNullValues() {
		return this.allowNullValues;
	}

	@Override
	@Nullable
	public ValueWrapper get(Object key) {
		return toValueWrapper(lookup(key));
	}

	@Override
	@SuppressWarnings("unchecked")
	@Nullable
	public <T> T get(Object key, @Nullable Class<T> type) {
		Object value = fromStoreValue(lookup(key));
		if (value != null && type != null && !type.isInstance(value)) {
			throw new IllegalStateException(
					"Cached value is not of required type [" + type.getName() + "]: " + value);
		}
		return (T) value;
	}

	/**
	 * 在底层存储中执行实际的查找。
	 * @param key the key whose associated value is to be returned
	 * @return the raw store value for the key, or {@code null} if none
	 */
	@Nullable
	protected abstract Object lookup(Object key);


	/**
	 * 将给定值从内部存储转换为从 get 方法返回的用户值（适配 {@code null}）。
	 * @param storeValue the store value
	 * @return the value to return to the user
	 */
	@Nullable
	protected Object fromStoreValue(@Nullable Object storeValue) {
		if (this.allowNullValues && storeValue == NullValue.INSTANCE) {
			return null;
		}
		return storeValue;
	}

	/**
	 * 将传入 put 方法的给定用户值转换为内部存储中的值（适配 {@code null}）。
	 * @param userValue the given user value
	 * @return the value to store
	 */
	protected Object toStoreValue(@Nullable Object userValue) {
		if (userValue == null) {
			if (this.allowNullValues) {
				return NullValue.INSTANCE;
			}
			throw new IllegalArgumentException(
					"Cache '" + getName() + "' is configured to not allow null values but null was provided");
		}
		return userValue;
	}

	/**
	 * 用 {@link SimpleValueWrapper} 包装给定的存储值，同时也会经过 {@link #fromStoreValue} 转换。适用
	 * 于 {@link #get(Object)} 和 {@link #putIfAbsent(Object, Object)} 实现。
	 * @param storeValue the original value
	 * @return the wrapped value
	 */
	@Nullable
	protected Cache.ValueWrapper toValueWrapper(@Nullable Object storeValue) {
		return (storeValue != null ? new SimpleValueWrapper(fromStoreValue(storeValue)) : null);
	}

}
