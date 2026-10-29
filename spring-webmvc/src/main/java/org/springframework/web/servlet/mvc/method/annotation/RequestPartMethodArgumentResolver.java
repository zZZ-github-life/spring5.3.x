/*
 * Copyright 2002-2022 the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.multipart.support.MultipartResolutionDelegate;
import org.springframework.web.multipart.support.RequestPartServletServerHttpRequest;

/**
 * 解析以下方法参数：
 * <ul>
 * <li>带有 @{@link RequestPart} 注解
 * <li>{@link MultipartFile} 类型，结合 Spring 的 {@link MultipartResolver} 抽象
 * <li>{@code javax.servlet.http.Part} 类型，结合 Servlet 3.0 多部分请求
 * </ul>
 *
 * <p>当参数带有 {@code @RequestPart} 注解时，部分的内容会经过
 * {@link HttpMessageConverter}，以根据请求部分的 'Content-Type'
 * 解析方法参数。这类似于 @{@link RequestBody}
 * 根据常规请求的内容解析参数的方式。
 *
 * <p>当参数未带有 {@code @RequestPart} 注解或未指定
 * 部分名称时，请求部分的名称将从方法参数名称
 * 派生。
 *
 * <p>如果参数带有任何
 * {@linkplain org.springframework.validation.annotation.ValidationAnnotationUtils#determineValidationHints
 * annotations that trigger validation}，则可能应用自动校验。如果校验失败，
 * 将抛出 {@link MethodArgumentNotValidException}；如果配置了
 * {@link org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver}，
 * 则返回 400 响应状态码。
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @since 3.1
 */
public class RequestPartMethodArgumentResolver extends AbstractMessageConverterMethodArgumentResolver {

	/**
	 * 仅包含转换器的基本构造函数。
	 */
	public RequestPartMethodArgumentResolver(List<HttpMessageConverter<?>> messageConverters) {
		super(messageConverters);
	}

	/**
	 * 包含转换器以及 {@code RequestBodyAdvice} 和
	 * {@code ResponseBodyAdvice} 的构造函数。
	 */
	public RequestPartMethodArgumentResolver(List<HttpMessageConverter<?>> messageConverters,
			List<Object> requestResponseBodyAdvice) {

		super(messageConverters, requestResponseBodyAdvice);
	}


	/**
	 * 给定的 {@linkplain MethodParameter method parameter} 是否
	 * 作为多部分参数受支持。支持以下方法参数：
	 * <ul>
	 * <li>带有 {@code @RequestPart} 注解
	 * <li>{@link MultipartFile} 类型，且未带有 {@code @RequestParam} 注解
	 * <li>{@code javax.servlet.http.Part} 类型，且未带有
	 * {@code @RequestParam} 注解
	 * </ul>
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		if (parameter.hasParameterAnnotation(RequestPart.class)) {
			return true;
		}
		else {
			if (parameter.hasParameterAnnotation(RequestParam.class)) {
				return false;
			}
			return MultipartResolutionDelegate.isMultipartArgument(parameter.nestedIfOptional());
		}
	}

	@Override
	@Nullable
	public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
			NativeWebRequest request, @Nullable WebDataBinderFactory binderFactory) throws Exception {

		HttpServletRequest servletRequest = request.getNativeRequest(HttpServletRequest.class);
		Assert.state(servletRequest != null, "No HttpServletRequest");

		RequestPart requestPart = parameter.getParameterAnnotation(RequestPart.class);
		boolean isRequired = ((requestPart == null || requestPart.required()) && !parameter.isOptional());

		String name = getPartName(parameter, requestPart);
		parameter = parameter.nestedIfOptional();
		Object arg = null;

		Object mpArg = MultipartResolutionDelegate.resolveMultipartArgument(name, parameter, servletRequest);
		if (mpArg != MultipartResolutionDelegate.UNRESOLVABLE) {
			arg = mpArg;
		}
		else {
			try {
				HttpInputMessage inputMessage = new RequestPartServletServerHttpRequest(servletRequest, name);
				arg = readWithMessageConverters(inputMessage, parameter, parameter.getNestedGenericParameterType());
				if (binderFactory != null) {
					WebDataBinder binder = binderFactory.createBinder(request, arg, name);
					if (arg != null) {
						validateIfApplicable(binder, parameter);
						if (binder.getBindingResult().hasErrors() && isBindExceptionRequired(binder, parameter)) {
							throw new MethodArgumentNotValidException(parameter, binder.getBindingResult());
						}
					}
					if (mavContainer != null) {
						mavContainer.addAttribute(BindingResult.MODEL_KEY_PREFIX + name, binder.getBindingResult());
					}
				}
			}
			catch (MissingServletRequestPartException | MultipartException ex) {
				if (isRequired) {
					throw ex;
				}
			}
		}

		if (arg == null && isRequired) {
			if (!MultipartResolutionDelegate.isMultipartRequest(servletRequest)) {
				throw new MultipartException("Current request is not a multipart request");
			}
			else {
				throw new MissingServletRequestPartException(name);
			}
		}
		return adaptArgumentIfNecessary(arg, parameter);
	}

	private String getPartName(MethodParameter methodParam, @Nullable RequestPart requestPart) {
		String partName = (requestPart != null ? requestPart.name() : "");
		if (partName.isEmpty()) {
			partName = methodParam.getParameterName();
			if (partName == null) {
				throw new IllegalArgumentException("Request part name for argument type [" +
						methodParam.getNestedParameterType().getName() +
						"] not specified, and parameter name information not found in class file either.");
			}
		}
		return partName;
	}

	@Override
	void closeStreamIfNecessary(InputStream body) {
		// RequestPartServletServerHttpRequest exposes individual part streams,
		// potentially from temporary files -> explicit close call after resolution
		// in order to prevent file descriptor leaks.
		try {
			body.close();
		}
		catch (IOException ex) {
			// ignore
		}
	}

}
