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

package org.springframework.aop.framework.autoproxy;

import java.util.ArrayList;
import java.util.List;

import org.springframework.aop.TargetSource;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

/**
 * 通过名称列表识别需要代理的 Bean 的自动代理创建器。支持直接匹配、"xxx*" 匹配和 "*xxx" 匹配。
 *
 * <p>有关配置细节，请参见父类 AbstractAutoProxyCreator 的 javadoc。通常，你会通过
 * "interceptorNames" 属性指定一组拦截器名称，以应用于所有被识别出的 Bean。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 10.10.2003
 * @see #setBeanNames
 * @see #isMatch
 * @see #setInterceptorNames
 * @see AbstractAutoProxyCreator
 */
@SuppressWarnings("serial")
public class BeanNameAutoProxyCreator extends AbstractAutoProxyCreator {

	private static final String[] NO_ALIASES = new String[0];

	@Nullable
	private List<String> beanNames;


	/**
	 * 设置应自动包装为代理的 Bean 的名称。名称可以以 "*" 结尾来指定前缀匹配，例如 "myBean,tx*" 将匹配
	 * 名为 "myBean" 的 Bean 以及所有名称以 "tx" 开头的 Bean。
	 * <p><b>NOTE:</b> 对于 FactoryBean，只会代理由该 FactoryBean 创建的对象。此默认行为自 Spring 2.0
	 * 起生效。如果你打算代理 FactoryBean 实例本身（罕见的用例，但这是 Spring 1.2 的默认行为），请指定
	 * 该 FactoryBean 的 Bean 名称并包含工厂 Bean 前缀 "&amp;"：例如 "&amp;myFactoryBean"。
	 * @see org.springframework.beans.factory.FactoryBean
	 * @see org.springframework.beans.factory.BeanFactory#FACTORY_BEAN_PREFIX
	 */
	public void setBeanNames(String... beanNames) {
		Assert.notEmpty(beanNames, "'beanNames' must not be empty");
		this.beanNames = new ArrayList<>(beanNames.length);
		for (String mappedName : beanNames) {
			this.beanNames.add(StringUtils.trimWhitespace(mappedName));
		}
	}


	/**
	 * 如果 Bean 名称与配置的受支持名称列表中的某一项匹配，则委托给
	 * {@link AbstractAutoProxyCreator#getCustomTargetSource(Class, String)}，否则返回 {@code null}。
	 * @since 5.3
	 * @see #setBeanNames(String...)
	 */
	@Override
	protected TargetSource getCustomTargetSource(Class<?> beanClass, String beanName) {
		return (isSupportedBeanName(beanClass, beanName) ?
				super.getCustomTargetSource(beanClass, beanName) : null);
	}

	/**
	 * 如果 Bean 名称与配置的受支持名称列表中的某一项匹配，则将其识别为需要代理的 Bean。
	 * @see #setBeanNames(String...)
	 */
	@Override
	@Nullable
	protected Object[] getAdvicesAndAdvisorsForBean(
			Class<?> beanClass, String beanName, @Nullable TargetSource targetSource) {

		return (isSupportedBeanName(beanClass, beanName) ?
				PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS : DO_NOT_PROXY);
	}

	/**
	 * 判断给定 Bean 类的 Bean 名称是否与配置的受支持名称列表中的某一项匹配。
	 * @param beanClass the class of the bean to advise
	 * @param beanName the name of the bean
	 * @return {@code true} if the given bean name is supported
	 * @see #setBeanNames(String...)
	 */
	private boolean isSupportedBeanName(Class<?> beanClass, String beanName) {
		if (this.beanNames != null) {
			boolean isFactoryBean = FactoryBean.class.isAssignableFrom(beanClass);
			for (String mappedName : this.beanNames) {
				if (isFactoryBean) {
					if (!mappedName.startsWith(BeanFactory.FACTORY_BEAN_PREFIX)) {
						continue;
					}
					mappedName = mappedName.substring(BeanFactory.FACTORY_BEAN_PREFIX.length());
				}
				if (isMatch(beanName, mappedName)) {
					return true;
				}
			}

			BeanFactory beanFactory = getBeanFactory();
			String[] aliases = (beanFactory != null ? beanFactory.getAliases(beanName) : NO_ALIASES);
			for (String alias : aliases) {
				for (String mappedName : this.beanNames) {
					if (isMatch(alias, mappedName)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * 判断给定 Bean 名称是否与映射名称匹配。
	 * <p>默认实现会检查 "xxx*"、"*xxx" 和 "*xxx*" 匹配以及直接相等。可在子类中覆盖。
	 * @param beanName the bean name to check
	 * @param mappedName the name in the configured list of names
	 * @return if the names match
	 * @see org.springframework.util.PatternMatchUtils#simpleMatch(String, String)
	 */
	protected boolean isMatch(String beanName, String mappedName) {
		return PatternMatchUtils.simpleMatch(mappedName, beanName);
	}

}
