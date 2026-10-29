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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.concurrent.Callable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.MessageSource;
import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.View;
import org.springframework.web.util.NestedServletException;

/**
 * 扩展 {@link InvocableHandlerMethod}，使其能够通过已注册的
 * {@link HandlerMethodReturnValueHandler} 处理返回值，
 * 并支持根据方法级别的
 * {@code @ResponseStatus} 注解设置响应状态。
 *
 * <p>{@code null} 返回值（包括 void）结合 {@code @ResponseStatus}
 * 注解、未修改检查条件
 * （参见 {@link ServletWebRequest#checkNotModified(long)}）或
 * 提供对响应流访问的方法参数时，可以被解释为
 * 请求处理结束。
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.1
 */
public class ServletInvocableHandlerMethod extends InvocableHandlerMethod {

	private static final Method CALLABLE_METHOD = ClassUtils.getMethod(Callable.class, "call");

	@Nullable
	private HandlerMethodReturnValueHandlerComposite returnValueHandlers;


	/**
	 * 根据给定的处理器和方法创建一个实例。
	 */
	public ServletInvocableHandlerMethod(Object handler, Method method) {
		super(handler, method);
	}

	/**
	 * {@link #ServletInvocableHandlerMethod(Object, Method)} 的变体，
	 * 还接受一个 {@link MessageSource}，例如用于解析
	 * {@code @ResponseStatus} 消息。
	 * @since 5.3.10
	 */
	public ServletInvocableHandlerMethod(Object handler, Method method, @Nullable MessageSource messageSource) {
		super(handler, method, messageSource);
	}

	/**
	 * 根据 {@code HandlerMethod} 创建一个实例。
	 */
	public ServletInvocableHandlerMethod(HandlerMethod handlerMethod) {
		super(handlerMethod);
	}


	/**
	 * 注册用于处理返回值的
	 * {@link HandlerMethodReturnValueHandler} 实例。
	 */
	public void setHandlerMethodReturnValueHandlers(HandlerMethodReturnValueHandlerComposite returnValueHandlers) {
		this.returnValueHandlers = returnValueHandlers;
	}


	/**
	 * 调用方法，并通过已配置的
	 * {@link HandlerMethodReturnValueHandler HandlerMethodReturnValueHandlers} 之一处理返回值。
	 * @param webRequest the current request
	 * @param mavContainer the ModelAndViewContainer for this request
	 * @param providedArgs "given" arguments matched by type (not resolved)
	 */
	public void invokeAndHandle(ServletWebRequest webRequest, ModelAndViewContainer mavContainer,
			Object... providedArgs) throws Exception {

		Object returnValue = invokeForRequest(webRequest, mavContainer, providedArgs);
		setResponseStatus(webRequest);

		if (returnValue == null) {
			if (isRequestNotModified(webRequest) || getResponseStatus() != null || mavContainer.isRequestHandled()) {
				disableContentCachingIfNecessary(webRequest);
				mavContainer.setRequestHandled(true);
				return;
			}
		}
		else if (StringUtils.hasText(getResponseStatusReason())) {
			mavContainer.setRequestHandled(true);
			return;
		}

		mavContainer.setRequestHandled(false);
		Assert.state(this.returnValueHandlers != null, "No return value handlers");
		try {
			this.returnValueHandlers.handleReturnValue(
					returnValue, getReturnValueType(returnValue), mavContainer, webRequest);
		}
		catch (Exception ex) {
			if (logger.isTraceEnabled()) {
				logger.trace(formatErrorForReturnValue(returnValue), ex);
			}
			throw ex;
		}
	}

