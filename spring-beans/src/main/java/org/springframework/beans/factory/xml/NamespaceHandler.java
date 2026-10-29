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

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.lang.Nullable;

/**
 * 由 {@link DefaultBeanDefinitionDocumentReader} 使用的基础接口，用于处理 Spring XML 配置文件中的
 * 自定义命名空间。
 *
 * <p>实现类应为自定义顶层标签返回 {@link BeanDefinitionParser} 接口的实现，为自定义嵌套标签返回
 * {@link BeanDefinitionDecorator} 接口的实现。
 *
 * <p>解析器在遇到位于 {@code <beans>} 标签正下方的自定义标签时会调用 {@link #parse}，在遇到位于
 * {@code <bean>} 标签正下方的自定义标签时会调用 {@link #decorate}。
 *
 * <p>编写自己自定义元素扩展的开发者通常不会直接实现此接口，而是使用所提供的
 * {@link NamespaceHandlerSupport} 类。
 *
 * @author Rob Harrop
 * @author Erik Wiersma
 * @since 2.0
 * @see DefaultBeanDefinitionDocumentReader
 * @see NamespaceHandlerResolver
 */
public interface NamespaceHandler {

	/**
	 * 由 {@link DefaultBeanDefinitionDocumentReader} 在构造之后、解析任何自定义元素之前调用。
	 * @see NamespaceHandlerSupport#registerBeanDefinitionParser(String, BeanDefinitionParser)
	 */
	void init();

	/**
	 * 解析指定的 {@link Element}，并将任何产生的 {@link BeanDefinition BeanDefinitions} 注册到嵌入在所
	 * 提供 {@link ParserContext} 中的
	 * {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}。<p>如果实现希望被嵌套
	 * 使用（例如）在 {@code <property>} 标签内，则应返回解析阶段产生的主 {@code BeanDefinition}。<p>如
	 * 果实现<strong>不会</strong>用于嵌套场景，则可以返回 {@code null}。
	 * @param element the element that is to be parsed into one or more {@code BeanDefinitions}
	 * @param parserContext the object encapsulating the current state of the parsing process
	 * @return the primary {@code BeanDefinition} (can be {@code null} as explained above)
	 */
	@Nullable
	BeanDefinition parse(Element element, ParserContext parserContext);

	/**
	 * 解析指定的 {@link Node} 并装饰所提供的 {@link BeanDefinitionHolder}，返回装饰后的定义。<p>根据正
	 * 在解析的是自定义属性还是元素，{@link Node} 可以是 {@link org.w3c.dom.Attr} 或 {@link Element}。
	 * <p>实现可以选择返回一个全新的定义，它将替换
	 * {@link org.springframework.beans.factory.BeanFactory} 中的原始定义。<p>所提供的
	 * {@link ParserContext} 可用于注册支持主定义所需的任何附加 bean。
	 * @param source the source element or attribute that is to be parsed
	 * @param definition the current bean definition
	 * @param parserContext the object encapsulating the current state of the parsing process
	 * @return the decorated definition (to be registered in the BeanFactory),
	 * or simply the original bean definition if no decoration is required.
	 * A {@code null} value is strictly speaking invalid, but will be leniently
	 * treated like the case where the original bean definition gets returned.
	 */
	@Nullable
	BeanDefinitionHolder decorate(Node source, BeanDefinitionHolder definition, ParserContext parserContext);

}
