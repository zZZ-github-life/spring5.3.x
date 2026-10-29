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

package org.springframework.aop.config;

import java.util.List;

import org.w3c.dom.Node;

import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionDecorator;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * 基类实现，供希望向生成的 Bean 添加
 * {@link org.aopalliance.intercept.MethodInterceptor interceptor} 的
 * {@link org.springframework.beans.factory.xml.BeanDefinitionDecorator BeanDefinitionDecorators}
 * 使用。
 *
 * <p>此基类控制 {@link ProxyFactoryBean} Bean 定义的创建，并将原始对象包装为
 * {@link ProxyFactoryBean} 的 {@code target} 属性的内部 Bean 定义。
 *
 * <p>链接会被正确处理，确保只创建一个 {@link ProxyFactoryBean} 定义。如果之前的
 * {@link org.springframework.beans.factory.xml.BeanDefinitionDecorator} 已经创建了
 * {@link org.springframework.aop.framework.ProxyFactoryBean}，则拦截器只会被添加到现有定义中。
 *
 * <p>子类只需为其希望添加的拦截器创建 {@code BeanDefinition}。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.aopalliance.intercept.MethodInterceptor
 */
public abstract class AbstractInterceptorDrivenBeanDefinitionDecorator implements BeanDefinitionDecorator {

	@Override
	public final BeanDefinitionHolder decorate(Node node, BeanDefinitionHolder definitionHolder, ParserContext parserContext) {
		BeanDefinitionRegistry registry = parserContext.getRegistry();

		// get the root bean name - will be the name of the generated proxy factory bean
		String existingBeanName = definitionHolder.getBeanName();
		BeanDefinition targetDefinition = definitionHolder.getBeanDefinition();
		BeanDefinitionHolder targetHolder = new BeanDefinitionHolder(targetDefinition, existingBeanName + ".TARGET");

		// delegate to subclass for interceptor definition
		BeanDefinition interceptorDefinition = createInterceptorDefinition(node);

		// generate name and register the interceptor
		String interceptorName = existingBeanName + '.' + getInterceptorNameSuffix(interceptorDefinition);
		BeanDefinitionReaderUtils.registerBeanDefinition(
				new BeanDefinitionHolder(interceptorDefinition, interceptorName), registry);

		BeanDefinitionHolder result = definitionHolder;

		if (!isProxyFactoryBeanDefinition(targetDefinition)) {
			// create the proxy definition
			RootBeanDefinition proxyDefinition = new RootBeanDefinition();
			// create proxy factory bean definition
			proxyDefinition.setBeanClass(ProxyFactoryBean.class);
			proxyDefinition.setScope(targetDefinition.getScope());
			proxyDefinition.setLazyInit(targetDefinition.isLazyInit());
			// set the target
			proxyDefinition.setDecoratedDefinition(targetHolder);
			proxyDefinition.getPropertyValues().add("target", targetHolder);
			// create the interceptor names list
			proxyDefinition.getPropertyValues().add("interceptorNames", new ManagedList<String>());
			// copy autowire settings from original bean definition.
			proxyDefinition.setAutowireCandidate(targetDefinition.isAutowireCandidate());
			proxyDefinition.setPrimary(targetDefinition.isPrimary());
			if (targetDefinition instanceof AbstractBeanDefinition) {
				proxyDefinition.copyQualifiersFrom((AbstractBeanDefinition) targetDefinition);
			}
			// wrap it in a BeanDefinitionHolder with bean name
			result = new BeanDefinitionHolder(proxyDefinition, existingBeanName);
		}

		addInterceptorNameToList(interceptorName, result.getBeanDefinition());
		return result;
	}

	@SuppressWarnings("unchecked")
	private void addInterceptorNameToList(String interceptorName, BeanDefinition beanDefinition) {
		List<String> list = (List<String>) beanDefinition.getPropertyValues().get("interceptorNames");
		Assert.state(list != null, "Missing 'interceptorNames' property");
		list.add(interceptorName);
	}

	private boolean isProxyFactoryBeanDefinition(BeanDefinition existingDefinition) {
		return ProxyFactoryBean.class.getName().equals(existingDefinition.getBeanClassName());
	}

	protected String getInterceptorNameSuffix(BeanDefinition interceptorDefinition) {
		String beanClassName = interceptorDefinition.getBeanClassName();
		return (StringUtils.hasLength(beanClassName) ?
				StringUtils.uncapitalize(ClassUtils.getShortName(beanClassName)) : "");
	}

	/**
	 * 子类应实现此方法，以返回其希望应用于被装饰 Bean 的拦截器的 {@code BeanDefinition}。
	 */
	protected abstract BeanDefinition createInterceptorDefinition(Node node);

}
