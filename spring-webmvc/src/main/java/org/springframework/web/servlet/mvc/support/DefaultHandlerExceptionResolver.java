/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.servlet.mvc.support;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver;

/**
 * {@link org.springframework.web.servlet.HandlerExceptionResolver}
 * 接口的默认实现，用于解析标准 Spring MVC 异常并将其转换为相应的
 * HTTP 状态码。
 *
 * <p>此异常解析器在常用的 Spring
 * {@link org.springframework.web.servlet.DispatcherServlet} 中默认启用。
 *
 * <table>
 * <caption>支持的异常</caption>
 * <thead>
 * <tr>
 * <th class="colFirst">异常</th>
 * <th class="colLast">HTTP 状态码</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr class="altColor">
 * <td><p>HttpRequestMethodNotSupportedException</p></td>
 * <td><p>405 (SC_METHOD_NOT_ALLOWED)</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>HttpMediaTypeNotSupportedException</p></td>
 * <td><p>415 (SC_UNSUPPORTED_MEDIA_TYPE)</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>HttpMediaTypeNotAcceptableException</p></td>
 * <td><p>406 (SC_NOT_ACCEPTABLE)</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>MissingPathVariableException</p></td>
 * <td><p>500 (SC_INTERNAL_SERVER_ERROR)</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>MissingServletRequestParameterException</p></td>
 * <td><p>400 (SC_BAD_REQUEST)</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>ServletRequestBindingException</p></td>
 * <td><p>400 (SC_BAD_REQUEST)</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>ConversionNotSupportedException</p></td>
 * <td><p>500 (SC_INTERNAL_SERVER_ERROR)</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>TypeMismatchException</p></td>
 * <td><p>400 (SC_BAD_REQUEST)</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>HttpMessageNotReadableException</p></td>
 * <td><p>400 (SC_BAD_REQUEST)</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>HttpMessageNotWritableException</p></td>
 * <td><p>500 (SC_INTERNAL_SERVER_ERROR)</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>MethodArgumentNotValidException</p></td>
 * <td><p>400 (SC_BAD_REQUEST)</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>MissingServletRequestPartException</p></td>
 * <td><p>400 (SC_BAD_REQUEST)</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>BindException</p></td>
 * <td><p>400 (SC_BAD_REQUEST)</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>NoHandlerFoundException</p></td>
 * <td><p>404 (SC_NOT_FOUND)</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>AsyncRequestTimeoutException</p></td>
 * <td><p>503 (SC_SERVICE_UNAVAILABLE)</p></td>
 * </tr>
 * </tbody>
 * </table>
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.0
 * @see org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
 */
public class DefaultHandlerExceptionResolver extends AbstractHandlerExceptionResolver {

	/**
	 * 当请求未找到映射的处理器时使用的日志类别。
	 * @see #pageNotFoundLogger
	 */
	public static final String PAGE_NOT_FOUND_LOG_CATEGORY = "org.springframework.web.servlet.PageNotFound";

	/**
	 * 当请求未找到映射的处理器时使用的附加 logger。
	 * @see #PAGE_NOT_FOUND_LOG_CATEGORY
	 */
	protected static final Log pageNotFoundLogger = LogFactory.getLog(PAGE_NOT_FOUND_LOG_CATEGORY);


	/**
	 * 将 {@linkplain #setOrder(int) order} 设置为 {@link #LOWEST_PRECEDENCE}。
	 */
	public DefaultHandlerExceptionResolver() {
		setOrder(Ordered.LOWEST_PRECEDENCE);
		setWarnLogCategory(getClass().getName());
	}


