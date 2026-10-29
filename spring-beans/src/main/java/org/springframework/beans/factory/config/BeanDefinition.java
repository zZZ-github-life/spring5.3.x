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

package org.springframework.beans.factory.config;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.core.AttributeAccessor;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

/**
 * BeanDefinition 描述一个 Bean 实例，它具有属性值、构造方法参数值，以及由具体实现提供的其他信息。
 *
 * <p>这只是一个最小接口：主要目的是允许 {@link BeanFactoryPostProcessor} 内省并修改属性值和其他
 * Bean 元数据。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 19.03.2004
 * @see ConfigurableListableBeanFactory#getBeanDefinition
 * @see org.springframework.beans.factory.support.RootBeanDefinition
 * @see org.springframework.beans.factory.support.ChildBeanDefinition
 */
public interface BeanDefinition extends AttributeAccessor, BeanMetadataElement {

	/**
	 * 标准单例作用域的作用域标识符：{@value}。<p>请注意，扩展的 Bean 工厂可能支持更多作用域。
	 * @see #setScope
	 * @see ConfigurableBeanFactory#SCOPE_SINGLETON
	 */
	String SCOPE_SINGLETON = ConfigurableBeanFactory.SCOPE_SINGLETON;

	/**
	 * 标准原型作用域的作用域标识符：{@value}。<p>请注意，扩展的 Bean 工厂可能支持更多作用域。
	 * @see #setScope
	 * @see ConfigurableBeanFactory#SCOPE_PROTOTYPE
	 */
	String SCOPE_PROTOTYPE = ConfigurableBeanFactory.SCOPE_PROTOTYPE;


	/**
	 * 角色提示，指示 {@code BeanDefinition} 是应用的主要组成部分。通常对应用户定义的 Bean。
	 */
	int ROLE_APPLICATION = 0;

	/**
	 * 角色提示，指示 {@code BeanDefinition} 是某个更大配置（通常是外层的
	 * {@link org.springframework.beans.factory.parsing.ComponentDefinition}）的支撑部分。
	 * {@code SUPPORT} Bean 被认为足够重要，在更仔细地查看特定
	 * {@link org.springframework.beans.factory.parsing.ComponentDefinition} 时应当予以关注，但在查看应
	 * 用的总体配置时则不必。
	 */
	int ROLE_SUPPORT = 1;

	/**
	 * 角色提示，指示 {@code BeanDefinition} 提供完全后台的角色，与最终用户无关。此提示用于注册完全属于
	 * {@link org.springframework.beans.factory.parsing.ComponentDefinition} 内部工作机制的 Bean。
	 */
	int ROLE_INFRASTRUCTURE = 2;


	// Modifiable attributes

	/**
	 * 设置此 Bean 定义的父定义名称（如果有）。
	 */
	void setParentName(@Nullable String parentName);

	/**
	 * 返回此 Bean 定义的父定义名称（如果有）。
	 */
	@Nullable
	String getParentName();

	/**
	 * 指定此 Bean 定义的 Bean 类名。<p>该类名可以在 Bean 工厂后处理期间修改，通常是用解析后的变体替换
	 * 原始类名。
	 * @see #setParentName
	 * @see #setFactoryBeanName
	 * @see #setFactoryMethodName
	 */
	void setBeanClassName(@Nullable String beanClassName);

	/**
	 * 返回此 Bean 定义的当前 Bean 类名。<p>请注意，在子定义覆盖或继承父定义的类名的情况下，这不必是运
	 * 行时实际使用的类名。此外，这可能只是调用工厂方法时所在的类，或者在通过工厂 Bean 引用调用方法的情
	 * 况下甚至可能为空。因此，<i>不要</i>将其视为运行时的确定 Bean 类型，而应仅在单个 Bean 定义级别将
	 * 其用于解析目的。
	 * @see #getParentName()
	 * @see #getFactoryBeanName()
	 * @see #getFactoryMethodName()
	 */
	@Nullable
	String getBeanClassName();

	/**
	 * 覆盖此 Bean 的目标作用域，指定新的作用域名称。
	 * @see #SCOPE_SINGLETON
	 * @see #SCOPE_PROTOTYPE
	 */
	void setScope(@Nullable String scope);

	/**
	 * 返回此 Bean 当前目标作用域的名称，如果尚不清楚则返回 {@code null}。
	 */
	@Nullable
	String getScope();

	/**
	 * 设置此 Bean 是否应延迟初始化。<p>如果为 {@code false}，则执行单例预先初始化的 Bean 工厂会在启动
	 * 时实例化该 Bean。
	 */
	void setLazyInit(boolean lazyInit);

	/**
	 * 返回此 Bean 是否应延迟初始化，即不在启动时提前实例化。仅适用于单例 Bean。
	 */
	boolean isLazyInit();

	/**
	 * 设置此 Bean 依赖初始化的 Bean 名称。Bean 工厂将保证这些 Bean 先被初始化。
	 */
	void setDependsOn(@Nullable String... dependsOn);

	/**
	 * 返回此 Bean 所依赖的 Bean 名称。
	 */
	@Nullable
	String[] getDependsOn();

	/**
	 * 设置此 Bean 是否可作为被自动装配到其他 Bean 中的候选者。<p>请注意，此标志旨在仅影响基于类型的自
	 * 动装配。它不影响按名称的显式引用，即使指定的 Bean 未被标记为自动装配候选者，此类引用也会被解析。
	 * 因此，如果名称匹配，按名称自动装配仍会注入 Bean。
	 */
	void setAutowireCandidate(boolean autowireCandidate);

