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

package org.springframework.core.env;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * 表示名称/值属性对来源的抽象基类。底层 {@linkplain #getSource() 源对象}
 * 可以是封装属性的任意类型 {@code T}。示例包括 {@link java.util.Properties} 对象、
 * {@link java.util.Map} 对象、{@code ServletContext} 和 {@code ServletConfig} 对象
 * （用于访问初始化参数）。浏览 {@code PropertySource} 类型层次结构可查看所提供的实现。
 *
 * <p>{@code PropertySource} 对象通常不会单独使用，而是通过 {@link PropertySources} 对象使用，
 * 该对象聚合属性源，并与 {@link PropertyResolver} 实现配合，
 * 在 {@code PropertySources} 集合中执行基于优先级的搜索。
 *
 * <p>{@code PropertySource} 的身份并非基于所封装属性的内容确定，
 * 而是仅基于 {@code PropertySource} 的 {@link #getName() 名称}确定。
 * 这在集合上下文中操作 {@code PropertySource} 对象时很有用。
 * 详情请参见 {@link MutablePropertySources} 中的操作以及
 * {@link #named(String)} 和 {@link #toString()} 方法。
 *
 * <p>请注意，在使用
 * &#64;{@link org.springframework.context.annotation.Configuration Configuration} 类时，
 * &#64;{@link org.springframework.context.annotation.PropertySource PropertySource} 注解
 * 提供了一种便捷且声明式的方式，用于向所在 {@code Environment} 添加属性源。
 *
 * @author Chris Beams
 * @since 3.1
 * @param <T> 源类型
 * @see PropertySources
 * @see PropertyResolver
 * @see PropertySourcesPropertyResolver
 * @see MutablePropertySources
 * @see org.springframework.context.annotation.PropertySource
 */
public abstract class PropertySource<T> {

	protected final Log logger = LogFactory.getLog(getClass());

	protected final String name;

	protected final T source;


	/**
	 * 使用给定的名称和源对象创建一个新的 {@code PropertySource}。
	 * @param name 关联的名称
	 * @param source 源对象
	 */
	public PropertySource(String name, T source) {
		Assert.hasText(name, "Property source name must contain at least one character");
		Assert.notNull(source, "Property source must not be null");
		this.name = name;
		this.source = source;
	}

	/**
	 * 使用给定的名称并以一个新的 {@code Object} 实例作为底层源，创建一个新的 {@code PropertySource}。
	 * <p>在测试场景中通常很有用，例如创建从不查询实际源、
	 * 而是返回硬编码值的匿名实现。
	 */
	@SuppressWarnings("unchecked")
	public PropertySource(String name) {
		this(name, (T) new Object());
	}


	/**
	 * 返回此 {@code PropertySource} 的名称。
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * 返回此 {@code PropertySource} 的底层源对象。
	 */
	public T getSource() {
		return this.source;
	}

	/**
	 * 返回此 {@code PropertySource} 是否包含给定的名称。
	 * <p>此实现只是检查 {@link #getProperty(String)} 是否返回 {@code null}。
	 * 子类可以在可能的情况下实现更高效的算法。
	 * @param name 要查找的属性名
	 */
	public boolean containsProperty(String name) {
		return (getProperty(name) != null);
	}

	/**
	 * 返回与给定名称关联的值，如果未找到则返回 {@code null}。
	 * @param name 要查找的属性
	 * @see PropertyResolver#getRequiredProperty(String)
	 */
	@Nullable
	public abstract Object getProperty(String name);


	/**
	 * 如果满足以下条件，则此 {@code PropertySource} 对象等于给定对象：
	 * <ul>
	 * <li>它们是同一个实例
	 * <li>两个对象的 {@code name} 属性相等
	 * </ul>
	 * <p>除 {@code name} 外，不会评估任何其他属性。
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof PropertySource &&
				ObjectUtils.nullSafeEquals(getName(), ((PropertySource<?>) other).getName())));
	}

	/**
	 * 返回由此 {@code PropertySource} 对象的 {@code name} 属性派生出的哈希码。
	 */
	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(getName());
	}

	/**
	 * 如果当前日志级别不包含 debug，则生成简洁输出（类型和名称）。
	 * 如果启用了 debug，则生成详细输出，包括 PropertySource 实例的哈希码
	 * 以及每个名称/值属性对。
	 * <p>这种可变的详细程度很有用，因为诸如系统属性或环境变量之类的属性源
	 * 可能包含任意数量的属性对，从而可能导致难以阅读异常和日志消息。
	 * @see Log#isDebugEnabled()
	 */
	@Override
	public String toString() {
		if (logger.isDebugEnabled()) {
			return getClass().getSimpleName() + "@" + System.identityHashCode(this) +
					" {name='" + getName() + "', properties=" + getSource() + "}";
		}
		else {
			return getClass().getSimpleName() + " {name='" + getName() + "'}";
		}
	}


	/**
	 * 返回一个仅用于集合比较目的的 {@code PropertySource} 实现。
	 * <p>主要用于内部使用，但给定一组 {@code PropertySource} 对象时，可以按如下方式使用：
	 * <pre class="code">
	 * {@code List<PropertySource<?>> sources = new ArrayList<PropertySource<?>>();
	 * sources.add(new MapPropertySource("sourceA", mapA));
	 * sources.add(new MapPropertySource("sourceB", mapB));
	 * assert sources.contains(PropertySource.named("sourceA"));
	 * assert sources.contains(PropertySource.named("sourceB"));
	 * assert !sources.contains(PropertySource.named("sourceC"));
	 * }</pre>
	 * 如果调用了除 {@code equals(Object)}、{@code hashCode()} 和 {@code toString()}
	 * 之外的任何方法，返回的 {@code PropertySource} 将抛出 {@code UnsupportedOperationException}。
	 * @param name 要创建并返回的比较用 {@code PropertySource} 的名称。
	 */
	public static PropertySource<?> named(String name) {
		return new ComparisonPropertySource(name);
	}


	/**
	 * {@code PropertySource}，用于在实际属性源无法在应用上下文创建时提前初始化的场景中作为占位符。
	 * 例如，基于 {@code ServletContext} 的属性源必须等到 {@code ServletContext} 对象
	 * 对其所在 {@code ApplicationContext} 可用之后才能初始化。
	 * 在这种情况下，应使用存根来占据属性源预期的默认位置/顺序，
	 * 然后在上下文刷新期间将其替换。
	 * @see org.springframework.context.support.AbstractApplicationContext#initPropertySources()
	 * @see org.springframework.web.context.support.StandardServletEnvironment
	 * @see org.springframework.web.context.support.ServletContextPropertySource
	 */
	public static class StubPropertySource extends PropertySource<Object> {

		public StubPropertySource(String name) {
			super(name, new Object());
		}

		/**
		 * 始终返回 {@code null}。
		 */
		@Override
		@Nullable
		public String getProperty(String name) {
			return null;
		}
	}


	/**
	 * 用于集合比较目的的 {@code PropertySource} 实现。
	 *
	 * @see PropertySource#named(String)
	 */
	static class ComparisonPropertySource extends StubPropertySource {

		private static final String USAGE_ERROR =
				"ComparisonPropertySource instances are for use with collection comparison only";

		public ComparisonPropertySource(String name) {
			super(name);
		}

		@Override
		public Object getSource() {
			throw new UnsupportedOperationException(USAGE_ERROR);
		}

		@Override
		public boolean containsProperty(String name) {
			throw new UnsupportedOperationException(USAGE_ERROR);
		}

		@Override
		@Nullable
		public String getProperty(String name) {
			throw new UnsupportedOperationException(USAGE_ERROR);
		}
	}

}