	/**
	 * 根据 {@link ResponseStatus} 注解设置响应状态。
	 */
	private void setResponseStatus(ServletWebRequest webRequest) throws IOException {
		HttpStatus status = getResponseStatus();
		if (status == null) {
			return;
		}

		HttpServletResponse response = webRequest.getResponse();
		if (response != null) {
			String reason = getResponseStatusReason();
			if (StringUtils.hasText(reason)) {
				response.sendError(status.value(), reason);
			}
			else {
				response.setStatus(status.value());
			}
		}

		// To be picked up by RedirectView
		webRequest.getRequest().setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, status);
	}

	/**
	 * 给定请求是否满足“未修改”条件？
	 * @see ServletWebRequest#checkNotModified(long)
	 * @see ServletWebRequest#checkNotModified(String)
	 */
	private boolean isRequestNotModified(ServletWebRequest webRequest) {
		return webRequest.isNotModified();
	}

	private void disableContentCachingIfNecessary(ServletWebRequest webRequest) {
		if (isRequestNotModified(webRequest)) {
			HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
			Assert.notNull(response, "Expected HttpServletResponse");
			if (StringUtils.hasText(response.getHeader(HttpHeaders.ETAG))) {
				HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
				Assert.notNull(request, "Expected HttpServletRequest");
			}
		}
	}

	private String formatErrorForReturnValue(@Nullable Object returnValue) {
		return "Error handling return value=[" + returnValue + "]" +
				(returnValue != null ? ", type=" + returnValue.getClass().getName() : "") +
				" in " + toString();
	}

	/**
	 * 创建一个嵌套的 ServletInvocableHandlerMethod 子类，它返回
	 * 给定值（如果该值是异常则抛出异常），而不是
	 * 实际调用控制器方法。这在处理
	 * 异步返回值（例如 Callable、DeferredResult、ListenableFuture）时很有用。
	 */
	ServletInvocableHandlerMethod wrapConcurrentResult(Object result) {
		return new ConcurrentResultHandlerMethod(result, new ConcurrentResultMethodParameter(result));
	}


	/**
	 * {@code ServletInvocableHandlerMethod} 的嵌套子类，它使用
	 * 简单的 {@link Callable} 而不是原始控制器作为处理器，
	 * 以便返回传给它的固定（并发）结果值。实际上
	 * 是以异步产生的返回值“恢复”处理。
	 */
	private class ConcurrentResultHandlerMethod extends ServletInvocableHandlerMethod {

		private final MethodParameter returnType;

		public ConcurrentResultHandlerMethod(final Object result, ConcurrentResultMethodParameter returnType) {
			super((Callable<Object>) () -> {
				if (result instanceof Exception) {
					throw (Exception) result;
				}
				else if (result instanceof Throwable) {
					throw new NestedServletException("Async processing failed", (Throwable) result);
				}
				return result;
			}, CALLABLE_METHOD);

			if (ServletInvocableHandlerMethod.this.returnValueHandlers != null) {
				setHandlerMethodReturnValueHandlers(ServletInvocableHandlerMethod.this.returnValueHandlers);
			}
			this.returnType = returnType;
		}

		/**
		 * 桥接到实际的控制器类型级别注解。
		 */
		@Override
		public Class<?> getBeanType() {
			return ServletInvocableHandlerMethod.this.getBeanType();
		}

		/**
		 * 桥接到实际返回值或声明的异步返回类型中的泛型类型，
		 * 例如 Foo 而非 {@code DeferredResult<Foo>}。
		 */
		@Override
		public MethodParameter getReturnValueType(@Nullable Object returnValue) {
			return this.returnType;
		}

		/**
		 * 桥接到控制器方法级别注解。
		 */
		@Override
		public <A extends Annotation> A getMethodAnnotation(Class<A> annotationType) {
			return ServletInvocableHandlerMethod.this.getMethodAnnotation(annotationType);
		}

		/**
		 * 桥接到控制器方法级别注解。
		 */
		@Override
		public <A extends Annotation> boolean hasMethodAnnotation(Class<A> annotationType) {
			return ServletInvocableHandlerMethod.this.hasMethodAnnotation(annotationType);
		}
	}


	/**
	 * 基于实际返回值类型的 MethodParameter 子类；如果实际返回值
	 * 为 null，则回退到声明的异步返回类型中的泛型类型，
	 * 例如 Foo 而非 {@code DeferredResult<Foo>}。
	 */
	private class ConcurrentResultMethodParameter extends HandlerMethodParameter {

		@Nullable
		private final Object returnValue;

		private final ResolvableType returnType;

		public ConcurrentResultMethodParameter(Object returnValue) {
			super(-1);
			this.returnValue = returnValue;
			this.returnType = (returnValue instanceof ReactiveTypeHandler.CollectedValuesList ?
					((ReactiveTypeHandler.CollectedValuesList) returnValue).getReturnType() :
					KotlinDetector.isSuspendingFunction(super.getMethod()) ?
					ResolvableType.forMethodParameter(getReturnType()) :
					ResolvableType.forType(super.getGenericParameterType()).getGeneric());
		}

		public ConcurrentResultMethodParameter(ConcurrentResultMethodParameter original) {
			super(original);
			this.returnValue = original.returnValue;
			this.returnType = original.returnType;
		}

		@Override
		public Class<?> getParameterType() {
			if (this.returnValue != null) {
				return this.returnValue.getClass();
			}
			if (!ResolvableType.NONE.equals(this.returnType)) {
				return this.returnType.toClass();
			}
			return super.getParameterType();
		}

		@Override
		public Type getGenericParameterType() {
			return this.returnType.getType();
		}

		@Override
		public <T extends Annotation> boolean hasMethodAnnotation(Class<T> annotationType) {
			// Ensure @ResponseBody-style handling for values collected from a reactive type
			// even if actual return type is ResponseEntity<Flux<T>>
			return (super.hasMethodAnnotation(annotationType) ||
					(annotationType == ResponseBody.class &&
							this.returnValue instanceof ReactiveTypeHandler.CollectedValuesList));
		}

		@Override
		public ConcurrentResultMethodParameter clone() {
			return new ConcurrentResultMethodParameter(this);
		}
	}

}
