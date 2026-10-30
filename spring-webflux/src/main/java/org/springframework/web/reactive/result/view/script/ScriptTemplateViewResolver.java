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

package org.springframework.web.reactive.result.view.script;

import org.springframework.web.reactive.result.view.AbstractUrlBasedView;
import org.springframework.web.reactive.result.view.UrlBasedViewResolver;

/**
 * {@link UrlBasedViewResolver} 的便捷子类，支持 {@link ScriptTemplateView}
 * 及其自定义子类。
 *
 * <p>此解析器创建的所有视图的视图类可以通过
 * {@link #setViewClass(Class)} 属性指定。
 *
 * <p><b>注意：</b>在串联 ViewResolver 时，此解析器会检查指定的模板资源是否存在，
 * 仅当确实找到模板时才返回非 null 的 View 对象。
 *
 * @author Sebastien Deleuze
 * @since 5.0
 * @see ScriptTemplateConfigurer
 */
public class ScriptTemplateViewResolver extends UrlBasedViewResolver {

	/**
	 * 将默认的 {@link #setViewClass view class} 设置为 {@link #requiredViewClass}：
	 * 默认为 {@link ScriptTemplateView}。
	 */
	public ScriptTemplateViewResolver() {
		setViewClass(requiredViewClass());
	}

	/**
	 * 一个便捷构造函数，允许将 {@link #setPrefix prefix} 和
	 * {@link #setSuffix suffix} 指定为构造函数参数。
	 * @param prefix 构建 URL 时前置到视图名称的前缀
	 * @param suffix 构建 URL 时附加到视图名称的后缀
	 */
	public ScriptTemplateViewResolver(String prefix, String suffix) {
		this();
		setPrefix(prefix);
		setSuffix(suffix);
	}


	@Override
	protected Class<?> requiredViewClass() {
		return ScriptTemplateView.class;
	}

	@Override
	protected AbstractUrlBasedView instantiateView() {
		return (getViewClass() == ScriptTemplateView.class ? new ScriptTemplateView() : super.instantiateView());
	}

}
