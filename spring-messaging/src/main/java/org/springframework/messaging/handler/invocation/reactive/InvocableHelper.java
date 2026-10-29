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

package org.springframework.messaging.handler.invocation.reactive;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.HandlerMethod;
import org.springframework.messaging.handler.MessagingAdviceBean;
import org.springframework.messaging.handler.invocation.AbstractExceptionHandlerMethodResolver;
import org.springframework.util.Assert;

/**
	 * 帮助初始化和调用 {@link InvocableHandlerMethod}，然后应用返回值处理和异常处理。
	 * 持有执行这些操作所需的所有必要配置。
	 *
	 * @author Rossen Stoyanchev
	 * @since 5.2
	 */
class InvocableHelper {

	private static final Log logger = LogFactory.getLog(InvocableHelper.class);


	private final HandlerMethodArgumentResolverComposite argumentResolvers =
			new HandlerMethodArgumentResolverComposite();

	private final HandlerMethodReturnValueHandlerComposite returnValueHandlers =
			new HandlerMethodReturnValueHandlerComposite();

	private ReactiveAdapterRegistry reactiveAdapterRegistry = ReactiveAdapterRegistry.getSharedInstance();

	private final Function<Class<?>, AbstractExceptionHandlerMethodResolver> exceptionMethodResolverFactory;

	private final Map<Class<?>, AbstractExceptionHandlerMethodResolver> exceptionHandlerCache =
			new ConcurrentHashMap<>(64);

	private final Map<MessagingAdviceBean, AbstractExceptionHandlerMethodResolver> exceptionHandlerAdviceCache =
			new LinkedHashMap<>(64);


	public InvocableHelper(
			Function<Class<?>, AbstractExceptionHandlerMethodResolver> exceptionMethodResolverFactory) {

		this.exceptionMethodResolverFactory = exceptionMethodResolverFactory;
	}

	/**
	 * 添加用于消息处理和异常处理方法的参数解析器。
	 */
	public void addArgumentResolvers(List<? extends HandlerMethodArgumentResolver> resolvers) {
		this.argumentResolvers.addResolvers(resolvers);
	}

	/**
	 * 返回已配置的解析器。
	 * @since 5.2.2
	 */
	public HandlerMethodArgumentResolverComposite getArgumentResolvers() {
		return this.argumentResolvers;
	}

	/**
	 * 添加用于消息处理和异常处理方法的返回值处理器。
	 */
	public void addReturnValueHandlers(List<? extends HandlerMethodReturnValueHandler> handlers) {
		this.returnValueHandlers.addHandlers(handlers);
	}

	/**
	 * 配置用于适配各种响应式类型的注册表。
	 * <p>默认情况下，这是使用默认设置的 {@link ReactiveAdapterRegistry} 实例。
	 */
	public void setReactiveAdapterRegistry(ReactiveAdapterRegistry registry) {
		Assert.notNull(registry, "ReactiveAdapterRegistry is required");
		this.reactiveAdapterRegistry = registry;
	}

	/**
	 * 返回已配置的用于适配响应式类型的注册表。
	 */
	public ReactiveAdapterRegistry getReactiveAdapterRegistry() {
		return this.reactiveAdapterRegistry;
	}

	/**
	 * 用于填充 MessagingAdviceBean 缓存的方法（例如以支持“全局”
	 * {@code @MessageExceptionHandler}）。
	 */
	public void registerExceptionHandlerAdvice(
			MessagingAdviceBean bean, AbstractExceptionHandlerMethodResolver resolver) {

		this.exceptionHandlerAdviceCache.put(bean, resolver);
	}


	/**
	 * 使用已配置的参数解析器创建 {@link InvocableHandlerMethod}。
	 * @param handlerMethod the target handler method to invoke
	 * @return the created instance
	 */

	public InvocableHandlerMethod initMessageMappingMethod(HandlerMethod handlerMethod) {
		InvocableHandlerMethod invocable = new InvocableHandlerMethod(handlerMethod);
		invocable.setArgumentResolvers(this.argumentResolvers.getResolvers());
		return invocable;
	}

	/**
	 * 为给定异常查找异常处理方法。
	 * <p>默认实现首先在 HandlerMethod 的类层次结构中搜索方法；如果未找到，
	 * 则继续搜索通过 {@link #registerExceptionHandlerAdvice} 注册的其他处理方法。
	 * @param handlerMethod the method where the exception was raised
	 * @param ex the exception raised or signaled
	 * @return a method to handle the exception, or {@code null}
	 */
	@Nullable
	public InvocableHandlerMethod initExceptionHandlerMethod(HandlerMethod handlerMethod, Throwable ex) {
		if (logger.isDebugEnabled()) {
			logger.debug("Searching for methods to handle " + ex.getClass().getSimpleName());
		}
		Class<?> beanType = handlerMethod.getBeanType();
		AbstractExceptionHandlerMethodResolver resolver = this.exceptionHandlerCache.get(beanType);
		if (resolver == null) {
			resolver = this.exceptionMethodResolverFactory.apply(beanType);
			this.exceptionHandlerCache.put(beanType, resolver);
		}
		InvocableHandlerMethod exceptionHandlerMethod = null;
		Method method = resolver.resolveMethod(ex);
		if (method != null) {
			exceptionHandlerMethod = new InvocableHandlerMethod(handlerMethod.getBean(), method);
		}
		else {
			for (Map.Entry<MessagingAdviceBean, AbstractExceptionHandlerMethodResolver> entry : this.exceptionHandlerAdviceCache.entrySet()) {
				MessagingAdviceBean advice = entry.getKey();
				if (advice.isApplicableToBeanType(beanType)) {
					resolver = entry.getValue();
					method = resolver.resolveMethod(ex);
					if (method != null) {
						exceptionHandlerMethod = new InvocableHandlerMethod(advice.resolveBean(), method);
						break;
					}
				}
			}
		}
		if (exceptionHandlerMethod != null) {
			logger.debug("Found exception handler " + exceptionHandlerMethod.getShortLogMessage());
			exceptionHandlerMethod.setArgumentResolvers(this.argumentResolvers.getResolvers());
		}
		else {
			logger.error("No exception handling method", ex);
		}
		return exceptionHandlerMethod;
	}


	public Mono<Void> handleMessage(HandlerMethod handlerMethod, Message<?> message) {
		InvocableHandlerMethod invocable = initMessageMappingMethod(handlerMethod);
		if (logger.isDebugEnabled()) {
			logger.debug("Invoking " + invocable.getShortLogMessage());
		}
		return invocable.invoke(message)
				.switchIfEmpty(Mono.defer(() -> handleReturnValue(null, invocable, message)))
				.flatMap(returnValue -> handleReturnValue(returnValue, invocable, message))
				.onErrorResume(ex -> {
					InvocableHandlerMethod exHandler = initExceptionHandlerMethod(handlerMethod, ex);
					if (exHandler == null) {
						return Mono.error(ex);
					}
					if (logger.isDebugEnabled()) {
						logger.debug("Invoking " + exHandler.getShortLogMessage());
					}
					return exHandler.invoke(message, ex)
							.switchIfEmpty(Mono.defer(() -> handleReturnValue(null, exHandler, message)))
							.flatMap(returnValue -> handleReturnValue(returnValue, exHandler, message));
				});
	}

	private Mono<Void> handleReturnValue(
			@Nullable Object returnValue, HandlerMethod handlerMethod, Message<?> message) {

		MethodParameter returnType = handlerMethod.getReturnType();
		return this.returnValueHandlers.handleReturnValue(returnValue, returnType, message);
	}

}
