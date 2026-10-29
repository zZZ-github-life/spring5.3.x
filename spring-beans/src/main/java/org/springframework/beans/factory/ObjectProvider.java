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
 * @param <T> the object type
 * @see BeanFactory#getBeanProvider
 * @see org.springframework.beans.factory.annotation.Autowired
 */
public interface ObjectProvider<T> extends ObjectFactory<T>, Iterable<T> {

	/**
	 * 返回此工厂所管理对象的一个实例（可能是共享的，也可能是独立的）。
	 * <p>允许像 {@link BeanFactory#getBean(String, Object...)} 那样指定显式的构造参数。
	 * @param args arguments to use when creating a corresponding instance
	 * @return an instance of the bean
	 * @throws BeansException in case of creation errors
	 * @see #getObject()
	 */
	T getObject(Object... args) throws BeansException;

	/**
	 * 返回此工厂所管理对象的一个实例（可能是共享的，也可能是独立的）。
	 * @return an instance of the bean, or {@code null} if not available
	 * @throws BeansException in case of creation errors
	 * @see #getObject()
	 */
	@Nullable
	T getIfAvailable() throws BeansException;

	/**
	 * 返回此工厂所管理对象的一个实例（可能是共享的，也可能是独立的）。
	 * @param defaultSupplier a callback for supplying a default object
	 * if none is present in the factory
	 * @return an instance of the bean, or the supplied default object
	 * if no such bean is available
	 * @throws BeansException in case of creation errors
	 * @since 5.0
	 * @see #getIfAvailable()
	 */
	default T getIfAvailable(Supplier<T> defaultSupplier) throws BeansException {
		T dependency = getIfAvailable();
		return (dependency != null ? dependency : defaultSupplier.get());
	}

	/**
	 * 如果可用，消费此工厂所管理对象的一个实例（可能是共享的，也可能是独立的）。
	 * @param dependencyConsumer a callback for processing the target object
	 * if available (not called otherwise)
	 * @throws BeansException in case of creation errors
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
	 * @return an instance of the bean, or {@code null} if not available or
	 * not unique (i.e. multiple candidates found with none marked as primary)
	 * @throws BeansException in case of creation errors
	 * @see #getObject()
	 */
	@Nullable
	T getIfUnique() throws BeansException;

	/**
	 * 返回此工厂所管理对象的一个实例（可能是共享的，也可能是独立的）。
	 * @param defaultSupplier a callback for supplying a default object
	 * if no unique candidate is present in the factory
	 * @return an instance of the bean, or the supplied default object
	 * if no such bean is available or if it is not unique in the factory
	 * (i.e. multiple candidates found with none marked as primary)
	 * @throws BeansException in case of creation errors
	 * @since 5.0
	 * @see #getIfUnique()
	 */
	default T getIfUnique(Supplier<T> defaultSupplier) throws BeansException {
		T dependency = getIfUnique();
		return (dependency != null ? dependency : defaultSupplier.get());
	}

	/**
	 * 如果唯一，消费此工厂所管理对象的一个实例（可能是共享的，也可能是独立的）。
	 * @param dependencyConsumer a callback for processing the target object
	 * if unique (not called otherwise)
	 * @throws BeansException in case of creation errors
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
