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

package org.springframework.web.servlet.view;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

/**
 * {@link org.springframework.web.servlet.ViewResolver} 实现的便捷基类。
 * 一旦解析完成，便缓存 {@link org.springframework.web.servlet.View} 对象：
 * 这意味着无论初始视图获取的代价有多高，
 * 视图解析都不会成为性能问题。
 *
 * <p>子类需要实现 {@link #loadView} 模板方法，
 * 为特定的视图名称和区域设置构建 View 对象。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #loadView
 */
public abstract class AbstractCachingViewResolver extends WebApplicationObjectSupport implements ViewResolver {

	/** 视图缓存的默认最大条目数：1024。 */
	public static final int DEFAULT_CACHE_LIMIT = 1024;

	/** 缓存 Map 中用于表示未解析视图的占位标记对象。 */
	private static final View UNRESOLVED_VIEW = new View() {
		@Override
		@Nullable
		public String getContentType() {
			return null;
		}
		@Override
		public void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) {
		}
	};

	/** 始终进行缓存的默认缓存过滤器。 */
	private static final CacheFilter DEFAULT_CACHE_FILTER = (view, viewName, locale) -> true;


	/** 缓存中的最大条目数。 */
	private volatile int cacheLimit = DEFAULT_CACHE_LIMIT;

	/** 是否在某个视图一次解析失败后不再重复解析。 */
	private boolean cacheUnresolved = true;

	/** 用于判断视图是否应被缓存的过滤器函数。 */
	private CacheFilter cacheFilter = DEFAULT_CACHE_FILTER;

	/** 用于快速访问 View 的缓存，无需全局锁即可返回已缓存的实例。 */
	private final Map<Object, View> viewAccessCache = new ConcurrentHashMap<>(DEFAULT_CACHE_LIMIT);

	/** 从视图键到 View 实例的映射，在创建 View 时进行同步。 */
	@SuppressWarnings("serial")
	private final Map<Object, View> viewCreationCache =
			new LinkedHashMap<Object, View>(DEFAULT_CACHE_LIMIT, 0.75f, true) {
				@Override
				protected boolean removeEldestEntry(Map.Entry<Object, View> eldest) {
					if (size() > getCacheLimit()) {
						viewAccessCache.remove(eldest.getKey());
						return true;
					}
					else {
						return false;
					}
				}
			};


	/**
	 * 指定视图缓存的最大条目数。
	 * 默认为 1024。
	 */
	public void setCacheLimit(int cacheLimit) {
		this.cacheLimit = cacheLimit;
	}

	/**
	 * 返回视图缓存的最大条目数。
	 */
	public int getCacheLimit() {
		return this.cacheLimit;
	}

	/**
	 * 启用或禁用缓存。
	 * <p>这等效于将 {@link #setCacheLimit "cacheLimit"}
	 * 属性分别设置为默认上限（1024）或 0。
	 * <p>默认值为 "true"：启用缓存。
	 * 仅出于调试和开发目的才禁用此功能。
	 */
	public void setCache(boolean cache) {
		this.cacheLimit = (cache ? DEFAULT_CACHE_LIMIT : 0);
	}

	/**
	 * 返回是否启用了缓存。
	 */
	public boolean isCache() {
		return (this.cacheLimit > 0);
	}

	/**
	 * 某个视图名称一旦解析为 {@code null}，是否应被缓存并
	 * 在后续自动解析为 {@code null}。
	 * <p>默认值为 "true"：自 Spring 3.1 起，未解析的视图名称会被缓存。
	 * 请注意，此标志仅在通用 {@link #setCache "cache"}
	 * 标志也保持默认值 "true" 时才生效。
	 * <p>特别值得关注的是，某些 AbstractUrlBasedView
	 * 实现（FreeMarker、Tiles）能够通过 {@link AbstractUrlBasedView#checkResource(Locale)}
	 * 检查底层资源是否存在。
	 * 当此标志设为 "false" 时，重新出现的底层资源会被发现并使用。
	 * 当此标志设为 "true" 时，仅进行一次检查。
	 */
	public void setCacheUnresolved(boolean cacheUnresolved) {
		this.cacheUnresolved = cacheUnresolved;
	}

	/**
	 * 返回是否启用了对未解析视图的缓存。
	 */
	public boolean isCacheUnresolved() {
		return this.cacheUnresolved;
	}

	/**
	 * 设置用于判断视图是否应被缓存的过滤器。
	 * 默认行为是缓存所有视图。
	 * @since 5.2
	 */
	public void setCacheFilter(CacheFilter cacheFilter) {
		Assert.notNull(cacheFilter, "CacheFilter must not be null");
		this.cacheFilter = cacheFilter;
	}

	/**
	 * 返回用于判断视图是否应被缓存的过滤器函数。
	 * @since 5.2
	 */
	public CacheFilter getCacheFilter() {
		return this.cacheFilter;
	}

	@Override
	@Nullable
	public View resolveViewName(String viewName, Locale locale) throws Exception {
		if (!isCache()) {
			return createView(viewName, locale);
		}
		else {
			Object cacheKey = getCacheKey(viewName, locale);
			View view = this.viewAccessCache.get(cacheKey);
			if (view == null) {
				synchronized (this.viewCreationCache) {
					view = this.viewCreationCache.get(cacheKey);
					if (view == null) {
						// Ask the subclass to create the View object.
						view = createView(viewName, locale);
						if (view == null && this.cacheUnresolved) {
							view = UNRESOLVED_VIEW;
						}
						if (view != null && this.cacheFilter.filter(view, viewName, locale)) {
							this.viewAccessCache.put(cacheKey, view);
							this.viewCreationCache.put(cacheKey, view);
						}
					}
				}
			}
			else {
				if (logger.isTraceEnabled()) {
					logger.trace(formatKey(cacheKey) + "served from cache");
				}
			}
			return (view != UNRESOLVED_VIEW ? view : null);
		}
	}

	private static String formatKey(Object cacheKey) {
		return "View with key [" + cacheKey + "] ";
	}

	/**
	 * 返回给定视图名称和给定区域设置的缓存键。
	 * <p>默认是由视图名称和区域设置后缀组成的 String。
	 * 可在子类中重写。
	 * <p>总体上需要考虑区域设置，因为不同的区域设置可能
	 * 导致使用不同的视图资源。
	 */
	protected Object getCacheKey(String viewName, Locale locale) {
		return viewName + '_' + locale;
	}

	/**
	 * 提供清除特定视图缓存的功能。
	 * <p>如果开发者能够在运行时修改视图
	 * （例如 FreeMarker 模板），随后需要清除指定视图的
	 * 缓存，此功能会很方便。
	 * @param viewName 需要移除其缓存视图对象（如果有）的视图名称
	 * @param locale 应移除其视图对象的 Locale
	 */
	public void removeFromCache(String viewName, Locale locale) {
		if (!isCache()) {
			logger.warn("Caching is OFF (removal not necessary)");
		}
		else {
			Object cacheKey = getCacheKey(viewName, locale);
			Object cachedView;
			synchronized (this.viewCreationCache) {
				this.viewAccessCache.remove(cacheKey);
				cachedView = this.viewCreationCache.remove(cacheKey);
			}
			if (logger.isDebugEnabled()) {
				// Some debug output might be useful...
				logger.debug(formatKey(cacheKey) +
						(cachedView != null ? "cleared from cache" : "not found in the cache"));
			}
		}
	}

	/**
	 * 清除整个视图缓存，移除所有已缓存的视图对象。
	 * 后续的解析调用将重新创建所需的视图对象。
	 */
	public void clearCache() {
		logger.debug("Clearing all views from the cache");
		synchronized (this.viewCreationCache) {
			this.viewAccessCache.clear();
			this.viewCreationCache.clear();
		}
	}


	/**
	 * 创建实际的 View 对象。
	 * <p>默认实现委托给 {@link #loadView}。
	 * 可以重写此方法以特殊方式解析特定的视图名称，
	 * 然后再委托给子类提供的实际 {@code loadView} 实现。
	 * @param viewName 要获取的视图名称
	 * @param locale 要为其获取视图的 Locale
	 * @return View 实例；如果未找到则为 {@code null}
	 * （可选，以便支持 ViewResolver 链）
	 * @throws Exception 如果无法解析视图
	 * @see #loadView
	 */
	@Nullable
	protected View createView(String viewName, Locale locale) throws Exception {
		return loadView(viewName, locale);
	}

	/**
	 * 子类必须实现此方法，为指定的视图构建 View 对象。
	 * 返回的 View 对象将由此 ViewResolver 基类
	 * 进行缓存。
	 * <p>不强制子类支持国际化：
	 * 不支持的子类可以简单地忽略 locale 参数。
	 * @param viewName 要获取的视图名称
	 * @param locale 要为其获取视图的 Locale
	 * @return View 实例；如果未找到则为 {@code null}
	 * （可选，以便支持 ViewResolver 链）
	 * @throws Exception 如果无法解析视图
	 * @see #resolveViewName
	 */
	@Nullable
	protected abstract View loadView(String viewName, Locale locale) throws Exception;


	/**
	 * 用于判断视图是否应被缓存的过滤器。
	 *
	 * @author Sergey Galkin
	 * @author Arjen Poutsma
	 * @since 5.2
	 */
	@FunctionalInterface
	public interface CacheFilter {

		/**
		 * 指示给定的视图是否应被缓存。
		 * 同时提供用于解析该视图的名称和区域设置。
		 * @param view 视图
		 * @param viewName 用于解析 {@code view} 的名称
		 * @param locale 用于解析 {@code view} 的 Locale
		 * @return 如果应缓存该视图则为 {@code true}；否则为 {@code false}
		 */
		boolean filter(View view, String viewName, Locale locale);
	}

}
