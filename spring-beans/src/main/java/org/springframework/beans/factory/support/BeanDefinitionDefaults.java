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

package org.springframework.beans.factory.support;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * 用于保存 {@code BeanDefinition} 属性默认值的简单持有者。
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 2.5
 * @see AbstractBeanDefinition#applyDefaults
 */
public class BeanDefinitionDefaults {

	@Nullable
	private Boolean lazyInit;

	private int autowireMode = AbstractBeanDefinition.AUTOWIRE_NO;

	private int dependencyCheck = AbstractBeanDefinition.DEPENDENCY_CHECK_NONE;

	@Nullable
	private String initMethodName;

	@Nullable
	private String destroyMethodName;


	/**
	 * 设置 Bean 是否应默认延迟初始化。<p>如果为 {@code false}，执行单例急切初始化的 Bean 工厂将在启动
	 * 时实例化该 Bean。
	 * @see AbstractBeanDefinition#setLazyInit
	 */
	public void setLazyInit(boolean lazyInit) {
		this.lazyInit = lazyInit;
	}

	/**
	 * 返回 Bean 是否应默认延迟初始化，即不在启动时急切实例化。仅适用于单例 Bean。
	 * @return 是否应用延迟初始化语义（默认 {@code false}）
	 */
	public boolean isLazyInit() {
		return (this.lazyInit != null && this.lazyInit.booleanValue());
	}

	/**
	 * 返回 Bean 是否应默认延迟初始化，即不在启动时急切实例化。仅适用于单例 Bean。
	 * @return 显式设置时的延迟初始化标志；否则为 {@code null}
	 * @since 5.2
	 */
	@Nullable
	public Boolean getLazyInit() {
		return this.lazyInit;
	}

	/**
	 * 设置自动装配模式。这决定是否发生 Bean 引用的自动检测和设置。默认值为 AUTOWIRE_NO，意味着不会进行
	 * 基于约定的按名称或按类型自动装配（不过，仍可能存在显式的注解驱动自动装配）。
	 * @param autowireMode 要设置的自动装配模式。
	 * 必须是 {@link AbstractBeanDefinition} 中定义的常量之一。
	 * @see AbstractBeanDefinition#setAutowireMode
	 */
	public void setAutowireMode(int autowireMode) {
		this.autowireMode = autowireMode;
	}

	/**
	 * 返回默认的自动装配模式。
	 */
	public int getAutowireMode() {
		return this.autowireMode;
	}

	/**
	 * 设置依赖检查代码。
	 * @param dependencyCheck 要设置的代码。
	 * 必须是 {@link AbstractBeanDefinition} 中定义的常量之一。
	 * @see AbstractBeanDefinition#setDependencyCheck
	 */
	public void setDependencyCheck(int dependencyCheck) {
		this.dependencyCheck = dependencyCheck;
	}

	/**
	 * 返回默认的依赖检查代码。
	 */
	public int getDependencyCheck() {
		return this.dependencyCheck;
	}

	/**
	 * 设置默认初始化方法的名称。<p>请注意，此方法并非强制应用于所有受影响的 Bean 定义，而是作为可选回
	 * 调，仅在确实存在时才调用。
	 * @see AbstractBeanDefinition#setInitMethodName
	 * @see AbstractBeanDefinition#setEnforceInitMethod
	 */
	public void setInitMethodName(@Nullable String initMethodName) {
		this.initMethodName = (StringUtils.hasText(initMethodName) ? initMethodName : null);
	}

	/**
	 * 返回默认初始化方法的名称。
	 */
	@Nullable
	public String getInitMethodName() {
		return this.initMethodName;
	}

	/**
	 * 设置默认销毁方法的名称。<p>请注意，此方法并非强制应用于所有受影响的 Bean 定义，而是作为可选回调，
	 * 仅在确实存在时才调用。
	 * @see AbstractBeanDefinition#setDestroyMethodName
	 * @see AbstractBeanDefinition#setEnforceDestroyMethod
	 */
	public void setDestroyMethodName(@Nullable String destroyMethodName) {
		this.destroyMethodName = (StringUtils.hasText(destroyMethodName) ? destroyMethodName : null);
	}

	/**
	 * 返回默认销毁方法的名称。
	 */
	@Nullable
	public String getDestroyMethodName() {
		return this.destroyMethodName;
	}

}
