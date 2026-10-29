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

package org.springframework.web.reactive.result.view;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.EscapedErrors;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 请求特定状态的上下文持有者，例如要使用的 {@link MessageSource}、当前区域设置、绑定错误等。提供对本地化消息和 Errors 实例的便捷访问。
 *
 * <p>适合向视图公开，并在 FreeMarker 模板和标签库中使用。
 *
 * <p>可以手动实例化，也可以通过 AbstractView 的 "requestContextAttribute" 属性自动作为模型属性公开给视图。
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class RequestContext {

	private final ServerWebExchange exchange;

	private final Map<String, Object> model;

	private final MessageSource messageSource;

	private Locale locale;

	private TimeZone timeZone;

	@Nullable
	private Boolean defaultHtmlEscape;

	@Nullable
	private Map<String, Errors> errorsMap;

	@Nullable
	private RequestDataValueProcessor dataValueProcessor;


	public RequestContext(ServerWebExchange exchange, Map<String, Object> model, MessageSource messageSource) {
		this(exchange, model, messageSource, null);
	}

	public RequestContext(ServerWebExchange exchange, Map<String, Object> model, MessageSource messageSource,
			@Nullable RequestDataValueProcessor dataValueProcessor) {

		Assert.notNull(exchange, "ServerWebExchange is required");
		Assert.notNull(model, "Model is required");
		Assert.notNull(messageSource, "MessageSource is required");
		this.exchange = exchange;
		this.model = model;
		this.messageSource = messageSource;

		LocaleContext localeContext = exchange.getLocaleContext();
		Locale locale = localeContext.getLocale();
		this.locale = (locale != null ? locale : Locale.getDefault());
		TimeZone timeZone = (localeContext instanceof TimeZoneAwareLocaleContext ?
				((TimeZoneAwareLocaleContext) localeContext).getTimeZone() : null);
		this.timeZone = (timeZone != null ? timeZone : TimeZone.getDefault());

		this.defaultHtmlEscape = null;  // TODO
		this.dataValueProcessor = dataValueProcessor;
	}


	protected final ServerWebExchange getExchange() {
		return this.exchange;
	}

	/**
	 * 返回此请求使用的 MessageSource。
	 */
	public MessageSource getMessageSource() {
		return this.messageSource;
	}

	/**
	 * 返回此 RequestContext 封装的模型 Map（如果有）。
	 * @return the populated model Map, or {@code null} if none available
	 */
	@Nullable
	public Map<String, Object> getModel() {
		return this.model;
	}

	/**
	 * 返回当前 Locale。
	 */
	public final Locale getLocale() {
		return this.locale;
	}

	/**
	 * 返回当前 TimeZone。
	 */
	public TimeZone getTimeZone() {
		return this.timeZone;
	}

	/**
	 * 将当前区域设置更改为指定的区域设置。
	 */
	public void changeLocale(Locale locale) {
		this.locale = locale;
	}

	/**
	 * 将当前区域设置更改为指定的区域设置和时区上下文。
	 */
	public void changeLocale(Locale locale, TimeZone timeZone) {
		this.locale = locale;
		this.timeZone = timeZone;
	}

	/**
	 * 在此 RequestContext 的范围内（取消）激活消息和错误的默认 HTML 转义。
	 * <p>TODO：目前没有应用程序范围的设置 ...
	 */
	public void setDefaultHtmlEscape(boolean defaultHtmlEscape) {
		this.defaultHtmlEscape = defaultHtmlEscape;
	}

	/**
	 * 默认 HTML 转义是否已激活？如果未给出显式默认值，则回退为 {@code false}。
	 */
	public boolean isDefaultHtmlEscape() {
		return (this.defaultHtmlEscape != null && this.defaultHtmlEscape.booleanValue());
	}

	/**
	 * 返回默认 HTML 转义设置，区分未指定默认值和显式值。
	 * @return whether default HTML escaping is enabled (null = no explicit default)
	 */
	@Nullable
	public Boolean getDefaultHtmlEscape() {
		return this.defaultHtmlEscape;
	}

	/**
	 * 返回要应用于表单标签库和重定向 URL 的 {@link RequestDataValueProcessor} 实例。
	 */
	@Nullable
	public RequestDataValueProcessor getRequestDataValueProcessor() {
		return this.dataValueProcessor;
	}

	/**
	 * 返回当前 Web 应用程序的上下文路径。这对于构建指向应用程序内其他资源的链接很有用。
	 * <p>委托给 {@link ServerHttpRequest#getPath()}。
	 */
	public String getContextPath() {
		return this.exchange.getRequest().getPath().contextPath().value();
	}

	/**
	 * 为给定相对 URL 返回上下文感知的 URL。
	 * @param relativeUrl the relative URL part
	 * @return a URL that points back to the current web application with an
	 * absolute path also URL-encoded accordingly
	 */
	public String getContextUrl(String relativeUrl) {
		String url = StringUtils.applyRelativePath(getContextPath() + "/", relativeUrl);
		return getExchange().transformUrl(url);
	}

	/**
	 * 为带占位符（使用花括号 {@code {}} 命名的键）的给定相对 URL 返回上下文感知的 URL。例如，传入相对 URL {@code foo/{bar}?spam={spam}} 和参数映射 {@code {bar=baz,spam=nuts}}，结果将是 {@code [contextpath]/foo/baz?spam=nuts}。
	 * @param relativeUrl the relative URL part
	 * @param params a map of parameters to insert as placeholders in the url
	 * @return a URL that points back to the current web application with an
	 * absolute path also URL-encoded accordingly
	 */
	public String getContextUrl(String relativeUrl, Map<String, ?> params) {
		String url = StringUtils.applyRelativePath(getContextPath() + "/", relativeUrl);
		url = UriComponentsBuilder.fromUriString(url).buildAndExpand(params).encode().toUri().toASCIIString();
		return getExchange().transformUrl(url);
	}

	/**
	 * 返回请求的请求路径。这对于作为 HTML 表单 action 目标很有用，也可以与原始查询字符串组合使用。
	 */
	public String getRequestPath() {
		return this.exchange.getRequest().getURI().getPath();
	}

	/**
	 * 返回当前请求的查询字符串。这对于结合原始请求路径构建 HTML 表单 action 目标很有用。
	 */
	public String getQueryString() {
		return this.exchange.getRequest().getURI().getQuery();
	}

	/**
	 * 使用 "defaultHtmlEscape" 设置检索给定代码的消息。
	 * @param code the code of the message
	 * @param defaultMessage the String to return if the lookup fails
	 * @return the message
	 */
	public String getMessage(String code, String defaultMessage) {
		return getMessage(code, null, defaultMessage, isDefaultHtmlEscape());
	}

	/**
	 * 使用 "defaultHtmlEscape" 设置检索给定代码的消息。
	 * @param code the code of the message
	 * @param args arguments for the message, or {@code null} if none
	 * @param defaultMessage the String to return if the lookup fails
	 * @return the message
	 */
	public String getMessage(String code, @Nullable Object[] args, String defaultMessage) {
		return getMessage(code, args, defaultMessage, isDefaultHtmlEscape());
	}

	/**
	 * 使用 "defaultHtmlEscape" 设置检索给定代码的消息。
	 * @param code the code of the message
	 * @param args arguments for the message as a List, or {@code null} if none
	 * @param defaultMessage the String to return if the lookup fails
	 * @return the message
	 */
	public String getMessage(String code, @Nullable List<?> args, String defaultMessage) {
		return getMessage(code, (args != null ? args.toArray() : null), defaultMessage, isDefaultHtmlEscape());
	}

	/**
	 * 检索给定代码的消息。
	 * @param code the code of the message
	 * @param args arguments for the message, or {@code null} if none
	 * @param defaultMessage the String to return if the lookup fails
	 * @param htmlEscape if the message should be HTML-escaped
	 * @return the message
	 */
	public String getMessage(String code, @Nullable Object[] args, String defaultMessage, boolean htmlEscape) {
		String msg = this.messageSource.getMessage(code, args, defaultMessage, this.locale);
		if (msg == null) {
			return "";
		}
		return (htmlEscape ? HtmlUtils.htmlEscape(msg) : msg);
	}

	/**
	 * 使用 "defaultHtmlEscape" 设置检索给定代码的消息。
	 * @param code the code of the message
	 * @return the message
	 * @throws org.springframework.context.NoSuchMessageException if not found
	 */
	public String getMessage(String code) throws NoSuchMessageException {
		return getMessage(code, null, isDefaultHtmlEscape());
	}

	/**
	 * 使用 "defaultHtmlEscape" 设置检索给定代码的消息。
	 * @param code the code of the message
	 * @param args arguments for the message, or {@code null} if none
	 * @return the message
	 * @throws org.springframework.context.NoSuchMessageException if not found
	 */
	public String getMessage(String code, @Nullable Object[] args) throws NoSuchMessageException {
		return getMessage(code, args, isDefaultHtmlEscape());
	}

	/**
	 * 使用 "defaultHtmlEscape" 设置检索给定代码的消息。
	 * @param code the code of the message
	 * @param args arguments for the message as a List, or {@code null} if none
	 * @return the message
	 * @throws org.springframework.context.NoSuchMessageException if not found
	 */
	public String getMessage(String code, @Nullable List<?> args) throws NoSuchMessageException {
		return getMessage(code, (args != null ? args.toArray() : null), isDefaultHtmlEscape());
	}

	/**
	 * 检索给定代码的消息。
	 * @param code the code of the message
	 * @param args arguments for the message, or {@code null} if none
	 * @param htmlEscape if the message should be HTML-escaped
	 * @return the message
	 * @throws org.springframework.context.NoSuchMessageException if not found
	 */
	public String getMessage(String code, @Nullable Object[] args, boolean htmlEscape) throws NoSuchMessageException {
		String msg = this.messageSource.getMessage(code, args, this.locale);
		return (htmlEscape ? HtmlUtils.htmlEscape(msg) : msg);
	}

	/**
	 * 使用 "defaultHtmlEscape" 设置检索给定的 MessageSourceResolvable（例如 ObjectError 实例）。
	 * @param resolvable the MessageSourceResolvable
	 * @return the message
	 * @throws org.springframework.context.NoSuchMessageException if not found
	 */
	public String getMessage(MessageSourceResolvable resolvable) throws NoSuchMessageException {
		return getMessage(resolvable, isDefaultHtmlEscape());
	}

	/**
	 * 检索给定的 MessageSourceResolvable（例如 ObjectError 实例）。
	 * @param resolvable the MessageSourceResolvable
	 * @param htmlEscape if the message should be HTML-escaped
	 * @return the message
	 * @throws org.springframework.context.NoSuchMessageException if not found
	 */
	public String getMessage(MessageSourceResolvable resolvable, boolean htmlEscape) throws NoSuchMessageException {
		String msg = this.messageSource.getMessage(resolvable, this.locale);
		return (htmlEscape ? HtmlUtils.htmlEscape(msg) : msg);
	}

	/**
	 * 使用 "defaultHtmlEscape" 设置检索给定绑定对象的 Errors 实例。
	 * @param name the name of the bind object
	 * @return the Errors instance, or {@code null} if not found
	 */
	@Nullable
	public Errors getErrors(String name) {
		return getErrors(name, isDefaultHtmlEscape());
	}

	/**
	 * 检索给定绑定对象的 Errors 实例。
	 * @param name the name of the bind object
	 * @param htmlEscape create an Errors instance with automatic HTML escaping?
	 * @return the Errors instance, or {@code null} if not found
	 */
	@Nullable
	public Errors getErrors(String name, boolean htmlEscape) {
		if (this.errorsMap == null) {
			this.errorsMap = new HashMap<>();
		}

		Errors errors = this.errorsMap.get(name);
		if (errors == null) {
			errors = getModelObject(BindingResult.MODEL_KEY_PREFIX + name);
			if (errors == null) {
				return null;
			}
		}

		if (errors instanceof BindException) {
			errors = ((BindException) errors).getBindingResult();
		}

		if (htmlEscape && !(errors instanceof EscapedErrors)) {
			errors = new EscapedErrors(errors);
		}
		else if (!htmlEscape && errors instanceof EscapedErrors) {
			errors = ((EscapedErrors) errors).getSource();
		}

		this.errorsMap.put(name, errors);
		return errors;
	}

	/**
	 * 从模型或请求属性中检索给定模型名称的模型对象。
	 * @param modelName the name of the model object
	 * @return the model object
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	protected <T> T getModelObject(String modelName) {
		T modelObject = (T) this.model.get(modelName);
		if (modelObject == null) {
			modelObject = this.exchange.getAttribute(modelName);
		}
		return modelObject;
	}

	/**
	 * 使用 "defaultHtmlEscape" 设置为给定绑定对象创建 BindStatus。
	 * @param path the bean and property path for which values and errors will
	 * be resolved (e.g. "person.age")
	 * @return the new BindStatus instance
	 * @throws IllegalStateException if no corresponding Errors object found
	 */
	public BindStatus getBindStatus(String path) throws IllegalStateException {
		return new BindStatus(this, path, isDefaultHtmlEscape());
	}

	/**
	 * 为给定绑定对象创建 BindStatus，使用 "defaultHtmlEscape" 设置。
	 * @param path the bean and property path for which values and errors will
	 * be resolved (e.g. "person.age")
	 * @param htmlEscape create a BindStatus with automatic HTML escaping?
	 * @return the new BindStatus instance
	 * @throws IllegalStateException if no corresponding Errors object found
	 */
	public BindStatus getBindStatus(String path, boolean htmlEscape) throws IllegalStateException {
		return new BindStatus(this, path, htmlEscape);
	}

}
