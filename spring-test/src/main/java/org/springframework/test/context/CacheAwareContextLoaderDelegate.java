/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.test.context;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.lang.Nullable;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;

/**
 * {@code CacheAwareContextLoaderDelegate} 负责{@linkplain #loadContext 加载}和{@linkplain #closeContext 关闭}应用程序
 * 上下文，并在幕后与
 * {@link org.springframework.test.context.cache.ContextCache ContextCache}
 * 透明交互。
 *
 * <p>注意：{@code CacheAwareContextLoaderDelegate} 不扩展
 * {@link ContextLoader} 或 {@link SmartContextLoader} 接口。
 *
 * @author Sam Brannen
 * @since 3.2.2
 */
public interface CacheAwareContextLoaderDelegate {

	/**
	 * 用于配置默认 {@code CacheAwareContextLoaderDelegate} 的
	 * 完全限定类名的系统属性。
	 * <p>也可以通过
	 * {@link org.springframework.core.SpringProperties} 机制配置。
	 * <p>如果未定义此属性，将使用
	 * {@link org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate
	 * DefaultCacheAwareContextLoaderDelegate} 作为默认值。
	 * @since 5.3.11
	 */
	String DEFAULT_CACHE_AWARE_CONTEXT_LOADER_DELEGATE_PROPERTY_NAME =
			"spring.test.context.default.CacheAwareContextLoaderDelegate";


	/**
	 * 确定提供的 {@link MergedContextConfiguration} 的
	 * {@linkplain ApplicationContext 应用程序上下文}是否已加载（即
	 * 存在于 {@code ContextCache} 中）。
	 * <p>此方法的实现<strong>不得</strong>作为副作用
	 * 加载应用程序上下文。此外，此方法的实现不应通过
	 * {@link org.springframework.test.context.cache.ContextCache#logStatistics()} 记录缓存统计信息。
	 * <p>此方法的默认实现始终返回 {@code false}。
	 * 因此，强烈建议自定义 {@code CacheAwareContextLoaderDelegate} 实现
	 * 覆写此方法并提供更有意义的实现。请注意，Spring 中标准的
	 * {@code CacheAwareContextLoaderDelegate} 实现已适当地覆写了此方法。
	 * @param mergedContextConfiguration 用于加载应用上下文的合并上下文配置；
	 * 永不为 {@code null}
	 * @return 如果应用上下文已加载，则返回 {@code true}。
	 * @since 5.2
	 * @see #loadContext
	 * @see #closeContext
	 */
	default boolean isContextLoaded(MergedContextConfiguration mergedContextConfiguration) {
		return false;
	}

	/**
	 * 通过委托给给定 {@code MergedContextConfiguration} 中配置的
	 * {@link ContextLoader}，加载提供的
	 * {@link MergedContextConfiguration} 的 {@linkplain ApplicationContext 应用程序上下文}。
	 * <p>如果上下文存在于 {@code ContextCache} 中，将直接
	 * 返回；否则，将加载它、存储到缓存中并返回。
	 * <p>应通过调用
	 * {@link org.springframework.test.context.cache.ContextCache#logStatistics()} 来记录缓存统计信息。
	 * @param mergedContextConfiguration 用于加载应用上下文的合并上下文配置；
	 * 永不为 {@code null}
	 * @return 应用上下文（永不为 {@code null}）
	 * @throws IllegalStateException 在检索或加载应用上下文时发生错误
	 * @see #isContextLoaded
	 * @see #closeContext
	 */
	ApplicationContext loadContext(MergedContextConfiguration mergedContextConfiguration);

	/**
	 * 从 {@code ContextCache} 中移除提供的
	 * {@link MergedContextConfiguration} 的 {@linkplain ApplicationContext 应用程序上下文}，
	 * 如果它是 {@link ConfigurableApplicationContext} 的实例，则
	 * {@linkplain ConfigurableApplicationContext#close() 关闭}它。
	 * <p>从缓存中移除上下文时，必须遵循提供的 {@code HierarchyMode} 的语义。
	 * 详情请参见 {@link HierarchyMode} 的 Javadoc。
	 * <p>一般而言，仅当单例 Bean 的状态已更改（可能影响未来与
	 * 上下文的交互）或需要提前从缓存中移除上下文时，才应调用此方法。
	 * @param mergedContextConfiguration 要关闭的应用上下文的合并上下文配置；
	 * 永不为 {@code null}
	 * @param hierarchyMode 层次模式；如果该上下文不属于层次结构，
	 * 则可为 {@code null}
	 * @since 4.1
	 * @see #isContextLoaded
	 * @see #loadContext
	 */
	void closeContext(MergedContextConfiguration mergedContextConfiguration, @Nullable HierarchyMode hierarchyMode);

}
