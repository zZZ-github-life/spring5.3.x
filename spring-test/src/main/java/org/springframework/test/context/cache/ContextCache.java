/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.test.context.cache;

import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * {@code ContextCache} 定义了在 <em>Spring TestContext Framework</em> 中缓存 Spring {@link ApplicationContext ApplicationContexts} 的 SPI。
 * <p>{@code ContextCache} 维护一个以 {@link MergedContextConfiguration} 实例为键的 {@code ApplicationContexts} 缓存，可以选择配置{@linkplain ContextCacheUtils#retrieveMaxCacheSize 最大大小}和自定义驱逐策略。
 * <h3>动机</h3>
 * <p>如果上下文初始化很复杂，上下文缓存可以带来显著的性能优势。虽然 Spring 上下文本身的初始化通常非常快，但上下文中的某些 bean——例如嵌入式数据库或用于 JPA 的 {@code LocalContainerEntityManagerFactoryBean}——可能需要数秒才能初始化。因此，通常只需在每个测试套件或 JVM 进程中执行一次此类初始化。
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 4.2
 * @see ContextCacheUtils#retrieveMaxCacheSize()
 */
public interface ContextCache {

	/**
	 * 用于报告 {@code ContextCache} 统计信息的日志类别名称。
	 */
	String CONTEXT_CACHE_LOGGING_CATEGORY = "org.springframework.test.context.cache";

	/**
	 * 上下文缓存的默认最大大小：{@value}。
	 * @since 4.3
	 * @see #MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME
	 */
	int DEFAULT_MAX_CONTEXT_CACHE_SIZE = 32;

	/**
	 * 用于将 {@link ContextCache} 的最大大小配置为正整数的系统属性。也可以通过 {@link org.springframework.core.SpringProperties} 机制进行配置。
	 * <p>请注意，{@code ContextCache} 的实现并非必须实际支持最大缓存大小。有关详情，请查阅相应实现的文档。
	 * @since 4.3
	 * @see #DEFAULT_MAX_CONTEXT_CACHE_SIZE
	 */
	String MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME = "spring.test.context.cache.maxSize";


	/**
	 * 确定给定键是否存在已缓存的上下文。
	 * @param key the context key (never {@code null})
	 * @return {@code true} if the cache contains a context with the given key
	 */
	boolean contains(MergedContextConfiguration key);

	/**
	 * 获取给定键对应的已缓存 {@code ApplicationContext}。
	 * <p>必须相应地更新{@linkplain #getHitCount() 命中}和{@linkplain #getMissCount() 未命中}计数。
	 * @param key the context key (never {@code null})
	 * @return the corresponding {@code ApplicationContext} instance, or {@code null}
	 * if not found in the cache
	 * @see #remove
	 */
	@Nullable
	ApplicationContext get(MergedContextConfiguration key);

	/**
	 * 显式将 {@code ApplicationContext} 实例添加到缓存中的给定键下，可以选择遵循自定义驱逐策略。
	 * @param key the context key (never {@code null})
	 * @param context the {@code ApplicationContext} instance (never {@code null})
	 */
	void put(MergedContextConfiguration key, ApplicationContext context);

	/**
	 * 从缓存中移除具有给定键的上下文，如果它是 {@code ConfigurableApplicationContext} 的实例，则显式{@linkplain org.springframework.context.ConfigurableApplicationContext#close() 关闭}它。
	 * <p>一般而言，应调用此方法以从缓存中正确驱逐上下文（例如由于自定义驱逐策略），或者单例 bean 的状态已被修改并可能影响与该上下文的后续交互时调用此方法。
	 * <p>此外，必须遵循所提供的 {@code HierarchyMode} 的语义。有关详情，请参见 {@link HierarchyMode} 的 Javadoc。
	 * @param key the context key; never {@code null}
	 * @param hierarchyMode the hierarchy mode; may be {@code null} if the context
	 * is not part of a hierarchy
	 */
	void remove(MergedContextConfiguration key, @Nullable HierarchyMode hierarchyMode);

	/**
	 * 确定缓存中当前存储的上下文数量。
	 * <p>如果缓存包含的元素超过 {@code Integer.MAX_VALUE} 个，则此方法必须返回 {@code Integer.MAX_VALUE}。
	 */
	int size();

	/**
	 * 确定缓存中当前跟踪的父上下文数量。
	 */
	int getParentContextCount();

	/**
	 * 获取此缓存的总命中计数。
	 * <p><em>命中</em>是指任何针对所查询键返回非 null 上下文的缓存访问。
	 */
	int getHitCount();

	/**
	 * 获取此缓存的总未命中计数。
	 * <p><em>未命中</em>是指任何针对所查询键返回 {@code null} 上下文的缓存访问。
	 */
	int getMissCount();

	/**
	 * 重置此缓存维护的所有状态，包括统计信息。
	 * @see #clear()
	 * @see #clearStatistics()
	 */
	void reset();

	/**
	 * 清除缓存中的所有上下文，同时清除上下文层次结构信息。
	 */
	void clear();

	/**
	 * 清除缓存的{@linkplain #getHitCount() 命中计数}和{@linkplain #getMissCount() 未命中计数}统计信息（即，将计数器重置为零）。
	 */
	void clearStatistics();

	/**
	 * 使用 {@value #CONTEXT_CACHE_LOGGING_CATEGORY} 日志类别，以 {@code DEBUG} 级别记录此 {@code ContextCache} 的统计信息。
	 * <p>应记录以下信息。
	 * <ul>
	 * <li>具体 {@code ContextCache} 实现的名称</li>
	 * <li>{@linkplain #size}</li>
	 * <li>{@linkplain #getParentContextCount() 父上下文数量}</li>
	 * <li>{@linkplain #getHitCount() 命中计数}</li>
	 * <li>{@linkplain #getMissCount() 未命中计数}</li>
	 * <li>任何其他有助于监控此缓存状态的信息</li>
	 * </ul>
	 */
	void logStatistics();

}
