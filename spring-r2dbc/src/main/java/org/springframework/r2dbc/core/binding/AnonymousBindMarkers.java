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

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * 使用静态占位符的匿名、基于索引的绑定标记。
 * 实例按占位符出现的顺序以序号位置进行绑定。
 * 此实现使用与索引相关联的匿名占位符来创建基于索引的绑定标记。
 *
 * <p>注意：匿名绑定标记存在问题，因为它们必须以生成时的相同顺序
 * 出现在生成的 SQL 中。这在将来可能会对复杂的生成语句带来挑战，
 * 例如包含子查询的语句，这些语句限制了安排绑定标记的自由度。
 *
 * @author Mark Paluch
 * @since 5.3
 */
class AnonymousBindMarkers implements BindMarkers {

	private static final AtomicIntegerFieldUpdater<AnonymousBindMarkers> COUNTER_INCREMENTER =
			AtomicIntegerFieldUpdater.newUpdater(AnonymousBindMarkers.class, "counter");


	private final String placeholder;

	// access via COUNTER_INCREMENTER
	@SuppressWarnings("unused")
	private volatile int counter = 0;


	/**
	 * 根据 {@code placeholder} 创建一个新的 {@link AnonymousBindMarkers} 实例。
	 * @param placeholder parameter bind marker
	 */
	AnonymousBindMarkers(String placeholder) {
		this.placeholder = placeholder;
	}


	@Override
	public BindMarker next() {
		int index = COUNTER_INCREMENTER.getAndIncrement(this);
		return new IndexedBindMarkers.IndexedBindMarker(this.placeholder, index);
	}

}
