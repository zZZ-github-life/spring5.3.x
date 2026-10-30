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

package org.springframework.web.servlet.handler;

import java.util.Map;

import org.springframework.http.server.PathContainer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
import org.springframework.web.util.pattern.PathPattern;

/**
 * 容器，用于保存通过 {@link MatchableHandlerMapping} 进行请求模式
 * 匹配的结果，并提供一个从模式中进一步提取
 * URI 模板变量的方法。
 *
 * @author Rossen Stoyanchev
 * @since 4.3.1
 */
public class RequestMatchResult {

	@Nullable
	private final PathPattern pathPattern;

	@Nullable
	private final PathContainer lookupPathContainer;


	@Nullable
	private final String pattern;

	@Nullable
	private final String lookupPath;

	@Nullable
	private final PathMatcher pathMatcher;


	/**
	 * 使用匹配的 {@code PathPattern} 创建实例。
	 * @param pathPattern 被匹配的模式
	 * @param lookupPath 映射路径
	 * @since 5.3
	 */
	public RequestMatchResult(PathPattern pathPattern, PathContainer lookupPath) {
		Assert.notNull(pathPattern, "PathPattern is required");
		Assert.notNull(lookupPath, "PathContainer is required");

		this.pattern = null;
		this.lookupPath = null;
		this.pathMatcher = null;

		this.pathPattern = pathPattern;
		this.lookupPathContainer = lookupPath;

	}

	/**
	 * 使用匹配的字符串模式创建实例。
	 * @param pattern 被匹配的模式，可能附加了 '/'
	 * @param lookupPath 映射路径
	 * @param pathMatcher 用于匹配的 PathMatcher 实例
	 */
	public RequestMatchResult(String pattern, String lookupPath, PathMatcher pathMatcher) {
		Assert.hasText(pattern, "'matchingPattern' is required");
		Assert.hasText(lookupPath, "'lookupPath' is required");
		Assert.notNull(pathMatcher, "PathMatcher is required");

		this.pattern = pattern;
		this.lookupPath = lookupPath;
		this.pathMatcher = pathMatcher;

		this.pathPattern = null;
		this.lookupPathContainer = null;
	}

	/**
	 * 从匹配的模式中提取 URI 模板变量，其定义方式如
	 * {@link PathMatcher#extractUriTemplateVariables} 所述。
	 * @return 包含 URI 模板变量的 Map
	 */
	@SuppressWarnings("ConstantConditions")
	public Map<String, String> extractUriTemplateVariables() {
		return (this.pathPattern != null ?
				this.pathPattern.matchAndExtract(this.lookupPathContainer).getUriVariables() :
				this.pathMatcher.extractUriTemplateVariables(this.pattern, this.lookupPath));
	}
}
