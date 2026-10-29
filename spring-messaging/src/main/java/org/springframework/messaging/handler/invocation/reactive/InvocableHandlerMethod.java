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

package org.springframework.messaging.handler.invocation.reactive;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import reactor.core.publisher.Mono;

import org.springframework.core.CoroutinesUtils;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.HandlerMethod;
import org.springframework.messaging.handler.invocation.MethodArgumentResolutionException;
import org.springframework.util.ObjectUtils;

/**
 * {@link HandlerMethod} 的扩展，它使用通过一组
 * {@link HandlerMethodArgumentResolver} 从当前 HTTP 请求解析出的参数值
 * 来调用底层方法。
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class InvocableHandlerMethod extends HandlerMethod {

	private static final Mono<Object[]> EMPTY_ARGS = Mono.just(new Object[0]);

	private static final Object NO_ARG_VALUE = new Object();


	private HandlerMethodArgumentResolverComposite resolvers = new HandlerMethodArgumentResolverComposite();

	private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

	private ReactiveAdapterRegistry reactiveAdapterRegistry = ReactiveAdapterRegistry.getSharedInstance();


	/**
	 * 从 {@code HandlerMethod} 创建实例。
	 */
	public InvocableHandlerMethod(HandlerMethod handlerMethod) {
		super(handlerMethod);
	}

	/**
	 * 从 Bean 实例和方法创建实例。
	 */
	public InvocableHandlerMethod(Object bean, Method method) {
		super(bean, method);
	}


	/**
	 * 配置用于针对 {@code ServerWebExchange} 解析方法参数值的参数解析器。
	 */
	public void setArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		this.resolvers.addResolvers(resolvers);
	}

	/**
	 * 返回已配置的参数解析器。
	 */
	public List<HandlerMethodArgumentResolver> getResolvers() {
		return this.resolvers.getResolvers();
	}

	/**
	 * 设置 ParameterNameDiscoverer，以便在需要时解析参数名称
	 * （例如默认请求属性名称）。
	 * <p>默认为 {@link DefaultParameterNameDiscoverer}。
	 */
	public void setParameterNameDiscoverer(ParameterNameDiscoverer nameDiscoverer) {
		this.parameterNameDiscoverer = nameDiscoverer;
	}

	/**
	 * 返回已配置的参数名称发现器。
	 */
	public ParameterNameDiscoverer getParameterNameDiscoverer() {
		return this.parameterNameDiscoverer;
	}

	/**
	 * 配置响应式适配器注册表。异步返回值需要它。
	 * <p>默认情况下，这是使用默认设置的 {@link ReactiveAdapterRegistry}。
	 */
	public void setReactiveAdapterRegistry(ReactiveAdapterRegistry registry) {
		this.reactiveAdapterRegistry = registry;
	}


	/**
	 * 为给定交换调用该方法。
	 * @param message the current message
	 * @param providedArgs optional list of argument values to match by type
	 * @return a Mono with the result from the invocation
	 */
	@SuppressWarnings("KotlinInternalInJava")
	public Mono<Object> invoke(Message<?> message, Object... providedArgs) {
		return getMethodArgumentValues(message, providedArgs).flatMap(args -> {
			Object value;
			boolean isSuspendingFunction = false;
			try {
				Method method = getBridgedMethod();
				if (KotlinDetector.isSuspendingFunction(method)) {
					isSuspendingFunction = true;
					value = CoroutinesUtils.invokeSuspendingFunction(method, getBean(), args);
				}
				else {
					value = method.invoke(getBean(), args);
				}
			}
			catch (IllegalArgumentException ex) {
				assertTargetBean(getBridgedMethod(), getBean(), args);
				String text = (ex.getMessage() != null ? ex.getMessage() : "Illegal argument");
				return Mono.error(new IllegalStateException(formatInvokeError(text, args), ex));
			}
			catch (InvocationTargetException ex) {
				return Mono.error(ex.getTargetException());
			}
			catch (Throwable ex) {
				// Unlikely to ever get here, but it must be handled...
				return Mono.error(new IllegalStateException(formatInvokeError("Invocation failure", args), ex));
			}

			MethodParameter returnType = getReturnType();
			Class<?> reactiveType = (isSuspendingFunction ? value.getClass() : returnType.getParameterType());
			ReactiveAdapter adapter = this.reactiveAdapterRegistry.getAdapter(reactiveType);
			return (isAsyncVoidReturnType(returnType, adapter) ?
					Mono.from(adapter.toPublisher(value)) : Mono.justOrEmpty(value));
		});
	}

	private Mono<Object[]> getMethodArgumentValues(Message<?> message, Object... providedArgs) {
		MethodParameter[] parameters = getMethodParameters();
		if (ObjectUtils.isEmpty(getMethodParameters())) {
			return EMPTY_ARGS;
		}

		List<Mono<Object>> argMonos = new ArrayList<>(parameters.length);
		for (MethodParameter parameter : parameters) {
			parameter.initParameterNameDiscovery(this.parameterNameDiscoverer);
			Object providedArg = findProvidedArgument(parameter, providedArgs);
			if (providedArg != null) {
				argMonos.add(Mono.just(providedArg));
				continue;
			}
			if (!this.resolvers.supportsParameter(parameter)) {
				return Mono.error(new MethodArgumentResolutionException(
						message, parameter, formatArgumentError(parameter, "No suitable resolver")));
			}
			try {
				argMonos.add(this.resolvers.resolveArgument(parameter, message)
						.defaultIfEmpty(NO_ARG_VALUE)
						.doOnError(ex -> logArgumentErrorIfNecessary(parameter, ex)));
			}
			catch (Exception ex) {
				logArgumentErrorIfNecessary(parameter, ex);
				argMonos.add(Mono.error(ex));
			}
		}
		return Mono.zip(argMonos, values ->
				Stream.of(values).map(value -> value != NO_ARG_VALUE ? value : null).toArray());
	}

	private void logArgumentErrorIfNecessary(MethodParameter parameter, Throwable ex) {
		// Leave stack trace for later, if error is not handled...
		String exMsg = ex.getMessage();
		if (exMsg != null && !exMsg.contains(parameter.getExecutable().toGenericString())) {
			if (logger.isDebugEnabled()) {
				logger.debug(formatArgumentError(parameter, exMsg));
			}
		}
	}

	private boolean isAsyncVoidReturnType(MethodParameter returnType, @Nullable ReactiveAdapter reactiveAdapter) {
		if (reactiveAdapter != null && reactiveAdapter.supportsEmpty()) {
			if (reactiveAdapter.isNoValue()) {
				return true;
			}
		}
		Type parameterType = returnType.getGenericParameterType();
		if (parameterType instanceof ParameterizedType) {
			ParameterizedType type = (ParameterizedType) parameterType;
			if (type.getActualTypeArguments().length == 1) {
				return Void.class.equals(type.getActualTypeArguments()[0]);
			}
		}
		Method method = returnType.getMethod();
		return method != null && KotlinDetector.isSuspendingFunction(method) &&
				Void.TYPE.equals(returnType.getParameterType());
	}

}
