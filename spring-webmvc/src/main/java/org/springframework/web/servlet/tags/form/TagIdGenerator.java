/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.servlet.tags.form;

import javax.servlet.jsp.PageContext;

/**
 * 用于为 JSP 标签生成 '{@code id}' 属性值的工具类。给定标签名称（大多数情况下是数据绑定路径），返回该名称在当前 {@link PageContext} 中的唯一 ID。每次为给定名称请求 ID 时，都会在名称后追加一个不断递增的计数器。例如，给定名称 '{@code person.name}'，第一次请求将得到 '{@code person.name1}'，第二次将得到 '{@code person.name2}'。这支持一种常见用例：为同一数据字段生成一组单选按钮或复选框，每个按钮都是独立的标签实例。
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
abstract class TagIdGenerator {

	/**
	 * 此标签创建的所有 {@link PageContext} 属性的前缀。
	 */
	private static final String PAGE_CONTEXT_ATTRIBUTE_PREFIX = TagIdGenerator.class.getName() + ".";

	/**
	 * 获取给定名称（在给定 {@link PageContext} 内）的下一个唯一 ID。
	 */
	public static String nextId(String name, PageContext pageContext) {
		String attributeName = PAGE_CONTEXT_ATTRIBUTE_PREFIX + name;
		Integer currentCount = (Integer) pageContext.getAttribute(attributeName);
		currentCount = (currentCount != null ? currentCount + 1 : 1);
		pageContext.setAttribute(attributeName, currentCount);
		return (name + currentCount);
	}

}
