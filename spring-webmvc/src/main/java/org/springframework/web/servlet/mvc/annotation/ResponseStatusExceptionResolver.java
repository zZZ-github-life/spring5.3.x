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

package org.springframework.web.servlet.mvc.annotation;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver;

/**
 * 一个 {@link org.springframework.web.servlet.HandlerExceptionResolver
 * HandlerExceptionResolver}，它使用 {@link ResponseStatus @ResponseStatus}
 * 注解将异常映射到 HTTP 状态码。
 *
 * <p>此异常解析器在
 * {@link org.springframework.web.servlet.DispatcherServlet DispatcherServlet}
 * 以及 MVC Java 配置和 MVC 命名空间中默认启用。
 *
 * <p>从 4.2 开始，此解析器还会递归查找原因异常上存在的
 * {@code @ResponseStatus}；从 4.2.2 开始，此解析器支持
 * 在自定义组合注解中覆盖 {@code @ResponseStatus} 的属性。
 *
 * <p>从 5.0 开始，此解析器还支持 {@link ResponseStatusException}。
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 3.0
 * @see ResponseStatus
 * @see ResponseStatusException
 */
public class ResponseStatusExceptionResolver extends AbstractHandlerExceptionResolver implements MessageSourceAware {

	@Nullable
	private MessageSource messageSource;


	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}


	@Override
	@Nullable
	protected ModelAndView doResolveException(
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler, Exception ex) {

		try {
			if (ex instanceof ResponseStatusException) {
				return resolveResponseStatusException((ResponseStatusException) ex, request, response, handler);
			}

			ResponseStatus status = AnnotatedElementUtils.findMergedAnnotation(ex.getClass(), ResponseStatus.class);
			if (status != null) {
				return resolveResponseStatus(status, request, response, handler, ex);
			}

			if (ex.getCause() instanceof Exception) {
				return doResolveException(request, response, handler, (Exception) ex.getCause());
			}
		}
		catch (Exception resolveEx) {
			if (logger.isWarnEnabled()) {
				logger.warn("Failure while trying to resolve exception [" + ex.getClass().getName() + "]", resolveEx);
			}
		}
		return null;
	}

	/**
	 * 处理 {@link ResponseStatus @ResponseStatus} 注解的模板方法。
	 * <p>默认实现使用注解中的状态码和原因
	 * 委托给 {@link #applyStatusAndReason}。
	 * @param responseStatus {@code @ResponseStatus} 注解
	 * @param request 当前 HTTP 请求
	 * @param response 当前 HTTP 响应
	 * @param handler 已执行的处理器，若在异常发生时未选择任何处理器则为 {@code null}
	 * （例如 multipart 解析失败）
	 * @param ex 异常
	 * @return 空的 ModelAndView，即异常已解析
	 */
	protected ModelAndView resolveResponseStatus(ResponseStatus responseStatus, HttpServletRequest request,
			HttpServletResponse response, @Nullable Object handler, Exception ex) throws Exception {

		int statusCode = responseStatus.code().value();
		String reason = responseStatus.reason();
		return applyStatusAndReason(statusCode, reason, response);
	}

	/**
	 * 处理 {@link ResponseStatusException} 的模板方法。
	 * <p>默认实现应用
	 * {@link ResponseStatusException#getResponseHeaders()} 中的响应头，并使用
	 * 异常中的状态码和原因委托给
	 * {@link #applyStatusAndReason}。
	 * @param ex 异常
	 * @param request 当前 HTTP 请求
	 * @param response 当前 HTTP 响应
	 * @param handler 已执行的处理器，若在异常发生时未选择任何处理器则为 {@code null}
	 * （例如 multipart 解析失败）
	 * @return 空的 ModelAndView，即异常已解析
	 * @since 5.0
	 */
	protected ModelAndView resolveResponseStatusException(ResponseStatusException ex,
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws Exception {

		ex.getResponseHeaders().forEach((name, values) ->
				values.forEach(value -> response.addHeader(name, value)));

		return applyStatusAndReason(ex.getRawStatusCode(), ex.getReason(), response);
	}

	/**
	 * 将解析出的状态码和原因应用到响应。
	 * <p>如果存在原因，默认实现使用
	 * {@link HttpServletResponse#sendError(int)} 或
	 * {@link HttpServletResponse#sendError(int, String)} 发送响应错误，
	 * 然后返回一个空的 ModelAndView。
	 * @param statusCode HTTP 状态码
	 * @param reason 关联的原因（可能为 {@code null} 或空）
	 * @param response 当前 HTTP 响应
	 * @since 5.0
	 */
	protected ModelAndView applyStatusAndReason(int statusCode, @Nullable String reason, HttpServletResponse response)
			throws IOException {

		if (!StringUtils.hasLength(reason)) {
			response.sendError(statusCode);
		}
		else {
			String resolvedReason = (this.messageSource != null ?
					this.messageSource.getMessage(reason, null, reason, LocaleContextHolder.getLocale()) :
					reason);
			response.sendError(statusCode, resolvedReason);
		}
		return new ModelAndView();
	}

}
