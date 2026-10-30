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

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.SpringProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.ui.ModelMap;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.ControllerAdviceBean;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;
import org.springframework.web.method.annotation.MapMethodProcessor;
import org.springframework.web.method.annotation.ModelMethodProcessor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.handler.AbstractHandlerMethodExceptionResolver;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * {@link AbstractHandlerMethodExceptionResolver}，通过
 * {@code @ExceptionHandler} 方法解析异常。
 *
 * <p>可以通过 {@link #setCustomArgumentResolvers} 和
 * {@link #setCustomReturnValueHandlers} 添加对自定义参数和返回值类型的支持。
 * 或者，要重新配置所有参数和返回值类型，请使用
 * {@link #setArgumentResolvers} 和 {@link #setReturnValueHandlers(List)}。
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 3.1
 */
public class ExceptionHandlerExceptionResolver extends AbstractHandlerMethodExceptionResolver
		implements ApplicationContextAware, InitializingBean {

	/**
	 * 由 {@code spring.xml.ignore} 系统属性控制的布尔标志，指示 Spring
	 * 忽略 XML，即不初始化与 XML 相关的基础设施。
	 * <p>默认值为 "false"。
	 */
	private static final boolean shouldIgnoreXml = SpringProperties.getFlag("spring.xml.ignore");


	@Nullable
	private List<HandlerMethodArgumentResolver> customArgumentResolvers;

	@Nullable
	private HandlerMethodArgumentResolverComposite argumentResolvers;

	@Nullable
	private List<HandlerMethodReturnValueHandler> customReturnValueHandlers;

	@Nullable
	private HandlerMethodReturnValueHandlerComposite returnValueHandlers;

	private List<HttpMessageConverter<?>> messageConverters;

	private ContentNegotiationManager contentNegotiationManager = new ContentNegotiationManager();

	private final List<Object> responseBodyAdvice = new ArrayList<>();

	@Nullable
	private ApplicationContext applicationContext;

	private final Map<Class<?>, ExceptionHandlerMethodResolver> exceptionHandlerCache =
			new ConcurrentHashMap<>(64);

	private final Map<ControllerAdviceBean, ExceptionHandlerMethodResolver> exceptionHandlerAdviceCache =
			new LinkedHashMap<>();


	public ExceptionHandlerExceptionResolver() {
		this.messageConverters = new ArrayList<>();
		this.messageConverters.add(new ByteArrayHttpMessageConverter());
		this.messageConverters.add(new StringHttpMessageConverter());
		if(!shouldIgnoreXml) {
			try {
				this.messageConverters.add(new SourceHttpMessageConverter<>());
			}
			catch (Error err) {
				// Ignore when no TransformerFactory implementation is available
			}
		}
		this.messageConverters.add(new AllEncompassingFormHttpMessageConverter());
	}


	/**
	 * 为自定义参数类型提供解析器。自定义解析器排在
	 * 内置解析器之后。要覆盖内置的参数解析支持，
	 * 请改用 {@link #setArgumentResolvers}。
	 */
	public void setCustomArgumentResolvers(@Nullable List<HandlerMethodArgumentResolver> argumentResolvers) {
		this.customArgumentResolvers = argumentResolvers;
	}

	/**
	 * 返回自定义参数解析器，或 {@code null}。
	 */
	@Nullable
	public List<HandlerMethodArgumentResolver> getCustomArgumentResolvers() {
		return this.customArgumentResolvers;
	}

	/**
	 * 配置受支持参数类型的完整列表，从而覆盖
	 * 原本会默认配置的解析器。
	 */
	public void setArgumentResolvers(@Nullable List<HandlerMethodArgumentResolver> argumentResolvers) {
		if (argumentResolvers == null) {
			this.argumentResolvers = null;
		}
		else {
			this.argumentResolvers = new HandlerMethodArgumentResolverComposite();
			this.argumentResolvers.addResolvers(argumentResolvers);
		}
	}

	/**
	 * 返回已配置的参数解析器；如果尚未通过
	 * {@link #afterPropertiesSet()} 初始化，则可能返回 {@code null}。
	 */
	@Nullable
	public HandlerMethodArgumentResolverComposite getArgumentResolvers() {
		return this.argumentResolvers;
	}

	/**
	 * 为自定义返回值类型提供处理器。自定义处理器
	 * 排在内置处理器之后。要覆盖内置的
	 * 返回值处理支持，请使用 {@link #setReturnValueHandlers}。
	 */
	public void setCustomReturnValueHandlers(@Nullable List<HandlerMethodReturnValueHandler> returnValueHandlers) {
		this.customReturnValueHandlers = returnValueHandlers;
	}

	/**
	 * 返回自定义返回值处理器，或 {@code null}。
	 */
	@Nullable
	public List<HandlerMethodReturnValueHandler> getCustomReturnValueHandlers() {
		return this.customReturnValueHandlers;
	}

	/**
	 * 配置受支持返回值类型的完整列表，从而
	 * 覆盖原本会默认配置的处理器。
	 */
	public void setReturnValueHandlers(@Nullable List<HandlerMethodReturnValueHandler> returnValueHandlers) {
		if (returnValueHandlers == null) {
			this.returnValueHandlers = null;
		}
		else {
			this.returnValueHandlers = new HandlerMethodReturnValueHandlerComposite();
			this.returnValueHandlers.addHandlers(returnValueHandlers);
		}
	}

	/**
	 * 返回已配置的处理器；如果尚未
	 * 通过 {@link #afterPropertiesSet()} 初始化，则可能返回 {@code null}。
	 */
	@Nullable
	public HandlerMethodReturnValueHandlerComposite getReturnValueHandlers() {
		return this.returnValueHandlers;
	}

	/**
	 * 设置要使用的消息体转换器。
	 * <p>这些转换器用于在 HTTP 请求和响应之间进行转换。
	 */
	public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		this.messageConverters = messageConverters;
	}

	/**
	 * 返回已配置的消息体转换器。
	 */
	public List<HttpMessageConverter<?>> getMessageConverters() {
		return this.messageConverters;
	}

	/**
	 * 设置用于确定所请求媒体类型的 {@link ContentNegotiationManager}。
	 * 如果未设置，则使用默认构造函数创建的实例。
	 */
	public void setContentNegotiationManager(ContentNegotiationManager contentNegotiationManager) {
		this.contentNegotiationManager = contentNegotiationManager;
	}

	/**
	 * 返回已配置的 {@link ContentNegotiationManager}。
	 */
	public ContentNegotiationManager getContentNegotiationManager() {
		return this.contentNegotiationManager;
	}

	/**
	 * 添加一个或多个组件，在带有 {@code @ResponseBody} 注解或返回
	 * {@code ResponseEntity} 的控制器方法执行之后、但在使用所选
	 * {@code HttpMessageConverter} 将主体写入响应之前调用。
	 */
	public void setResponseBodyAdvice(@Nullable List<ResponseBodyAdvice<?>> responseBodyAdvice) {
		if (responseBodyAdvice != null) {
			this.responseBodyAdvice.addAll(responseBodyAdvice);
		}
	}

	@Override
	public void setApplicationContext(@Nullable ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Nullable
	public ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}


	@Override
	public void afterPropertiesSet() {
		// Do this first, it may add ResponseBodyAdvice beans
		initExceptionHandlerAdviceCache();

		if (this.argumentResolvers == null) {
			List<HandlerMethodArgumentResolver> resolvers = getDefaultArgumentResolvers();
			this.argumentResolvers = new HandlerMethodArgumentResolverComposite().addResolvers(resolvers);
		}
		if (this.returnValueHandlers == null) {
			List<HandlerMethodReturnValueHandler> handlers = getDefaultReturnValueHandlers();
			this.returnValueHandlers = new HandlerMethodReturnValueHandlerComposite().addHandlers(handlers);
		}
	}

	private void initExceptionHandlerAdviceCache() {
		if (getApplicationContext() == null) {
			return;
		}

		List<ControllerAdviceBean> adviceBeans = ControllerAdviceBean.findAnnotatedBeans(getApplicationContext());
		for (ControllerAdviceBean adviceBean : adviceBeans) {
			Class<?> beanType = adviceBean.getBeanType();
			if (beanType == null) {
				throw new IllegalStateException("Unresolvable type for ControllerAdviceBean: " + adviceBean);
			}
			ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(beanType);
			if (resolver.hasExceptionMappings()) {
				this.exceptionHandlerAdviceCache.put(adviceBean, resolver);
			}
			if (ResponseBodyAdvice.class.isAssignableFrom(beanType)) {
				this.responseBodyAdvice.add(adviceBean);
			}
		}

		if (logger.isDebugEnabled()) {
			int handlerSize = this.exceptionHandlerAdviceCache.size();
			int adviceSize = this.responseBodyAdvice.size();
			if (handlerSize == 0 && adviceSize == 0) {
				logger.debug("ControllerAdvice beans: none");
			}
			else {
				logger.debug("ControllerAdvice beans: " +
						handlerSize + " @ExceptionHandler, " + adviceSize + " ResponseBodyAdvice");
			}
		}
	}

	/**
	 * 返回一个不可修改的 Map，其中包含在 ApplicationContext 中发现的
	 * {@link ControllerAdvice @ControllerAdvice} bean。如果在 bean 通过
	 * {@link #afterPropertiesSet()} 初始化之前调用此方法，返回的 map 将为空。
	 */
	public Map<ControllerAdviceBean, ExceptionHandlerMethodResolver> getExceptionHandlerAdviceCache() {
		return Collections.unmodifiableMap(this.exceptionHandlerAdviceCache);
	}

	/**
	 * 返回要使用的参数解析器列表，包括内置解析器
	 * 以及通过 {@link #setCustomArgumentResolvers} 提供的自定义解析器。
	 */
	protected List<HandlerMethodArgumentResolver> getDefaultArgumentResolvers() {
		List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();

		// Annotation-based argument resolution
		resolvers.add(new SessionAttributeMethodArgumentResolver());
		resolvers.add(new RequestAttributeMethodArgumentResolver());

		// Type-based argument resolution
		resolvers.add(new ServletRequestMethodArgumentResolver());
		resolvers.add(new ServletResponseMethodArgumentResolver());
		resolvers.add(new RedirectAttributesMethodArgumentResolver());
		resolvers.add(new ModelMethodProcessor());

		// Custom arguments
		if (getCustomArgumentResolvers() != null) {
			resolvers.addAll(getCustomArgumentResolvers());
		}

		// Catch-all
		resolvers.add(new PrincipalMethodArgumentResolver());

		return resolvers;
	}

	/**
	 * 返回要使用的返回值处理器列表，包括内置处理器
	 * 以及通过 {@link #setReturnValueHandlers} 提供的自定义处理器。
	 */
	protected List<HandlerMethodReturnValueHandler> getDefaultReturnValueHandlers() {
		List<HandlerMethodReturnValueHandler> handlers = new ArrayList<>();

		// Single-purpose return value types
		handlers.add(new ModelAndViewMethodReturnValueHandler());
		handlers.add(new ModelMethodProcessor());
		handlers.add(new ViewMethodReturnValueHandler());
		handlers.add(new HttpEntityMethodProcessor(
				getMessageConverters(), this.contentNegotiationManager, this.responseBodyAdvice));

		// Annotation-based return value types
		handlers.add(new ServletModelAttributeMethodProcessor(false));
		handlers.add(new RequestResponseBodyMethodProcessor(
				getMessageConverters(), this.contentNegotiationManager, this.responseBodyAdvice));

		// Multi-purpose return value types
		handlers.add(new ViewNameMethodReturnValueHandler());
		handlers.add(new MapMethodProcessor());

		// Custom return value types
		if (getCustomReturnValueHandlers() != null) {
			handlers.addAll(getCustomReturnValueHandlers());
		}

		// Catch-all
		handlers.add(new ServletModelAttributeMethodProcessor(true));

		return handlers;
	}

	@Override
	protected boolean hasGlobalExceptionHandlers() {
		return !this.exceptionHandlerAdviceCache.isEmpty();
	}

	/**
	 * 查找 {@code @ExceptionHandler} 方法并调用它以处理所抛出的异常。
	 */
	@Override
	@Nullable
	protected ModelAndView doResolveHandlerMethodException(HttpServletRequest request,
			HttpServletResponse response, @Nullable HandlerMethod handlerMethod, Exception exception) {

		ServletInvocableHandlerMethod exceptionHandlerMethod = getExceptionHandlerMethod(handlerMethod, exception);
		if (exceptionHandlerMethod == null) {
			return null;
		}

		if (this.argumentResolvers != null) {
			exceptionHandlerMethod.setHandlerMethodArgumentResolvers(this.argumentResolvers);
		}
		if (this.returnValueHandlers != null) {
			exceptionHandlerMethod.setHandlerMethodReturnValueHandlers(this.returnValueHandlers);
		}

		ServletWebRequest webRequest = new ServletWebRequest(request, response);
		ModelAndViewContainer mavContainer = new ModelAndViewContainer();

		ArrayList<Throwable> exceptions = new ArrayList<>();
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Using @ExceptionHandler " + exceptionHandlerMethod);
			}
			// Expose causes as provided arguments as well
			Throwable exToExpose = exception;
			while (exToExpose != null) {
				exceptions.add(exToExpose);
				Throwable cause = exToExpose.getCause();
				exToExpose = (cause != exToExpose ? cause : null);
			}
			Object[] arguments = new Object[exceptions.size() + 1];
			exceptions.toArray(arguments);  // efficient arraycopy call in ArrayList
			arguments[arguments.length - 1] = handlerMethod;
			exceptionHandlerMethod.invokeAndHandle(webRequest, mavContainer, arguments);
		}
		catch (Throwable invocationEx) {
			// Any other than the original exception (or a cause) is unintended here,
			// probably an accident (e.g. failed assertion or the like).
			if (!exceptions.contains(invocationEx) && logger.isWarnEnabled()) {
				logger.warn("Failure in @ExceptionHandler " + exceptionHandlerMethod, invocationEx);
			}
			// Continue with default processing of the original exception...
			return null;
		}

		if (mavContainer.isRequestHandled()) {
			return new ModelAndView();
		}
		else {
			ModelMap model = mavContainer.getModel();
			HttpStatus status = mavContainer.getStatus();
			ModelAndView mav = new ModelAndView(mavContainer.getViewName(), model, status);
			mav.setViewName(mavContainer.getViewName());
			if (!mavContainer.isViewReference()) {
				mav.setView((View) mavContainer.getView());
			}
			if (model instanceof RedirectAttributes) {
				Map<String, ?> flashAttributes = ((RedirectAttributes) model).getFlashAttributes();
				RequestContextUtils.getOutputFlashMap(request).putAll(flashAttributes);
			}
			return mav;
		}
	}

	/**
	 * 为给定异常查找 {@code @ExceptionHandler} 方法。默认
	 * 实现首先在控制器的类层次结构中搜索方法，
	 * 如果未找到，则在假定检测到某些 {@linkplain ControllerAdvice @ControllerAdvice}
	 * Spring 管理的 bean 的情况下，继续搜索其他 {@code @ExceptionHandler}
	 * 方法。
	 * @param handlerMethod 引发异常的方法（可能为 {@code null}）
	 * @param exception 引发的异常
	 * @return 用于处理该异常的方法，若无则为 {@code null}
	 */
	@Nullable
	protected ServletInvocableHandlerMethod getExceptionHandlerMethod(
			@Nullable HandlerMethod handlerMethod, Exception exception) {

		Class<?> handlerType = null;

		if (handlerMethod != null) {
			// Local exception handler methods on the controller class itself.
			// To be invoked through the proxy, even in case of an interface-based proxy.
			handlerType = handlerMethod.getBeanType();
			ExceptionHandlerMethodResolver resolver = this.exceptionHandlerCache.get(handlerType);
			if (resolver == null) {
				resolver = new ExceptionHandlerMethodResolver(handlerType);
				this.exceptionHandlerCache.put(handlerType, resolver);
			}
			Method method = resolver.resolveMethod(exception);
			if (method != null) {
				return new ServletInvocableHandlerMethod(handlerMethod.getBean(), method, this.applicationContext);
			}
			// For advice applicability check below (involving base packages, assignable types
			// and annotation presence), use target class instead of interface-based proxy.
			if (Proxy.isProxyClass(handlerType)) {
				handlerType = AopUtils.getTargetClass(handlerMethod.getBean());
			}
		}

		for (Map.Entry<ControllerAdviceBean, ExceptionHandlerMethodResolver> entry : this.exceptionHandlerAdviceCache.entrySet()) {
			ControllerAdviceBean advice = entry.getKey();
			if (advice.isApplicableToBeanType(handlerType)) {
				ExceptionHandlerMethodResolver resolver = entry.getValue();
				Method method = resolver.resolveMethod(exception);
				if (method != null) {
					return new ServletInvocableHandlerMethod(advice.resolveBean(), method, this.applicationContext);
				}
			}
		}

		return null;
	}

}