	@Override
	@Nullable
	protected ModelAndView doResolveException(
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler, Exception ex) {

		try {
			if (ex instanceof HttpRequestMethodNotSupportedException) {
				return handleHttpRequestMethodNotSupported(
						(HttpRequestMethodNotSupportedException) ex, request, response, handler);
			}
			else if (ex instanceof HttpMediaTypeNotSupportedException) {
				return handleHttpMediaTypeNotSupported(
						(HttpMediaTypeNotSupportedException) ex, request, response, handler);
			}
			else if (ex instanceof HttpMediaTypeNotAcceptableException) {
				return handleHttpMediaTypeNotAcceptable(
						(HttpMediaTypeNotAcceptableException) ex, request, response, handler);
			}
			else if (ex instanceof MissingPathVariableException) {
				return handleMissingPathVariable(
						(MissingPathVariableException) ex, request, response, handler);
			}
			else if (ex instanceof MissingServletRequestParameterException) {
				return handleMissingServletRequestParameter(
						(MissingServletRequestParameterException) ex, request, response, handler);
			}
			else if (ex instanceof ServletRequestBindingException) {
				return handleServletRequestBindingException(
						(ServletRequestBindingException) ex, request, response, handler);
			}
			else if (ex instanceof ConversionNotSupportedException) {
				return handleConversionNotSupported(
						(ConversionNotSupportedException) ex, request, response, handler);
			}
			else if (ex instanceof TypeMismatchException) {
				return handleTypeMismatch(
						(TypeMismatchException) ex, request, response, handler);
			}
			else if (ex instanceof HttpMessageNotReadableException) {
				return handleHttpMessageNotReadable(
						(HttpMessageNotReadableException) ex, request, response, handler);
			}
			else if (ex instanceof HttpMessageNotWritableException) {
				return handleHttpMessageNotWritable(
						(HttpMessageNotWritableException) ex, request, response, handler);
			}
			else if (ex instanceof MethodArgumentNotValidException) {
				return handleMethodArgumentNotValidException(
						(MethodArgumentNotValidException) ex, request, response, handler);
			}
			else if (ex instanceof MissingServletRequestPartException) {
				return handleMissingServletRequestPartException(
						(MissingServletRequestPartException) ex, request, response, handler);
			}
			else if (ex instanceof BindException) {
				return handleBindException((BindException) ex, request, response, handler);
			}
			else if (ex instanceof NoHandlerFoundException) {
				return handleNoHandlerFoundException(
						(NoHandlerFoundException) ex, request, response, handler);
			}
			else if (ex instanceof AsyncRequestTimeoutException) {
				return handleAsyncRequestTimeoutException(
						(AsyncRequestTimeoutException) ex, request, response, handler);
			}
		}
		catch (Exception handlerEx) {
			if (logger.isWarnEnabled()) {
				logger.warn("Failure while trying to resolve exception [" + ex.getClass().getName() + "]", handlerEx);
			}
		}
		return null;
	}

