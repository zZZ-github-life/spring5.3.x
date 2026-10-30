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
	 * @param element 构建该 Bean 定义所依据的元素
	 * @param definition 要注册的 Bean 定义
	 * @param parserContext 封装解析过程当前状态的对象；
	 * 提供对 {@link org.springframework.beans.factory.support.BeanDefinitionRegistry} 的访问
	 * @return 解析后的 id
	 * @throws BeanDefinitionStoreException 如果无法为给定的 Bean 定义生成唯一名称
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
	 * @param definition 要注册的 Bean 定义
	 * @param registry 要将该 Bean 注册到的注册表
	 * @see BeanDefinitionReaderUtils#registerBeanDefinition(BeanDefinitionHolder, BeanDefinitionRegistry)
	 */
	protected void registerBeanDefinition(BeanDefinitionHolder definition, BeanDefinitionRegistry registry) {
		BeanDefinitionReaderUtils.registerBeanDefinition(definition, registry);
	}


	/**
	 * 核心模板方法，用于将提供的 {@link Element} 实际解析为一个或多个
	 * {@link BeanDefinition BeanDefinitions}。
	 * @param element 要解析为一个或多个 {@link BeanDefinition BeanDefinitions} 的元素
	 * @param parserContext 封装解析过程当前状态的对象；
	 * 提供对 {@link org.springframework.beans.factory.support.BeanDefinitionRegistry} 的访问
	 * @return 解析所提供的 {@link Element} 得到的主要 {@link BeanDefinition}
	 * @see #parse(org.w3c.dom.Element, ParserContext)
	 * @see #postProcessComponentDefinition(org.springframework.beans.factory.parsing.BeanComponentDefinition)
	 */
	@Nullable
	protected abstract AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext);

	/**
	 * 是否应生成 ID，而不是从传入的 {@link Element} 中读取？<p>默认禁用；子类可以覆盖此方法以启用 ID
	 * 生成。请注意，此标志表示<i>始终</i>生成 ID；在这种情况下，解析器甚至不会检查“id”属性。
	 * @return 解析器是否应始终生成 id
	 */
	protected boolean shouldGenerateId() {
		return false;
	}

	/**
	 * 如果传入的 {@link Element} 未显式指定“id”属性，是否应改为生成 ID？<p>默认禁用；子类可以覆盖此方
	 * 法以启用作为回退的 ID 生成：在这种情况下，解析器会先检查“id”属性，仅当未指定值时才会回退到生成的
	 * ID。
	 * @return 解析器在未指定 id 时是否应生成 id
	 */
	protected boolean shouldGenerateIdAsFallback() {
		return false;
	}

	/**
	 * 确定是否应将元素的“name”属性解析为 Bean 定义别名，即备用的 Bean 定义名称。<p>默认实现返回
	 * {@code true}。
	 * @return 解析器是否应将 "name" 属性解析为别名
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
	 * @return {@code true} 表示在解析 Bean 定义后触发组件注册事件；
	 * {@code false} 表示抑制该事件
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
	 * @param componentDefinition 要处理的 {@link BeanComponentDefinition}
	 */
	protected void postProcessComponentDefinition(BeanComponentDefinition componentDefinition) {
	}

}
