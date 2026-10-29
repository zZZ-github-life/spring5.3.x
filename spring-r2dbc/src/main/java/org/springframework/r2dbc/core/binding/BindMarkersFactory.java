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

import java.util.function.Function;

import org.springframework.util.Assert;

/**
 * 此类创建新的 {@link BindMarkers} 实例，用于将参数绑定到
 * 特定的 {@link io.r2dbc.spi.Statement}。
 *
 * <p>绑定标记通常可表示为占位符和标识符。占位符用于要执行的查询中，
 * 以便底层数据库系统可以用实际值替换占位符。标识符在 R2DBC 驱动程序中
 * 用于将值绑定到绑定标记。当使用基于索引或命名的绑定标记时，
 * 标识符通常是整个绑定标记的一部分。
 *
 * @author Mark Paluch
 * @since 5.3
 * @see BindMarkers
 * @see io.r2dbc.spi.Statement
 */
@FunctionalInterface
public interface BindMarkersFactory {

	/**
	 * 创建一个新的 {@link BindMarkers} 实例。
	 */
	BindMarkers create();

	/**
	 * 返回 {@link BindMarkersFactory} 是否使用可标识的占位符：
	 * 如果仅凭 {@link BindMarker#getPlaceholder() 占位符}标识符
	 * 无法区分多个占位符，则返回 {@code false}。
	 */
	default boolean identifiablePlaceholders() {
		return true;
	}


	// Static factory methods

	/**
	 * 创建基于索引的 {@link BindMarkers}，使用索引来绑定参数。
	 * 允许自定义绑定标记占位符 {@code prefix}，
	 * 以在查询中将绑定标记表示为占位符。
	 * @param prefix bind parameter prefix that is included in
	 * {@link BindMarker#getPlaceholder()} but not the actual identifier
	 * @param beginWith the first index to use
	 * @return a {@link BindMarkersFactory} using {@code prefix} and {@code beginWith}
	 * @see io.r2dbc.spi.Statement#bindNull(int, Class)
	 * @see io.r2dbc.spi.Statement#bind(int, Object)
	 */
	static BindMarkersFactory indexed(String prefix, int beginWith) {
		Assert.notNull(prefix, "Prefix must not be null");
		return () -> new IndexedBindMarkers(prefix, beginWith);
	}

	/**
	 * 使用静态占位符创建匿名的、基于索引的绑定标记。
	 * 实例按占位符出现的顺序以序号位置进行绑定。
	 * 此实现使用与索引相关联的匿名占位符来创建基于索引的绑定标记。
	 * @param placeholder parameter placeholder
	 * @return a {@link BindMarkersFactory} using {@code placeholder}
	 * @see io.r2dbc.spi.Statement#bindNull(int, Class)
	 * @see io.r2dbc.spi.Statement#bind(int, Object)
	 */
	static BindMarkersFactory anonymous(String placeholder) {
		Assert.hasText(placeholder, "Placeholder must not be empty!");
		return new BindMarkersFactory() {
			@Override
			public BindMarkers create() {
				return new AnonymousBindMarkers(placeholder);
			}
			@Override
			public boolean identifiablePlaceholders() {
				return false;
			}
		};
	}

	/**
	 * 使用标识符创建命名的 {@link BindMarkers} 以绑定参数。
	 * 命名绑定标记可以支持 {@link BindMarkers#next(String) 名称提示}。
	 * 如果未提供 {@link BindMarkers#next(String) 提示}，命名绑定标记可以
	 * 使用计数器或随机值源来生成唯一的绑定标记。
	 * 允许自定义绑定标记占位符 {@code prefix} 和 {@code namePrefix}，
	 * 以在查询中将绑定标记表示为占位符。
	 * @param prefix bind parameter prefix that is included in
	 * {@link BindMarker#getPlaceholder()} but not the actual identifier
	 * @param namePrefix prefix for bind marker name that is included in
	 * {@link BindMarker#getPlaceholder()} and the actual identifier
	 * @param maxLength maximal length of parameter names when using name hints
	 * @return a {@link BindMarkersFactory} using {@code prefix} and {@code beginWith}
	 * @see io.r2dbc.spi.Statement#bindNull(String, Class)
	 * @see io.r2dbc.spi.Statement#bind(String, Object)
	 */
	static BindMarkersFactory named(String prefix, String namePrefix, int maxLength) {
		return named(prefix, namePrefix, maxLength, Function.identity());
	}

	/**
	 * 使用标识符创建命名的 {@link BindMarkers} 以绑定参数。
	 * 命名绑定标记支持 {@link BindMarkers#next(String) 名称提示}。
	 * 如果未提供 {@link BindMarkers#next(String) 提示}，命名绑定标记可以
	 * 使用计数器或随机值源来生成唯一的绑定标记。
	 * @param prefix bind parameter prefix that is included in
	 * {@link BindMarker#getPlaceholder()} but not the actual identifier
	 * @param namePrefix prefix for bind marker name that is included in
	 * {@link BindMarker#getPlaceholder()} and the actual identifier
	 * @param maxLength maximal length of parameter names when using name hints
	 * @param hintFilterFunction filter {@link Function} to consider
	 * database-specific limitations in bind marker/variable names such as ASCII chars only
	 * @return a {@link BindMarkersFactory} using {@code prefix} and {@code beginWith}
	 * @see io.r2dbc.spi.Statement#bindNull(String, Class)
	 * @see io.r2dbc.spi.Statement#bind(String, Object)
	 */
	static BindMarkersFactory named(String prefix, String namePrefix, int maxLength,
			Function<String, String> hintFilterFunction) {

		Assert.notNull(prefix, "Prefix must not be null");
		Assert.notNull(namePrefix, "Index prefix must not be null");
		Assert.notNull(hintFilterFunction, "Hint filter function must not be null");
		return () -> new NamedBindMarkers(prefix, namePrefix, maxLength, hintFilterFunction);
	}

}
