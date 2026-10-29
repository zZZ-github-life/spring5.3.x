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

package org.springframework.beans.factory.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.lang.Nullable;

/**
 * 用于仅需解析和定义<i>单个</i> {@code BeanDefinition} 的 {@link BeanDefinitionParser} 实现基类。
 *
 * <p>当希望从任意复杂的 XML 元素创建单个 Bean 定义时，请扩展此解析器类。当希望从相对简单的自定义
 * XML 元素创建单个 Bean 定义时，可以考虑扩展 {@link AbstractSimpleBeanDefinitionParser}。
 *
 * <p>生成的 {@code BeanDefinition} 将自动注册到
 * {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}。你只需
 * {@link #doParse 解析}自定义 XML {@link Element}，将其转换为单个 {@code BeanDefinition}。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rick Evans
 * @since 2.0
 * @see #getBeanClass
 * @see #getBeanClassName
 * @see #doParse
 */
public abstract class AbstractSingleBeanDefinitionParser extends AbstractBeanDefinitionParser {

	/**
	 * 为 {@link #getBeanClass bean Class} 创建 {@link BeanDefinitionBuilder} 实例，并将其传递给
	 * {@link #doParse} 策略方法。
	 * @param element the element that is to be parsed into a single BeanDefinition
	 * @param parserContext the object encapsulating the current state of the parsing process
	 * @return the BeanDefinition resulting from the parsing of the supplied {@link Element}
	 * @throws IllegalStateException if the bean {@link Class} returned from
	 * {@link #getBeanClass(org.w3c.dom.Element)} is {@code null}
	 * @see #doParse
	 */
	@Override
	protected final AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition();
		String parentName = getParentName(element);
		if (parentName != null) {
			builder.getRawBeanDefinition().setParentName(parentName);
		}
		Class<?> beanClass = getBeanClass(element);
		if (beanClass != null) {
			builder.getRawBeanDefinition().setBeanClass(beanClass);
		}
		else {
			String beanClassName = getBeanClassName(element);
			if (beanClassName != null) {
				builder.getRawBeanDefinition().setBeanClassName(beanClassName);
			}
		}
		builder.getRawBeanDefinition().setSource(parserContext.extractSource(element));
		BeanDefinition containingBd = parserContext.getContainingBeanDefinition();
		if (containingBd != null) {
			// Inner bean definition must receive same scope as containing bean.
			builder.setScope(containingBd.getScope());
		}
		if (parserContext.isDefaultLazyInit()) {
			// Default-lazy-init applies to custom bean definitions as well.
			builder.setLazyInit(true);
		}
		doParse(element, parserContext, builder);
		return builder.getBeanDefinition();
	}

	/**
	 * 在当前 Bean 被定义为子 Bean 的情况下，确定当前解析的 Bean 的父级名称。<p>默认实现返回
	 * {@code null}，表示根 Bean 定义。
	 * @param element the {@code Element} that is being parsed
	 * @return the name of the parent bean for the currently parsed bean,
	 * or {@code null} if none
	 */
	@Nullable
	protected String getParentName(Element element) {
		return null;
	}

	/**
	 * 确定与提供的 {@link Element} 对应的 Bean 类。<p>请注意，对于应用类，通常更倾向于覆盖
	 * {@link #getBeanClassName}，以避免直接依赖 Bean 实现类。这样即使插件类路径上没有应用类，也可以在
	 * IDE 插件中使用 BeanDefinitionParser 及其 NamespaceHandler。
	 * @param element the {@code Element} that is being parsed
	 * @return the {@link Class} of the bean that is being defined via parsing
	 * the supplied {@code Element}, or {@code null} if none
	 * @see #getBeanClassName
	 */
	@Nullable
	protected Class<?> getBeanClass(Element element) {
		return null;
	}

	/**
	 * 确定与提供的 {@link Element} 对应的 Bean 类名。
	 * @param element the {@code Element} that is being parsed
	 * @return the class name of the bean that is being defined via parsing
	 * the supplied {@code Element}, or {@code null} if none
	 * @see #getBeanClass
	 */
	@Nullable
	protected String getBeanClassName(Element element) {
		return null;
	}

	/**
	 * 按要求解析提供的 {@link Element} 并填充提供的 {@link BeanDefinitionBuilder}。<p>默认实现委托给不
	 * 带 ParserContext 参数的 {@code doParse} 版本。
	 * @param element the XML element being parsed
	 * @param parserContext the object encapsulating the current state of the parsing process
	 * @param builder used to define the {@code BeanDefinition}
	 * @see #doParse(Element, BeanDefinitionBuilder)
	 */
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		doParse(element, builder);
	}

	/**
	 * 按要求解析提供的 {@link Element} 并填充提供的 {@link BeanDefinitionBuilder}。<p>默认实现不执行任
	 * 何操作。
	 * @param element the XML element being parsed
	 * @param builder used to define the {@code BeanDefinition}
	 */
	protected void doParse(Element element, BeanDefinitionBuilder builder) {
	}

}
