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

package org.springframework.messaging.handler;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * 封装处理器方法的信息，包括一个{@linkplain #getMethod() 方法}和一个
 * {@linkplain #getBean() Bean}。提供对方法参数、方法返回值、方法注解等的便捷访问。
 *
 * <p>此类可以使用 Bean 实例或 Bean 名称（例如延迟初始化 Bean、原型 Bean）创建。
 * 使用 {@link #createWithResolvedBean()} 获取通过关联的 {@link BeanFactory}
 * 解析出 Bean 实例的 {@code HandlerMethod} 实例。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 */
public class HandlerMethod {

	/** 公开以便使用回退日志记录器进行包装。 */
	public static final Log defaultLogger = LogFactory.getLog(HandlerMethod.class);


	private final Object bean;

	@Nullable
	private final BeanFactory beanFactory;

	private final Class<?> beanType;

	private final Method method;

	private final Method bridgedMethod;

	private final MethodParameter[] parameters;

	@Nullable
	private HandlerMethod resolvedFromHandlerMethod;

	protected Log logger = defaultLogger;


	/**
	 * 从 Bean 实例和方法创建实例。
	 */
	public HandlerMethod(Object bean, Method method) {
		Assert.notNull(bean, "Bean is required");
		Assert.notNull(method, "Method is required");
		this.bean = bean;
		this.beanFactory = null;
		this.beanType = ClassUtils.getUserClass(bean);
		this.method = method;
		this.bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
		ReflectionUtils.makeAccessible(this.bridgedMethod);
		this.parameters = initMethodParameters();
	}

	/**
	 * 从 Bean 实例、方法名称和参数类型创建实例。
	 * @throws NoSuchMethodException 当找不到该方法时
	 */
	public HandlerMethod(Object bean, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
		Assert.notNull(bean, "Bean is required");
		Assert.notNull(methodName, "Method name is required");
		this.bean = bean;
		this.beanFactory = null;
		this.beanType = ClassUtils.getUserClass(bean);
		this.method = bean.getClass().getMethod(methodName, parameterTypes);
		this.bridgedMethod = BridgeMethodResolver.findBridgedMethod(this.method);
		ReflectionUtils.makeAccessible(this.bridgedMethod);
		this.parameters = initMethodParameters();
	}

	/**
	 * 从 Bean 名称、方法和 {@code BeanFactory} 创建实例。
	 * 之后可以使用 {@link #createWithResolvedBean()} 方法
	 * 使用已初始化的 Bean 重新创建 {@code HandlerMethod}。
	 */
	public HandlerMethod(String beanName, BeanFactory beanFactory, Method method) {
		Assert.hasText(beanName, "Bean name is required");
		Assert.notNull(beanFactory, "BeanFactory is required");
		Assert.notNull(method, "Method is required");
		this.bean = beanName;
		this.beanFactory = beanFactory;
		Class<?> beanType = beanFactory.getType(beanName);
		if (beanType == null) {
			throw new IllegalStateException("Cannot resolve bean type for bean with name '" + beanName + "'");
		}
		this.beanType = ClassUtils.getUserClass(beanType);
		this.method = method;
		this.bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
		ReflectionUtils.makeAccessible(this.bridgedMethod);
		this.parameters = initMethodParameters();
	}

	/**
	 * 供子类使用的拷贝构造函数。
	 */
	protected HandlerMethod(HandlerMethod handlerMethod) {
		Assert.notNull(handlerMethod, "HandlerMethod is required");
		this.bean = handlerMethod.bean;
		this.beanFactory = handlerMethod.beanFactory;
		this.beanType = handlerMethod.beanType;
		this.method = handlerMethod.method;
		this.bridgedMethod = handlerMethod.bridgedMethod;
		this.parameters = handlerMethod.parameters;
		this.resolvedFromHandlerMethod = handlerMethod.resolvedFromHandlerMethod;
	}

	/**
	 * 使用已解析的处理器重新创建 HandlerMethod。
	 */
	private HandlerMethod(HandlerMethod handlerMethod, Object handler) {
		Assert.notNull(handlerMethod, "HandlerMethod is required");
		Assert.notNull(handler, "Handler object is required");
		this.bean = handler;
		this.beanFactory = handlerMethod.beanFactory;
		this.beanType = handlerMethod.beanType;
		this.method = handlerMethod.method;
		this.bridgedMethod = handlerMethod.bridgedMethod;
		this.parameters = handlerMethod.parameters;
		this.resolvedFromHandlerMethod = handlerMethod;
	}


	private MethodParameter[] initMethodParameters() {
		int count = this.bridgedMethod.getParameterCount();
		MethodParameter[] result = new MethodParameter[count];
		for (int i = 0; i < count; i++) {
			result[i] = new HandlerMethodParameter(i);
		}
		return result;
	}


	/**
	 * 设置替代日志记录器，以代替基于类名的日志记录器。
	 * @param logger 要使用的日志记录器
	 * @since 5.1
	 */
	public void setLogger(Log logger) {
		this.logger = logger;
	}

	/**
	 * 返回当前配置的 Logger。
	 * @since 5.1
	 */
	public Log getLogger() {
		return logger;
	}

	/**
	 * 返回此处理器方法的 Bean。
	 */
	public Object getBean() {
		return this.bean;
	}

	/**
	 * 返回此处理器方法的方法。
	 */
	public Method getMethod() {
		return this.method;
	}

	/**
	 * 返回此处理器方法的处理器类型。
	 * <p>注意，如果 Bean 类型是由 CGLIB 生成的类，则返回原始的用户定义类。
	 */
	public Class<?> getBeanType() {
		return this.beanType;
	}

	/**
	 * 如果 Bean 方法是桥接方法，则返回被桥接的（用户定义的）方法。
	 * 否则返回与 {@link #getMethod()} 相同的方法。
	 */
	protected Method getBridgedMethod() {
		return this.bridgedMethod;
	}

	/**
	 * 返回此处理器方法的方法参数。
	 */
	public MethodParameter[] getMethodParameters() {
		return this.parameters;
	}

	/**
	 * 返回 HandlerMethod 的返回类型。
	 */
	public MethodParameter getReturnType() {
		return new HandlerMethodParameter(-1);
	}

	/**
	 * 返回实际的返回值类型。
	 */
	public MethodParameter getReturnValueType(@Nullable Object returnValue) {
		return new ReturnValueMethodParameter(returnValue);
	}

	/**
	 * 如果方法返回类型为 void，则返回 {@code true}，否则返回 {@code false}。
	 */
	public boolean isVoid() {
		return Void.TYPE.equals(getReturnType().getParameterType());
	}

	/**
	 * 返回底层方法上的单个注解；如果给定方法本身上找不到注解，
	 * 则遍历其父方法查找。
	 * <p>从 Spring Framework 4.3 开始，还支持带属性覆盖的<em>合并</em>组合注解。
	 * @param annotationType 要在方法上内省查找的注解类型
	 * @return 该注解；如果未找到则返回 {@code null}
	 * @see AnnotatedElementUtils#findMergedAnnotation
	 */
	@Nullable
	public <A extends Annotation> A getMethodAnnotation(Class<A> annotationType) {
		return AnnotatedElementUtils.findMergedAnnotation(this.method, annotationType);
	}

	/**
	 * 返回参数是否使用给定注解类型声明。
	 * @param annotationType 要查找的注解类型
	 * @since 4.3
	 * @see AnnotatedElementUtils#hasAnnotation
	 */
	public <A extends Annotation> boolean hasMethodAnnotation(Class<A> annotationType) {
		return AnnotatedElementUtils.hasAnnotation(this.method, annotationType);
	}

	/**
	 * 返回通过 {@link #createWithResolvedBean()} 解析出此 HandlerMethod 实例的
	 * 原始 HandlerMethod。
	 * @since 4.3
	 */
	@Nullable
	public HandlerMethod getResolvedFromHandlerMethod() {
		return this.resolvedFromHandlerMethod;
	}

	/**
	 * 如果提供的实例包含的是 Bean 名称而不是对象实例，
	 * 则在创建并返回 {@link HandlerMethod} 之前会先解析该 Bean 名称。
	 */
	public HandlerMethod createWithResolvedBean() {
		Object handler = this.bean;
		if (this.bean instanceof String) {
			Assert.state(this.beanFactory != null, "Cannot resolve bean name without BeanFactory");
			String beanName = (String) this.bean;
			handler = this.beanFactory.getBean(beanName);
		}
		return new HandlerMethod(this, handler);
	}

	/**
	 * 返回此处理器方法的简短表示，用于日志消息。
	 */
	public String getShortLogMessage() {
		int args = this.method.getParameterCount();
		return getBeanType().getSimpleName() + "#" + this.method.getName() + "[" + args + " args]";
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof HandlerMethod)) {
			return false;
		}
		HandlerMethod otherMethod = (HandlerMethod) other;
		return (this.bean.equals(otherMethod.bean) && this.method.equals(otherMethod.method));
	}

	@Override
	public int hashCode() {
		return (this.bean.hashCode() * 31 + this.method.hashCode());
	}

	@Override
	public String toString() {
		return this.method.toGenericString();
	}


	// Support methods for use in "InvocableHandlerMethod" sub-class variants..

	@Nullable
	protected static Object findProvidedArgument(MethodParameter parameter, @Nullable Object... providedArgs) {
		if (!ObjectUtils.isEmpty(providedArgs)) {
			for (Object providedArg : providedArgs) {
				if (parameter.getParameterType().isInstance(providedArg)) {
					return providedArg;
				}
			}
		}
		return null;
	}

	protected static String formatArgumentError(MethodParameter param, String message) {
		return "Could not resolve parameter [" + param.getParameterIndex() + "] in " +
				param.getExecutable().toGenericString() + (StringUtils.hasText(message) ? ": " + message : "");
	}

	/**
	 * 断言目标 Bean 类是声明给定方法的类的实例。在某些情况下，
	 * 请求处理时的实际端点实例可能是 JDK 动态代理（延迟初始化、原型 Bean 等）。
	 * 需要代理的端点类应优先使用基于类的代理机制。
	 */
	protected void assertTargetBean(Method method, Object targetBean, Object[] args) {
		Class<?> methodDeclaringClass = method.getDeclaringClass();
		Class<?> targetBeanClass = targetBean.getClass();
		if (!methodDeclaringClass.isAssignableFrom(targetBeanClass)) {
			String text = "The mapped handler method class '" + methodDeclaringClass.getName() +
					"' is not an instance of the actual endpoint bean class '" +
					targetBeanClass.getName() + "'. If the endpoint requires proxying " +
					"(e.g. due to @Transactional), please use class-based proxying.";
			throw new IllegalStateException(formatInvokeError(text, args));
		}
	}

	protected String formatInvokeError(String text, Object[] args) {

		String formattedArgs = IntStream.range(0, args.length)
				.mapToObj(i -> (args[i] != null ?
						"[" + i + "] [type=" + args[i].getClass().getName() + "] [value=" + args[i] + "]" :
						"[" + i + "] [null]"))
				.collect(Collectors.joining(",\n", " ", " "));

		return text + "\n" +
				"Endpoint [" + getBeanType().getName() + "]\n" +
				"Method [" + getBridgedMethod().toGenericString() + "] " +
				"with argument values:\n" + formattedArgs;
	}


	/**
	 * 具有 HandlerMethod 特定行为的 MethodParameter。
	 */
	protected class HandlerMethodParameter extends SynthesizingMethodParameter {

		public HandlerMethodParameter(int index) {
			super(HandlerMethod.this.bridgedMethod, index);
		}

		protected HandlerMethodParameter(HandlerMethodParameter original) {
			super(original);
		}

		@Override
		public Class<?> getContainingClass() {
			return HandlerMethod.this.getBeanType();
		}

		@Override
		public <T extends Annotation> T getMethodAnnotation(Class<T> annotationType) {
			return HandlerMethod.this.getMethodAnnotation(annotationType);
		}

		@Override
		public <T extends Annotation> boolean hasMethodAnnotation(Class<T> annotationType) {
			return HandlerMethod.this.hasMethodAnnotation(annotationType);
		}

		@Override
		public HandlerMethodParameter clone() {
			return new HandlerMethodParameter(this);
		}
	}


	/**
	 * 基于实际返回值类型的 HandlerMethod 返回值的 MethodParameter。
	 */
	private class ReturnValueMethodParameter extends HandlerMethodParameter {

		@Nullable
		private final Class<?> returnValueType;

		public ReturnValueMethodParameter(@Nullable Object returnValue) {
			super(-1);
			this.returnValueType = (returnValue != null ? returnValue.getClass() : null);
		}

		protected ReturnValueMethodParameter(ReturnValueMethodParameter original) {
			super(original);
			this.returnValueType = original.returnValueType;
		}

		@Override
		public Class<?> getParameterType() {
			return (this.returnValueType != null ? this.returnValueType : super.getParameterType());
		}

		@Override
		public ReturnValueMethodParameter clone() {
			return new ReturnValueMethodParameter(this);
		}
	}

}
