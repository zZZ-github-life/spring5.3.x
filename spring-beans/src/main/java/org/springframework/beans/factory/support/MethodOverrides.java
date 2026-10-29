/*
 * Copyright 2002-2019 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.lang.Nullable;

/**
 * 方法覆盖的集合，用于确定 Spring IoC 容器在运行时将覆盖受管对象上的哪些方法（如果有）。
 *
 * <p>当前支持的 {@link MethodOverride} 变体是 {@link LookupOverride} 和 {@link ReplaceOverride}。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 1.1
 * @see MethodOverride
 */
public class MethodOverrides {

	private final Set<MethodOverride> overrides = new CopyOnWriteArraySet<>();


	/**
	 * 创建新的 MethodOverrides。
	 */
	public MethodOverrides() {
	}

	/**
	 * 深拷贝构造器。
	 */
	public MethodOverrides(MethodOverrides other) {
		addOverrides(other);
	}


	/**
	 * 将所有给定的方法覆盖复制到此对象中。
	 */
	public void addOverrides(@Nullable MethodOverrides other) {
		if (other != null) {
			this.overrides.addAll(other.overrides);
		}
	}

	/**
	 * 添加给定的方法覆盖。
	 */
	public void addOverride(MethodOverride override) {
		this.overrides.add(override);
	}

	/**
	 * 返回此对象包含的所有方法覆盖。
	 * @return a Set of MethodOverride objects
	 * @see MethodOverride
	 */
	public Set<MethodOverride> getOverrides() {
		return this.overrides;
	}

	/**
	 * 返回方法覆盖集合是否为空。
	 */
	public boolean isEmpty() {
		return this.overrides.isEmpty();
	}

	/**
	 * 返回给定方法的覆盖（如果有）。
	 * @param method method to check for overrides for
	 * @return the method override, or {@code null} if none
	 */
	@Nullable
	public MethodOverride getOverride(Method method) {
		MethodOverride match = null;
		for (MethodOverride candidate : this.overrides) {
			if (candidate.matches(method)) {
				match = candidate;
			}
		}
		return match;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof MethodOverrides)) {
			return false;
		}
		MethodOverrides that = (MethodOverrides) other;
		return this.overrides.equals(that.overrides);
	}

	@Override
	public int hashCode() {
		return this.overrides.hashCode();
	}

}
