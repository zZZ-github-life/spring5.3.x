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

package org.springframework.web.servlet.mvc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;

/**
 * 简单的 {@code Controller} 实现，它将 URL 的虚拟路径
 * 转换为视图名称并返回该视图。
 *
 * <p>可以选择添加 {@link #setPrefix prefix} 前缀和/或附加
 * {@link #setSuffix suffix} 后缀，以基于 URL 文件名构建视图名称。
 *
 * <p>以下是一些示例：
 * <ol>
 * <li>{@code "/index" -> "index"}</li>
 * <li>{@code "/index.html" -> "index"}</li>
 * <li>{@code "/index.html"} + 前缀 {@code "pre_"} 和后缀 {@code "_suf" -> "pre_index_suf"}</li>
 * <li>{@code "/products/view.html" -> "products/view"}</li>
 * </ol>
 *
 * <p>感谢 David Barri 提出支持前缀/后缀的建议！
 * @author Alef Arendsen
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @see #setPrefix
 * @see #setSuffix
 */
public class UrlFilenameViewController extends AbstractUrlViewController {

	private String prefix = "";

	private String suffix = "";

	/** 将请求 URL 路径字符串转换为视图名称字符串。 */
	private final Map<String, String> viewNameCache = new ConcurrentHashMap<>(256);


	/**
	 * 设置要添加到请求 URL 文件名之前的前缀，
	 * 用于构建视图名称。
	 */
	public void setPrefix(@Nullable String prefix) {
		this.prefix = (prefix != null ? prefix : "");
	}

	/**
	 * 返回要添加到请求 URL 文件名之前的前缀。
	 */
	protected String getPrefix() {
		return this.prefix;
	}

	/**
	 * 设置要附加到请求 URL 文件名之后的后缀，
	 * 用于构建视图名称。
	 */
	public void setSuffix(@Nullable String suffix) {
		this.suffix = (suffix != null ? suffix : "");
	}

	/**
	 * 返回要附加到请求 URL 文件名之后的后缀。
	 */
	protected String getSuffix() {
		return this.suffix;
	}


	/**
	 * 基于 URL 文件名返回视图名称，
	 * 并在适当时应用前缀/后缀。
	 * @see #extractViewNameFromUrlPath
	 * @see #setPrefix
	 * @see #setSuffix
	 */
	@Override
	protected String getViewNameForRequest(HttpServletRequest request) {
		String uri = extractOperableUrl(request);
		return getViewNameForUrlPath(uri);
	}

	/**
	 * 从给定请求中提取 URL 路径，
	 * 以便用于提取视图名称。
	 * @param request current HTTP request
	 * @return the URL to use for view name extraction
	 */
	protected String extractOperableUrl(HttpServletRequest request) {
		String urlPath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		if (!StringUtils.hasText(urlPath)) {
			urlPath = ServletRequestPathUtils.getCachedPathValue(request);
		}
		return urlPath;
	}

	/**
	 * 基于 URL 文件名返回视图名称，
	 * 并在适当时应用前缀/后缀。
	 * @param uri the request URI; for example {@code "/index.html"}
	 * @return the extracted URI filename; for example {@code "index"}
	 * @see #extractViewNameFromUrlPath
	 * @see #postProcessViewName
	 */
	protected String getViewNameForUrlPath(String uri) {
		return this.viewNameCache.computeIfAbsent(uri, u -> postProcessViewName(extractViewNameFromUrlPath(u)));
	}

	/**
	 * 从给定请求 URI 中提取 URL 文件名。
	 * @param uri the request URI; for example {@code "/index.html"}
	 * @return the extracted URI filename; for example {@code "index"}
	 */
	protected String extractViewNameFromUrlPath(String uri) {
		int start = (uri.charAt(0) == '/' ? 1 : 0);
		int lastIndex = uri.lastIndexOf('.');
		int end = (lastIndex < 0 ? uri.length() : lastIndex);
		return uri.substring(start, end);
	}

	/**
	 * 基于由 URL 路径指示的给定视图名称
	 * 构建完整的视图名称。
	 * <p>默认实现只是简单地应用前缀和后缀。
	 * 可以覆盖此方法，例如用于处理大写/小写转换等。
	 * @param viewName the original view name, as indicated by the URL path
	 * @return the full view name to use
	 * @see #getPrefix()
	 * @see #getSuffix()
	 */
	protected String postProcessViewName(String viewName) {
		return getPrefix() + viewName + getSuffix();
	}

}
