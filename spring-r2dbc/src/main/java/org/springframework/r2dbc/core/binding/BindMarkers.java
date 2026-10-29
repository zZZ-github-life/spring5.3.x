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

/**
 * 绑定标记表示 SQL 查询中用于替换实际参数的占位符。
 * 使用绑定标记可以创建安全的查询，这样查询字符串就无需包含转义值，
 * 而是由驱动程序以适当的表示形式对参数进行编码。
 *
 * <p>{@link BindMarkers} 是有状态的，只能用于一个或多个参数的
 * 单次绑定过程。它维护绑定索引/绑定参数名称。
 *
 * @author Mark Paluch
 * @since 5.3
 * @see BindMarker
 * @see BindMarkersFactory
 * @see io.r2dbc.spi.Statement#bind
 */
@FunctionalInterface
public interface BindMarkers {

	/**
	 * 创建一个新的 {@link BindMarker}。
	 */
	BindMarker next();

	/**
	 * 创建一个接受 {@code hint} 的新的 {@link BindMarker}。
	 * 允许实现考虑、忽略或过滤名称提示，
	 * 以创建更具表现力的绑定标记。
	 * @param hint an optional name hint that can be used as part of the bind marker
	 * @return a new {@link BindMarker}
	 */
	default BindMarker next(String hint) {
		return next();
	}

}
