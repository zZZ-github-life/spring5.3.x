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

package org.springframework.web.servlet.mvc.method.annotation;

import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.util.WebUtils;

/**
 * {@link ControllerAdvice @ControllerAdvice} 类的便捷基类，
 * 这些类希望通过 {@code @ExceptionHandler} 方法在所有
 * {@code @RequestMapping} 方法之间提供集中式异常处理。
 *
 * <p>此基类提供了一个用于处理 Spring MVC 内部异常的
 * {@code @ExceptionHandler} 方法。该方法返回 {@code ResponseEntity}，
 * 以便通过 {@link HttpMessageConverter message converter} 写入响应；这与
 * {@link org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver
 * DefaultHandlerExceptionResolver} 不同，后者返回
 * {@link org.springframework.web.servlet.ModelAndView ModelAndView}。
 *
 * <p>如果不需要将错误内容写入响应体，或者
 * 使用视图解析（例如通过 {@code ContentNegotiatingViewResolver}），
 * 那么 {@code DefaultHandlerExceptionResolver} 就足够了。
 *
 * <p>请注意，要使 {@code @ControllerAdvice} 子类被检测到，
 * 必须配置 {@link ExceptionHandlerExceptionResolver}。
 * @author Rossen Stoyanchev
 * @since 3.2
 * @see #handleException(Exception, WebRequest)
 * @see org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver
 */
public abstract class ResponseEntityExceptionHandler {

	/**
	 * 当请求未找到映射的处理器时使用的日志类别。
	 * @see #pageNotFoundLogger
	 */
	public static final String PAGE_NOT_FOUND_LOG_CATEGORY = "org.springframework.web.servlet.PageNotFound";

	/**
	 * 当请求未找到映射的处理器时使用的专用 logger。
	 * @see #PAGE_NOT_FOUND_LOG_CATEGORY
	 */
	protected static final Log pageNotFoundLogger = LogFactory.getLog(PAGE_NOT_FOUND_LOG_CATEGORY);

	/**
	 * 供子类使用的通用 logger。
	 */
	protected final Log logger = LogFactory.getLog(getClass());


	/**
	 * 提供对标准 Spring MVC 异常的处理。
	 * @param ex 目标异常
	 * @param request 当前请求
	 */
	@ExceptionHandler({
			HttpRequestMethodNotSupportedException.class,
			HttpMediaTypeNotSupportedException.class,
			HttpMediaTypeNotAcceptableException.class,
			MissingPathVariableException.class,
			MissingServletRequestParameterException.class,
			ServletRequestBindingException.class,
			ConversionNotSupportedException.class,
			TypeMismatchException.class,
			HttpMessageNotReadableException.class,
			HttpMessageNotWritableException.class,
			MethodArgumentNotValidException.class,
			MissingServletRequestPartException.class,
			BindException.class,
			NoHandlerFoundException.class,
			AsyncRequestTimeoutException.class
		})
	@Nullable
	public final ResponseEntity<Object> handleException(Exception ex, WebRequest request) throws Exception {
		HttpHeaders headers = new HttpHeaders();

		if (ex instanceof HttpRequestMethodNotSupportedException) {
			HttpStatus status = HttpStatus.METHOD_NOT_ALLOWED;
			return handleHttpRequestMethodNotSupported((HttpRequestMethodNotSupportedException) ex, headers, status, request);
		}
		else if (ex instanceof HttpMediaTypeNotSupportedException) {
			HttpStatus status = HttpStatus.UNSUPPORTED_MEDIA_TYPE;
			return handleHttpMediaTypeNotSupported((HttpMediaTypeNotSupportedException) ex, headers, status, request);
		}
		else if (ex instanceof HttpMediaTypeNotAcceptableException) {
			HttpStatus status = HttpStatus.NOT_ACCEPTABLE;
			return handleHttpMediaTypeNotAcceptable((HttpMediaTypeNotAcceptableException) ex, headers, status, request);
		}
		else if (ex instanceof MissingPathVariableException) {
			HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
			return handleMissingPathVariable((MissingPathVariableException) ex, headers, status, request);
		}
		else if (ex instanceof MissingServletRequestParameterException) {
			HttpStatus status = HttpStatus.BAD_REQUEST;
			return handleMissingServletRequestParameter((MissingServletRequestParameterException) ex, headers, status, request);
		}
		else if (ex instanceof ServletRequestBindingException) {
			HttpStatus status = HttpStatus.BAD_REQUEST;
			return handleServletRequestBindingException((ServletRequestBindingException) ex, headers, status, request);
		}
		else if (ex instanceof ConversionNotSupportedException) {
			HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
			return handleConversionNotSupported((ConversionNotSupportedException) ex, headers, status, request);
		}
		else if (ex instanceof TypeMismatchException) {
			HttpStatus status = HttpStatus.BAD_REQUEST;
			return handleTypeMismatch((TypeMismatchException) ex, headers, status, request);
		}
		else if (ex instanceof HttpMessageNotReadableException) {
			HttpStatus status = HttpStatus.BAD_REQUEST;
			return handleHttpMessageNotReadable((HttpMessageNotReadableException) ex, headers, status, request);
		}
		else if (ex instanceof HttpMessageNotWritableException) {
			HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
			return handleHttpMessageNotWritable((HttpMessageNotWritableException) ex, headers, status, request);
		}
		else if (ex instanceof MethodArgumentNotValidException) {
			HttpStatus status = HttpStatus.BAD_REQUEST;
			return handleMethodArgumentNotValid((MethodArgumentNotValidException) ex, headers, status, request);
		}
		else if (ex instanceof MissingServletRequestPartException) {
			HttpStatus status = HttpStatus.BAD_REQUEST;
			return handleMissingServletRequestPart((MissingServletRequestPartException) ex, headers, status, request);
		}
		else if (ex instanceof BindException) {
			HttpStatus status = HttpStatus.BAD_REQUEST;
			return handleBindException((BindException) ex, headers, status, request);
		}
		else if (ex instanceof NoHandlerFoundException) {
			HttpStatus status = HttpStatus.NOT_FOUND;
			return handleNoHandlerFoundException((NoHandlerFoundException) ex, headers, status, request);
		}
		else if (ex instanceof AsyncRequestTimeoutException) {
			HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
			return handleAsyncRequestTimeoutException((AsyncRequestTimeoutException) ex, headers, status, request);
		}
		else {
			// Unknown exception, typically a wrapper with a common MVC exception as cause
			// (since @ExceptionHandler type declarations also match first-level causes):
			// We only deal with top-level MVC exceptions here, so let's rethrow the given
			// exception for further processing through the HandlerExceptionResolver chain.
			throw ex;
		}
	}

