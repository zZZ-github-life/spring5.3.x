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

package org.springframework.ui.context;

import org.springframework.lang.Nullable;

/**
 * ThemeSource 的子接口，由能够按层次解析主题消息的对象实现。
 *
 * @author Jean-Pierre Pawlak
 * @author Juergen Hoeller
 */
public interface HierarchicalThemeSource extends ThemeSource {

	/**
	 * 设置父级，当此对象无法解析主题消息时，将尝试使用该父级来解析。
	 * @param parent the parent ThemeSource that will be used to
	 * resolve messages that this object can't resolve.
	 * May be {@code null}, in which case no further resolution is possible.
	 */
	void setParentThemeSource(@Nullable ThemeSource parent);

	/**
	 * 返回此 ThemeSource 的父级，如果没有则为 {@code null}。
	 */
	@Nullable
	ThemeSource getParentThemeSource();

}
