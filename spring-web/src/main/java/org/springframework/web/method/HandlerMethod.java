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

package org.springframework.web.method;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 封装处理器方法的相关信息，该处理器方法由一个
 * {@linkplain #getMethod() 方法} 和一个 {@linkplain #getBean() Bean} 组成。
 * 提供对方法参数、方法返回值、方法注解等的便捷访问。
 *
 * <p>该类既可以使用 Bean 实例创建，也可以使用 Bean 名称创建
 * （例如延迟初始化的 Bean、原型 Bean）。使用 {@link #createWithResolvedBean()}
 * 可以获得一个 {@code HandlerMethod} 实例，其中的 Bean 实例已通过关联的
 * {@link BeanFactory} 解析完成。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.1
 */
public class HandlerMethod {

	/** 可供子类使用的日志记录器。 */
	protected static final Log logger = LogFactory.getLog(HandlerMethod.class);

	private final Object bean;

	@Nullable
	private final BeanFactory beanFactory;

	@Nullable
	private final MessageSource messageSource;

	private final Class<?> beanType;

	private final Method method;

	private final Method bridgedMethod;

	private final MethodParameter[] parameters;

	@Nullable
	private HttpStatus responseStatus;

	@Nullable
	private String responseStatusReason;

	@Nullable
	private HandlerMethod resolvedFromHandlerMethod;

	@Nullable
	private volatile List<Annotation[][]> interfaceParameterAnnotations;

	private final String description;


	/**
	 * 根据 Bean 实例和方法创建实例。
	 */
	public HandlerMethod(Object bean, Method method) {
		this(bean, method, null);
	}

	/**
	 * 是 {@link #HandlerMethod(Object, Method)} 的变体，
	 * 还接受一个供子类使用的 {@link MessageSource}。
	 * @since 5.3.10
	 */
	protected HandlerMethod(Object bean, Method method, @Nullable MessageSource messageSource) {
		Assert.notNull(bean, "Bean is required");
		Assert.notNull(method, "Method is required");
		this.bean = bean;
		this.beanFactory = null;
		this.messageSource = messageSource;
		this.beanType = ClassUtils.getUserClass(bean);
		this.method = method;
		this.bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
		ReflectionUtils.makeAccessible(this.bridgedMethod);
		this.parameters = initMethodParameters();
		evaluateResponseStatus();
		this.description = initDescription(this.beanType, this.method);
	}

	/**
	 * 根据 Bean 实例、方法名和参数类型创建实例。
	 * @throws NoSuchMethodException 当找不到该方法时
	 */
	public HandlerMethod(Object bean, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
		Assert.notNull(bean, "Bean is required");
		Assert.notNull(methodName, "Method name is required");
		this.bean = bean;
		this.beanFactory = null;
		this.messageSource = null;
		this.beanType = ClassUtils.getUserClass(bean);
		this.method = bean.getClass().getMethod(methodName, parameterTypes);
		this.bridgedMethod = BridgeMethodResolver.findBridgedMethod(this.method);
		ReflectionUtils.makeAccessible(this.bridgedMethod);
		this.parameters = initMethodParameters();
		evaluateResponseStatus();
		this.description = initDescription(this.beanType, this.method);
	}

	/**
	 * 根据 Bean 名称、方法和 {@code BeanFactory} 创建实例。
	 * 之后可以使用 {@link #createWithResolvedBean()} 方法，
	 * 以已初始化的 Bean 重新创建 {@code HandlerMethod}。
	 */
	public HandlerMethod(String beanName, BeanFactory beanFactory, Method method) {
		this(beanName, beanFactory, null, method);
	}

	/**
	 * 是 {@link #HandlerMethod(String, BeanFactory, Method)} 的变体，
	 * 还接受一个 {@link MessageSource}。
	 */
	public HandlerMethod(
			String beanName, BeanFactory beanFactory,
			@Nullable MessageSource messageSource, Method method) {

		Assert.hasText(beanName, "Bean name is required");
		Assert.notNull(beanFactory, "BeanFactory is required");
		Assert.notNull(method, "Method is required");
		this.bean = beanName;
		this.beanFactory = beanFactory;
		this.messageSource = messageSource;
		Class<?> beanType = beanFactory.getType(beanName);
		if (beanType == null) {
			throw new IllegalStateException("Cannot resolve bean type for bean with name '" + beanName + "'");
		}
		this.beanType = ClassUtils.getUserClass(beanType);
		this.method = method;
		this.bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
		ReflectionUtils.makeAccessible(this.bridgedMethod);
		this.parameters = initMethodParameters();
		evaluateResponseStatus();
		this.description = initDescription(this.beanType, this.method);
	}

	/**
	 * 供子类使用的拷贝构造函数。
	 */
	protected HandlerMethod(HandlerMethod handlerMethod) {
		Assert.notNull(handlerMethod, "HandlerMethod is required");
		this.bean = handlerMethod.bean;
		this.beanFactory = handlerMethod.beanFactory;
		this.messageSource = handlerMethod.messageSource;
		this.beanType = handlerMethod.beanType;
		this.method = handlerMethod.method;
		this.bridgedMethod = handlerMethod.bridgedMethod;
		this.parameters = handlerMethod.parameters;
		this.responseStatus = handlerMethod.responseStatus;
		this.responseStatusReason = handlerMethod.responseStatusReason;
		this.description = handlerMethod.description;
		this.resolvedFromHandlerMethod = handlerMethod.resolvedFromHandlerMethod;
	}

	/**
	 * 使用解析后的处理器重新创建 HandlerMethod。
	 */
	private HandlerMethod(HandlerMethod handlerMethod, Object handler) {
		Assert.notNull(handlerMethod, "HandlerMethod is required");
		Assert.notNull(handler, "Handler object is required");
		this.bean = handler;
		this.beanFactory = handlerMethod.beanFactory;
		this.messageSource = handlerMethod.messageSource;
		this.beanType = handlerMethod.beanType;
		this.method = handlerMethod.method;
		this.bridgedMethod = handlerMethod.bridgedMethod;
		this.parameters = handlerMethod.parameters;
		this.responseStatus = handlerMethod.responseStatus;
		this.responseStatusReason = handlerMethod.responseStatusReason;
		this.resolvedFromHandlerMethod = handlerMethod;
		this.description = handlerMethod.description;
	}

	private MethodParameter[] initMethodParameters() {
		int count = this.bridgedMethod.getParameterCount();
		MethodParameter[] result = new MethodParameter[count];
		for (int i = 0; i < count; i++) {
			result[i] = new HandlerMethodParameter(i);
		}
		return result;
	}

	private void evaluateResponseStatus() {
		ResponseStatus annotation = getMethodAnnotation(ResponseStatus.class);
		if (annotation == null) {
			annotation = AnnotatedElementUtils.findMergedAnnotation(getBeanType(), ResponseStatus.class);
		}
		if (annotation != null) {
			String reason = annotation.reason();
			String resolvedReason = (StringUtils.hasText(reason) && this.messageSource != null ?
					this.messageSource.getMessage(reason, null, reason, LocaleContextHolder.getLocale()) :
					reason);

			this.responseStatus = annotation.code();
			this.responseStatusReason = resolvedReason;
		}
	}

	private static String initDescription(Class<?> beanType, Method method) {
		StringJoiner joiner = new StringJoiner(", ", "(", ")");
		for (Class<?> paramType : method.getParameterTypes()) {
			joiner.add(paramType.getSimpleName());
		}
		return beanType.getName() + "#" + method.getName() + joiner.toString();
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
	 * 此方法返回此处理器方法的处理器类型。
	 * <p>请注意，如果 Bean 类型是 CGLIB 生成的类，则返回原始的用户定义类。
	 */
	public Class<?> getBeanType() {
		return this.beanType;
	}

	/**
	 * 如果 Bean 方法是桥接方法，则此方法返回被桥接的（用户定义的）方法。
	 * 否则，返回与 {@link #getMethod()} 相同的方法。
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
	 * 返回指定的响应状态（如果有）。
	 * @since 4.3.8
	 * @see ResponseStatus#code()
	 */
	@Nullable
	protected HttpStatus getResponseStatus() {
		return this.responseStatus;
	}

	/**
	 * 返回关联的响应状态原因（如果有）。
	 * @since 4.3.8
	 * @see ResponseStatus#reason()
	 */
	@Nullable
	protected String getResponseStatusReason() {
		return this.responseStatusReason;
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
	 * 返回底层方法上的单个注解；如果在给定方法本身上找不到注解，
	 * 则会遍历其父类方法进行查找。
	 * <p>自 Spring Framework 4.2.2 起，还支持带属性覆盖的<em>合并</em>组合注解。
	 * @param annotationType 用于内省该方法的注解类型
	 * @return 该注解，如果未找到则为 {@code null}
	 * @see AnnotatedElementUtils#findMergedAnnotation
	 */
	@Nullable
	public <A extends Annotation> A getMethodAnnotation(Class<A> annotationType) {
		return AnnotatedElementUtils.findMergedAnnotation(this.method, annotationType);
	}

	/**
	 * 返回该参数是否声明了给定的注解类型。
	 * @param annotationType 要查找的注解类型
	 * @since 4.3
	 * @see AnnotatedElementUtils#hasAnnotation
	 */
	public <A extends Annotation> boolean hasMethodAnnotation(Class<A> annotationType) {
		return AnnotatedElementUtils.hasAnnotation(this.method, annotationType);
	}

	/**
	 * 返回此 HandlerMethod 实例经 {@link #createWithResolvedBean()}
	 * 解析之前的原 HandlerMethod。
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
	 * 返回此处理器方法的简短表示形式，用于日志消息。
	 * @since 4.3
	 */
	public String getShortLogMessage() {
		return getBeanType().getName() + "#" + this.method.getName() +
				"[" + this.method.getParameterCount() + " args]";
	}


	private List<Annotation[][]> getInterfaceParameterAnnotations() {
		List<Annotation[][]> parameterAnnotations = this.interfaceParameterAnnotations;
		if (parameterAnnotations == null) {
			parameterAnnotations = new ArrayList<>();
			for (Class<?> ifc : ClassUtils.getAllInterfacesForClassAsSet(this.method.getDeclaringClass())) {
				for (Method candidate : ifc.getMethods()) {
					if (isOverrideFor(candidate)) {
						parameterAnnotations.add(candidate.getParameterAnnotations());
					}
				}
			}
			this.interfaceParameterAnnotations = parameterAnnotations;
		}
		return parameterAnnotations;
	}

	private boolean isOverrideFor(Method candidate) {
		if (!candidate.getName().equals(this.method.getName()) ||
				candidate.getParameterCount() != this.method.getParameterCount()) {
			return false;
		}
		Class<?>[] paramTypes = this.method.getParameterTypes();
		if (Arrays.equals(candidate.getParameterTypes(), paramTypes)) {
			return true;
		}
		for (int i = 0; i < paramTypes.length; i++) {
			if (paramTypes[i] !=
					ResolvableType.forMethodParameter(candidate, i, this.method.getDeclaringClass()).resolve()) {
				return false;
			}
		}
		return true;
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
		return this.description;
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
	 * 断言目标 Bean 类是给定方法声明所在类的实例。在某些情况下，
	 * 请求处理时的实际控制器实例可能是 JDK 动态代理（延迟初始化、
	 * 原型 Bean 等）。需要代理的 {@code @Controller} 应优先使用
	 * 基于类的代理机制。
	 */
	protected void assertTargetBean(Method method, Object targetBean, Object[] args) {
		Class<?> methodDeclaringClass = method.getDeclaringClass();
		Class<?> targetBeanClass = targetBean.getClass();
		if (!methodDeclaringClass.isAssignableFrom(targetBeanClass)) {
			String text = "The mapped handler method class '" + methodDeclaringClass.getName() +
					"' is not an instance of the actual controller bean class '" +
					targetBeanClass.getName() + "'. If the controller requires proxying " +
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
				"Controller [" + getBeanType().getName() + "]\n" +
				"Method [" + getBridgedMethod().toGenericString() + "] " +
				"with argument values:\n" + formattedArgs;
	}


	/**
	 * 具有 HandlerMethod 特定行为的 MethodParameter。
	 */
	protected class HandlerMethodParameter extends SynthesizingMethodParameter {

		@Nullable
		private volatile Annotation[] combinedAnnotations;

		public HandlerMethodParameter(int index) {
			super(HandlerMethod.this.bridgedMethod, index);
		}

		protected HandlerMethodParameter(HandlerMethodParameter original) {
			super(original);
		}

		@Override
		@NonNull
		public Method getMethod() {
			return HandlerMethod.this.bridgedMethod;
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
		public Annotation[] getParameterAnnotations() {
			Annotation[] anns = this.combinedAnnotations;
			if (anns == null) {
				anns = super.getParameterAnnotations();
				int index = getParameterIndex();
				if (index >= 0) {
					for (Annotation[][] ifcAnns : getInterfaceParameterAnnotations()) {
						if (index < ifcAnns.length) {
							Annotation[] paramAnns = ifcAnns[index];
							if (paramAnns.length > 0) {
								List<Annotation> merged = new ArrayList<>(anns.length + paramAnns.length);
								merged.addAll(Arrays.asList(anns));
								for (Annotation paramAnn : paramAnns) {
									boolean existingType = false;
									for (Annotation ann : anns) {
										if (ann.annotationType() == paramAnn.annotationType()) {
											existingType = true;
											break;
										}
									}
									if (!existingType) {
										merged.add(adaptAnnotation(paramAnn));
									}
								}
								anns = merged.toArray(new Annotation[0]);
							}
						}
					}
				}
				this.combinedAnnotations = anns;
			}
			return anns;
		}

		@Override
		public HandlerMethodParameter clone() {
			return new HandlerMethodParameter(this);
		}
	}


	/**
	 * 基于实际返回值表示 HandlerMethod 返回类型的 MethodParameter。
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
