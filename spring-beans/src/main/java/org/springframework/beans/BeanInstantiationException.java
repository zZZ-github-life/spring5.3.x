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

package org.springframework.beans;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.springframework.lang.Nullable;

/**
 * Bean 实例化失败时抛出的异常。携带出错的 Bean 类。
 *
 * @author Juergen Hoeller
 * @since 1.2.8
 */
@SuppressWarnings("serial")
public class BeanInstantiationException extends FatalBeanException {

	private final Class<?> beanClass;

	@Nullable
	private final Constructor<?> constructor;

	@Nullable
	private final Method constructingMethod;


	/**
	 * 创建一个新的 BeanInstantiationException。
	 * @param beanClass the offending bean class
	 * @param msg the detail message
	 */
	public BeanInstantiationException(Class<?> beanClass, String msg) {
		this(beanClass, msg, null);
	}

	/**
	 * 创建一个新的 BeanInstantiationException。
	 * @param beanClass the offending bean class
	 * @param msg the detail message
	 * @param cause the root cause
	 */
	public BeanInstantiationException(Class<?> beanClass, String msg, @Nullable Throwable cause) {
		super("Failed to instantiate [" + beanClass.getName() + "]: " + msg, cause);
		this.beanClass = beanClass;
		this.constructor = null;
		this.constructingMethod = null;
	}

	/**
	 * 创建一个新的 BeanInstantiationException。
	 * @param constructor the offending constructor
	 * @param msg the detail message
	 * @param cause the root cause
	 * @since 4.3
	 */
	public BeanInstantiationException(Constructor<?> constructor, String msg, @Nullable Throwable cause) {
		super("Failed to instantiate [" + constructor.getDeclaringClass().getName() + "]: " + msg, cause);
		this.beanClass = constructor.getDeclaringClass();
		this.constructor = constructor;
		this.constructingMethod = null;
	}

	/**
	 * 创建一个新的 BeanInstantiationException。
	 * @param constructingMethod the delegate for bean construction purposes
	 * (typically, but not necessarily, a static factory method)
	 * @param msg the detail message
	 * @param cause the root cause
	 * @since 4.3
	 */
	public BeanInstantiationException(Method constructingMethod, String msg, @Nullable Throwable cause) {
		super("Failed to instantiate [" + constructingMethod.getReturnType().getName() + "]: " + msg, cause);
		this.beanClass = constructingMethod.getReturnType();
		this.constructor = null;
		this.constructingMethod = constructingMethod;
	}


	/**
	 * 返回出错的 Bean 类（永不为 {@code null}）。
	 * @return the class that was to be instantiated
	 */
	public Class<?> getBeanClass() {
		return this.beanClass;
	}

	/**
	 * 返回出错的构造方法（如果已知）。
	 * @return the constructor in use, or {@code null} in case of a
	 * factory method or in case of default instantiation
	 * @since 4.3
	 */
	@Nullable
	public Constructor<?> getConstructor() {
		return this.constructor;
	}

	/**
	 * 返回用于 Bean 构造的委托对象（如果已知）。
	 * @return the method in use (typically a static factory method),
	 * or {@code null} in case of constructor-based instantiation
	 * @since 4.3
	 */
	@Nullable
	public Method getConstructingMethod() {
		return this.constructingMethod;
	}

}
