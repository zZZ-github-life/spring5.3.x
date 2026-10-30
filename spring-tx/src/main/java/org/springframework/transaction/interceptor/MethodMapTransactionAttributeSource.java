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

package org.springframework.transaction.interceptor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringValueResolver;

/**
 * 简单的{@link TransactionAttributeSource}实现, 允许将属性按方法存储在{@link Map}中。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 24.04.2003
 * @see #isMatch
 * @see NameMatchTransactionAttributeSource
 */
public class MethodMapTransactionAttributeSource
		implements TransactionAttributeSource, EmbeddedValueResolverAware, BeanClassLoaderAware, InitializingBean {

	/** 可供子类使用的日志记录器。 */
	protected final Log logger = LogFactory.getLog(getClass());

	/** 从方法名到属性值的映射。 */
	@Nullable
	private Map<String, TransactionAttribute> methodMap;

	@Nullable
	private StringValueResolver embeddedValueResolver;

	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private boolean eagerlyInitialized = false;

	private boolean initialized = false;

	/** 从Method到TransactionAttribute的映射。 */
	private final Map<Method, TransactionAttribute> transactionAttributeMap = new HashMap<>();

	/** 从Method到注册时使用的名称模式的映射。 */
	private final Map<Method, String> methodNameMap = new HashMap<>();


	/**
	 * 设置名称/属性映射, 由"FQCN.method"格式的方法名
	 * (例如"com.mycompany.mycode.MyClass.myMethod")和{@link TransactionAttribute}实例
	 * (或要转换为{@code TransactionAttribute}实例的字符串)组成。
	 * <p>用于通过setter注入进行配置, 通常在Spring bean工厂中。依赖于之后调用{@link #afterPropertiesSet()}。
	 * @param methodMap 所述从方法名称到属性值的 {@link Map}
	 * @see TransactionAttribute
	 * @see TransactionAttributeEditor
	 */
	public void setMethodMap(Map<String, TransactionAttribute> methodMap) {
		this.methodMap = methodMap;
	}

	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.embeddedValueResolver = resolver;
	}

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}


	/**
	 * 立即初始化指定的{@link #setMethodMap(java.util.Map) "methodMap"}(如果有)。
	 * @see #initMethodMap(java.util.Map)
	 */
	@Override
	public void afterPropertiesSet() {
		initMethodMap(this.methodMap);
		this.eagerlyInitialized = true;
		this.initialized = true;
	}

	/**
	 * 初始化指定的{@link #setMethodMap(java.util.Map) "methodMap"}(如果有)。
	 * @param methodMap 从方法名称到 {@code TransactionAttribute} 实例的 Map
	 * @see #setMethodMap
	 */
	protected void initMethodMap(@Nullable Map<String, TransactionAttribute> methodMap) {
		if (methodMap != null) {
			methodMap.forEach(this::addTransactionalMethod);
		}
	}


	/**
	 * 为事务方法添加属性。
	 * <p>方法名可以以"*"结尾或开头, 以匹配多个方法。
	 * @param name 类名和方法名，以点号分隔
	 * @param attr 与方法关联的属性
	 * @throws IllegalArgumentException 名称无效时
	 */
	public void addTransactionalMethod(String name, TransactionAttribute attr) {
		Assert.notNull(name, "Name must not be null");
		int lastDotIndex = name.lastIndexOf('.');
		if (lastDotIndex == -1) {
			throw new IllegalArgumentException("'" + name + "' is not a valid method name: format is FQN.methodName");
		}
		String className = name.substring(0, lastDotIndex);
		String methodName = name.substring(lastDotIndex + 1);
		Class<?> clazz = ClassUtils.resolveClassName(className, this.beanClassLoader);
		addTransactionalMethod(clazz, methodName, attr);
	}

	/**
	 * 为事务方法添加属性。
	 * 方法名可以以"*"结尾或开头, 以匹配多个方法。
	 * @param clazz 目标接口或类
	 * @param mappedName 映射的方法名称
	 * @param attr 与方法关联的属性
	 */
	public void addTransactionalMethod(Class<?> clazz, String mappedName, TransactionAttribute attr) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(mappedName, "Mapped name must not be null");
		String name = clazz.getName() + '.'  + mappedName;

		Method[] methods = clazz.getDeclaredMethods();
		List<Method> matchingMethods = new ArrayList<>();
		for (Method method : methods) {
			if (isMatch(method.getName(), mappedName)) {
				matchingMethods.add(method);
			}
		}
		if (matchingMethods.isEmpty()) {
			throw new IllegalArgumentException(
					"Could not find method '" + mappedName + "' on class [" + clazz.getName() + "]");
		}

		// Register all matching methods
		for (Method method : matchingMethods) {
			String regMethodName = this.methodNameMap.get(method);
			if (regMethodName == null || (!regMethodName.equals(name) && regMethodName.length() <= name.length())) {
				// No already registered method name, or more specific
				// method name specification now -> (re-)register method.
				if (logger.isDebugEnabled() && regMethodName != null) {
					logger.debug("Replacing attribute for transactional method [" + method + "]: current name '" +
							name + "' is more specific than '" + regMethodName + "'");
				}
				this.methodNameMap.put(method, name);
				addTransactionalMethod(method, attr);
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug("Keeping attribute for transactional method [" + method + "]: current name '" +
							name + "' is not more specific than '" + regMethodName + "'");
				}
			}
		}
	}

	/**
	 * 为事务方法添加属性。
	 * @param method 该方法
	 * @param attr 与方法关联的属性
	 */
	public void addTransactionalMethod(Method method, TransactionAttribute attr) {
		Assert.notNull(method, "Method must not be null");
		Assert.notNull(attr, "TransactionAttribute must not be null");
		if (logger.isDebugEnabled()) {
			logger.debug("Adding transactional method [" + method + "] with attribute [" + attr + "]");
		}
		if (this.embeddedValueResolver != null && attr instanceof DefaultTransactionAttribute) {
			((DefaultTransactionAttribute) attr).resolveAttributeStrings(this.embeddedValueResolver);
		}
		this.transactionAttributeMap.put(method, attr);
	}

	/**
	 * 返回给定的方法名是否匹配映射名称。
	 * <p>默认实现检查"xxx*"、"*xxx"和"*xxx*"匹配, 以及直接相等。
	 * @param methodName 该类的方法名称
	 * @param mappedName 描述符中的名称
	 * @return 名称是否匹配
	 * @see org.springframework.util.PatternMatchUtils#simpleMatch(String, String)
	 */
	protected boolean isMatch(String methodName, String mappedName) {
		return PatternMatchUtils.simpleMatch(mappedName, methodName);
	}


	@Override
	@Nullable
	public TransactionAttribute getTransactionAttribute(Method method, @Nullable Class<?> targetClass) {
		if (this.eagerlyInitialized) {
			return this.transactionAttributeMap.get(method);
		}
		else {
			synchronized (this.transactionAttributeMap) {
				if (!this.initialized) {
					initMethodMap(this.methodMap);
					this.initialized = true;
				}
				return this.transactionAttributeMap.get(method);
			}
		}
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof MethodMapTransactionAttributeSource)) {
			return false;
		}
		MethodMapTransactionAttributeSource otherTas = (MethodMapTransactionAttributeSource) other;
		return ObjectUtils.nullSafeEquals(this.methodMap, otherTas.methodMap);
	}

	@Override
	public int hashCode() {
		return MethodMapTransactionAttributeSource.class.hashCode();
	}

	@Override
	public String toString() {
		return getClass().getName() + ": " + this.methodMap;
	}

}
