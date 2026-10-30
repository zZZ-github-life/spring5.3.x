/*
 * Copyright 2002-2014 the original author or authors.
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
import org.w3c.dom.NamedNodeMap;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.core.Conventions;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 当待解析元素上的属性名与所配置 {@link Class} 上的属性名之间存在一一对应关系时使用的便捷基类。
 *
 * <p>当希望从相对简单的自定义 XML 元素创建单个 Bean 定义时，请扩展此解析器类。生成的
 * {@code BeanDefinition} 将自动注册到相关的
 * {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}。
 *
 * <p>希望通过一个示例能立即阐明这个特定解析器类的用法。考虑以下类定义：
 *
 * <pre class="code">public class SimpleCache implements Cache {
 *
 *     public void setName(String name) {...}
 *     public void setTimeout(int timeout) {...}
 *     public void setEvictionPolicy(EvictionPolicy policy) {...}
 *
 *     // remaining class definition elided for clarity...
 * }</pre>
 *
 * <p>然后假设已定义以下 XML 标签，以便轻松配置上述类的实例；
 *
 * <pre class="code">&lt;caching:cache name="..." timeout="..." eviction-policy="..."/&gt;</pre>
 *
 * <p>负责编写解析器以将上述 XML 标签解析为实际 {@code SimpleCache} Bean 定义的 Java 开发者，只需完
 * 成以下内容：
 *
 * <pre class="code">public class SimpleCacheBeanDefinitionParser extends AbstractSimpleBeanDefinitionParser {
 *
 *     protected Class getBeanClass(Element element) {
 *         return SimpleCache.class;
 *     }
 * }</pre>
 *
 * <p>请注意，{@code AbstractSimpleBeanDefinitionParser} 仅限于使用属性值填充所创建的 Bean 定义。如
 * 果希望从提供的 XML 元素中解析构造器参数和嵌套元素，则必须实现
 * {@link #postProcess(org.springframework.beans.factory.support.BeanDefinitionBuilder, org.w3c.dom.Element)}
 * 方法并自行完成此类解析，或者（更可能的情况是）直接继承
 * {@link AbstractSingleBeanDefinitionParser} 或 {@link AbstractBeanDefinitionParser} 类。
 *
 * <p>将 {@code SimpleCacheBeanDefinitionParser} 实际注册到 Spring XML 解析基础设施的过程在 Spring
 * Framework 参考文档中（某个附录里）有描述。
 *
 * <p>如需查看此解析器实际运行的示例（可以说是这样），请查看
 * {@link org.springframework.beans.factory.xml.UtilNamespaceHandler.PropertiesBeanDefinitionParser}
 * 的源代码；细心的（甚至不那么细心的）读者会立即注意到，该实现中几乎没有代码。
 * {@code PropertiesBeanDefinitionParser} 会从如下所示的 XML 元素填充
 * {@link org.springframework.beans.factory.config.PropertiesFactoryBean}：
 *
 * <pre class="code">&lt;util:properties location="jdbc.properties"/&gt;</pre>
 *
 * <p>细心的读者会注意到，{@code <util:properties/>} 元素上唯一的属性与
 * {@code PropertiesFactoryBean} 上的
 * {@link org.springframework.beans.factory.config.PropertiesFactoryBean#setLocation(org.springframework.core.io.Resource)}
 * 方法名相匹配（由此说明的一般用法对任意数量的属性都成立）。{@code PropertiesBeanDefinitionParser}
 * 实际需要做的只是提供 {@link #getBeanClass(org.w3c.dom.Element)} 的实现，使其返回
 * {@code PropertiesFactoryBean} 类型。
 *
 * @author Rob Harrop
 * @author Rick Evans
 * @author Juergen Hoeller
 * @since 2.0
 * @see Conventions#attributeNameToPropertyName(String)
 */
public abstract class AbstractSimpleBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

	/**
	 * 按要求解析提供的 {@link Element} 并填充提供的 {@link BeanDefinitionBuilder}。<p>此实现会将提供的
	 * 元素上存在的任何属性映射为 {@link org.springframework.beans.PropertyValue} 实例，并
	 * {@link BeanDefinitionBuilder#addPropertyValue(String, Object) 将其添加}到
	 * {@link org.springframework.beans.factory.config.BeanDefinition 构建器}中。<p>
	 * {@link #extractPropertyName(String)} 方法用于协调属性名与 JavaBean 属性名。
	 * @param element 正在解析的 XML 元素
	 * @param builder 用于定义 {@code BeanDefinition}
	 * @see #extractPropertyName(String)
	 */
	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		NamedNodeMap attributes = element.getAttributes();
		for (int x = 0; x < attributes.getLength(); x++) {
			Attr attribute = (Attr) attributes.item(x);
			if (isEligibleAttribute(attribute, parserContext)) {
				String propertyName = extractPropertyName(attribute.getLocalName());
				Assert.state(StringUtils.hasText(propertyName),
						"Illegal property name returned from 'extractPropertyName(String)': cannot be null or empty.");
				builder.addPropertyValue(propertyName, attribute.getValue());
			}
		}
		postProcess(builder, element);
	}

	/**
	 * 确定给定属性是否符合转换为相应 Bean 属性值的条件。<p>默认实现认为除“id”属性和命名空间声明属性之
	 * 外的任何属性都符合条件。
	 * @param attribute 要检查的 XML 属性
	 * @param parserContext 该 {@code ParserContext}
	 * @see #isEligibleAttribute(String)
	 */
	protected boolean isEligibleAttribute(Attr attribute, ParserContext parserContext) {
		String fullName = attribute.getName();
		return (!fullName.equals("xmlns") && !fullName.startsWith("xmlns:") &&
				isEligibleAttribute(parserContext.getDelegate().getLocalName(attribute)));
	}

	/**
	 * 确定给定属性是否符合转换为相应 Bean 属性值的条件。<p>默认实现认为除“id”属性之外的任何属性都符合
	 * 条件。
	 * @param attributeName 直接从正在解析的 XML 元素中获取的属性名称（永不为 {@code null}）
	 */
	protected boolean isEligibleAttribute(String attributeName) {
		return !ID_ATTRIBUTE.equals(attributeName);
	}

	/**
	 * 从提供的属性名中提取 JavaBean 属性名。<p>默认实现使用
	 * {@link Conventions#attributeNameToPropertyName(String)} 方法执行提取。<p>返回的名称必须遵循标准
	 * 的 JavaBean 属性名约定。例如，对于具有 setter 方法 '{@code setBingoHallFavourite(String)}' 的类，
	 * 返回的名称最好是 '{@code bingoHallFavourite}'（大小写完全一致）。
	 * @param attributeName 直接从正在解析的 XML 元素中获取的属性名称（永不为 {@code null}）
	 * @return 提取出的 JavaBean 属性名称（绝不能为 {@code null}）
	 */
	protected String extractPropertyName(String attributeName) {
		return Conventions.attributeNameToPropertyName(attributeName);
	}

	/**
	 * 钩子方法，派生类可以实现它，以便在解析完成后检查/更改 Bean 定义。<p>默认实现不执行任何操作。
	 * @param beanDefinition 正在构建的已解析（且可能已完全定义）的 Bean 定义
	 * @param element 作为该 Bean 定义元数据来源的 XML 元素
	 */
	protected void postProcess(BeanDefinitionBuilder beanDefinition, Element element) {
	}

}
