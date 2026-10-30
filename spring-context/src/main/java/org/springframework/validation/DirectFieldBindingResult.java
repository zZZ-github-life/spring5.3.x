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

package org.springframework.validation;

import org.springframework.beans.ConfigurablePropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.lang.Nullable;

/**
 * Errors 和 BindingResult 接口的特殊实现，支持在值对象上注册和评估绑定错误。直接访问字段，而不经过
 * JavaBean getter。
 *
 * <p>从 Spring 4.1 开始，此实现能够遍历嵌套字段。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see DataBinder#getBindingResult()
 * @see DataBinder#initDirectFieldAccess()
 * @see BeanPropertyBindingResult
 */
@SuppressWarnings("serial")
public class DirectFieldBindingResult extends AbstractPropertyBindingResult {

	@Nullable
	private final Object target;

	private final boolean autoGrowNestedPaths;

	@Nullable
	private transient ConfigurablePropertyAccessor directFieldAccessor;


	/**
	 * 创建一个新的 DirectFieldBindingResult 实例。
	 * @param target 要绑定到的目标对象。
	 * @param objectName 目标对象的名称。
	 */
	public DirectFieldBindingResult(@Nullable Object target, String objectName) {
		this(target, objectName, true);
	}

	/**
	 * 创建一个新的 DirectFieldBindingResult 实例。
	 * @param target 要绑定到的目标对象。
	 * @param objectName 目标对象的名称。
	 * @param autoGrowNestedPaths 是否“自动增长”包含 null 值的嵌套路径。
	 */
	public DirectFieldBindingResult(@Nullable Object target, String objectName, boolean autoGrowNestedPaths) {
		super(objectName);
		this.target = target;
		this.autoGrowNestedPaths = autoGrowNestedPaths;
	}


	@Override
	@Nullable
	public final Object getTarget() {
		return this.target;
	}

	/**
	 * 返回此实例使用的 DirectFieldAccessor。
	 * 如果之前不存在，则创建一个新的。
	 * @see #createDirectFieldAccessor()
	 */
	@Override
	public final ConfigurablePropertyAccessor getPropertyAccessor() {
		if (this.directFieldAccessor == null) {
			this.directFieldAccessor = createDirectFieldAccessor();
			this.directFieldAccessor.setExtractOldValueForEditor(true);
			this.directFieldAccessor.setAutoGrowNestedPaths(this.autoGrowNestedPaths);
		}
		return this.directFieldAccessor;
	}

	/**
	 * 为底层目标对象创建一个新的 DirectFieldAccessor。
	 * @see #getTarget()
	 */
	protected ConfigurablePropertyAccessor createDirectFieldAccessor() {
		if (this.target == null) {
			throw new IllegalStateException("Cannot access fields on null target instance '" + getObjectName() + "'");
		}
		return PropertyAccessorFactory.forDirectFieldAccess(this.target);
	}

}
