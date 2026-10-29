/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.server.i18n;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.lang.Nullable;
import org.springframework.web.server.ServerWebExchange;

/**
 * 基于 Web 的区域设置上下文解析策略接口，既允许通过请求解析区域设置上下文，
 * 也允许通过 HTTP 交换修改区域设置上下文。
 *
 * <p>{@link org.springframework.context.i18n.LocaleContext} 对象可能包含
 * 关联的时区以及其他与区域设置相关的信息。
 *
 * @author Sebastien Deleuze
 * @since 5.0
 * @see LocaleContext
 */
public interface LocaleContextResolver {

	/**
	 * 通过给定的交换解析当前区域设置上下文。
	 * <p>返回的上下文可能是
	 * {@link org.springframework.context.i18n.TimeZoneAwareLocaleContext}，
	 * 其中包含带有相关时区信息的区域设置。
	 * 只需应用 {@code instanceof} 检查并相应地向下转型即可。
	 * <p>自定义解析器实现还可以在返回的上下文中返回额外的设置，
	 * 同样可以通过向下转型来访问。
	 * @param exchange current server exchange
	 * @return the current locale context (never {@code null})
	 */
	LocaleContext resolveLocaleContext(ServerWebExchange exchange);

	/**
	 * 将当前区域设置上下文设置为给定值，
	 * 其中可能包含带有相关时区信息的区域设置。
	 * @param exchange current server exchange
	 * @param localeContext the new locale context, or {@code null} to clear the locale
	 * @throws UnsupportedOperationException if the LocaleResolver implementation
	 * does not support dynamic changing of the locale or time zone
	 * @see org.springframework.context.i18n.SimpleLocaleContext
	 * @see org.springframework.context.i18n.SimpleTimeZoneAwareLocaleContext
	 */
	void setLocaleContext(ServerWebExchange exchange, @Nullable LocaleContext localeContext);

}
