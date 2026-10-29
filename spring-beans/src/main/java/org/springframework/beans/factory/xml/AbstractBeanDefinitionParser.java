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

package org.springframework.beans.factory.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * 抽象的 {@link BeanDefinitionParser} 实现，提供若干便捷方法以及一个
 * {@link AbstractBeanDefinitionParser#parseInternal 模板方法}，子类必须覆盖该方法以提供实际的解析
 * 逻辑。
 *
 * <p>当需要将任意复杂的 XML 解析为一个或多个 {@link BeanDefinition BeanDefinitions} 时，请使用此
 * {@link BeanDefinitionParser} 实现。如果只想将 XML 解析为单个 {@code BeanDefinition}，可以考虑本
 * 类更简单的便捷扩展，即 {@link AbstractSingleBeanDefinitionParser} 和
 * {@link AbstractSimpleBeanDefinitionParser}。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Dave Syer
 * @since 2.0
 */
public abstract class AbstractBeanDefinitionParser implements BeanDefinitionParser {

	/** 用于 "id" 属性的常量。 */
	public static final String ID_ATTRIBUTE = "id";

	/** 用于 "name" 属性的常量。 */
	public static final String NAME_ATTRIBUTE = "name";


	@Override
	@Nullable
	public final BeanDefinition parse(Element element, ParserContext parserContext) {
		AbstractBeanDefinition definition = parseInternal(element, parserContext);
		if (definition != null && !parserContext.isNested()) {
			try {
				String id = resolveId(element, definition, parserContext);
				if (!StringUtils.hasText(id)) {
					parserContext.getReaderContext().error(
							"Id is required for element '" + parserContext.getDelegate().getLocalName(element)
									+ "' when used as a top-level tag", element);
				}
				String[] aliases = null;
				if (shouldParseNameAsAliases()) {
					String name = element.getAttribute(NAME_ATTRIBUTE);
					if (StringUtils.hasLength(name)) {
						aliases = StringUtils.trimArrayElements(StringUtils.commaDelimitedListToStringArray(name));
					}
				}
				BeanDefinitionHolder holder = new BeanDefinitionHolder(definition, id, aliases);
				registerBeanDefinition(holder, parserContext.getRegistry());
				if (shouldFireEvents()) {
					BeanComponentDefinition componentDefinition = new BeanComponentDefinition(holder);
					postProcessComponentDefinition(componentDefinition);
					parserContext.registerComponent(componentDefinition);
				}
			}
			catch (BeanDefinitionStoreException ex) {
				String msg = ex.getMessage();
				parserContext.getReaderContext().error((msg != null ? msg : ex.toString()), element);
				return null;
			}
		}
		return definition;
	}

	/**
	 * 解析所提供 {@link BeanDefinition} 的 ID。<p>使用{@link #shouldGenerateId 生成}时，会自动生成名称。
	 * 否则，ID 从“id”属性中提取，可能通过{@link #shouldGenerateIdAsFallback() 回退}到生成的 id。
	 * @param element the element that the bean definition has been built from
	 * @param definition the bean definition to be registered
	 * @param parserContext the object encapsulating the current state of the parsing process;
	 * provides access to a {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}
	 * @return the resolved id
	 * @throws BeanDefinitionStoreException if no unique name could be generated
	 * for the given bean definition
	 */
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
			throws BeanDefinitionStoreException {

		if (shouldGenerateId()) {
			return parserContext.getReaderContext().generateBeanName(definition);
		}
		else {
			String id = element.getAttribute(ID_ATTRIBUTE);
			if (!StringUtils.hasText(id) && shouldGenerateIdAsFallback()) {
				id = parserContext.getReaderContext().generateBeanName(definition);
			}
			return id;
		}
	}

	/**
	 * 将提供的 {@link BeanDefinitionHolder bean} 注册到提供的 {@link BeanDefinitionRegistry registry}。
	 * <p>子类可以覆盖此方法，以控制是否实际注册提供的 {@link BeanDefinitionHolder bean}，或注册更多
	 * Bean。<p>默认实现仅在 {@code isNested} 参数为 {@code false} 时，才将提供的
	 * {@link BeanDefinitionHolder bean} 注册到提供的 {@link BeanDefinitionRegistry registry}，因为通常
	 * 不希望将内部 Bean 注册为顶级 Bean。
	 * @param definition the bean definition to be registered
	 * @param registry the registry that the bean is to be registered with
	 * @see BeanDefinitionReaderUtils#registerBeanDefinition(BeanDefinitionHolder, BeanDefinitionRegistry)
	 */
	protected void registerBeanDefinition(BeanDefinitionHolder definition, BeanDefinitionRegistry registry) {
		BeanDefinitionReaderUtils.registerBeanDefinition(definition, registry);
	}


	/**
	 * 核心模板方法，用于将提供的 {@link Element} 实际解析为一个或多个
	 * {@link BeanDefinition BeanDefinitions}。
	 * @param element the element that is to be parsed into one or more {@link BeanDefinition BeanDefinitions}
	 * @param parserContext the object encapsulating the current state of the parsing process;
	 * provides access to a {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}
	 * @return the primary {@link BeanDefinition} resulting from the parsing of the supplied {@link Element}
	 * @see #parse(org.w3c.dom.Element, ParserContext)
	 * @see #postProcessComponentDefinition(org.springframework.beans.factory.parsing.BeanComponentDefinition)
	 */
	@Nullable
	protected abstract AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext);

	/**
	 * 是否应生成 ID，而不是从传入的 {@link Element} 中读取？<p>默认禁用；子类可以覆盖此方法以启用 ID
	 * 生成。请注意，此标志表示<i>始终</i>生成 ID；在这种情况下，解析器甚至不会检查“id”属性。
	 * @return whether the parser should always generate an id
	 */
	protected boolean shouldGenerateId() {
		return false;
	}

	/**
	 * 如果传入的 {@link Element} 未显式指定“id”属性，是否应改为生成 ID？<p>默认禁用；子类可以覆盖此方
	 * 法以启用作为回退的 ID 生成：在这种情况下，解析器会先检查“id”属性，仅当未指定值时才会回退到生成的
	 * ID。
	 * @return whether the parser should generate an id if no id was specified
	 */
	protected boolean shouldGenerateIdAsFallback() {
		return false;
	}

	/**
	 * 确定是否应将元素的“name”属性解析为 Bean 定义别名，即备用的 Bean 定义名称。<p>默认实现返回
	 * {@code true}。
	 * @return whether the parser should evaluate the "name" attribute as aliases
	 * @since 4.1.5
	 */
	protected boolean shouldParseNameAsAliases() {
		return true;
	}

	/**
	 * 确定此解析器是否应在解析 Bean 定义后触发
	 * {@link org.springframework.beans.factory.parsing.BeanComponentDefinition} 事件。<p>此实现默认返
	 * 回 {@code true}；也就是说，当 Bean 定义完全解析后会触发事件。覆盖此方法并返回 {@code false} 可抑
	 * 制该事件。
	 * @return {@code true} in order to fire a component registration event
	 * after parsing the bean definition; {@code false} to suppress the event
	 * @see #postProcessComponentDefinition
	 * @see org.springframework.beans.factory.parsing.ReaderContext#fireComponentRegistered
	 */
	protected boolean shouldFireEvents() {
		return true;
	}

	/**
	 * 钩子方法，在 {@link BeanComponentDefinition} 的主解析完成之后、但在该
	 * {@link BeanComponentDefinition} 注册到
	 * {@link org.springframework.beans.factory.support.BeanDefinitionRegistry} 之前调用。<p>派生类可以
	 * 覆盖此方法，以提供在所有解析完成后要执行的任何自定义逻辑。<p>默认实现为空操作。
	 * @param componentDefinition the {@link BeanComponentDefinition} that is to be processed
	 */
	protected void postProcessComponentDefinition(BeanComponentDefinition componentDefinition) {
	}

}
