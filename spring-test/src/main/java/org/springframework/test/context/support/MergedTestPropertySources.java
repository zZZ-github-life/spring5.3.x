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

package org.springframework.test.context.support;

import java.util.Arrays;

import org.springframework.core.style.ToStringCreator;
import org.springframework.lang.Nullable;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.Assert;

/**
 * {@code MergedTestPropertySources} 封装了在测试类及其所有父类和外围类上通过 {@link TestPropertySource @TestPropertySource} 声明的<em>合并</em>属性源。
 *
 * @author Sam Brannen
 * @since 4.1
 * @see TestPropertySource
 */
class MergedTestPropertySources {

	private static final MergedTestPropertySources empty = new MergedTestPropertySources(new String[0], new String[0]);

	private final String[] locations;

	private final String[] properties;


	/**
	 * 用于创建<em>空</em> {@code MergedTestPropertySources} 实例的工厂。
	 */
	static MergedTestPropertySources empty() {
		return empty;
	}


	/**
	 * 使用所提供的 {@code locations} 和 {@code properties} 创建 {@code MergedTestPropertySources} 实例。
	 * @param locations the resource locations of properties files; may be
	 * empty but never {@code null}
	 * @param properties the properties in the form of {@code key=value} pairs;
	 * may be empty but never {@code null}
	 */
	MergedTestPropertySources(String[] locations, String[] properties) {
		Assert.notNull(locations, "The locations array must not be null");
		Assert.notNull(properties, "The properties array must not be null");
		this.locations = locations;
		this.properties = properties;
	}

	/**
	 * 获取属性文件的资源位置。
	 * @see TestPropertySource#locations()
	 */
	String[] getLocations() {
		return this.locations;
	}

	/**
	 * 获取<em>键值</em>对形式的属性。
	 * @see TestPropertySource#properties()
	 */
	String[] getProperties() {
		return this.properties;
	}

	/**
	 * 通过比较两个对象的{@linkplain #getLocations() 位置}和{@linkplain #getProperties() 属性}，确定所提供的对象是否等于此 {@code MergedTestPropertySources} 实例。
	 * @since 5.3
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || other.getClass() != getClass()) {
			return false;
		}

		MergedTestPropertySources that = (MergedTestPropertySources) other;
		if (!Arrays.equals(this.locations, that.locations)) {
			return false;
		}
		if (!Arrays.equals(this.properties, that.properties)) {
			return false;
		}

		return true;
	}

	/**
	 * 为此 {@code MergedTestPropertySources} 实例的所有属性生成唯一的哈希码。
	 * @since 5.3
	 */
	@Override
	public int hashCode() {
		int result = Arrays.hashCode(this.locations);
		result = 31 * result + Arrays.hashCode(this.properties);
		return result;
	}

	/**
	 * 提供此 {@code MergedTestPropertySources} 实例的字符串表示。
	 * @since 5.3
	 */
	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("locations", Arrays.toString(this.locations))
				.append("properties", Arrays.toString(this.properties))
				.toString();
	}

}
