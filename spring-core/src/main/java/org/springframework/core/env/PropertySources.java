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

package org.springframework.core.env;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.lang.Nullable;

/**
 * 包含一个或多个 {@link PropertySource} 对象的持有者。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see PropertySource
 */
public interface PropertySources extends Iterable<PropertySource<?>> {

	/**
	 * 返回包含各属性源的顺序 {@link Stream}。
	 * @since 5.1
	 */
	default Stream<PropertySource<?>> stream() {
		return StreamSupport.stream(spliterator(), false);
	}

	/**
	 * 返回是否包含具有给定名称的属性源。
	 * @param name the {@linkplain PropertySource#getName() name of the property source} to find
	 */
	boolean contains(String name);

	/**
	 * 返回具有给定名称的属性源，如果未找到则返回 {@code null}。
	 * @param name the {@linkplain PropertySource#getName() name of the property source} to find
	 */
	@Nullable
	PropertySource<?> get(String name);

}
