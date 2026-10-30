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

package org.springframework.web.servlet.view;

import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * {@link UrlBasedViewResolver} 的便捷子类，支持
 * {@link InternalResourceView}（即 Servlet 和 JSP）及其子类，
 * 例如 {@link JstlView}。
 *
 * <p>此解析器生成的所有视图的视图类可通过
 * {@link #setViewClass} 指定。详情请参见 {@link UrlBasedViewResolver} 的 javadoc。
 * 默认值为 {@link InternalResourceView}，如果类路径上存在
 * JSTL API，则为 {@link JstlView}。
 *
 * <p>顺便提一下，将仅作为视图使用的 JSP 文件放在
 * WEB-INF 下是一种好做法，这样可以避免直接访问它们（例如通过手动输入的 URL）。
 * 这样只有控制器才能访问它们。
 *
 * <p><b>注意：</b>在链接 ViewResolver 时，InternalResourceViewResolver
 * 必须始终放在最后，因为它会尝试解析任何视图名称，
 * 而不管底层资源是否实际存在。
 *
 * @author Juergen Hoeller
 * @since 17.02.2003
 * @see #setViewClass
 * @see #setPrefix
 * @see #setSuffix
 * @see #setRequestContextAttribute
 * @see InternalResourceView
 * @see JstlView
 */
public class InternalResourceViewResolver extends UrlBasedViewResolver {

	private static final boolean jstlPresent = ClassUtils.isPresent(
			"javax.servlet.jsp.jstl.core.Config", InternalResourceViewResolver.class.getClassLoader());

	@Nullable
	private Boolean alwaysInclude;


	/**
	 * 将默认的 {@link #setViewClass view class} 设为 {@link #requiredViewClass}：
	 * 默认是 {@link InternalResourceView}，如果存在 JSTL API
	 * 则为 {@link JstlView}。
	 */
	public InternalResourceViewResolver() {
		Class<?> viewClass = requiredViewClass();
		if (InternalResourceView.class == viewClass && jstlPresent) {
			viewClass = JstlView.class;
		}
		setViewClass(viewClass);
	}

	/**
	 * 便捷构造函数，允许通过构造参数指定 {@link #setPrefix prefix}
	 * 和 {@link #setSuffix suffix}。
	 * @param prefix 构建 URL 时添加到视图名称前面的前缀
	 * @param suffix 构建 URL 时追加到视图名称后面的后缀
	 * @since 4.3
	 */
	public InternalResourceViewResolver(String prefix, String suffix) {
		this();
		setPrefix(prefix);
		setSuffix(suffix);
	}


	/**
	 * 指定是否始终 include 视图而不是 forward 到它。
	 * <p>默认为 "false"。开启此标志即可强制使用
	 * Servlet include，即使 forward 是可行的。
	 * @see InternalResourceView#setAlwaysInclude
	 */
	public void setAlwaysInclude(boolean alwaysInclude) {
		this.alwaysInclude = alwaysInclude;
	}


	@Override
	protected Class<?> requiredViewClass() {
		return InternalResourceView.class;
	}

	@Override
	protected AbstractUrlBasedView instantiateView() {
		return (getViewClass() == InternalResourceView.class ? new InternalResourceView() :
				(getViewClass() == JstlView.class ? new JstlView() : super.instantiateView()));
	}

	@Override
	protected AbstractUrlBasedView buildView(String viewName) throws Exception {
		InternalResourceView view = (InternalResourceView) super.buildView(viewName);
		if (this.alwaysInclude != null) {
			view.setAlwaysInclude(this.alwaysInclude);
		}
		view.setPreventDispatchLoop(true);
		return view;
	}

}
