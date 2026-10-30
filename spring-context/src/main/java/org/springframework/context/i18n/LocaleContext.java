/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.context.i18n;

import java.util.Locale;

import org.springframework.lang.Nullable;

/**
 * 用于确定当前 Locale 的策略接口。
 *
 * <p>LocaleContext 实例可以通过 LocaleContextHolder 类与线程关联。
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see LocaleContextHolder#getLocale()
 * @see TimeZoneAwareLocaleContext
 */
public interface LocaleContext {

	/**
	 * 返回当前 Locale，它可以是固定的，也可以根据实现策略动态确定。
	 * @return 当前 Locale；如果未关联特定 Locale，则为 {@code null}
	 */
	@Nullable
	Locale getLocale();

}
