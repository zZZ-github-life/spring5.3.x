/*
 * Copyright 2002-2021 the original author or authors.
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

import java.util.function.Supplier;

import org.springframework.beans.factory.config.AutowiredPropertyMarker;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * 以编程方式使用构建器模式构造
 * {@link org.springframework.beans.factory.config.BeanDefinition BeanDefinitions} 的手段。主要用于
 * 实现 Spring 2.0 {@link org.springframework.beans.factory.xml.NamespaceHandler NamespaceHandlers}
 * 时使用。
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public final class BeanDefinitionBuilder {

	/**
	 * 创建一个新的 {@code BeanDefinitionBuilder}，用于构造 {@link GenericBeanDefinition}。
	 */
	public static BeanDefinitionBuilder genericBeanDefinition() {
		return new BeanDefinitionBuilder(new GenericBeanDefinition());
	}

	/**
	 * 创建一个新的 {@code BeanDefinitionBuilder}，用于构造 {@link GenericBeanDefinition}。
	 * @param beanClassName the class name for the bean that the definition is being created for
	 */
	public static BeanDefinitionBuilder genericBeanDefinition(String beanClassName) {
		BeanDefinitionBuilder builder = new BeanDefinitionBuilder(new GenericBeanDefinition());
		builder.beanDefinition.setBeanClassName(beanClassName);
		return builder;
	}

	/**
	 * 创建一个新的 {@code BeanDefinitionBuilder}，用于构造 {@link GenericBeanDefinition}。
	 * @param beanClass the {@code Class} of the bean that the definition is being created for
	 */
	public static BeanDefinitionBuilder genericBeanDefinition(Class<?> beanClass) {
		BeanDefinitionBuilder builder = new BeanDefinitionBuilder(new GenericBeanDefinition());
		builder.beanDefinition.setBeanClass(beanClass);
		return builder;
	}

	/**
	 * 创建一个新的 {@code BeanDefinitionBuilder}，用于构造 {@link GenericBeanDefinition}。
	 * @param beanClass the {@code Class} of the bean that the definition is being created for
	 * @param instanceSupplier a callback for creating an instance of the bean
	 * @since 5.0
	 */
	public static <T> BeanDefinitionBuilder genericBeanDefinition(Class<T> beanClass, Supplier<T> instanceSupplier) {
		BeanDefinitionBuilder builder = new BeanDefinitionBuilder(new GenericBeanDefinition());
		builder.beanDefinition.setBeanClass(beanClass);
		builder.beanDefinition.setInstanceSupplier(instanceSupplier);
		return builder;
	}

	/**
	 * 创建一个新的 {@code BeanDefinitionBuilder}，用于构造 {@link RootBeanDefinition}。
	 * @param beanClassName the class name for the bean that the definition is being created for
	 */
	public static BeanDefinitionBuilder rootBeanDefinition(String beanClassName) {
		return rootBeanDefinition(beanClassName, null);
	}

	/**
	 * 创建一个新的 {@code BeanDefinitionBuilder}，用于构造 {@link RootBeanDefinition}。
	 * @param beanClassName the class name for the bean that the definition is being created for
	 * @param factoryMethodName the name of the method to use to construct the bean instance
	 */
	public static BeanDefinitionBuilder rootBeanDefinition(String beanClassName, @Nullable String factoryMethodName) {
		BeanDefinitionBuilder builder = new BeanDefinitionBuilder(new RootBeanDefinition());
		builder.beanDefinition.setBeanClassName(beanClassName);
		builder.beanDefinition.setFactoryMethodName(factoryMethodName);
		return builder;
	}

	/**
	 * 创建一个新的 {@code BeanDefinitionBuilder}，用于构造 {@link RootBeanDefinition}。
	 * @param beanClass the {@code Class} of the bean that the definition is being created for
	 */
	public static BeanDefinitionBuilder rootBeanDefinition(Class<?> beanClass) {
		return rootBeanDefinition(beanClass, (String) null);
	}

	/**
	 * 创建一个新的 {@code BeanDefinitionBuilder}，用于构造 {@link RootBeanDefinition}。
	 * @param beanClass the {@code Class} of the bean that the definition is being created for
	 * @param factoryMethodName the name of the method to use to construct the bean instance
	 */
	public static BeanDefinitionBuilder rootBeanDefinition(Class<?> beanClass, @Nullable String factoryMethodName) {
		BeanDefinitionBuilder builder = new BeanDefinitionBuilder(new RootBeanDefinition());
		builder.beanDefinition.setBeanClass(beanClass);
		builder.beanDefinition.setFactoryMethodName(factoryMethodName);
		return builder;
	}

	/**
	 * 创建一个新的 {@code BeanDefinitionBuilder}，用于构造 {@link RootBeanDefinition}。
	 * @param beanType the {@link ResolvableType type} of the bean that the definition is being created for
	 * @param instanceSupplier a callback for creating an instance of the bean
	 * @since 5.3.9
	 */
	public static <T> BeanDefinitionBuilder rootBeanDefinition(ResolvableType beanType, Supplier<T> instanceSupplier) {
		RootBeanDefinition beanDefinition = new RootBeanDefinition();
		beanDefinition.setTargetType(beanType);
		beanDefinition.setInstanceSupplier(instanceSupplier);
		return new BeanDefinitionBuilder(beanDefinition);
	}

	/**
	 * 创建一个新的 {@code BeanDefinitionBuilder}，用于构造 {@link RootBeanDefinition}。
	 * @param beanClass the {@code Class} of the bean that the definition is being created for
	 * @param instanceSupplier a callback for creating an instance of the bean
	 * @since 5.3.9
	 * @see #rootBeanDefinition(ResolvableType, Supplier)
	 */
	public static <T> BeanDefinitionBuilder rootBeanDefinition(Class<T> beanClass, Supplier<T> instanceSupplier) {
		return rootBeanDefinition(ResolvableType.forClass(beanClass), instanceSupplier);
	}

	/**
	 * 创建一个新的 {@code BeanDefinitionBuilder}，用于构造 {@link ChildBeanDefinition}。
	 * @param parentName the name of the parent bean
	 */
	public static BeanDefinitionBuilder childBeanDefinition(String parentName) {
		return new BeanDefinitionBuilder(new ChildBeanDefinition(parentName));
	}


	/**
	 * 我们正在创建的 {@code BeanDefinition} 实例。
	 */
	private final AbstractBeanDefinition beanDefinition;

	/**
	 * 我们当前相对于构造函数参数的位置。
	 */
	private int constructorArgIndex;


	/**
	 * 强制使用工厂方法。
	 */
	private BeanDefinitionBuilder(AbstractBeanDefinition beanDefinition) {
		this.beanDefinition = beanDefinition;
	}

	/**
	 * 以原始（未校验）形式返回当前的 BeanDefinition 对象。
	 * @see #getBeanDefinition()
	 */
	public AbstractBeanDefinition getRawBeanDefinition() {
		return this.beanDefinition;
	}

	/**
	 * 校验并返回创建的 BeanDefinition 对象。
	 */
	public AbstractBeanDefinition getBeanDefinition() {
		this.beanDefinition.validate();
		return this.beanDefinition;
	}


	/**
	 * 设置此 Bean 定义的父定义的名称。
	 */
	public BeanDefinitionBuilder setParentName(String parentName) {
		this.beanDefinition.setParentName(parentName);
		return this;
	}

	/**
	 * 设置要用于此定义的静态工厂方法的名称，该方法将在此 Bean 的类上调用。
	 */
	public BeanDefinitionBuilder setFactoryMethod(String factoryMethod) {
		this.beanDefinition.setFactoryMethodName(factoryMethod);
		return this;
	}

	/**
	 * 设置要用于此定义的非静态工厂方法的名称，包括要在其上调用该方法的工厂实例的 Bean 名称。
	 * @param factoryMethod the name of the factory method
	 * @param factoryBean the name of the bean to call the specified factory method on
	 * @since 4.3.6
	 */
	public BeanDefinitionBuilder setFactoryMethodOnBean(String factoryMethod, String factoryBean) {
		this.beanDefinition.setFactoryMethodName(factoryMethod);
		this.beanDefinition.setFactoryBeanName(factoryBean);
		return this;
	}

	/**
	 * 添加带索引的构造函数参数值。当前索引在内部跟踪，所有添加都发生在当前位置。
	 */
	public BeanDefinitionBuilder addConstructorArgValue(@Nullable Object value) {
		this.beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(
				this.constructorArgIndex++, value);
		return this;
	}

	/**
	 * 添加对指定名称 Bean 的引用作为构造函数参数。
	 * @see #addConstructorArgValue(Object)
	 */
	public BeanDefinitionBuilder addConstructorArgReference(String beanName) {
		this.beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(
				this.constructorArgIndex++, new RuntimeBeanReference(beanName));
		return this;
	}

	/**
	 * 在给定属性名称下添加所提供的属性值。
	 */
	public BeanDefinitionBuilder addPropertyValue(String name, @Nullable Object value) {
		this.beanDefinition.getPropertyValues().add(name, value);
		return this;
	}

	/**
	 * 在指定属性下添加对指定 Bean 名称的引用。
	 * @param name the name of the property to add the reference to
	 * @param beanName the name of the bean being referenced
	 */
	public BeanDefinitionBuilder addPropertyReference(String name, String beanName) {
		this.beanDefinition.getPropertyValues().add(name, new RuntimeBeanReference(beanName));
		return this;
	}

	/**
	 * 为指定 Bean 上的指定属性添加自动装配标记。
	 * @param name the name of the property to mark as autowired
	 * @since 5.2
	 * @see AutowiredPropertyMarker
	 */
	public BeanDefinitionBuilder addAutowiredProperty(String name) {
		this.beanDefinition.getPropertyValues().add(name, AutowiredPropertyMarker.INSTANCE);
		return this;
	}

	/**
	 * 为此定义设置初始化方法。
	 */
	public BeanDefinitionBuilder setInitMethodName(@Nullable String methodName) {
		this.beanDefinition.setInitMethodName(methodName);
		return this;
	}

	/**
	 * 为此定义设置销毁方法。
	 */
	public BeanDefinitionBuilder setDestroyMethodName(@Nullable String methodName) {
		this.beanDefinition.setDestroyMethodName(methodName);
		return this;
	}


	/**
	 * 为此定义设置作用域。
	 * @see org.springframework.beans.factory.config.BeanDefinition#SCOPE_SINGLETON
	 * @see org.springframework.beans.factory.config.BeanDefinition#SCOPE_PROTOTYPE
	 */
	public BeanDefinitionBuilder setScope(@Nullable String scope) {
		this.beanDefinition.setScope(scope);
		return this;
	}

	/**
	 * 设置此定义是否为抽象。
	 */
	public BeanDefinitionBuilder setAbstract(boolean flag) {
		this.beanDefinition.setAbstract(flag);
		return this;
	}

	/**
	 * 设置此定义的 Bean 是否应延迟初始化。
	 */
	public BeanDefinitionBuilder setLazyInit(boolean lazy) {
		this.beanDefinition.setLazyInit(lazy);
		return this;
	}

	/**
	 * 为此定义设置自动装配模式。
	 */
	public BeanDefinitionBuilder setAutowireMode(int autowireMode) {
		this.beanDefinition.setAutowireMode(autowireMode);
		return this;
	}

	/**
	 * 为此定义设置依赖检查模式。
	 */
	public BeanDefinitionBuilder setDependencyCheck(int dependencyCheck) {
		this.beanDefinition.setDependencyCheck(dependencyCheck);
		return this;
	}

	/**
	 * 将指定 Bean 名称追加到此定义所依赖的 Bean 列表中。
	 */
	public BeanDefinitionBuilder addDependsOn(String beanName) {
		if (this.beanDefinition.getDependsOn() == null) {
			this.beanDefinition.setDependsOn(beanName);
		}
		else {
			String[] added = ObjectUtils.addObjectToArray(this.beanDefinition.getDependsOn(), beanName);
			this.beanDefinition.setDependsOn(added);
		}
		return this;
	}

	/**
	 * 设置此 Bean 是否为首选自动装配候选者。
	 * @since 5.1.11
	 */
	public BeanDefinitionBuilder setPrimary(boolean primary) {
		this.beanDefinition.setPrimary(primary);
		return this;
	}

	/**
	 * 设置此定义的角色。
	 */
	public BeanDefinitionBuilder setRole(int role) {
		this.beanDefinition.setRole(role);
		return this;
	}

	/**
	 * 设置此 Bean 是否为“合成”的，即并非由应用自身定义。
	 * @since 5.3.9
	 */
	public BeanDefinitionBuilder setSynthetic(boolean synthetic) {
		this.beanDefinition.setSynthetic(synthetic);
		return this;
	}

	/**
	 * 将给定的定制器应用到基础 Bean 定义。
	 * @since 5.0
	 */
	public BeanDefinitionBuilder applyCustomizers(BeanDefinitionCustomizer... customizers) {
		for (BeanDefinitionCustomizer customizer : customizers) {
			customizer.customize(this.beanDefinition);
		}
		return this;
	}

}
