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

import java.io.Serializable;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.ConfigurablePropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.lang.Nullable;

/**
 * {@link Errors} 和 {@link BindingResult} 接口的默认实现，用于在 JavaBean 对象上注册和评估绑定错误。
 *
 * <p>执行标准的 JavaBean 属性访问，也支持嵌套属性。通常，应用代码会使用 {@code Errors} 接口或
 * {@code BindingResult} 接口。{@link DataBinder} 通过 {@link DataBinder#getBindingResult()} 返回其
 * {@code BindingResult}。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see DataBinder#getBindingResult()
 * @see DataBinder#initBeanPropertyAccess()
 * @see DirectFieldBindingResult
 */
@SuppressWarnings("serial")
public class BeanPropertyBindingResult extends AbstractPropertyBindingResult implements Serializable {

	@Nullable
	private final Object target;

	private final boolean autoGrowNestedPaths;

	private final int autoGrowCollectionLimit;

	@Nullable
	private transient BeanWrapper beanWrapper;


	/**
	 * 创建 {@link BeanPropertyBindingResult} 类的新实例。
	 * @param target 要绑定到的目标 Bean
	 * @param objectName 目标对象的名称
	 */
	public BeanPropertyBindingResult(@Nullable Object target, String objectName) {
		this(target, objectName, true, Integer.MAX_VALUE);
	}

	/**
	 * 创建 {@link BeanPropertyBindingResult} 类的新实例。
	 * @param target 要绑定到的目标 Bean
	 * @param objectName 目标对象的名称
	 * @param autoGrowNestedPaths 是否对包含 null 值的嵌套路径进行“自动增长”
	 * @param autoGrowCollectionLimit 数组和集合自动增长的限值
	 */
	public BeanPropertyBindingResult(@Nullable Object target, String objectName,
			boolean autoGrowNestedPaths, int autoGrowCollectionLimit) {

		super(objectName);
		this.target = target;
		this.autoGrowNestedPaths = autoGrowNestedPaths;
		this.autoGrowCollectionLimit = autoGrowCollectionLimit;
	}


	@Override
	@Nullable
	public final Object getTarget() {
		return this.target;
	}

	/**
	 * 返回此实例使用的 {@link BeanWrapper}。如果之前不存在，则创建一个新的。
	 * @see #createBeanWrapper()
	 */
	@Override
	public final ConfigurablePropertyAccessor getPropertyAccessor() {
		if (this.beanWrapper == null) {
			this.beanWrapper = createBeanWrapper();
			this.beanWrapper.setExtractOldValueForEditor(true);
			this.beanWrapper.setAutoGrowNestedPaths(this.autoGrowNestedPaths);
			this.beanWrapper.setAutoGrowCollectionLimit(this.autoGrowCollectionLimit);
		}
		return this.beanWrapper;
	}

	/**
	 * 为底层目标对象创建新的 {@link BeanWrapper}。
	 * @see #getTarget()
	 */
	protected BeanWrapper createBeanWrapper() {
		if (this.target == null) {
			throw new IllegalStateException("Cannot access properties on null bean instance '" + getObjectName() + "'");
		}
		return PropertyAccessorFactory.forBeanPropertyAccess(this.target);
	}

}
