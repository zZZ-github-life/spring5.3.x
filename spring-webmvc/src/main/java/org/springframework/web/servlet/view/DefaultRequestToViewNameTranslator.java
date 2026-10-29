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

package org.springframework.web.servlet.view;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.RequestToViewNameTranslator;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UrlPathHelper;

/**
 * {@link RequestToViewNameTranslator}，直接将传入请求的 URI
 * 转换为视图名称。
 *
 * <p>可以显式定义为
 * {@link org.springframework.web.servlet.DispatcherServlet} 上下文中的 {@code viewNameTranslator} bean。
 * 否则，将使用一个普通的默认实例。
 *
 * <p>默认转换会去除 URI 的前导斜杠、尾随斜杠
 * 以及文件扩展名，并将结果作为视图名称返回，
 * 同时按需添加所配置的 {@link #setPrefix prefix} 和
 * {@link #setSuffix suffix}。
 *
 * <p>可以分别通过
 * {@link #setStripLeadingSlash stripLeadingSlash} 和
 * {@link #setStripExtension stripExtension} 属性来禁用
 * 去除前导斜杠和文件扩展名。
 *
 * <p>下面列出一些请求到视图名称转换的示例。
 * <ul>
 * <li>{@code http://localhost:8080/gamecast/display.html} &raquo; {@code display}</li>
 * <li>{@code http://localhost:8080/gamecast/displayShoppingCart.html} &raquo; {@code displayShoppingCart}</li>
 * <li>{@code http://localhost:8080/gamecast/admin/index.html} &raquo; {@code admin/index}</li>
 * </ul>
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.web.servlet.RequestToViewNameTranslator
 * @see org.springframework.web.servlet.ViewResolver
 */
public class DefaultRequestToViewNameTranslator implements RequestToViewNameTranslator {

	private static final String SLASH = "/";


	private String prefix = "";

	private String suffix = "";

	private String separator = SLASH;

	private boolean stripLeadingSlash = true;

	private boolean stripTrailingSlash = true;

	private boolean stripExtension = true;


	/**
	 * 设置要前置到生成的视图名称之前的前缀。
	 * @param prefix the prefix to prepend to generated view names
	 */
	public void setPrefix(@Nullable String prefix) {
		this.prefix = (prefix != null ? prefix : "");
	}

	/**
	 * 设置要追加到生成的视图名称之后的 suffix。
	 * @param suffix the suffix to append to generated view names
	 */
	public void setSuffix(@Nullable String suffix) {
		this.suffix = (suffix != null ? suffix : "");
	}

	/**
	 * 设置用于替换视图名称中分隔符 '{@code /}' 的值。
	 * 默认行为是仍然将 '{@code /}' 作为分隔符。
	 */
	public void setSeparator(String separator) {
		this.separator = separator;
	}

	/**
	 * 设置生成视图名称时是否应从 URI 中去除前导斜杠。
	 * 默认为 "true"。
	 */
	public void setStripLeadingSlash(boolean stripLeadingSlash) {
		this.stripLeadingSlash = stripLeadingSlash;
	}

	/**
	 * 设置生成视图名称时是否应从 URI 中去除尾随斜杠。
	 * 默认为 "true"。
	 */
	public void setStripTrailingSlash(boolean stripTrailingSlash) {
		this.stripTrailingSlash = stripTrailingSlash;
	}

	/**
	 * 设置生成视图名称时是否应从 URI 中去除文件扩展名。
	 * 默认为 "true"。
	 */
	public void setStripExtension(boolean stripExtension) {
		this.stripExtension = stripExtension;
	}

	/**
	 * 底层 {@link #setUrlPathHelper UrlPathHelper} 上同名属性的快捷方式。
	 * @see org.springframework.web.util.UrlPathHelper#setAlwaysUseFullPath
	 * @deprecated as of 5.3, the path is resolved externally and obtained with
	 * {@link ServletRequestPathUtils#getCachedPathValue(ServletRequest)}
	 */
	@Deprecated
	public void setAlwaysUseFullPath(boolean alwaysUseFullPath) {
	}

	/**
	 * 底层 {@link #setUrlPathHelper UrlPathHelper} 上同名属性的快捷方式。
	 * @see org.springframework.web.util.UrlPathHelper#setUrlDecode
	 * @deprecated as of 5.3, the path is resolved externally and obtained with
	 * {@link ServletRequestPathUtils#getCachedPathValue(ServletRequest)}
	 */
	@Deprecated
	public void setUrlDecode(boolean urlDecode) {
	}

	/**
	 * 设置是否应从请求 URI 中去除 ";"（分号）内容。
	 * @see org.springframework.web.util.UrlPathHelper#setRemoveSemicolonContent(boolean)
	 * @deprecated as of 5.3, the path is resolved externally and obtained with
	 * {@link ServletRequestPathUtils#getCachedPathValue(ServletRequest)}
	 */
	@Deprecated
	public void setRemoveSemicolonContent(boolean removeSemicolonContent) {
	}

	/**
	 * 设置用于解析查找路径的 {@link org.springframework.web.util.UrlPathHelper}。
	 * <p>可使用此属性以自定义子类覆盖默认的 UrlPathHelper，
	 * 或在多个 Web 组件之间共享公共的 UrlPathHelper 设置。
	 * @deprecated as of 5.3, the path is resolved externally and obtained with
	 * {@link ServletRequestPathUtils#getCachedPathValue(ServletRequest)}
	 */
	@Deprecated
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
	}


	/**
	 * 根据所配置的参数，将传入的 {@link HttpServletRequest} 的请求 URI
	 * 转换为视图名称。
	 * @throws IllegalArgumentException if neither a parsed RequestPath, nor a
	 * String lookupPath have been resolved and cached as a request attribute.
	 * @see ServletRequestPathUtils#getCachedPath(ServletRequest)
	 * @see #transformPath
	 */
	@Override
	public String getViewName(HttpServletRequest request) {
		String path = ServletRequestPathUtils.getCachedPathValue(request);
		return (this.prefix + transformPath(path) + this.suffix);
	}

	/**
	 * 转换请求 URI（在 Web 应用上下文中），去除
	 * 斜杠和扩展名，并按需替换分隔符。
	 * @param lookupPath the lookup path for the current request,
	 * as determined by the UrlPathHelper
	 * @return the transformed path, with slashes and extensions stripped
	 * if desired
	 */
	@Nullable
	protected String transformPath(String lookupPath) {
		String path = lookupPath;
		if (this.stripLeadingSlash && path.startsWith(SLASH)) {
			path = path.substring(1);
		}
		if (this.stripTrailingSlash && path.endsWith(SLASH)) {
			path = path.substring(0, path.length() - 1);
		}
		if (this.stripExtension) {
			path = StringUtils.stripFilenameExtension(path);
		}
		if (!SLASH.equals(this.separator)) {
			path = StringUtils.replace(path, SLASH, this.separator);
		}
		return path;
	}

}
