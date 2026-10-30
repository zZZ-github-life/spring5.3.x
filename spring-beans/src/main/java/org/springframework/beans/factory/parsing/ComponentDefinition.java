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

package org.springframework.beans.factory.parsing;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;

/**
 * 描述某一配置上下文中呈现的一组 {@link BeanDefinition BeanDefinitions} 和
 * {@link BeanReference BeanReferences} 的逻辑视图的接口。
 *
 * <p>随着{@link org.springframework.beans.factory.xml.NamespaceHandler 可插拔的自定义 XML 标签}的
 * 引入，现在一个逻辑配置实体（在这里是一个 XML 标签）可以创建多个
 * {@link BeanDefinition BeanDefinitions} 和 {@link BeanReference RuntimeBeanReferences}，从而提供
 * 更简洁的配置并为最终用户带来更大的便利。因此，不能再假定每个配置实体（例如 XML 标签）都映射到一
 * 个 {@link BeanDefinition}。对于希望为配置 Spring 应用提供可视化或支持的工具供应商和其他用户而言，
 * 重要的是要有某种机制，将 {@link org.springframework.beans.factory.BeanFactory} 中的
 * {@link BeanDefinition BeanDefinitions} 以对最终用户具有具体意义的方式关联回配置数据。因此，
 * {@link org.springframework.beans.factory.xml.NamespaceHandler} 实现能够为每个正在配置的逻辑实体
 * 以 {@code ComponentDefinition} 的形式发布事件。第三方随后可以
 * {@link ReaderEventListener 订阅这些事件}，从而获得以用户为中心的 Bean 元数据视图。
 *
 * <p>每个 {@code ComponentDefinition} 都有一个特定于配置的{@link #getSource 源对象}。对于基于 XML
 * 的配置，这通常是包含用户提供的配置信息的 {@link org.w3c.dom.Node}。除此之外，
 * {@code ComponentDefinition} 中包含的每个 {@link BeanDefinition} 都有自己的
 * {@link BeanDefinition#getSource() 源对象}，它可能指向一组不同且更具体的配置数据。更进一步，诸如
 * {@link org.springframework.beans.PropertyValue PropertyValues} 这样的单个 Bean 元数据片段也可能
 * 具有源对象，从而提供更细粒度的细节。源对象的提取通过 {@link SourceExtractor} 处理，可按需进行自
 * 定义。
 *
 * <p>虽然可以通过 {@link #getBeanReferences} 直接访问重要的 {@link BeanReference BeanReferences}，
 * 但工具可能希望检查所有 {@link BeanDefinition BeanDefinitions} 以收集完整的
 * {@link BeanReference BeanReferences} 集合。实现类必须提供验证整个逻辑实体的配置所需的所有
 * {@link BeanReference BeanReferences}，以及提供配置的完整用户可视化所需的所有引用。预计某些
 * {@link BeanReference BeanReferences} 对校验或配置的用户视图并不重要，因此可以省略。工具可能希望
 * 显示通过所提供的 {@link BeanDefinition BeanDefinitions} 获得的任何额外
 * {@link BeanReference BeanReferences}，但这并不属于典型情况。
 *
 * <p>工具可以通过检查{@link BeanDefinition#getRole 角色标识符}来判断所包含的
 * {@link BeanDefinition BeanDefinitions} 的重要性。角色本质上是向工具提示配置提供者认为某个
 * {@link BeanDefinition} 对最终用户有多重要。预计工具<strong>不会</strong>显示给定
 * {@code ComponentDefinition} 的所有 {@link BeanDefinition BeanDefinitions}，而是选择基于角色进行
 * 过滤。工具可以使这种过滤可由用户配置。应特别注意
 * {@link BeanDefinition#ROLE_INFRASTRUCTURE INFRASTRUCTURE 角色标识符}。归类为此角色的
 * {@link BeanDefinition BeanDefinitions} 对最终用户完全不重要，仅出于内部实现原因而需要。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 * @see AbstractComponentDefinition
 * @see CompositeComponentDefinition
 * @see BeanComponentDefinition
 * @see ReaderEventListener#componentRegistered(ComponentDefinition)
 */
public interface ComponentDefinition extends BeanMetadataElement {

	/**
	 * 获取此 {@code ComponentDefinition} 的用户可见名称。
	 *
	 * <p>它应直接链接回给定上下文中该组件对应的配置数据。
	 */
	String getName();

	/**
	 * 返回所描述组件的友好描述。
	 *
	 * <p>建议实现类从 {@code toString()} 返回相同的值。
	 */
	String getDescription();

	/**
	 * 返回为构成此 {@code ComponentDefinition} 而注册的 {@link BeanDefinition BeanDefinitions}。
	 *
	 * <p>需要注意的是，一个 {@code ComponentDefinition} 很可能通过{@link BeanReference 引用}与其他
	 * {@link BeanDefinition BeanDefinitions} 相关联，但这些引用<strong>不会</strong>包含在内，因为它们
	 * 可能无法立即获得。重要的 {@link BeanReference BeanReferences} 可从 {@link #getBeanReferences()}
	 * 获取。
	 * @return BeanDefinition 数组；若没有则为空数组
	 */
	BeanDefinition[] getBeanDefinitions();

	/**
	 * 返回表示此组件中所有相关内部 Bean 的 {@link BeanDefinition BeanDefinitions}。
	 *
	 * <p>关联的 {@link BeanDefinition BeanDefinitions} 中可能存在其他内部 Bean，但这些内部 Bean 不被视
	 * 为校验或用户可视化所需。
	 * @return BeanDefinition 数组；若没有则为空数组
	 */
	BeanDefinition[] getInnerBeanDefinitions();

	/**
	 * 返回被认为对此 {@code ComponentDefinition} 重要的 {@link BeanReference BeanReferences} 集合。
	 *
	 * <p>关联的 {@link BeanDefinition BeanDefinitions} 中可能存在其他
	 * {@link BeanReference BeanReferences}，但这些引用不被视为校验或用户可视化所需。
	 * @return BeanReference 数组；若没有则为空数组
	 */
	BeanReference[] getBeanReferences();

}
