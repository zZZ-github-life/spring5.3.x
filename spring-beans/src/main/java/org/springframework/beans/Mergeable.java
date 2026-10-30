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

package org.springframework.beans;

import org.springframework.lang.Nullable;

/**
 * 表示其值集合可以与父对象的值集合合并的对象的接口。
 *
 * @author Rob Harrop
 * @since 2.0
 * @see org.springframework.beans.factory.support.ManagedSet
 * @see org.springframework.beans.factory.support.ManagedList
 * @see org.springframework.beans.factory.support.ManagedMap
 * @see org.springframework.beans.factory.support.ManagedProperties
 */
public interface Mergeable {

	/**
	 * 此特定实例是否启用了合并？
	 */
	boolean isMergeEnabled();

	/**
	 * 将当前值集合与所提供的对象的值集合合并。
	 * <p>所提供的对象被视为父对象，被调用方的值集合中的值必须覆盖所提供的对象中的值。
	 * @param parent 要合并的对象
	 * @return 合并操作的结果
	 * @throws IllegalArgumentException 如果提供的 parent 为 {@code null}
	 * @throws IllegalStateException 如果此实例未启用合并
	 * （即 {@code mergeEnabled} 等于 {@code false}）。
	 */
	Object merge(@Nullable Object parent);

}
