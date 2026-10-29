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

package org.springframework.context.support;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.springframework.context.HierarchicalMessageSource;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * {@link HierarchicalMessageSource} 接口的抽象实现，实现了消息变体的通用处理，使为具体
 * MessageSource 实现特定策略变得容易。
 *
 * <p>子类必须实现抽象方法 {@link #resolveCode}。为了高效解析不带参数的消息，还应重写
 * {@link #resolveCodeWithoutArguments} 方法，以便在不涉及 MessageFormat 的情况下解析消息。
 *
 * <p><b>注意：</b>默认情况下，仅当为消息传入了参数时，才会通过 MessageFormat 解析消息文本。在没有
 * 参数的情况下，消息文本将按原样返回。因此，应仅对带有实际参数的消息使用 MessageFormat 转义，并让
 * 所有其他消息保持未转义。如果希望转义所有消息，请将 "alwaysUseMessageFormat" 标志设置为 "true"。
 *
 * <p>不仅支持将 MessageSourceResolvable 作为主消息，还支持解析本身也是 MessageSourceResolvable 的
 * 消息参数。
 *
 * <p>此类不实现按代码缓存消息，因此子类可以随时间动态更改消息。鼓励子类以感知修改的方式缓存消息，
 * 从而支持已更新消息的热部署。
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @see #resolveCode(String, java.util.Locale)
 * @see #resolveCodeWithoutArguments(String, java.util.Locale)
 * @see #setAlwaysUseMessageFormat
 * @see java.text.MessageFormat
 */
public abstract class AbstractMessageSource extends MessageSourceSupport implements HierarchicalMessageSource {

	@Nullable
	private MessageSource parentMessageSource;

	@Nullable
	private Properties commonMessages;

	private boolean useCodeAsDefaultMessage = false;


	@Override
	public void setParentMessageSource(@Nullable MessageSource parent) {
		this.parentMessageSource = parent;
	}

	@Override
	@Nullable
	public MessageSource getParentMessageSource() {
		return this.parentMessageSource;
	}

	/**
	 * 指定与区域设置无关的通用消息，以消息代码为键，以完整消息 String（可能包含参数占位符）为值。
	 *
	 * <p>也可以链接到外部定义的 Properties 对象，例如通过
	 * {@link org.springframework.beans.factory.config.PropertiesFactoryBean} 定义的对象。
	 */
	public void setCommonMessages(@Nullable Properties commonMessages) {
		this.commonMessages = commonMessages;
	}

	/**
	 * 返回定义与区域设置无关的通用消息的 Properties 对象（如果有）。
	 */
	@Nullable
	protected Properties getCommonMessages() {
		return this.commonMessages;
	}

	/**
	 * 设置是否使用消息代码作为默认消息，而不是抛出 NoSuchMessageException。对开发和调试很有用。默认值
	 * 为 "false"。
	 *
	 * <p>注意：对于具有多个代码的 MessageSourceResolvable（如 FieldError）以及具有父 MessageSource 的
	 * MessageSource，请<i>不要</i>在<i>父级</i>中激活 "useCodeAsDefaultMessage"：否则，父级将返回第一
	 * 个代码作为消息，而不会尝试检查后续代码。
	 *
	 * <p>为了在父级启用 "useCodeAsDefaultMessage" 的情况下正常工作，AbstractMessageSource 和
	 * AbstractApplicationContext 包含特殊检查，以便在可用时委托给内部 {@link #getMessageInternal} 方法。
	 * 不过，通常建议仅在开发期间使用 "useCodeAsDefaultMessage"，而不要在生产环境中依赖它。
	 * @see #getMessage(String, Object[], Locale)
	 * @see org.springframework.validation.FieldError
	 */
	public void setUseCodeAsDefaultMessage(boolean useCodeAsDefaultMessage) {
		this.useCodeAsDefaultMessage = useCodeAsDefaultMessage;
	}

	/**
	 * 返回是否使用消息代码作为默认消息，而不是抛出 NoSuchMessageException。对开发和调试很有用。默认值
	 * 为 "false"。
	 *
	 * <p>或者，可以考虑重写 {@link #getDefaultMessage} 方法，为无法解析的代码返回自定义回退消息。
	 * @see #getDefaultMessage(String)
	 */
	protected boolean isUseCodeAsDefaultMessage() {
		return this.useCodeAsDefaultMessage;
	}


	@Override
	public final String getMessage(String code, @Nullable Object[] args, @Nullable String defaultMessage, Locale locale) {
		String msg = getMessageInternal(code, args, locale);
		if (msg != null) {
			return msg;
		}
		if (defaultMessage == null) {
			return getDefaultMessage(code);
		}
		return renderDefaultMessage(defaultMessage, args, locale);
	}

	@Override
	public final String getMessage(String code, @Nullable Object[] args, Locale locale) throws NoSuchMessageException {
		String msg = getMessageInternal(code, args, locale);
		if (msg != null) {
			return msg;
		}
		String fallback = getDefaultMessage(code);
		if (fallback != null) {
			return fallback;
		}
		throw new NoSuchMessageException(code, locale);
	}

	@Override
	public final String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
		String[] codes = resolvable.getCodes();
		if (codes != null) {
			for (String code : codes) {
				String message = getMessageInternal(code, resolvable.getArguments(), locale);
				if (message != null) {
					return message;
				}
			}
		}
		String defaultMessage = getDefaultMessage(resolvable, locale);
		if (defaultMessage != null) {
			return defaultMessage;
		}
		throw new NoSuchMessageException(!ObjectUtils.isEmpty(codes) ? codes[codes.length - 1] : "", locale);
	}


	/**
	 * 在给定的 Locale 中将给定代码和参数解析为消息，如果未找到则返回 {@code null}。<i>不会</i>回退到将
	 * 代码作为默认消息。由 {@code getMessage} 方法调用。
	 * @param code the code to lookup up, such as 'calculator.noRateSet'
	 * @param args array of arguments that will be filled in for params
	 * within the message
	 * @param locale the locale in which to do the lookup
	 * @return the resolved message, or {@code null} if not found
	 * @see #getMessage(String, Object[], String, Locale)
	 * @see #getMessage(String, Object[], Locale)
	 * @see #getMessage(MessageSourceResolvable, Locale)
	 * @see #setUseCodeAsDefaultMessage
	 */
	@Nullable
	protected String getMessageInternal(@Nullable String code, @Nullable Object[] args, @Nullable Locale locale) {
		if (code == null) {
			return null;
		}
		if (locale == null) {
			locale = Locale.getDefault();
		}
		Object[] argsToUse = args;

		if (!isAlwaysUseMessageFormat() && ObjectUtils.isEmpty(args)) {
			// Optimized resolution: no arguments to apply,
			// therefore no MessageFormat needs to be involved.
			// Note that the default implementation still uses MessageFormat;
			// this can be overridden in specific subclasses.
			String message = resolveCodeWithoutArguments(code, locale);
			if (message != null) {
				return message;
			}
		}

		else {
			// Resolve arguments eagerly, for the case where the message
			// is defined in a parent MessageSource but resolvable arguments
			// are defined in the child MessageSource.
			argsToUse = resolveArguments(args, locale);

			MessageFormat messageFormat = resolveCode(code, locale);
			if (messageFormat != null) {
				synchronized (messageFormat) {
					return messageFormat.format(argsToUse);
				}
			}
		}

		// Check locale-independent common messages for the given message code.
		Properties commonMessages = getCommonMessages();
		if (commonMessages != null) {
			String commonMessage = commonMessages.getProperty(code);
			if (commonMessage != null) {
				return formatMessage(commonMessage, args, locale);
			}
		}

		// Not found -> check parent, if any.
		return getMessageFromParent(code, argsToUse, locale);
	}

	/**
	 * 如果存在父 {@code MessageSource}，则尝试从中检索给定消息。
	 * @param code the code to lookup up, such as 'calculator.noRateSet'
	 * @param args array of arguments that will be filled in for params
	 * within the message
	 * @param locale the locale in which to do the lookup
	 * @return the resolved message, or {@code null} if not found
	 * @see #getParentMessageSource()
	 */
	@Nullable
	protected String getMessageFromParent(String code, @Nullable Object[] args, Locale locale) {
		MessageSource parent = getParentMessageSource();
		if (parent != null) {
			if (parent instanceof AbstractMessageSource) {
				// Call internal method to avoid getting the default code back
				// in case of "useCodeAsDefaultMessage" being activated.
				return ((AbstractMessageSource) parent).getMessageInternal(code, args, locale);
			}
			else {
				// Check parent MessageSource, returning null if not found there.
				// Covers custom MessageSource impls and DelegatingMessageSource.
				return parent.getMessage(code, args, null, locale);
			}
		}
		// Not found in parent either.
		return null;
	}

	/**
	 * 获取给定 {@code MessageSourceResolvable} 的默认消息。
	 *
	 * <p>此实现会在可用时完整呈现默认消息，或者在主消息代码被用作默认消息时直接返回纯默认消息
	 * {@code String}。
	 * @param resolvable the value object to resolve a default message for
	 * @param locale the current locale
	 * @return the default message, or {@code null} if none
	 * @since 4.3.6
	 * @see #renderDefaultMessage(String, Object[], Locale)
	 * @see #getDefaultMessage(String)
	 */
	@Nullable
	protected String getDefaultMessage(MessageSourceResolvable resolvable, Locale locale) {
		String defaultMessage = resolvable.getDefaultMessage();
		String[] codes = resolvable.getCodes();
		if (defaultMessage != null) {
			if (resolvable instanceof DefaultMessageSourceResolvable &&
					!((DefaultMessageSourceResolvable) resolvable).shouldRenderDefaultMessage()) {
				// Given default message does not contain any argument placeholders
				// (and isn't escaped for alwaysUseMessageFormat either) -> return as-is.
				return defaultMessage;
			}
			if (!ObjectUtils.isEmpty(codes) && defaultMessage.equals(codes[0])) {
				// Never format a code-as-default-message, even with alwaysUseMessageFormat=true
				return defaultMessage;
			}
			return renderDefaultMessage(defaultMessage, resolvable.getArguments(), locale);
		}
		return (!ObjectUtils.isEmpty(codes) ? getDefaultMessage(codes[0]) : null);
	}

	/**
	 * 返回给定代码的回退默认消息（如果有）。
	 *
	 * <p>默认行为是：如果激活了 "useCodeAsDefaultMessage"，则返回代码本身，否则不返回回退消息。在没有
	 * 回退消息的情况下，调用者通常会从 {@code getMessage} 收到 {@code NoSuchMessageException}。
	 * @param code the message code that we couldn't resolve
	 * and that we didn't receive an explicit default message for
	 * @return the default message to use, or {@code null} if none
	 * @see #setUseCodeAsDefaultMessage
	 */
	@Nullable
	protected String getDefaultMessage(String code) {
		if (isUseCodeAsDefaultMessage()) {
			return code;
		}
		return null;
	}


	/**
	 * 搜索给定的对象数组，查找所有 MessageSourceResolvable 对象并解析它们。
	 *
	 * <p>允许消息将 MessageSourceResolvable 作为参数。
	 * @param args array of arguments for a message
	 * @param locale the locale to resolve through
	 * @return an array of arguments with any MessageSourceResolvables resolved
	 */
	@Override
	protected Object[] resolveArguments(@Nullable Object[] args, Locale locale) {
		if (ObjectUtils.isEmpty(args)) {
			return super.resolveArguments(args, locale);
		}
		List<Object> resolvedArgs = new ArrayList<>(args.length);
		for (Object arg : args) {
			if (arg instanceof MessageSourceResolvable) {
				resolvedArgs.add(getMessage((MessageSourceResolvable) arg, locale));
			}
			else {
				resolvedArgs.add(arg);
			}
		}
		return resolvedArgs.toArray();
	}

	/**
	 * 子类可以重写此方法，以优化的方式解析不带参数的消息，即在不涉及 MessageFormat 的情况下进行解析。
	 *
	 * <p>默认实现<i>确实</i>使用 MessageFormat，方式是委托给 {@link #resolveCode} 方法。鼓励子类用优化
	 * 的解析方式替换它。
	 *
	 * <p>遗憾的是，{@code java.text.MessageFormat} 的实现效率不高。特别是，它无法首先检测出消息模式不
	 * 包含参数占位符。因此，对于不带参数的消息，建议绕过 MessageFormat。
	 * @param code the code of the message to resolve
	 * @param locale the locale to resolve the code for
	 * (subclasses are encouraged to support internationalization)
	 * @return the message String, or {@code null} if not found
	 * @see #resolveCode
	 * @see java.text.MessageFormat
	 */
	@Nullable
	protected String resolveCodeWithoutArguments(String code, Locale locale) {
		MessageFormat messageFormat = resolveCode(code, locale);
		if (messageFormat != null) {
			synchronized (messageFormat) {
				return messageFormat.format(new Object[0]);
			}
		}
		return null;
	}

	/**
	 * 子类必须实现此方法以解析消息。
	 *
	 * <p>返回 MessageFormat 实例而不是消息 String，以便在子类中对 MessageFormat 进行适当的缓存。
	 *
	 * <p><b>鼓励子类为不带参数的消息提供优化的解析，不涉及 MessageFormat。</b>有关详细信息，请参阅
	 * {@link #resolveCodeWithoutArguments} 的 javadoc。
	 * @param code the code of the message to resolve
	 * @param locale the locale to resolve the code for
	 * (subclasses are encouraged to support internationalization)
	 * @return the MessageFormat for the message, or {@code null} if not found
	 * @see #resolveCodeWithoutArguments(String, java.util.Locale)
	 */
	@Nullable
	protected abstract MessageFormat resolveCode(String code, Locale locale);

}