	/**
	 * 返回此 Bean 是否可作为被自动装配到其他 Bean 中的候选者。
	 */
	boolean isAutowireCandidate();

	/**
	 * 设置此 Bean 是否为首选自动装配候选者。<p>如果在多个匹配候选者中恰好有一个 Bean 的该值为
	 * {@code true}，它将作为决胜依据。
	 */
	void setPrimary(boolean primary);

	/**
	 * 返回此 Bean 是否为首选自动装配候选者。
	 */
	boolean isPrimary();

	/**
	 * 指定要使用的工厂 Bean（如果有）。这是要在其上调用指定工厂方法的 Bean 的名称。
	 * @see #setFactoryMethodName
	 */
	void setFactoryBeanName(@Nullable String factoryBeanName);

	/**
	 * 返回工厂 Bean 名称（如果有）。
	 */
	@Nullable
	String getFactoryBeanName();

	/**
	 * 指定工厂方法（如果有）。将使用构造方法参数调用此方法；如果未指定参数，则以无参方式调用。该方法将
	 * 在指定的工厂 Bean（如果有）上调用，否则作为本地 Bean 类的静态方法调用。
	 * @see #setFactoryBeanName
	 * @see #setBeanClassName
	 */
	void setFactoryMethodName(@Nullable String factoryMethodName);

	/**
	 * 返回工厂方法（如果有）。
	 */
	@Nullable
	String getFactoryMethodName();

	/**
	 * 返回此 Bean 的构造方法参数值。<p>返回的实例可以在 Bean 工厂后处理期间修改。
	 * @return the ConstructorArgumentValues object (never {@code null})
	 */
	ConstructorArgumentValues getConstructorArgumentValues();

	/**
	 * 返回此 Bean 是否定义了构造方法参数值。
	 * @since 5.0.2
	 */
	default boolean hasConstructorArgumentValues() {
		return !getConstructorArgumentValues().isEmpty();
	}

	/**
	 * 返回要应用到 Bean 新实例的属性值。<p>返回的实例可以在 Bean 工厂后处理期间修改。
	 * @return the MutablePropertyValues object (never {@code null})
	 */
	MutablePropertyValues getPropertyValues();

	/**
	 * 返回此 Bean 是否定义了属性值。
	 * @since 5.0.2
	 */
	default boolean hasPropertyValues() {
		return !getPropertyValues().isEmpty();
	}

	/**
	 * 设置初始化方法的名称。
	 * @since 5.1
	 */
	void setInitMethodName(@Nullable String initMethodName);

	/**
	 * 返回初始化方法的名称。
	 * @since 5.1
	 */
	@Nullable
	String getInitMethodName();

	/**
	 * 设置销毁方法的名称。
	 * @since 5.1
	 */
	void setDestroyMethodName(@Nullable String destroyMethodName);

	/**
	 * 返回销毁方法的名称。
	 * @since 5.1
	 */
	@Nullable
	String getDestroyMethodName();

	/**
	 * 为此 {@code BeanDefinition} 设置角色提示。角色提示为框架和工具提供关于特定
	 * {@code BeanDefinition} 的角色和重要性的指示。
	 * @since 5.1
	 * @see #ROLE_APPLICATION
	 * @see #ROLE_SUPPORT
	 * @see #ROLE_INFRASTRUCTURE
	 */
	void setRole(int role);

	/**
	 * 获取此 {@code BeanDefinition} 的角色提示。角色提示为框架和工具提供关于特定
	 * {@code BeanDefinition} 的角色和重要性的指示。
	 * @see #ROLE_APPLICATION
	 * @see #ROLE_SUPPORT
	 * @see #ROLE_INFRASTRUCTURE
	 */
	int getRole();

	/**
	 * 设置此 Bean 定义的人类可读描述。
	 * @since 5.1
	 */
	void setDescription(@Nullable String description);

	/**
	 * 返回此 Bean 定义的人类可读描述。
	 */
	@Nullable
	String getDescription();


	// Read-only attributes

	/**
	 * 返回此 Bean 定义的可解析类型，基于 Bean 类或其他特定元数据。<p>这通常在运行时合并的 Bean 定义上
	 * 完全解析，但在配置时的定义实例上不一定如此。
	 * @return the resolvable type (potentially {@link ResolvableType#NONE})
	 * @since 5.2
	 * @see ConfigurableBeanFactory#getMergedBeanDefinition
	 */
	ResolvableType getResolvableType();

	/**
	 * 返回此 Bean 是否为 <b>Singleton</b>，即在所有调用中返回单个共享实例。
	 * @see #SCOPE_SINGLETON
	 */
	boolean isSingleton();

	/**
	 * 返回此 Bean 是否为 <b>Prototype</b>，即每次调用都返回独立实例。
	 * @since 3.0
	 * @see #SCOPE_PROTOTYPE
	 */
	boolean isPrototype();

	/**
	 * 返回此 Bean 是否为 "abstract"，即不打算被实例化。
	 */
	boolean isAbstract();

	/**
	 * 返回此 Bean 定义来源资源的描述（用于在出错时显示上下文）。
	 */
	@Nullable
	String getResourceDescription();

	/**
	 * 返回原始 BeanDefinition，如果没有则返回 {@code null}。<p>允许获取被装饰的 Bean 定义（如果有）。
	 * <p>请注意，此方法返回直接来源。要查找用户定义的原始 BeanDefinition，请沿来源链迭代。
	 */
	@Nullable
	BeanDefinition getOriginatingBeanDefinition();

}
