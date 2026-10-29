/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.beans.factory.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.Mergeable;
import org.springframework.lang.Nullable;

/**
 * 用于保存受管 List 元素的标记集合类，其中可能包含运行时 Bean 引用（将被解析为 Bean 对象）。
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 27.05.2003
 * @param <E> the element type
 */
@SuppressWarnings("serial")
public class ManagedList<E> extends ArrayList<E> implements Mergeable, BeanMetadataElement {

	@Nullable
	private Object source;

	@Nullable
	private String elementTypeName;

	private boolean mergeEnabled;


	public ManagedList() {
	}

	public ManagedList(int initialCapacity) {
		super(initialCapacity);
	}


	/**
	 * 创建一个包含任意数量元素的新实例。
	 * @param elements the elements to be contained in the list
	 * @param <E> the {@code List}'s element type
	 * @return a {@code ManagedList} containing the specified elements
	 * @since 5.3.16
	 */
	@SafeVarargs
	public static <E> ManagedList<E> of(E... elements) {
		ManagedList<E> list = new ManagedList<>();
		Collections.addAll(list, elements);
		return list;
	}

	/**
	 * 为此元数据元素设置配置源 {@code Object}。
	 * <p>对象的具体类型取决于所使用的配置机制。
	 */
	public void setSource(@Nullable Object source) {
		this.source = source;
	}

	@Override
	@Nullable
	public Object getSource() {
		return this.source;
	}

	/**
	 * 设置此 list 要使用的默认元素类型名（类名）。
	 */
	public void setElementTypeName(String elementTypeName) {
		this.elementTypeName = elementTypeName;
	}

	/**
	 * 返回此 list 要使用的默认元素类型名（类名）。
	 */
	@Nullable
	public String getElementTypeName() {
		return this.elementTypeName;
	}

	/**
	 * 设置当存在 'parent' 集合值时，是否为此集合启用合并。
	 */
	public void setMergeEnabled(boolean mergeEnabled) {
		this.mergeEnabled = mergeEnabled;
	}

	@Override
	public boolean isMergeEnabled() {
		return this.mergeEnabled;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<E> merge(@Nullable Object parent) {
		if (!this.mergeEnabled) {
			throw new IllegalStateException("Not allowed to merge when the 'mergeEnabled' property is set to 'false'");
		}
		if (parent == null) {
			return this;
		}
		if (!(parent instanceof List)) {
			throw new IllegalArgumentException("Cannot merge with object of type [" + parent.getClass() + "]");
		}
		List<E> merged = new ManagedList<>();
		merged.addAll((List<E>) parent);
		merged.addAll(this);
		return merged;
	}

}
