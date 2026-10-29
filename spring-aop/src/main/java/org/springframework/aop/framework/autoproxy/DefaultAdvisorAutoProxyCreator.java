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

package org.springframework.aop.framework.autoproxy;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.lang.Nullable;

/**
 * {@code BeanPostProcessor} 实现，基于当前 {@code BeanFactory} 中所有候选的 {@code Advisor} 创建
 * AOP 代理。此类完全通用；它不包含处理任何特定切面（例如池化切面）的特殊代码。
 *
 * <p>可以过滤 Advisor——例如，为了在同一工厂中使用多个此类型的后处理器——方法是将 {@code usePrefix}
 * 属性设为 true，此时只会使用以 DefaultAdvisorAutoProxyCreator 的 Bean 名称加一个点（例如 "aapc."）
 * 开头的 Advisor。可以通过设置 {@code advisorBeanNamePrefix} 属性来更改这个取自 Bean 名称的默认
 * 前缀。在这种情况下也会使用分隔符 (.)。
 *
 * @author Rod Johnson
 * @author Rob Harrop
 */
@SuppressWarnings("serial")
public class DefaultAdvisorAutoProxyCreator extends AbstractAdvisorAutoProxyCreator implements BeanNameAware {

	/** bean 名称前缀与其余部分之间的分隔符。 */
	public static final String SEPARATOR = ".";


	private boolean usePrefix = false;

	@Nullable
	private String advisorBeanNamePrefix;


	/**
	 * 设置是否只包含 Bean 名称中带有特定前缀的 Advisor。
	 * <p>默认值为 {@code false}，即包含所有 {@code Advisor} 类型的 Bean。
	 * @see #setAdvisorBeanNamePrefix
	 */
	public void setUsePrefix(boolean usePrefix) {
		this.usePrefix = usePrefix;
	}

	/**
	 * 返回是否只包含 Bean 名称中带有特定前缀的 Advisor。
	 */
	public boolean isUsePrefix() {
		return this.usePrefix;
	}

	/**
	 * 设置 Bean 名称的前缀，带有该前缀的 Bean 将被此对象纳入自动代理。应设置该前缀以避免循环引用。默认
	 * 值为本对象的 Bean 名称加一个点。
	 * @param advisorBeanNamePrefix the exclusion prefix
	 */
	public void setAdvisorBeanNamePrefix(@Nullable String advisorBeanNamePrefix) {
		this.advisorBeanNamePrefix = advisorBeanNamePrefix;
	}

	/**
	 * 返回 Bean 名称的前缀，带有该前缀的 Bean 将被此对象纳入自动代理。
	 */
	@Nullable
	public String getAdvisorBeanNamePrefix() {
		return this.advisorBeanNamePrefix;
	}

	@Override
	public void setBeanName(String name) {
		// If no infrastructure bean name prefix has been set, override it.
		if (this.advisorBeanNamePrefix == null) {
			this.advisorBeanNamePrefix = name + SEPARATOR;
		}
	}


	/**
	 * 如果启用，则将具有指定前缀的 {@code Advisor} Bean 视为合格。
	 * @see #setUsePrefix
	 * @see #setAdvisorBeanNamePrefix
	 */
	@Override
	protected boolean isEligibleAdvisorBean(String beanName) {
		if (!isUsePrefix()) {
			return true;
		}
		String prefix = getAdvisorBeanNamePrefix();
		return (prefix != null && beanName.startsWith(prefix));
	}

}
