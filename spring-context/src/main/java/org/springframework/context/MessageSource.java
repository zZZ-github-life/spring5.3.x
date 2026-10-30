/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.context;

import java.util.Locale;

import org.springframework.lang.Nullable;

/**
 * 用于解析消息的策略接口，支持对这类消息进行参数化和国际化。
 *
 * <p>Spring 为生产环境提供了两种开箱即用的实现：
 * <ul>
 * <li>{@link org.springframework.context.support.ResourceBundleMessageSource}：构建在标准 {@link java.util.ResourceBundle} 之上，并共享其限制。
 * <li>{@link org.springframework.context.support.ReloadableResourceBundleMessageSource}：高度可配置，尤其是在重新加载消息定义方面。
 * </ul>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.context.support.ResourceBundleMessageSource
 * @see org.springframework.context.support.ReloadableResourceBundleMessageSource
 */
public interface MessageSource {

	/**
	 * 尝试解析消息。如果未找到消息，则返回默认消息。
	 * @param code 要查找的消息代码，例如 'calculator.noRateSet'。
	 * 建议 MessageSource 用户以限定类名或包名作为消息名称的基础，
	 * 以避免潜在冲突并确保最大程度的清晰性。
	 * @param args 用于填充消息中参数的参数数组
	 * （消息中的参数形如 "{0}"、"{1,date}"、"{2,time}"），如果没有则为 {@code null}
	 * @param defaultMessage 查找失败时要返回的默认消息
	 * @param locale 用于执行查找的区域设置
	 * @return 如果查找成功则为解析出的消息，
	 * 否则为作为参数传入的默认消息（可能为 {@code null}）
	 * @see #getMessage(MessageSourceResolvable, Locale)
	 * @see java.text.MessageFormat
	 */
	@Nullable
	String getMessage(String code, @Nullable Object[] args, @Nullable String defaultMessage, Locale locale);

	/**
	 * 尝试解析消息。如果找不到消息，则视为错误。
	 * @param code 要查找的消息代码，例如 'calculator.noRateSet'。
	 * 建议 MessageSource 用户以限定类名或包名作为消息名称的基础，
	 * 以避免潜在冲突并确保最大程度的清晰性。
	 * @param args 用于填充消息中参数的参数数组
	 * （消息中的参数形如 "{0}"、"{1,date}"、"{2,time}"），如果没有则为 {@code null}
	 * @param locale 用于执行查找的区域设置
	 * @return 解析出的消息（永不为 {@code null}）
	 * @throws NoSuchMessageException 如果未找到相应的消息
	 * @see #getMessage(MessageSourceResolvable, Locale)
	 * @see java.text.MessageFormat
	 */
	String getMessage(String code, @Nullable Object[] args, Locale locale) throws NoSuchMessageException;

	/**
	 * 尝试使用传入的 {@code MessageSourceResolvable} 参数中包含的所有属性来解析消息。
	 *
	 * <p>注意：此方法必须抛出 {@code NoSuchMessageException}，因为在调用此方法时，我们无法确定该可解析
	 * 对象的 {@code defaultMessage} 属性是否为 {@code null}。
	 * @param resolvable 存储解析消息所需属性的值对象（可能包含默认消息）
	 * @param locale 用于执行查找的区域设置
	 * @return 解析出的消息（永不为 {@code null}，
	 * 因为即使是由 {@code MessageSourceResolvable} 提供的默认消息也必须非 null）
	 * @throws NoSuchMessageException 如果未找到相应的消息
	 * （且 {@code MessageSourceResolvable} 未提供默认消息）
	 * @see MessageSourceResolvable#getCodes()
	 * @see MessageSourceResolvable#getArguments()
	 * @see MessageSourceResolvable#getDefaultMessage()
	 * @see java.text.MessageFormat
	 */
	String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException;

}