	/**
	 * 处理针对特定 HTTP 请求方法未找到请求处理器方法的情况。
	 * <p>默认实现会记录一条警告、发送 HTTP 405 错误、设置 "Allow" 头，
	 * 并返回空的 {@code ModelAndView}。或者，可以选择一个回退视图，
	 * 或者将 HttpRequestMethodNotSupportedException 原样重新抛出。
	 * @param ex the HttpRequestMethodNotSupportedException to be handled
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler, or {@code null} if none chosen
	 * at the time of the exception (for example, if multipart resolution failed)
	 * @return an empty ModelAndView indicating the exception was handled
	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
	 */
	protected ModelAndView handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		String[] supportedMethods = ex.getSupportedMethods();
		if (supportedMethods != null) {
			response.setHeader("Allow", StringUtils.arrayToDelimitedString(supportedMethods, ", "));
		}
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, ex.getMessage());
		return new ModelAndView();
	}

	/**
	 * 处理未找到可用于 PUT 或 POST 内容的
	 * {@linkplain org.springframework.http.converter.HttpMessageConverter message converters} 的情况。
	 * <p>默认实现会发送 HTTP 415 错误、设置 "Accept" 头，
	 * 并返回空的 {@code ModelAndView}。或者，可以选择一个回退视图，
	 * 或者将 HttpMediaTypeNotSupportedException 原样重新抛出。
	 * @param ex the HttpMediaTypeNotSupportedException to be handled
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler
	 * @return an empty ModelAndView indicating the exception was handled
	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
	 */
	protected ModelAndView handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		List<MediaType> mediaTypes = ex.getSupportedMediaTypes();
		if (!CollectionUtils.isEmpty(mediaTypes)) {
			response.setHeader("Accept", MediaType.toString(mediaTypes));
			if (request.getMethod().equals("PATCH")) {
				response.setHeader("Accept-Patch", MediaType.toString(mediaTypes));
			}
		}
		response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
		return new ModelAndView();
	}

	/**
	 * 处理未找到客户端可接受（通过 {@code Accept} 头表示）的
	 * {@linkplain org.springframework.http.converter.HttpMessageConverter message converters} 的情况。
	 * <p>默认实现会发送 HTTP 406 错误并返回空的 {@code ModelAndView}。
	 * 或者，可以选择一个回退视图，或者将 HttpMediaTypeNotAcceptableException
	 * 原样重新抛出。
	 * @param ex the HttpMediaTypeNotAcceptableException to be handled
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler
	 * @return an empty ModelAndView indicating the exception was handled
	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
	 */
	protected ModelAndView handleHttpMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex,
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
		return new ModelAndView();
	}

	/**
	 * 处理声明的路径变量与任何提取出的 URI 变量都不匹配的情况。
	 * <p>默认实现会发送 HTTP 500 错误，并返回空的 {@code ModelAndView}。
	 * 或者，可以选择一个回退视图，或者将 MissingPathVariableException
	 * 原样重新抛出。
	 * @param ex the MissingPathVariableException to be handled
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler
	 * @return an empty ModelAndView indicating the exception was handled
	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
	 * @since 4.2
	 */
	protected ModelAndView handleMissingPathVariable(MissingPathVariableException ex,
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
		return new ModelAndView();
	}

	/**
	 * 处理缺少必需参数的情况。
	 * <p>默认实现会发送 HTTP 400 错误，并返回空的 {@code ModelAndView}。
	 * 或者，可以选择一个回退视图，或者将 MissingServletRequestParameterException
	 * 原样重新抛出。
	 * @param ex the MissingServletRequestParameterException to be handled
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler
	 * @return an empty ModelAndView indicating the exception was handled
	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
	 */
	protected ModelAndView handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
		return new ModelAndView();
	}

	/**
	 * 处理发生不可恢复的绑定异常的情况——例如缺少必需的请求头、必需的 cookie。
	 * <p>默认实现会发送 HTTP 400 错误，并返回空的 {@code ModelAndView}。
	 * 或者，可以选择一个回退视图，或者将异常原样重新抛出。
	 * @param ex the exception to be handled
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler
	 * @return an empty ModelAndView indicating the exception was handled
	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
	 */
	protected ModelAndView handleServletRequestBindingException(ServletRequestBindingException ex,
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
		return new ModelAndView();
	}

	/**
	 * 处理无法执行 {@link org.springframework.web.bind.WebDataBinder} 转换的情况。
	 * <p>默认实现会发送 HTTP 500 错误，并返回空的 {@code ModelAndView}。
	 * 或者，可以选择一个回退视图，或者将 ConversionNotSupportedException
	 * 原样重新抛出。
	 * @param ex the ConversionNotSupportedException to be handled
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler
	 * @return an empty ModelAndView indicating the exception was handled
	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
	 */
	protected ModelAndView handleConversionNotSupported(ConversionNotSupportedException ex,
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		sendServerError(ex, request, response);
		return new ModelAndView();
	}

	/**
	 * 处理发生 {@link org.springframework.web.bind.WebDataBinder} 转换错误的情况。
	 * <p>默认实现会发送 HTTP 400 错误，并返回空的 {@code ModelAndView}。
	 * 或者，可以选择一个回退视图，或者将 TypeMismatchException 原样重新抛出。
	 * @param ex the TypeMismatchException to be handled
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler
	 * @return an empty ModelAndView indicating the exception was handled
	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
	 */
	protected ModelAndView handleTypeMismatch(TypeMismatchException ex,
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		response.sendError(HttpServletResponse.SC_BAD_REQUEST);
		return new ModelAndView();
	}

	/**
	 * 处理
	 * {@linkplain org.springframework.http.converter.HttpMessageConverter message converter}
	 * 无法从 HTTP 请求读取的情况。
	 * <p>默认实现会发送 HTTP 400 错误，并返回空的 {@code ModelAndView}。
	 * 或者，可以选择一个回退视图，或者将 HttpMessageNotReadableException
	 * 原样重新抛出。
	 * @param ex the HttpMessageNotReadableException to be handled
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler
	 * @return an empty ModelAndView indicating the exception was handled
	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
	 */
	protected ModelAndView handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		response.sendError(HttpServletResponse.SC_BAD_REQUEST);
		return new ModelAndView();
	}

	/**
	 * 处理
	 * {@linkplain org.springframework.http.converter.HttpMessageConverter message converter}
	 * 无法向 HTTP 请求写入的情况。
	 * <p>默认实现会发送 HTTP 500 错误，并返回空的 {@code ModelAndView}。
	 * 或者，可以选择一个回退视图，或者将 HttpMessageNotWritableException
	 * 原样重新抛出。
	 * @param ex the HttpMessageNotWritableException to be handled
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler
	 * @return an empty ModelAndView indicating the exception was handled
	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
	 */
	protected ModelAndView handleHttpMessageNotWritable(HttpMessageNotWritableException ex,
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		sendServerError(ex, request, response);
		return new ModelAndView();
	}

	/**
	 * 处理带有 {@code @Valid} 注解的参数（例如
	 * {@link RequestBody} 或 {@link RequestPart} 参数）校验失败的情况。
	 * <p>默认情况下，向客户端返回 HTTP 400 错误。
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler
	 * @return an empty ModelAndView indicating the exception was handled
	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
	 */
	protected ModelAndView handleMethodArgumentNotValidException(MethodArgumentNotValidException ex,
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		response.sendError(HttpServletResponse.SC_BAD_REQUEST);
		return new ModelAndView();
	}

	/**
	 * 处理需要 {@linkplain RequestPart @RequestPart}、{@link MultipartFile}
	 * 或 {@code javax.servlet.http.Part} 参数但该参数缺失的情况。
	 * <p>默认情况下，向客户端返回 HTTP 400 错误。
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler
	 * @return an empty ModelAndView indicating the exception was handled
	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
	 */
	protected ModelAndView handleMissingServletRequestPartException(MissingServletRequestPartException ex,
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
		return new ModelAndView();
	}

	/**
	 * 处理 {@linkplain ModelAttribute @ModelAttribute} 方法参数
	 * 存在绑定或校验错误，且其后没有另一个
	 * {@link BindingResult} 类型的方法参数的情况。
	 * <p>默认情况下，向客户端返回 HTTP 400 错误。
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler
	 * @return an empty ModelAndView indicating the exception was handled
	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
	 */
	protected ModelAndView handleBindException(BindException ex, HttpServletRequest request,
			HttpServletResponse response, @Nullable Object handler) throws IOException {

		response.sendError(HttpServletResponse.SC_BAD_REQUEST);
		return new ModelAndView();
	}

	/**
	 * 处理分派期间未找到处理器的情况。
	 * <p>默认实现会发送 HTTP 404 错误并返回空的
	 * {@code ModelAndView}。或者，可以选择一个回退视图，
	 * 或者将 NoHandlerFoundException 原样重新抛出。
	 * @param ex the NoHandlerFoundException to be handled
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler, or {@code null} if none chosen
	 * at the time of the exception (for example, if multipart resolution failed)
	 * @return an empty ModelAndView indicating the exception was handled
	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
	 * @since 4.0
	 */
	protected ModelAndView handleNoHandlerFoundException(NoHandlerFoundException ex,
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		pageNotFoundLogger.warn(ex.getMessage());
		response.sendError(HttpServletResponse.SC_NOT_FOUND);
		return new ModelAndView();
	}

	/**
	 * 处理异步请求超时的情况。
	 * <p>默认实现会发送 HTTP 503 错误。
	 * @param ex the {@link AsyncRequestTimeoutException} to be handled
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler, or {@code null} if none chosen
	 * at the time of the exception (for example, if multipart resolution failed)
	 * @return an empty ModelAndView indicating the exception was handled
	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
	 * @since 4.2.8
	 */
	protected ModelAndView handleAsyncRequestTimeoutException(AsyncRequestTimeoutException ex,
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		if (!response.isCommitted()) {
			response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
		}
		else {
			logger.warn("Async request timed out");
		}
		return new ModelAndView();
	}

	/**
	 * 调用以发送服务器错误。将状态设为 500，并将
	 * 请求属性 "javax.servlet.error.exception" 设置为该 Exception。
	 */
	protected void sendServerError(Exception ex, HttpServletRequest request, HttpServletResponse response)
			throws IOException {

		request.setAttribute("javax.servlet.error.exception", ex);
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	}

}
