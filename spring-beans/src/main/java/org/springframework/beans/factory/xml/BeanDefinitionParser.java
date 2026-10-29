/*
 * Copyright 2002-2011 the original author or authors.
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
import org.springframework.lang.Nullable;

/**
 * 由 {@link DefaultBeanDefinitionDocumentReader} 用于处理自定义顶级标签（直接位于 {@code <beans/>}
 * 下）的接口。
 *
 * <p>实现可以自由地将自定义标签中的元数据转换为所需数量的 {@link BeanDefinition BeanDefinitions}。
 *
 * <p>解析器会从自定义标签所在命名空间关联的 {@link NamespaceHandler} 中定位
 * {@link BeanDefinitionParser}。
 *
 * @author Rob Harrop
 * @since 2.0
 * @see NamespaceHandler
 * @see AbstractBeanDefinitionParser
 */
public interface BeanDefinitionParser {

	/**
	 * 解析指定的 {@link Element}，并将生成的 {@link BeanDefinition BeanDefinition(s)} 注册到嵌入在提供
	 * 的 {@link ParserContext} 中的
	 * {@link org.springframework.beans.factory.xml.ParserContext#getRegistry() BeanDefinitionRegistry}。
	 * <p>如果实现会被以嵌套方式使用（例如作为 {@code <property/>} 标签中的内部标签），则必须返回解析
	 * 得到的主要 {@link BeanDefinition}。如果实现<strong>不会</strong>以嵌套方式使用，则可以返回
	 * {@code null}。
	 * @param element the element that is to be parsed into one or more {@link BeanDefinition BeanDefinitions}
	 * @param parserContext the object encapsulating the current state of the parsing process;
	 * provides access to a {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}
	 * @return the primary {@link BeanDefinition}
	 */
	@Nullable
	BeanDefinition parse(Element element, ParserContext parserContext);

}
