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

package org.springframework.web.method.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.springframework.context.MessageSource;
import org.springframework.core.CoroutinesUtils;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;

/**
 * {@link HandlerMethod} 的扩展，通过一组
 * {@link HandlerMethodArgumentResolver} 从当前 HTTP 请求中解析参数值，
 * 并以此来调用底层方法。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 3.1
 */
public class InvocableHandlerMethod extends HandlerMethod {

	private static final Object[] EMPTY_ARGS = new Object[0];


	private HandlerMethodArgumentResolverComposite resolvers = new HandlerMethodArgumentResolverComposite();

	private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

	@Nullable
	private WebDataBinderFactory dataBinderFactory;


	/**
	 * 从 {@code HandlerMethod} 创建实例。
	 */
	public InvocableHandlerMethod(HandlerMethod handlerMethod) {
		super(handlerMethod);
	}

	/**
	 * 根据 Bean 实例和方法创建实例。
	 */
	public InvocableHandlerMethod(Object bean, Method method) {
		super(bean, method);
	}

	/**
	 * 是 {@link #InvocableHandlerMethod(Object, Method)} 的变体，
	 * 还接受一个 {@link MessageSource}，供子类使用。
	 * @since 5.3.10
	 */
	protected InvocableHandlerMethod(Object bean, Method method, @Nullable MessageSource messageSource) {
		super(bean, method, messageSource);
	}

	/**
	 * 使用给定的 Bean 实例、方法名和参数类型构造新的处理器方法。
	 * @param bean 对象 Bean
	 * @param methodName 方法名称
	 * @param parameterTypes 方法参数类型
	 * @throws NoSuchMethodException 当找不到该方法时
	 */
	public InvocableHandlerMethod(Object bean, String methodName, Class<?>... parameterTypes)
			throws NoSuchMethodException {

		super(bean, methodName, parameterTypes);
	}


	/**
	 * 设置用于解析方法参数值的
	 * {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers}。
	 */
	public void setHandlerMethodArgumentResolvers(HandlerMethodArgumentResolverComposite argumentResolvers) {
		this.resolvers = argumentResolvers;
	}

	/**
	 * 设置 ParameterNameDiscoverer，以便在需要时解析参数名称
	 * （例如默认的请求属性名称）。
	 * <p>默认为 {@link org.springframework.core.DefaultParameterNameDiscoverer}。
	 */
	public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
		this.parameterNameDiscoverer = parameterNameDiscoverer;
	}

	/**
	 * 设置要传递给参数解析器的 {@link WebDataBinderFactory}，
	 * 以便它们为数据绑定和类型转换目的创建 {@link WebDataBinder}。
	 */
	public void setDataBinderFactory(WebDataBinderFactory dataBinderFactory) {
		this.dataBinderFactory = dataBinderFactory;
	}


	/**
	 * 在给定请求的上下文中解析参数值后调用该方法。
	 * <p>参数值通常通过
	 * {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers}
	 * 解析得到。不过，{@code providedArgs} 参数可以提供直接使用的参数值，
	 * 即无需参数解析。提供的参数值示例包括
	 * {@link WebDataBinder}、{@link SessionStatus} 或抛出的异常实例。
	 * 提供的参数值会在参数解析器之前检查。
	 * <p>委托给 {@link #getMethodArgumentValues}，并使用解析出的参数
	 * 调用 {@link #doInvoke}。
	 * @param request 当前请求
	 * @param mavContainer 此请求的 ModelAndViewContainer
	 * @param providedArgs 按类型匹配的“给定”参数，不进行解析
	 * @return 被调用方法返回的原始值
	 * @throws Exception 如果找不到合适的参数解析器，
	 * 或方法抛出异常时引发
	 * @see #getMethodArgumentValues
	 * @see #doInvoke
	 */
	@Nullable
	public Object invokeForRequest(NativeWebRequest request, @Nullable ModelAndViewContainer mavContainer,
			Object... providedArgs) throws Exception {

		Object[] args = getMethodArgumentValues(request, mavContainer, providedArgs);
		if (logger.isTraceEnabled()) {
			logger.trace("Arguments: " + Arrays.toString(args));
		}
		return doInvoke(args);
	}

	/**
	 * 获取当前请求的方法参数值，检查提供的参数值，
	 * 并回退到已配置的参数解析器。
	 * <p>结果数组将传入 {@link #doInvoke}。
	 * @since 5.1.2
	 */
	protected Object[] getMethodArgumentValues(NativeWebRequest request, @Nullable ModelAndViewContainer mavContainer,
			Object... providedArgs) throws Exception {

		MethodParameter[] parameters = getMethodParameters();
		if (ObjectUtils.isEmpty(parameters)) {
			return EMPTY_ARGS;
		}

		Object[] args = new Object[parameters.length];
		for (int i = 0; i < parameters.length; i++) {
			MethodParameter parameter = parameters[i];
			parameter.initParameterNameDiscovery(this.parameterNameDiscoverer);
			args[i] = findProvidedArgument(parameter, providedArgs);
			if (args[i] != null) {
				continue;
			}
			if (!this.resolvers.supportsParameter(parameter)) {
				throw new IllegalStateException(formatArgumentError(parameter, "No suitable resolver"));
			}
			try {
				args[i] = this.resolvers.resolveArgument(parameter, mavContainer, request, this.dataBinderFactory);
			}
			catch (Exception ex) {
				// Leave stack trace for later, exception may actually be resolved and handled...
				if (logger.isDebugEnabled()) {
					String exMsg = ex.getMessage();
					if (exMsg != null && !exMsg.contains(parameter.getExecutable().toGenericString())) {
						logger.debug(formatArgumentError(parameter, exMsg));
					}
				}
				throw ex;
			}
		}
		return args;
	}

	/**
	 * 使用给定的参数值调用处理器方法。
	 */
	@Nullable
	protected Object doInvoke(Object... args) throws Exception {
		Method method = getBridgedMethod();
		try {
			if (KotlinDetector.isSuspendingFunction(method)) {
				return CoroutinesUtils.invokeSuspendingFunction(method, getBean(), args);
			}
			return method.invoke(getBean(), args);
		}
		catch (IllegalArgumentException ex) {
			assertTargetBean(method, getBean(), args);
			String text = (ex.getMessage() != null ? ex.getMessage() : "Illegal argument");
			throw new IllegalStateException(formatInvokeError(text, args), ex);
		}
		catch (InvocationTargetException ex) {
			// Unwrap for HandlerExceptionResolvers ...
			Throwable targetException = ex.getTargetException();
			if (targetException instanceof RuntimeException) {
				throw (RuntimeException) targetException;
			}
			else if (targetException instanceof Error) {
				throw (Error) targetException;
			}
			else if (targetException instanceof Exception) {
				throw (Exception) targetException;
			}
			else {
				throw new IllegalStateException(formatInvokeError("Invocation failure", args), targetException);
			}
		}
	}

}
