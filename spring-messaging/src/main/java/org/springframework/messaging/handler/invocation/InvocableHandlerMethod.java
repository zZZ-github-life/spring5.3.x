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

package org.springframework.messaging.handler.invocation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.HandlerMethod;
import org.springframework.util.ObjectUtils;

/**
 * {@link HandlerMethod} 的扩展，它使用通过一组
 * {@link HandlerMethodArgumentResolver} 从当前 HTTP 请求解析出的参数值
 * 来调用底层方法。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 */
public class InvocableHandlerMethod extends HandlerMethod {

	private static final Object[] EMPTY_ARGS = new Object[0];


	private HandlerMethodArgumentResolverComposite resolvers = new HandlerMethodArgumentResolverComposite();

	private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();


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
	 * 使用给定 Bean 实例、方法名称和参数构造新的处理器方法。
	 * @param bean the object bean
	 * @param methodName the method name
	 * @param parameterTypes the method parameter types
	 * @throws NoSuchMethodException when the method cannot be found
	 */
	public InvocableHandlerMethod(Object bean, String methodName, Class<?>... parameterTypes)
			throws NoSuchMethodException {

		super(bean, methodName, parameterTypes);
	}


	/**
	 * 设置用于解析方法参数值的
	 * {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers}。
	 */
	public void setMessageMethodArgumentResolvers(HandlerMethodArgumentResolverComposite argumentResolvers) {
		this.resolvers = argumentResolvers;
	}

	/**
	 * 设置 ParameterNameDiscoverer，以便在需要时解析参数名称
	 * （例如默认请求属性名称）。
	 * <p>默认为 {@link org.springframework.core.DefaultParameterNameDiscoverer}。
	 */
	public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
		this.parameterNameDiscoverer = parameterNameDiscoverer;
	}


	/**
	 * 在给定消息的上下文中解析方法参数值后调用该方法。
	 * <p>参数值通常通过
	 * {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers} 解析。
	 * 不过 {@code providedArgs} 参数可以直接提供要使用的参数值，
	 * 即无需参数解析。
	 * <p>委托给 {@link #getMethodArgumentValues}，并使用解析出的参数调用
	 * {@link #doInvoke}。
	 * @param message the current message being processed
	 * @param providedArgs "given" arguments matched by type, not resolved
	 * @return the raw value returned by the invoked method
	 * @throws Exception raised if no suitable argument resolver can be found,
	 * or if the method raised an exception
	 * @see #getMethodArgumentValues
	 * @see #doInvoke
	 */
	@Nullable
	public Object invoke(Message<?> message, Object... providedArgs) throws Exception {
		Object[] args = getMethodArgumentValues(message, providedArgs);
		if (logger.isTraceEnabled()) {
			logger.trace("Arguments: " + Arrays.toString(args));
		}
		return doInvoke(args);
	}

	/**
	 * 获取当前消息的方法参数值，检查提供的参数值，
	 * 并回退到已配置的参数解析器。
	 * <p>结果数组将传入 {@link #doInvoke}。
	 * @since 5.1.2
	 */
	protected Object[] getMethodArgumentValues(Message<?> message, Object... providedArgs) throws Exception {
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
				throw new MethodArgumentResolutionException(
						message, parameter, formatArgumentError(parameter, "No suitable resolver"));
			}
			try {
				args[i] = this.resolvers.resolveArgument(parameter, message);
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
	 * 使用给定参数值调用处理器方法。
	 */
	@Nullable
	protected Object doInvoke(Object... args) throws Exception {
		try {
			return getBridgedMethod().invoke(getBean(), args);
		}
		catch (IllegalArgumentException ex) {
			assertTargetBean(getBridgedMethod(), getBean(), args);
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

	MethodParameter getAsyncReturnValueType(@Nullable Object returnValue) {
		return new AsyncResultMethodParameter(returnValue);
	}


	private class AsyncResultMethodParameter extends HandlerMethodParameter {

		@Nullable
		private final Object returnValue;

		private final ResolvableType returnType;

		public AsyncResultMethodParameter(@Nullable Object returnValue) {
			super(-1);
			this.returnValue = returnValue;
			this.returnType = ResolvableType.forType(super.getGenericParameterType()).getGeneric();
		}

		protected AsyncResultMethodParameter(AsyncResultMethodParameter original) {
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
		public AsyncResultMethodParameter clone() {
			return new AsyncResultMethodParameter(this);
		}
	}

}
