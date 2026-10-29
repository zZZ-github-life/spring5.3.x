/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.beans.support;

import java.io.Serializable;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * {@link SortDefinition} 接口的可变实现。支持在再次设置同一属性时切换升序值。
 *
 * @author Juergen Hoeller
 * @author Jean-Pierre Pawlak
 * @since 26.05.2003
 * @see #setToggleAscendingOnProperty
 */
@SuppressWarnings("serial")
public class MutableSortDefinition implements SortDefinition, Serializable {

	private String property = "";

	private boolean ignoreCase = true;

	private boolean ascending = true;

	private boolean toggleAscendingOnProperty = false;


	/**
	 * 创建一个空的 MutableSortDefinition，通过其 Bean 属性填充。
	 * @see #setProperty
	 * @see #setIgnoreCase
	 * @see #setAscending
	 */
	public MutableSortDefinition() {
	}

	/**
	 * 复制构造函数：创建一个新的 MutableSortDefinition，镜像给定的排序定义。
	 * @param source the original sort definition
	 */
	public MutableSortDefinition(SortDefinition source) {
		this.property = source.getProperty();
		this.ignoreCase = source.isIgnoreCase();
		this.ascending = source.isAscending();
	}

	/**
	 * 为给定设置创建一个 MutableSortDefinition。
	 * @param property the property to compare
	 * @param ignoreCase whether upper and lower case in String values should be ignored
	 * @param ascending whether to sort ascending (true) or descending (false)
	 */
	public MutableSortDefinition(String property, boolean ignoreCase, boolean ascending) {
		this.property = property;
		this.ignoreCase = ignoreCase;
		this.ascending = ascending;
	}

	/**
	 * 创建一个新的 MutableSortDefinition。
	 * @param toggleAscendingOnSameProperty whether to toggle the ascending flag
	 * if the same property gets set again (that is, {@code setProperty} gets
	 * called with already set property name again).
	 */
	public MutableSortDefinition(boolean toggleAscendingOnSameProperty) {
		this.toggleAscendingOnProperty = toggleAscendingOnSameProperty;
	}


	/**
	 * 设置要比较的属性。
	 * <p>如果该属性与当前属性相同，则在 "toggleAscendingOnProperty" 启用时反转排序顺序，否则直接忽略。
	 * @see #setToggleAscendingOnProperty
	 */
	public void setProperty(String property) {
		if (!StringUtils.hasLength(property)) {
			this.property = "";
		}
		else {
			// Implicit toggling of ascending?
			if (isToggleAscendingOnProperty()) {
				this.ascending = (!property.equals(this.property) || !this.ascending);
			}
			this.property = property;
		}
	}

	@Override
	public String getProperty() {
		return this.property;
	}

	/**
	 * 设置是否忽略 String 值中的大小写。
	 */
	public void setIgnoreCase(boolean ignoreCase) {
		this.ignoreCase = ignoreCase;
	}

	@Override
	public boolean isIgnoreCase() {
		return this.ignoreCase;
	}

	/**
	 * 设置是按升序（true）还是降序（false）排序。
	 */
	public void setAscending(boolean ascending) {
		this.ascending = ascending;
	}

	@Override
	public boolean isAscending() {
		return this.ascending;
	}

	/**
	 * 设置当再次设置同一属性时（即再次以已设置的属性名调用 {@link #setProperty}）是否切换升序标志。
	 * <p>这对通过 Web 请求进行参数绑定特别有用：再次点击字段标题时，可能期望按相反顺序重新排序。
	 */
	public void setToggleAscendingOnProperty(boolean toggleAscendingOnProperty) {
		this.toggleAscendingOnProperty = toggleAscendingOnProperty;
	}

	/**
	 * 返回当再次设置同一属性时（即再次以已设置的属性名调用 {@code setProperty}）是否切换升序标志。
	 */
	public boolean isToggleAscendingOnProperty() {
		return this.toggleAscendingOnProperty;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof SortDefinition)) {
			return false;
		}
		SortDefinition otherSd = (SortDefinition) other;
		return (getProperty().equals(otherSd.getProperty()) &&
				isAscending() == otherSd.isAscending() &&
				isIgnoreCase() == otherSd.isIgnoreCase());
	}

	@Override
	public int hashCode() {
		int hashCode = getProperty().hashCode();
		hashCode = 29 * hashCode + (isIgnoreCase() ? 1 : 0);
		hashCode = 29 * hashCode + (isAscending() ? 1 : 0);
		return hashCode;
	}

}
