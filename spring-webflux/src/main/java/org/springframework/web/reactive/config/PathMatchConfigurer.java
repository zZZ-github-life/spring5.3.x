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

package org.springframework.web.reactive.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.springframework.lang.Nullable;

/**
 * 用于协助为 {@code HandlerMapping} 配置路径匹配选项。
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 */
public class PathMatchConfigurer {

	@Nullable
	private Boolean trailingSlashMatch;


	@Nullable
	private Boolean caseSensitiveMatch;

	@Nullable
	private Map<String, Predicate<Class<?>>> pathPrefixes;


	/**
	 * 是否在匹配 URL 时忽略大小写。如果启用，映射到 "/users" 的方法将不会匹配 "/Users/"。
	 * <p>默认值为 {@code false}。
	 */
	public PathMatchConfigurer setUseCaseSensitiveMatch(Boolean caseSensitiveMatch) {
		this.caseSensitiveMatch = caseSensitiveMatch;
		return this;
	}

	/**
	 * 是否在匹配 URL 时忽略末尾斜杠。如果启用，映射到 "/users" 的方法也会匹配 "/users/"。
	 * <p>默认值为 {@code true}。
	 */
	public PathMatchConfigurer setUseTrailingSlashMatch(Boolean trailingSlashMatch) {
		this.trailingSlashMatch = trailingSlashMatch;
		return this;
	}

	/**
	 * 配置要应用到匹配的控制器方法上的路径前缀。
	 * <p>前缀用于丰富每个 {@code @RequestMapping} 方法的映射，这些方法的控制器类型需与相应的 {@code Predicate} 匹配。将使用第一个匹配的谓词对应的前缀。
	 * <p>可考虑使用 {@link org.springframework.web.method.HandlerTypePredicate HandlerTypePredicate} 对控制器进行分组。
	 * @param prefix 要应用的路径前缀
	 * @param predicate 用于匹配控制器类型的谓词
	 * @since 5.1
	 */
	public PathMatchConfigurer addPathPrefix(String prefix, Predicate<Class<?>> predicate) {
		if (this.pathPrefixes == null) {
			this.pathPrefixes = new LinkedHashMap<>();
		}
		this.pathPrefixes.put(prefix, predicate);
		return this;
	}


	@Nullable
	protected Boolean isUseTrailingSlashMatch() {
		return this.trailingSlashMatch;
	}

	@Nullable
	protected Boolean isUseCaseSensitiveMatch() {
		return this.caseSensitiveMatch;
	}

	@Nullable
	protected Map<String, Predicate<Class<?>>> getPathPrefixes() {
		return this.pathPrefixes;
	}
}
