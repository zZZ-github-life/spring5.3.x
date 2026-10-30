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

package org.springframework.web.servlet;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.Nullable;

/**
 * 用于 Web 交互的 MVC 视图。实现类负责渲染内容并暴露模型。
 * 单个视图可暴露多个模型属性。
 *
 * <p>本类以及与之相关的 MVC 方法在 Rod Johnson（Wrox，2002）所著的
 * <a href="https://www.amazon.com/exec/obidos/tg/detail/-/0764543857/">Expert One-On-One J2EE Design and Development</a>
 * 一书第 12 章中有详细讨论。
 *
 * <p>视图实现之间可能存在很大差异。最显而易见的是基于 JSP 的实现。
 * 其他实现可能基于 XSLT，或使用 HTML 生成库。
 * 该接口的设计旨在避免限制可能实现的范围。
 *
 * <p>视图应当是 bean。它们通常由 ViewResolver 实例化为 bean。
 * 由于该接口是无状态的，视图实现应当是线程安全的。
 *
 * @author Rod Johnson
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @see org.springframework.web.servlet.view.AbstractView
 * @see org.springframework.web.servlet.view.InternalResourceView
 */
public interface View {

	/**
	 * {@link HttpServletRequest} 属性的名称，该属性包含响应状态码。
	 * <p>注意：并非所有 View 实现都必须支持该属性。
	 * @since 3.0
	 */
	String RESPONSE_STATUS_ATTRIBUTE = View.class.getName() + ".responseStatus";

	/**
	 * {@link HttpServletRequest} 属性的名称，该属性包含带有路径变量的 Map。
	 * 该 Map 的键为基于 String 的 URI 模板变量名称，值为对应的基于 Object 的值
	 * ——从 URL 的各段中提取并经过类型转换。
	 * <p>注意：并非所有 View 实现都必须支持该属性。
	 * @since 3.1
	 */
	String PATH_VARIABLES = View.class.getName() + ".pathVariables";

	/**
	 * 在内容协商过程中选定的 {@link org.springframework.http.MediaType}，
	 * 它可能比 View 所配置的媒体类型更具体。例如：
	 * "application/vnd.example-v1+xml" 与 "application/*+xml"。
	 * @since 3.2
	 */
	String SELECTED_CONTENT_TYPE = View.class.getName() + ".selectedContentType";


	/**
	 * 返回视图的内容类型（如果已预先确定）。
	 * <p>可用于预先检查视图的内容类型，即在实际尝试渲染之前。
	 * @return 内容类型 String（可选地包含字符集）；
	 * 如果未预先确定则为 {@code null}
	 */
	@Nullable
	default String getContentType() {
		return null;
	}

	/**
	 * 使用指定的模型渲染视图。
	 * <p>第一步是准备请求：在 JSP 场景中，这意味着将模型对象设置为请求属性。
	 * 第二步是实际渲染视图，例如通过 RequestDispatcher 引入 JSP。
	 * @param model 以名称 String 为键、对应模型对象为值的 Map
	 * （模型为空时 Map 也可以为 {@code null}）
	 * @param request 当前 HTTP 请求
	 * @param response 我们正在构建的 HTTP 响应
	 * @throws Exception 如果渲染失败
	 */
	void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response)
			throws Exception;

}
