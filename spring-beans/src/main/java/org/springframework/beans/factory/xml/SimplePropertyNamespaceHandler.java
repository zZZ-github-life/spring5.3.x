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

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.core.Conventions;
import org.springframework.lang.Nullable;

/**
 * 简单的 {@code NamespaceHandler} 实现，将自定义属性直接映射到 Bean 属性。需要注意的重要一点是，此
 * {@code NamespaceHandler} 没有对应的 schema，因为无法预先知道所有可能的属性名称。
 *
 * <p>下面展示了此 {@code NamespaceHandler} 的用法示例：
 *
 * <pre class="code">
 * &lt;bean id=&quot;rob&quot; class=&quot;..TestBean&quot; p:name=&quot;Rob Harrop&quot; p:spouse-ref=&quot;sally&quot;/&gt;</pre>
 *
 * 这里的 '{@code p:name}' 直接对应于类 '{@code TestBean}' 上的 '{@code name}' 属性。'
 * {@code p:spouse-ref}' 属性对应于 '{@code spouse}' 属性，并且它不是具体值，而是包含将被注入到该属
 * 性中的 bean 的名称。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public class SimplePropertyNamespaceHandler implements NamespaceHandler {

	private static final String REF_SUFFIX = "-ref";


	@Override
	public void init() {
	}

	@Override
	@Nullable
	public BeanDefinition parse(Element element, ParserContext parserContext) {
		parserContext.getReaderContext().error(
				"Class [" + getClass().getName() + "] does not support custom elements.", element);
		return null;
	}

	@Override
	public BeanDefinitionHolder decorate(Node node, BeanDefinitionHolder definition, ParserContext parserContext) {
		if (node instanceof Attr) {
			Attr attr = (Attr) node;
			String propertyName = parserContext.getDelegate().getLocalName(attr);
			String propertyValue = attr.getValue();
			MutablePropertyValues pvs = definition.getBeanDefinition().getPropertyValues();
			if (pvs.contains(propertyName)) {
				parserContext.getReaderContext().error("Property '" + propertyName + "' is already defined using " +
						"both <property> and inline syntax. Only one approach may be used per property.", attr);
			}
			if (propertyName.endsWith(REF_SUFFIX)) {
				propertyName = propertyName.substring(0, propertyName.length() - REF_SUFFIX.length());
				pvs.add(Conventions.attributeNameToPropertyName(propertyName), new RuntimeBeanReference(propertyValue));
			}
			else {
				pvs.add(Conventions.attributeNameToPropertyName(propertyName), propertyValue);
			}
		}
		return definition;
	}

}
