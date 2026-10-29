/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.remoting.support;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * 封装一次远程调用，以可序列化的方式提供核心的方法调用属性。用于 RMI 和基于 HTTP 的序列化调用器。
 *
 * <p>这是一个 SPI 类，通常不由应用程序直接使用。可以派生子类以添加额外的调用参数。
 *
 * <p>{@link RemoteInvocation} 和 {@link RemoteInvocationResult} 都设计为既可用于标准 Java 序列化，
 * 也可用于 JavaBean 风格的序列化。
 *
 * @author Juergen Hoeller
 * @since 25.02.2004
 * @see RemoteInvocationResult
 * @see RemoteInvocationFactory
 * @see RemoteInvocationExecutor
 * @see org.springframework.remoting.rmi.RmiProxyFactoryBean
 * @see org.springframework.remoting.rmi.RmiServiceExporter
 * @see org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean
 * @see org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter
 */
public class RemoteInvocation implements Serializable {

	/** 使用 Spring 1.1 的 serialVersionUID 以实现互操作性。 */
	private static final long serialVersionUID = 6876024250231820554L;


	private String methodName;

	private Class<?>[] parameterTypes;

	private Object[] arguments;

	private Map<String, Serializable> attributes;


	/**
	 * 为给定的 AOP 方法调用创建一个新的 RemoteInvocation。
	 * @param methodInvocation the AOP invocation to convert
	 */
	public RemoteInvocation(MethodInvocation methodInvocation) {
		this.methodName = methodInvocation.getMethod().getName();
		this.parameterTypes = methodInvocation.getMethod().getParameterTypes();
		this.arguments = methodInvocation.getArguments();
	}

	/**
	 * 为给定的参数创建一个新的 RemoteInvocation。
	 * @param methodName the name of the method to invoke
	 * @param parameterTypes the parameter types of the method
	 * @param arguments the arguments for the invocation
	 */
	public RemoteInvocation(String methodName, Class<?>[] parameterTypes, Object[] arguments) {
		this.methodName = methodName;
		this.parameterTypes = parameterTypes;
		this.arguments = arguments;
	}

	/**
	 * 创建一个新的 RemoteInvocation，用于 JavaBean 风格的反序列化（例如使用 Jackson）。
	 */
	public RemoteInvocation() {
	}


	/**
	 * 设置目标方法的名称。
	 * <p>此 setter 用于 JavaBean 风格的反序列化。
	 */
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	/**
	 * 返回目标方法的名称。
	 */
	public String getMethodName() {
		return this.methodName;
	}

	/**
	 * 设置目标方法的参数类型。
	 * <p>此 setter 用于 JavaBean 风格的反序列化。
	 */
	public void setParameterTypes(Class<?>[] parameterTypes) {
		this.parameterTypes = parameterTypes;
	}

	/**
	 * 返回目标方法的参数类型。
	 */
	public Class<?>[] getParameterTypes() {
		return this.parameterTypes;
	}

	/**
	 * 设置目标方法调用的参数。
	 * <p>此 setter 用于 JavaBean 风格的反序列化。
	 */
	public void setArguments(Object[] arguments) {
		this.arguments = arguments;
	}

	/**
	 * 返回目标方法调用的参数。
	 */
	public Object[] getArguments() {
		return this.arguments;
	}


	/**
	 * 添加一个额外的调用属性。可用于在不进行 RemoteInvocation 子类化的情况下添加额外的调用上下文。
	 * <p>属性键必须唯一，且不允许覆盖已有的属性。
	 * <p>该实现会避免不必要地创建属性 Map，以尽量减小序列化体积。
	 * @param key the attribute key
	 * @param value the attribute value
	 * @throws IllegalStateException if the key is already bound
	 */
	public void addAttribute(String key, Serializable value) throws IllegalStateException {
		if (this.attributes == null) {
			this.attributes = new HashMap<>();
		}
		if (this.attributes.containsKey(key)) {
			throw new IllegalStateException("There is already an attribute with key '" + key + "' bound");
		}
		this.attributes.put(key, value);
	}

	/**
	 * 获取给定键对应的属性（如果有）。
	 * <p>该实现会避免不必要地创建属性 Map，以尽量减小序列化体积。
	 * @param key the attribute key
	 * @return the attribute value, or {@code null} if not defined
	 */
	@Nullable
	public Serializable getAttribute(String key) {
		if (this.attributes == null) {
			return null;
		}
		return this.attributes.get(key);
	}

	/**
	 * 设置属性 Map。仅用于特殊目的：最好使用 {@link #addAttribute} 和 {@link #getAttribute}。
	 * @param attributes the attributes Map
	 * @see #addAttribute
	 * @see #getAttribute
	 */
	public void setAttributes(@Nullable Map<String, Serializable> attributes) {
		this.attributes = attributes;
	}

	/**
	 * 返回属性 Map。主要用于调试目的：最好使用 {@link #addAttribute} 和 {@link #getAttribute}。
	 * @return the attributes Map, or {@code null} if none created
	 * @see #addAttribute
	 * @see #getAttribute
	 */
	@Nullable
	public Map<String, Serializable> getAttributes() {
		return this.attributes;
	}


	/**
	 * 在给定的目标对象上执行此调用。通常当服务器收到 RemoteInvocation 时调用。
	 * @param targetObject the target object to apply the invocation to
	 * @return the invocation result
	 * @throws NoSuchMethodException if the method name could not be resolved
	 * @throws IllegalAccessException if the method could not be accessed
	 * @throws InvocationTargetException if the method invocation resulted in an exception
	 * @see java.lang.reflect.Method#invoke
	 */
	public Object invoke(Object targetObject)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

		Method method = targetObject.getClass().getMethod(this.methodName, this.parameterTypes);
		return method.invoke(targetObject, this.arguments);
	}


	@Override
	public String toString() {
		return "RemoteInvocation: method name '" + this.methodName + "'; parameter types " +
				ClassUtils.classNamesToString(this.parameterTypes);
	}

}
