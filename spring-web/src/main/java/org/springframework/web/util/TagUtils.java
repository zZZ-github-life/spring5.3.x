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

package org.springframework.web.util;

import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;

import org.springframework.util.Assert;

/**
 * 用于标签库相关代码的实用工具类，提供将 {@link String 字符串}
 * 转换为 Web 作用域等功能。
 *
 * <p>
 * <ul>
 * <li>{@code page} 将转换为
 * {@link javax.servlet.jsp.PageContext#PAGE_SCOPE PageContext.PAGE_SCOPE}
 * <li>{@code request} 将转换为
 * {@link javax.servlet.jsp.PageContext#REQUEST_SCOPE PageContext.REQUEST_SCOPE}
 * <li>{@code session} 将转换为
 * {@link javax.servlet.jsp.PageContext#SESSION_SCOPE PageContext.SESSION_SCOPE}
 * <li>{@code application} 将转换为
 * {@link javax.servlet.jsp.PageContext#APPLICATION_SCOPE PageContext.APPLICATION_SCOPE}
 * </ul>
 *
 * @author Alef Arendsen
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rick Evans
 */
public abstract class TagUtils {

	/** 标识页面作用域的常量。 */
	public static final String SCOPE_PAGE = "page";

	/** 标识请求作用域的常量。 */
	public static final String SCOPE_REQUEST = "request";

	/** 标识会话作用域的常量。 */
	public static final String SCOPE_SESSION = "session";

	/** 标识应用作用域的常量。 */
	public static final String SCOPE_APPLICATION = "application";


	/**
	 * 确定给定输入 {@code String} 的作用域。
	 * <p>如果该 {@code String} 不匹配 'request'、'session'、
	 * 'page' 或 'application'，此方法将返回 {@link PageContext#PAGE_SCOPE}。
	 * @param scope the {@code String} to inspect
	 * @return the scope found, or {@link PageContext#PAGE_SCOPE} if no scope matched
	 * @throws IllegalArgumentException if the supplied {@code scope} is {@code null}
	 */
	public static int getScope(String scope) {
		Assert.notNull(scope, "Scope to search for cannot be null");
		if (scope.equals(SCOPE_REQUEST)) {
			return PageContext.REQUEST_SCOPE;
		}
		else if (scope.equals(SCOPE_SESSION)) {
			return PageContext.SESSION_SCOPE;
		}
		else if (scope.equals(SCOPE_APPLICATION)) {
			return PageContext.APPLICATION_SCOPE;
		}
		else {
			return PageContext.PAGE_SCOPE;
		}
	}

	/**
	 * 确定提供的 {@link Tag} 是否具有提供类型的任何祖先标签。
	 * @param tag the tag whose ancestors are to be checked
	 * @param ancestorTagClass the ancestor {@link Class} being searched for
	 * @return {@code true} if the supplied {@link Tag} has any ancestor tag
	 * of the supplied type
	 * @throws IllegalArgumentException if either of the supplied arguments is {@code null};
	 * or if the supplied {@code ancestorTagClass} is not type-assignable to
	 * the {@link Tag} class
	 */
	public static boolean hasAncestorOfType(Tag tag, Class<?> ancestorTagClass) {
		Assert.notNull(tag, "Tag cannot be null");
		Assert.notNull(ancestorTagClass, "Ancestor tag class cannot be null");
		if (!Tag.class.isAssignableFrom(ancestorTagClass)) {
			throw new IllegalArgumentException(
					"Class '" + ancestorTagClass.getName() + "' is not a valid Tag type");
		}
		Tag ancestor = tag.getParent();
		while (ancestor != null) {
			if (ancestorTagClass.isAssignableFrom(ancestor.getClass())) {
				return true;
			}
			ancestor = ancestor.getParent();
		}
		return false;
	}

	/**
	 * 确定提供的 {@link Tag} 是否具有提供类型的任何祖先标签，
	 * 如果没有，则抛出 {@link IllegalStateException}。
	 * @param tag the tag whose ancestors are to be checked
	 * @param ancestorTagClass the ancestor {@link Class} being searched for
	 * @param tagName the name of the {@code tag}; for example '{@code option}'
	 * @param ancestorTagName the name of the ancestor {@code tag}; for example '{@code select}'
	 * @throws IllegalStateException if the supplied {@code tag} does not
	 * have a tag of the supplied {@code parentTagClass} as an ancestor
	 * @throws IllegalArgumentException if any of the supplied arguments is {@code null},
	 * or in the case of the {@link String}-typed arguments, is composed wholly
	 * of whitespace; or if the supplied {@code ancestorTagClass} is not
	 * type-assignable to the {@link Tag} class
	 * @see #hasAncestorOfType(javax.servlet.jsp.tagext.Tag, Class)
	 */
	public static void assertHasAncestorOfType(Tag tag, Class<?> ancestorTagClass, String tagName,
			String ancestorTagName) {

		Assert.hasText(tagName, "'tagName' must not be empty");
		Assert.hasText(ancestorTagName, "'ancestorTagName' must not be empty");
		if (!TagUtils.hasAncestorOfType(tag, ancestorTagClass)) {
			throw new IllegalStateException("The '" + tagName +
					"' tag can only be used inside a valid '" + ancestorTagName + "' tag.");
		}
	}

}
