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

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * 用于从父级继承设置的 bean 的 Bean 定义。子 Bean 定义对父 Bean 定义有固定依赖。
 *
 * <p>子 Bean 定义将继承父级的构造参数值、属性值和方法重写，并可以选择添加新值。如果指定了 init 方
 * 法、destroy 方法和/或静态工厂方法，它们将覆盖父级的相应设置。其余设置<i>始终</i>取自子定义：
 * depends on、autowire 模式、依赖检查、单例、延迟初始化。
 *
 * <p><b>注意：</b>自 Spring 2.5 起，以编程方式注册 Bean 定义的首选方式是
 * {@link GenericBeanDefinition} 类，它允许通过 {@link GenericBeanDefinition#setParentName} 方法动
 * 态定义父级依赖。这实际上在大多数用例中取代了 ChildBeanDefinition 类。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see GenericBeanDefinition
 * @see RootBeanDefinition
 */
@SuppressWarnings("serial")
public class ChildBeanDefinition extends AbstractBeanDefinition {

	@Nullable
	private String parentName;


	/**
	 * 为给定父级创建一个新的 ChildBeanDefinition，将通过其 Bean 属性和配置方法进行配置。
	 * @param parentName 父 Bean 的名称
	 * @see #setBeanClass
	 * @see #setScope
	 * @see #setConstructorArgumentValues
	 * @see #setPropertyValues
	 */
	public ChildBeanDefinition(String parentName) {
		super();
		this.parentName = parentName;
	}

	/**
	 * 为给定父级创建一个新的 ChildBeanDefinition。
	 * @param parentName 父 Bean 的名称
	 * @param pvs 子 Bean 的附加属性值
	 */
	public ChildBeanDefinition(String parentName, MutablePropertyValues pvs) {
		super(null, pvs);
		this.parentName = parentName;
	}

	/**
	 * 为给定父级创建一个新的 ChildBeanDefinition。
	 * @param parentName 父 Bean 的名称
	 * @param cargs 要应用的构造器参数值
	 * @param pvs 子 Bean 的附加属性值
	 */
	public ChildBeanDefinition(
			String parentName, ConstructorArgumentValues cargs, MutablePropertyValues pvs) {

		super(cargs, pvs);
		this.parentName = parentName;
	}

	/**
	 * 为给定父级创建一个新的 ChildBeanDefinition，并提供构造参数和属性值。
	 * @param parentName 父 Bean 的名称
	 * @param beanClass 要实例化的 Bean 的类
	 * @param cargs 要应用的构造器参数值
	 * @param pvs 要应用的属性值
	 */
	public ChildBeanDefinition(
			String parentName, Class<?> beanClass, ConstructorArgumentValues cargs, MutablePropertyValues pvs) {

		super(cargs, pvs);
		this.parentName = parentName;
		setBeanClass(beanClass);
	}

	/**
	 * 为给定父级创建一个新的 ChildBeanDefinition，并提供构造参数和属性值。接收 Bean 类名以避免 Bean 类
	 * 的急切加载。
	 * @param parentName 父 Bean 的名称
	 * @param beanClassName 要实例化的类的名称
	 * @param cargs 要应用的构造器参数值
	 * @param pvs 要应用的属性值
	 */
	public ChildBeanDefinition(
			String parentName, String beanClassName, ConstructorArgumentValues cargs, MutablePropertyValues pvs) {

		super(cargs, pvs);
		this.parentName = parentName;
		setBeanClassName(beanClassName);
	}

	/**
	 * 创建给定 Bean 定义的深拷贝，作为新的 ChildBeanDefinition。
	 * @param original 要从中复制的原始 Bean 定义
	 */
	public ChildBeanDefinition(ChildBeanDefinition original) {
		super(original);
	}


	@Override
	public void setParentName(@Nullable String parentName) {
		this.parentName = parentName;
	}

	@Override
	@Nullable
	public String getParentName() {
		return this.parentName;
	}

	@Override
	public void validate() throws BeanDefinitionValidationException {
		super.validate();
		if (this.parentName == null) {
			throw new BeanDefinitionValidationException("'parentName' must be set in ChildBeanDefinition");
		}
	}


	@Override
	public AbstractBeanDefinition cloneBeanDefinition() {
		return new ChildBeanDefinition(this);
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ChildBeanDefinition)) {
			return false;
		}
		ChildBeanDefinition that = (ChildBeanDefinition) other;
		return (ObjectUtils.nullSafeEquals(this.parentName, that.parentName) && super.equals(other));
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(this.parentName) * 29 + super.hashCode();
	}

	@Override
	public String toString() {
		return "Child bean with parent '" + this.parentName + "': " + super.toString();
	}

}
