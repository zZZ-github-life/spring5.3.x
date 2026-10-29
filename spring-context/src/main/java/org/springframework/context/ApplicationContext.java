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

package org.springframework.context;

import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.lang.Nullable;

/**
 * 为应用提供配置的核心接口。
 * 在应用运行期间它是只读的，但如果实现支持，则可以重新加载。
 *
 * <p>ApplicationContext 提供以下功能：
 * <ul>
 * <li>用于访问应用组件的 Bean 工厂方法，继承自 {@link org.springframework.beans.factory.ListableBeanFactory}。
 * <li>以通用方式加载文件资源的能力，继承自 {@link org.springframework.core.io.ResourceLoader} 接口。
 * <li>向已注册监听器发布事件的能力，继承自 {@link ApplicationEventPublisher} 接口。
 * <li>解析消息的能力，支持国际化，继承自 {@link MessageSource} 接口。
 * <li>从父上下文继承。后代上下文中的定义始终优先。例如，这意味着整个 Web 应用可以共用一个父上下文，而每个 Servlet 拥有自己独立的子上下文，与其他任何 Servlet 的子上下文无关。
 * </ul>
 *
 * <p>除了标准的 {@link org.springframework.beans.factory.BeanFactory} 生命周期能力之外，
 * ApplicationContext 实现还会检测并调用 {@link ApplicationContextAware} Bean，以及
 * {@link ResourceLoaderAware}、{@link ApplicationEventPublisherAware} 和
 * {@link MessageSourceAware} Bean。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see ConfigurableApplicationContext
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.core.io.ResourceLoader
 */
public interface ApplicationContext extends EnvironmentCapable, ListableBeanFactory, HierarchicalBeanFactory,
		MessageSource, ApplicationEventPublisher, ResourcePatternResolver {

	/**
	 * 返回此应用上下文的唯一 id。
	 * @return the unique id of the context, or {@code null} if none
	 */
	@Nullable
	String getId();

	/**
	 * 返回此上下文所属的已部署应用的名称。
	 * @return a name for the deployed application, or the empty String by default
	 */
	String getApplicationName();

	/**
	 * 返回此上下文的友好名称。
	 * @return a display name for this context (never {@code null})
	 */
	String getDisplayName();

	/**
	 * 返回此上下文首次加载时的时间戳。
	 * @return the timestamp (ms) when this context was first loaded
	 */
	long getStartupDate();

	/**
	 * 返回父上下文；如果没有父上下文且此上下文是上下文层次结构的根，则返回 {@code null}。
	 * @return the parent context, or {@code null} if there is no parent
	 */
	@Nullable
	ApplicationContext getParent();

	/**
	 * 为此上下文暴露 AutowireCapableBeanFactory 功能。
	 *
	 * <p>这通常不用于应用代码，除非是为了初始化位于应用上下文之外的 Bean 实例，并对其应用 Spring Bean
	 * 生命周期（全部或部分）。
	 *
	 * <p>另外，由 {@link ConfigurableApplicationContext} 接口暴露的内部 BeanFactory 也提供了对
	 * {@link AutowireCapableBeanFactory} 接口的访问。当前方法主要作为 ApplicationContext 接口上的一种
	 * 便捷、特定的设施。
	 *
	 * <p><b>注意：从 4.2 起，在应用上下文关闭后，此方法将始终抛出 IllegalStateException。</b> 在当前
	 * Spring Framework 版本中，只有可刷新的应用上下文才会有此行为；从 4.2 起，所有应用上下文实现都将被
	 * 要求遵循此约定。
	 * @return the AutowireCapableBeanFactory for this context
	 * @throws IllegalStateException if the context does not support the
	 * {@link AutowireCapableBeanFactory} interface, or does not hold an
	 * autowire-capable bean factory yet (e.g. if {@code refresh()} has
	 * never been called), or if the context has been closed already
	 * @see ConfigurableApplicationContext#refresh()
	 * @see ConfigurableApplicationContext#getBeanFactory()
	 */
	AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException;

}
