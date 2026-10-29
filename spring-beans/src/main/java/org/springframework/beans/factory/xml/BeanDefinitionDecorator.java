/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.beans.factory.xml;

import org.w3c.dom.Node;

import org.springframework.beans.factory.config.BeanDefinitionHolder;

/**
 * 由 {@link DefaultBeanDefinitionDocumentReader} 用于处理自定义嵌套标签（直接位于 {@code <bean>}
 * 下）的接口。
 *
 * <p>装饰也可能基于应用于 {@code <bean>} 标签的自定义属性而进行。实现可以自由地将自定义标签中的元
 * 数据转换为所需数量的
 * {@link org.springframework.beans.factory.config.BeanDefinition BeanDefinitions}，并转换外层
 * {@code <bean>} 标签的 {@link org.springframework.beans.factory.config.BeanDefinition}，甚至可能
 * 返回一个完全不同的 {@link org.springframework.beans.factory.config.BeanDefinition} 来替换原始定
 * 义。
 *
 * <p>{@link BeanDefinitionDecorator BeanDefinitionDecorators} 应意识到它们可能属于一个链。特别是，
 * {@link BeanDefinitionDecorator} 应意识到之前的 {@link BeanDefinitionDecorator} 可能已将原始
 * {@link org.springframework.beans.factory.config.BeanDefinition} 替换为
 * {@link org.springframework.aop.framework.ProxyFactoryBean} 定义，从而允许添加自定义的
 * {@link org.aopalliance.intercept.MethodInterceptor interceptors}。
 *
 * <p>希望为外层 Bean 添加拦截器的 {@link BeanDefinitionDecorator BeanDefinitionDecorators} 应扩展
 * {@link org.springframework.aop.config.AbstractInterceptorDrivenBeanDefinitionDecorator}，它负责
 * 处理链式关系，确保只创建一个代理，并且该代理包含链中的所有拦截器。
 *
 * <p>解析器会从自定义标签所在命名空间的 {@link NamespaceHandler} 中定位
 * {@link BeanDefinitionDecorator}。
 *
 * @author Rob Harrop
 * @since 2.0
 * @see NamespaceHandler
 * @see BeanDefinitionParser
 */
public interface BeanDefinitionDecorator {

	/**
	 * 解析指定的 {@link Node}（元素或属性），并装饰提供的
	 * {@link org.springframework.beans.factory.config.BeanDefinition}，返回装饰后的定义。<p>实现可以选
	 * 择返回一个全新的定义，它将替换生成的 {@link org.springframework.beans.factory.BeanFactory} 中的
	 * 原始定义。<p>提供的 {@link ParserContext} 可用于注册支持主定义所需的任何附加 Bean。
	 */
	BeanDefinitionHolder decorate(Node node, BeanDefinitionHolder definition, ParserContext parserContext);

}
