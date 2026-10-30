/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.util.function;

import java.util.function.Supplier;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 一个 {@link java.util.function.Supplier} 装饰器，用于缓存单例结果，并可通过 {@link #get()}（可为
 * null）和 {@link #obtain()}（null 安全）获取该结果。
 *
 * <p>{@code SingletonSupplier} 既可以通过 {@code of} 工厂方法构造，也可以通过提供默认 supplier 作
 * 为回退的构造函数构造。这对于方法引用形式的 supplier 特别有用：当方法返回 {@code null} 时回退到默
 * 认 supplier，并缓存结果。
 *
 * @author Juergen Hoeller
 * @since 5.1
 * @param <T> 该 Supplier 提供的结果类型
 */
public class SingletonSupplier<T> implements Supplier<T> {

	@Nullable
	private final Supplier<? extends T> instanceSupplier;

	@Nullable
	private final Supplier<? extends T> defaultSupplier;

	@Nullable
	private volatile T singletonInstance;


	/**
	 * 使用给定的单例实例以及当该实例为 {@code null} 时使用的默认 supplier 构建一个
	 * {@code SingletonSupplier}。
	 * @param instance 单例实例（可能为 {@code null}）
	 * @param defaultSupplier 作为回退的默认 supplier
	 */
	public SingletonSupplier(@Nullable T instance, Supplier<? extends T> defaultSupplier) {
		this.instanceSupplier = null;
		this.defaultSupplier = defaultSupplier;
		this.singletonInstance = instance;
	}

	/**
	 * 使用给定的实例 supplier 以及当该实例为 {@code null} 时使用的默认 supplier 构建一个
	 * {@code SingletonSupplier}。
	 * @param instanceSupplier 即时实例 supplier
	 * @param defaultSupplier 作为回退的默认 supplier
	 */
	public SingletonSupplier(@Nullable Supplier<? extends T> instanceSupplier, Supplier<? extends T> defaultSupplier) {
		this.instanceSupplier = instanceSupplier;
		this.defaultSupplier = defaultSupplier;
	}

	private SingletonSupplier(Supplier<? extends T> supplier) {
		this.instanceSupplier = supplier;
		this.defaultSupplier = null;
	}

	private SingletonSupplier(T singletonInstance) {
		this.instanceSupplier = null;
		this.defaultSupplier = null;
		this.singletonInstance = singletonInstance;
	}


	/**
	 * 获取此 supplier 的共享单例实例。
	 * @return 单例实例（如果没有则为 {@code null}）
	 */
	@Override
	@Nullable
	public T get() {
		T instance = this.singletonInstance;
		if (instance == null) {
			synchronized (this) {
				instance = this.singletonInstance;
				if (instance == null) {
					if (this.instanceSupplier != null) {
						instance = this.instanceSupplier.get();
					}
					if (instance == null && this.defaultSupplier != null) {
						instance = this.defaultSupplier.get();
					}
					this.singletonInstance = instance;
				}
			}
		}
		return instance;
	}

	/**
	 * 获取此 supplier 的共享单例实例。
	 * @return 单例实例（永不为 {@code null}）
	 * @throws IllegalStateException 没有实例时
	 */
	public T obtain() {
		T instance = get();
		Assert.state(instance != null, "No instance from Supplier");
		return instance;
	}


	/**
	 * 使用给定的单例实例构建一个 {@code SingletonSupplier}。
	 * @param instance 单例实例（永不为 {@code null}）
	 * @return 单例 supplier（永不为 {@code null}）
	 */
	public static <T> SingletonSupplier<T> of(T instance) {
		return new SingletonSupplier<>(instance);
	}

	/**
	 * 使用给定的单例实例构建一个 {@code SingletonSupplier}。
	 * @param instance 单例实例（可能为 {@code null}）
	 * @return 单例 supplier，如果实例为 {@code null} 则为 {@code null}
	 */
	@Nullable
	public static <T> SingletonSupplier<T> ofNullable(@Nullable T instance) {
		return (instance != null ? new SingletonSupplier<>(instance) : null);
	}

	/**
	 * 使用给定的 supplier 构建一个 {@code SingletonSupplier}。
	 * @param supplier 实例 supplier（永不为 {@code null}）
	 * @return 单例 supplier（永不为 {@code null}）
	 */
	public static <T> SingletonSupplier<T> of(Supplier<T> supplier) {
		return new SingletonSupplier<>(supplier);
	}

	/**
	 * 使用给定的 supplier 构建一个 {@code SingletonSupplier}。
	 * @param supplier 实例 supplier（可能为 {@code null}）
	 * @return 单例 supplier，如果实例 supplier 为 {@code null} 则为 {@code null}
	 */
	@Nullable
	public static <T> SingletonSupplier<T> ofNullable(@Nullable Supplier<T> supplier) {
		return (supplier != null ? new SingletonSupplier<>(supplier) : null);
	}

}
