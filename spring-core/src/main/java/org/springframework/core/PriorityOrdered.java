/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.core;

/**
 * {@link Ordered} 接口的扩展，表达一种<em>优先级</em>排序：无论 order 值如何，
 * {@code PriorityOrdered} 对象总是先于<em>普通</em> {@link Ordered} 对象应用。
 *
 * <p>在对一组 {@code Ordered} 对象排序时，{@code PriorityOrdered} 对象和<em>普通</em>
 * {@code Ordered} 对象实际上被视为两个独立的子集，其中 {@code PriorityOrdered} 对象集合排在<em>普
 * 通</em> {@code Ordered} 对象集合之前，并在这些子集内部应用相对排序。
 *
 * <p>这主要是一个特殊用途接口，在框架内部用于那些特别需要首先识别<em>高优先级</em>对象（甚至可能无
 * 需获取其余对象）的场景。一个典型示例：Spring
 * {@link org.springframework.context.ApplicationContext} 中的高优先级后处理器。
 *
 * <p>注意：{@code PriorityOrdered} 后处理器 Bean 会在一个特殊阶段初始化，早于其他后处理器 Bean。这
 * 会微妙地影响其自动装配行为：它们只会针对那些在类型匹配时不需要急切初始化的 Bean 进行自动装配。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 2.5
 * @see org.springframework.beans.factory.config.PropertyOverrideConfigurer
 * @see org.springframework.beans.factory.config.PropertyPlaceholderConfigurer
 */
public interface PriorityOrdered extends Ordered {
}
