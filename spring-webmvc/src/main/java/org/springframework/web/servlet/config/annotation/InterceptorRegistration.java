/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.servlet.config.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.pattern.PathPattern;

/**
 * 辅助创建 {@link MappedInterceptor}。
 *
 * @author Rossen Stoyanchev
 * @author Keith Donald
 * @since 3.1
 */
public class InterceptorRegistration {

	private final HandlerInterceptor interceptor;

	@Nullable
	private List<String> includePatterns;

	@Nullable
	private List<String> excludePatterns;

	@Nullable
	private PathMatcher pathMatcher;

	private int order = 0;


	/**
	 * 创建 {@link InterceptorRegistration} 实例。
	 */
	public InterceptorRegistration(HandlerInterceptor interceptor) {
		Assert.notNull(interceptor, "Interceptor is required");
		this.interceptor = interceptor;
	}


	/**
	 * 添加该拦截器应包含的 URL 模式。
	 * <p>关于模式语法，当解析模式被
	 * {@link PathMatchConfigurer#setPatternParser 启用}时请参见 {@link PathPattern}，
	 * 否则请参见 {@link AntPathMatcher}。两者语法大体相同，
	 * 但 {@link PathPattern} 更贴合 Web 用途且更高效。
	 */
	public InterceptorRegistration addPathPatterns(String... patterns) {
		return addPathPatterns(Arrays.asList(patterns));
	}

	/**
	 * {@link #addPathPatterns(String...)} 的基于列表的变体。
	 * @since 5.0.3
	 */
	public InterceptorRegistration addPathPatterns(List<String> patterns) {
		this.includePatterns = (this.includePatterns != null ?
				this.includePatterns : new ArrayList<>(patterns.size()));
		this.includePatterns.addAll(patterns);
		return this;
	}

	/**
	 * 添加该拦截器应排除的 URL 模式。
	 * <p>关于模式语法，当解析模式被
	 * {@link PathMatchConfigurer#setPatternParser 启用}时请参见 {@link PathPattern}，
	 * 否则请参见 {@link AntPathMatcher}。两者语法大体相同，
	 * 但 {@link PathPattern} 更贴合 Web 用途且更高效。
	 */
	public InterceptorRegistration excludePathPatterns(String... patterns) {
		return excludePathPatterns(Arrays.asList(patterns));
	}

	/**
	 * {@link #excludePathPatterns(String...)} 的基于列表的变体。
	 * @since 5.0.3
	 */
	public InterceptorRegistration excludePathPatterns(List<String> patterns) {
		this.excludePatterns = (this.excludePatterns != null ?
				this.excludePatterns : new ArrayList<>(patterns.size()));
		this.excludePatterns.addAll(patterns);
		return this;
	}

	/**
	 * 配置用于将 URL 路径与包含和排除模式进行匹配的 PathMatcher。
	 * <p>这是一个高级属性，仅当需要自定义的 {@link AntPathMatcher} 或自定义 PathMatcher
	 * 时才应使用。
	 * <p>默认为 {@link AntPathMatcher}。
	 * <p><strong>注意：</strong>设置 {@code PathMatcher} 会强制使用
	 * 字符串模式匹配，即使有已
	 * {@link ServletRequestPathUtils#parseAndCache 解析}的 {@code RequestPath} 可用。
	 */
	public InterceptorRegistration pathMatcher(PathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
		return this;
	}

	/**
	 * 指定要使用的排序位置。默认为 0。
	 * @since 4.3.23
	 */
	public InterceptorRegistration order(int order){
		this.order = order;
		return this;
	}

	/**
	 * 返回要使用的排序位置。
	 */
	protected int getOrder() {
		return this.order;
	}

	/**
	 * 构建底层的拦截器。如果提供了 URL 模式，返回的类型为 {@link MappedInterceptor}；
	 * 否则为 {@link HandlerInterceptor}。
	 */
	protected Object getInterceptor() {

		if (this.includePatterns == null && this.excludePatterns == null) {
			return this.interceptor;
		}

		MappedInterceptor mappedInterceptor = new MappedInterceptor(
				StringUtils.toStringArray(this.includePatterns),
				StringUtils.toStringArray(this.excludePatterns),
				this.interceptor);

		if (this.pathMatcher != null) {
			mappedInterceptor.setPathMatcher(this.pathMatcher);
		}

		return mappedInterceptor;
	}

}