	/**
	 * 自定义 HttpRequestMethodNotSupportedException 的响应。
	 * <p>此方法会记录一条警告、设置 "Allow" 头，并委托给
	 * {@link #handleExceptionInternal}。
	 * @param ex 异常
	 * @param headers 要写入响应的响应头
	 * @param status 选定的响应状态
	 * @param request 当前请求
	 * @return {@code ResponseEntity} 实例
	 */
	protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
			HttpRequestMethodNotSupportedException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		pageNotFoundLogger.warn(ex.getMessage());

		Set<HttpMethod> supportedMethods = ex.getSupportedHttpMethods();
		if (!CollectionUtils.isEmpty(supportedMethods)) {
			headers.setAllow(supportedMethods);
		}
		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义 HttpMediaTypeNotSupportedException 的响应。
	 * <p>此方法设置 "Accept" 头，并委托给
	 * {@link #handleExceptionInternal}。
	 * @param ex 该异常
	 * @param headers 要写入响应的头信息
	 * @param status 选定的响应状态
	 * @param request 当前请求
	 * @return 一个 {@code ResponseEntity} 实例
	 */
	protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
			HttpMediaTypeNotSupportedException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		List<MediaType> mediaTypes = ex.getSupportedMediaTypes();
		if (!CollectionUtils.isEmpty(mediaTypes)) {
			headers.setAccept(mediaTypes);
			if (request instanceof ServletWebRequest) {
				ServletWebRequest servletWebRequest = (ServletWebRequest) request;
				if (HttpMethod.PATCH.equals(servletWebRequest.getHttpMethod())) {
					headers.setAcceptPatch(mediaTypes);
				}
			}
		}

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义 HttpMediaTypeNotAcceptableException 的响应。
	 * <p>此方法委托给 {@link #handleExceptionInternal}。
	 * @param ex 该异常
	 * @param headers 要写入响应的头信息
	 * @param status 选定的响应状态
	 * @param request 当前请求
	 * @return 一个 {@code ResponseEntity} 实例
	 */
	protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(
			HttpMediaTypeNotAcceptableException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义 MissingPathVariableException 的响应。
	 * <p>此方法委托给 {@link #handleExceptionInternal}。
	 * @param ex 该异常
	 * @param headers 要写入响应的头信息
	 * @param status 选定的响应状态
	 * @param request 当前请求
	 * @return 一个 {@code ResponseEntity} 实例
	 * @since 4.2
	 */
	protected ResponseEntity<Object> handleMissingPathVariable(
			MissingPathVariableException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义 MissingServletRequestParameterException 的响应。
	 * <p>此方法委托给 {@link #handleExceptionInternal}。
	 * @param ex 该异常
	 * @param headers 要写入响应的头信息
	 * @param status 选定的响应状态
	 * @param request 当前请求
	 * @return 一个 {@code ResponseEntity} 实例
	 */
	protected ResponseEntity<Object> handleMissingServletRequestParameter(
			MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义 ServletRequestBindingException 的响应。
	 * <p>此方法委托给 {@link #handleExceptionInternal}。
	 * @param ex 该异常
	 * @param headers 要写入响应的头信息
	 * @param status 选定的响应状态
	 * @param request 当前请求
	 * @return 一个 {@code ResponseEntity} 实例
	 */
	protected ResponseEntity<Object> handleServletRequestBindingException(
			ServletRequestBindingException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义 ConversionNotSupportedException 的响应。
	 * <p>此方法委托给 {@link #handleExceptionInternal}。
	 * @param ex 该异常
	 * @param headers 要写入响应的头信息
	 * @param status 选定的响应状态
	 * @param request 当前请求
	 * @return 一个 {@code ResponseEntity} 实例
	 */
	protected ResponseEntity<Object> handleConversionNotSupported(
			ConversionNotSupportedException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义 TypeMismatchException 的响应。
	 * <p>此方法委托给 {@link #handleExceptionInternal}。
	 * @param ex 该异常
	 * @param headers 要写入响应的头信息
	 * @param status 选定的响应状态
	 * @param request 当前请求
	 * @return 一个 {@code ResponseEntity} 实例
	 */
	protected ResponseEntity<Object> handleTypeMismatch(
			TypeMismatchException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义 HttpMessageNotReadableException 的响应。
	 * <p>此方法委托给 {@link #handleExceptionInternal}。
	 * @param ex 该异常
	 * @param headers 要写入响应的头信息
	 * @param status 选定的响应状态
	 * @param request 当前请求
	 * @return 一个 {@code ResponseEntity} 实例
	 */
	protected ResponseEntity<Object> handleHttpMessageNotReadable(
			HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义 HttpMessageNotWritableException 的响应。
	 * <p>此方法委托给 {@link #handleExceptionInternal}。
	 * @param ex 该异常
	 * @param headers 要写入响应的头信息
	 * @param status 选定的响应状态
	 * @param request 当前请求
	 * @return 一个 {@code ResponseEntity} 实例
	 */
	protected ResponseEntity<Object> handleHttpMessageNotWritable(
			HttpMessageNotWritableException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义 MethodArgumentNotValidException 的响应。
	 * <p>此方法委托给 {@link #handleExceptionInternal}。
	 * @param ex 该异常
	 * @param headers 要写入响应的头信息
	 * @param status 选定的响应状态
	 * @param request 当前请求
	 * @return 一个 {@code ResponseEntity} 实例
	 */
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义 MissingServletRequestPartException 的响应。
	 * <p>此方法委托给 {@link #handleExceptionInternal}。
	 * @param ex 该异常
	 * @param headers 要写入响应的头信息
	 * @param status 选定的响应状态
	 * @param request 当前请求
	 * @return 一个 {@code ResponseEntity} 实例
	 */
	protected ResponseEntity<Object> handleMissingServletRequestPart(
			MissingServletRequestPartException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义 BindException 的响应。
	 * <p>此方法委托给 {@link #handleExceptionInternal}。
	 * @param ex 该异常
	 * @param headers 要写入响应的头信息
	 * @param status 选定的响应状态
	 * @param request 当前请求
	 * @return 一个 {@code ResponseEntity} 实例
	 */
	protected ResponseEntity<Object> handleBindException(
			BindException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义 NoHandlerFoundException 的响应。
	 * <p>此方法委托给 {@link #handleExceptionInternal}。
	 * @param ex 该异常
	 * @param headers 要写入响应的头信息
	 * @param status 选定的响应状态
	 * @param request 当前请求
	 * @return 一个 {@code ResponseEntity} 实例
	 * @since 4.0
	 */
	protected ResponseEntity<Object> handleNoHandlerFoundException(
			NoHandlerFoundException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义 AsyncRequestTimeoutException 的响应。
	 * <p>此方法委托给 {@link #handleExceptionInternal}。
	 * @param ex 该异常
	 * @param headers 要写入响应的头信息
	 * @param status 选定的响应状态
	 * @param webRequest 当前请求
	 * @return 一个 {@code ResponseEntity} 实例
	 * @since 4.2.8
	 */
	@Nullable
	protected ResponseEntity<Object> handleAsyncRequestTimeoutException(
			AsyncRequestTimeoutException ex, HttpHeaders headers, HttpStatus status, WebRequest webRequest) {

		if (webRequest instanceof ServletWebRequest) {
			ServletWebRequest servletWebRequest = (ServletWebRequest) webRequest;
			HttpServletResponse response = servletWebRequest.getResponse();
			if (response != null && response.isCommitted()) {
				if (logger.isWarnEnabled()) {
					logger.warn("Async request timed out");
				}
				return null;
			}
		}

		return handleExceptionInternal(ex, null, headers, status, webRequest);
	}

	/**
	 * 集中自定义所有异常类型的响应体的地方。
	 * <p>默认实现会设置 {@link WebUtils#ERROR_EXCEPTION_ATTRIBUTE}
	 * 请求属性，并根据给定的响应体、头和状态创建
	 * {@link ResponseEntity}。
	 * @param ex 该异常
	 * @param body 响应体
	 * @param headers 响应的头信息
	 * @param status 响应状态
	 * @param request 当前请求
	 */
	protected ResponseEntity<Object> handleExceptionInternal(
			Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatus status, WebRequest request) {

		if (HttpStatus.INTERNAL_SERVER_ERROR.equals(status)) {
			request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex, WebRequest.SCOPE_REQUEST);
		}
		return new ResponseEntity<>(body, headers, status);
	}

}
