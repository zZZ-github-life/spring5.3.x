/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.beans.BeanMetadataElement;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * 表示 IoC 容器对受管对象上某个方法的覆盖的对象。
 *
 * <p>请注意，覆盖机制<em>并非</em>用于插入横切代码的通用手段：请为此使用 AOP。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 1.1
 */
public abstract class MethodOverride implements BeanMetadataElement {

	private final String methodName;

	private boolean overloaded = true;

	@Nullable
	private Object source;


	/**
	 * 为给定方法构造一个新的覆盖。
	 * @param methodName the name of the method to override
	 */
	protected MethodOverride(String methodName) {
		Assert.notNull(methodName, "Method name must not be null");
		this.methodName = methodName;
	}


	/**
	 * 返回要覆盖的方法的名称。
	 */
	public String getMethodName() {
		return this.methodName;
	}

	/**
	 * 设置被覆盖的方法是否<em>重载</em>（即是否需要执行参数类型匹配以区分同名方法）。
	 * <p>默认值为 {@code true}；可以切换为 {@code false} 以优化运行时性能。
	 */
	protected void setOverloaded(boolean overloaded) {
		this.overloaded = overloaded;
	}

	/**
	 * 返回被覆盖的方法是否<em>重载</em>（即是否需要执行参数类型匹配以区分同名方法）。
	 */
	protected boolean isOverloaded() {
		return this.overloaded;
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
	 * 子类必须重写此方法，以指示它们是否<em>匹配</em>给定方法。这允许检查参数列表以及方法名。
	 * @param method the method to check
	 * @return whether this override matches the given method
	 */
	public abstract boolean matches(Method method);


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof MethodOverride)) {
			return false;
		}
		MethodOverride that = (MethodOverride) other;
		return (ObjectUtils.nullSafeEquals(this.methodName, that.methodName) &&
				ObjectUtils.nullSafeEquals(this.source, that.source));
	}

	@Override
	public int hashCode() {
		int hashCode = ObjectUtils.nullSafeHashCode(this.methodName);
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.source);
		return hashCode;
	}

}
