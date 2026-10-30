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

package org.springframework.beans.factory;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.beans.BeansException;
import org.springframework.lang.Nullable;

/**
 * 专为注入点设计的 {@link ObjectFactory} 变体，支持以编程方式表达可选性以及对非唯一情况的宽松处理。
 *
 *
 * <p>从 5.1 开始，该接口继承 {@link Iterable} 并提供 {@link Stream} 支持。因此它可用于 {@code for}
 * 循环，提供 {@link #forEach} 迭代，并允许以集合风格通过 {@link #stream} 访问。
 *
 * @author Juergen Hoeller
 * @since 4.3
 * @param <T> 对象类型
 * @see BeanFactory#getBeanProvider
 * @see org.springframework.beans.factory.annotation.Autowired
 */
public interface ObjectProvider<T> extends ObjectFactory<T>, Iterable<T> {

	/**
	 * 返回此工厂所管理对象的一个实例（可能是共享的，也可能是独立的）。
	 * <p>允许像 {@link BeanFactory#getBean(String, Object...)} 那样指定显式的构造参数。
	 * @param args 创建相应实例时使用的参数
	 * @return Bean 的实例
	 * @throws BeansException 如果发生创建错误
	 * @see #getObject()
	 */
	T getObject(Object... args) throws BeansException;

	/**
	 * 返回此工厂所管理对象的一个实例（可能是共享的，也可能是独立的）。
	 * @return Bean 的实例；若不可用则为 {@code null}
	 * @throws BeansException 如果发生创建错误
	 * @see #getObject()
	 */
	@Nullable
	T getIfAvailable() throws BeansException;

	/**
	 * 返回此工厂所管理对象的一个实例（可能是共享的，也可能是独立的）。
	 * @param defaultSupplier 当工厂中不存在对象时用于提供默认对象的回调
	 * @return Bean 的实例；若没有这样的 Bean 可用则为提供的默认对象
	 * @throws BeansException 如果发生创建错误
	 * @since 5.0
	 * @see #getIfAvailable()
	 */
	default T getIfAvailable(Supplier<T> defaultSupplier) throws BeansException {
		T dependency = getIfAvailable();
		return (dependency != null ? dependency : defaultSupplier.get());
	}

	/**
	 * 如果可用，消费此工厂所管理对象的一个实例（可能是共享的，也可能是独立的）。
	 * @param dependencyConsumer 用于处理目标对象（若可用）的回调（否则不会被调用）
	 * @throws BeansException 如果发生创建错误
	 * @since 5.0
	 * @see #getIfAvailable()
	 */
	default void ifAvailable(Consumer<T> dependencyConsumer) throws BeansException {
		T dependency = getIfAvailable();
		if (dependency != null) {
			dependencyConsumer.accept(dependency);
		}
	}

	/**
	 * 返回此工厂所管理对象的一个实例（可能是共享的，也可能是独立的）。
	 * @return Bean 的实例；若不可用或不唯一
	 * （即找到多个候选且均未标记为 primary）则为 {@code null}
	 * @throws BeansException 如果发生创建错误
	 * @see #getObject()
	 */
	@Nullable
	T getIfUnique() throws BeansException;

	/**
	 * 返回此工厂所管理对象的一个实例（可能是共享的，也可能是独立的）。
	 * @param defaultSupplier 当工厂中不存在唯一候选时用于提供默认对象的回调
	 * @return Bean 的实例；若没有这样的 Bean 可用或它在工厂中不唯一
	 * （即找到多个候选且均未标记为 primary）则为提供的默认对象
	 * @throws BeansException 如果发生创建错误
	 * @since 5.0
	 * @see #getIfUnique()
	 */
	default T getIfUnique(Supplier<T> defaultSupplier) throws BeansException {
		T dependency = getIfUnique();
		return (dependency != null ? dependency : defaultSupplier.get());
	}

	/**
	 * 如果唯一，消费此工厂所管理对象的一个实例（可能是共享的，也可能是独立的）。
	 * @param dependencyConsumer 用于处理目标对象（若唯一）的回调（否则不会被调用）
	 * @throws BeansException 如果发生创建错误
	 * @since 5.0
	 * @see #getIfAvailable()
	 */
	default void ifUnique(Consumer<T> dependencyConsumer) throws BeansException {
		T dependency = getIfUnique();
		if (dependency != null) {
			dependencyConsumer.accept(dependency);
		}
	}

	/**
	 * 返回遍历所有匹配对象实例的 {@link Iterator}，不保证特定顺序（但通常按注册顺序）。
	 * @since 5.1
	 * @see #stream()
	 */
	@Override
	default Iterator<T> iterator() {
		return stream().iterator();
	}

	/**
	 * 返回遍历所有匹配对象实例的顺序 {@link Stream}，不保证特定顺序（但通常按注册顺序）。
	 * @since 5.1
	 * @see #iterator()
	 * @see #orderedStream()
	 */
	default Stream<T> stream() {
		throw new UnsupportedOperationException("Multi element access not supported");
	}

	/**
	 * 返回遍历所有匹配对象实例的顺序 {@link Stream}，并按工厂的通用顺序比较器预先排序。
	 * <p>在标准的 Spring 应用上下文中，将按照 {@link org.springframework.core.Ordered} 约定排序；对于
	 * 基于注解的配置，还会考虑 {@link org.springframework.core.annotation.Order} 注解，这与 list/array
	 * 类型的多元素注入点类似。
	 * @since 5.1
	 * @see #stream()
	 * @see org.springframework.core.OrderComparator
	 */
	default Stream<T> orderedStream() {
		throw new UnsupportedOperationException("Ordered element access not supported");
	}

}
