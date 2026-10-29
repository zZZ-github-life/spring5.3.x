/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.springframework.lang.Nullable;

/**
 * 辅助类，允许以声明方式指定要调用的方法，无论是静态方法还是非静态方法。
 *
 * <p>用法：指定 "targetClass"/"targetMethod" 或 "targetObject"/"targetMethod"，可选地指定参数，然
 * 后准备调用器。之后，你可以任意多次调用该方法，并获得调用结果。
 *
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @since 19.02.2004
 * @see #prepare
 * @see #invoke
 */
public class MethodInvoker {

	private static final Object[] EMPTY_ARGUMENTS = new Object[0];


	@Nullable
	protected Class<?> targetClass;

	@Nullable
	private Object targetObject;

	@Nullable
	private String targetMethod;

	@Nullable
	private String staticMethod;

	@Nullable
	private Object[] arguments;

	/** 我们将要调用的方法。 */
	@Nullable
	private Method methodObject;


	/**
	 * 设置要在其上调用目标方法的目标类。仅当目标方法是静态方法时才需要；否则无论如何都需要指定目标对象。
	 * @see #setTargetObject
	 * @see #setTargetMethod
	 */
	public void setTargetClass(@Nullable Class<?> targetClass) {
		this.targetClass = targetClass;
	}

	/**
	 * 返回要在其上调用目标方法的目标类。
	 */
	@Nullable
	public Class<?> getTargetClass() {
		return this.targetClass;
	}

	/**
	 * 设置要在其上调用目标方法的目标对象。仅当目标方法不是静态方法时才需要；否则指定目标类就足够了。
	 * @see #setTargetClass
	 * @see #setTargetMethod
	 */
	public void setTargetObject(@Nullable Object targetObject) {
		this.targetObject = targetObject;
		if (targetObject != null) {
			this.targetClass = targetObject.getClass();
		}
	}

	/**
	 * 返回要在其上调用目标方法的目标对象。
	 */
	@Nullable
	public Object getTargetObject() {
		return this.targetObject;
	}

	/**
	 * 设置要调用的方法的名称。取决于是否设置了目标对象，可以指静态方法或非静态方法。
	 * @see #setTargetClass
	 * @see #setTargetObject
	 */
	public void setTargetMethod(@Nullable String targetMethod) {
		this.targetMethod = targetMethod;
	}

	/**
	 * 返回要调用的方法的名称。
	 */
	@Nullable
	public String getTargetMethod() {
		return this.targetMethod;
	}

	/**
	 * 设置要调用的完全限定静态方法名，例如 "example.MyExampleClass.myExampleMethod"。这是指定
	 * targetClass 和 targetMethod 的便捷替代方式。
	 * @see #setTargetClass
	 * @see #setTargetMethod
	 */
	public void setStaticMethod(String staticMethod) {
		this.staticMethod = staticMethod;
	}

	/**
	 * 设置方法调用的参数。如果未设置此属性，或 Object 数组长度为 0，则假定方法没有参数。
	 */
	public void setArguments(Object... arguments) {
		this.arguments = arguments;
	}

	/**
	 * 返回方法调用的参数。
	 */
	public Object[] getArguments() {
		return (this.arguments != null ? this.arguments : EMPTY_ARGUMENTS);
	}


	/**
	 * 准备指定的方法。
	 * 之后可以任意多次调用该方法。
	 * @see #getPreparedMethod
	 * @see #invoke
	 */
	public void prepare() throws ClassNotFoundException, NoSuchMethodException {
		if (this.staticMethod != null) {
			int lastDotIndex = this.staticMethod.lastIndexOf('.');
			if (lastDotIndex == -1 || lastDotIndex == this.staticMethod.length()) {
				throw new IllegalArgumentException(
						"staticMethod must be a fully qualified class plus method name: " +
						"e.g. 'example.MyExampleClass.myExampleMethod'");
			}
			String className = this.staticMethod.substring(0, lastDotIndex);
			String methodName = this.staticMethod.substring(lastDotIndex + 1);
			this.targetClass = resolveClassName(className);
			this.targetMethod = methodName;
		}

		Class<?> targetClass = getTargetClass();
		String targetMethod = getTargetMethod();
		Assert.notNull(targetClass, "Either 'targetClass' or 'targetObject' is required");
		Assert.notNull(targetMethod, "Property 'targetMethod' is required");

		Object[] arguments = getArguments();
		Class<?>[] argTypes = new Class<?>[arguments.length];
		for (int i = 0; i < arguments.length; ++i) {
			argTypes[i] = (arguments[i] != null ? arguments[i].getClass() : Object.class);
		}

		// Try to get the exact method first.
		try {
			this.methodObject = targetClass.getMethod(targetMethod, argTypes);
		}
		catch (NoSuchMethodException ex) {
			// Just rethrow exception if we can't get any match.
			this.methodObject = findMatchingMethod();
			if (this.methodObject == null) {
				throw ex;
			}
		}
	}

	/**
	 * 将给定的类名解析为 Class。
	 * <p>默认实现使用 {@code ClassUtils.forName}，并使用线程上下文类加载器。
	 * @param className the class name to resolve
	 * @return the resolved Class
	 * @throws ClassNotFoundException if the class name was invalid
	 */
	protected Class<?> resolveClassName(String className) throws ClassNotFoundException {
		return ClassUtils.forName(className, ClassUtils.getDefaultClassLoader());
	}

	/**
	 * 查找与指定参数匹配的、具有指定名称的方法。
	 * @return a matching method, or {@code null} if none
	 * @see #getTargetClass()
	 * @see #getTargetMethod()
	 * @see #getArguments()
	 */
	@Nullable
	protected Method findMatchingMethod() {
		String targetMethod = getTargetMethod();
		Object[] arguments = getArguments();
		int argCount = arguments.length;

		Class<?> targetClass = getTargetClass();
		Assert.state(targetClass != null, "No target class set");
		Method[] candidates = ReflectionUtils.getAllDeclaredMethods(targetClass);
		int minTypeDiffWeight = Integer.MAX_VALUE;
		Method matchingMethod = null;

		for (Method candidate : candidates) {
			if (candidate.getName().equals(targetMethod)) {
				if (candidate.getParameterCount() == argCount) {
					Class<?>[] paramTypes = candidate.getParameterTypes();
					int typeDiffWeight = getTypeDifferenceWeight(paramTypes, arguments);
					if (typeDiffWeight < minTypeDiffWeight) {
						minTypeDiffWeight = typeDiffWeight;
						matchingMethod = candidate;
					}
				}
			}
		}

		return matchingMethod;
	}

	/**
	 * 返回将要调用的已准备 Method 对象。
	 * <p>例如可用于确定返回类型。
	 * @return the prepared Method object (never {@code null})
	 * @throws IllegalStateException if the invoker hasn't been prepared yet
	 * @see #prepare
	 * @see #invoke
	 */
	public Method getPreparedMethod() throws IllegalStateException {
		if (this.methodObject == null) {
			throw new IllegalStateException("prepare() must be called prior to invoke() on MethodInvoker");
		}
		return this.methodObject;
	}

	/**
	 * 返回此调用器是否已经准备好，即是否已经允许访问 {@link #getPreparedMethod()}。
	 */
	public boolean isPrepared() {
		return (this.methodObject != null);
	}

	/**
	 * 调用指定的方法。
	 * <p>调用器必须事先已经准备好。
	 * @return the object (possibly null) returned by the method invocation,
	 * or {@code null} if the method has a void return type
	 * @throws InvocationTargetException if the target method threw an exception
	 * @throws IllegalAccessException if the target method couldn't be accessed
	 * @see #prepare
	 */
	@Nullable
	public Object invoke() throws InvocationTargetException, IllegalAccessException {
		// In the static case, target will simply be {@code null}.
		Object targetObject = getTargetObject();
		Method preparedMethod = getPreparedMethod();
		if (targetObject == null && !Modifier.isStatic(preparedMethod.getModifiers())) {
			throw new IllegalArgumentException("Target method must not be non-static without a target");
		}
		ReflectionUtils.makeAccessible(preparedMethod);
		return preparedMethod.invoke(targetObject, getArguments());
	}


	/**
	 * 用于判断候选方法的声明参数类型与该方法预期被调用时所用的具体参数列表之间匹配程度的算法。
	 * <p>确定一个权重，表示类型与参数之间的类层次结构差异。直接匹配，即类型 Integer &rarr; 类 Integer
	 * 的参数，不会增加结果——所有直接匹配意味着权重为 0。类型 Object 与类 Integer 的参数匹配会使权重增
	 * 加 2，因为层次结构中向上 2 步的父类（即 Object）是最后一个仍然匹配所需类型 Object 的类。类型
	 * Number 与类 Integer 相应会使权重增加 1，因为层次结构中向上 1 步的父类（即 Number）仍然匹配所需类
	 * 型 Number。因此，对于类型为 Integer 的参数，构造函数 (Integer) 优先于构造函数 (Number)，而后者又
	 * 优先于构造函数 (Object)。所有参数权重都会被累加。
	 * <p>注意：这是 MethodInvoker 自身使用的算法，也是 Spring 的 Bean 容器中用于选择构造函数和工厂方法
	 * 的算法（在宽松构造函数解析的情况下，这也是常规 Bean 定义的默认方式）。
	 * @param paramTypes the parameter types to match
	 * @param args the arguments to match
	 * @return the accumulated weight for all arguments
	 */
	public static int getTypeDifferenceWeight(Class<?>[] paramTypes, Object[] args) {
		int result = 0;
		for (int i = 0; i < paramTypes.length; i++) {
			if (!ClassUtils.isAssignableValue(paramTypes[i], args[i])) {
				return Integer.MAX_VALUE;
			}
			if (args[i] != null) {
				Class<?> paramType = paramTypes[i];
				Class<?> superClass = args[i].getClass().getSuperclass();
				while (superClass != null) {
					if (paramType.equals(superClass)) {
						result = result + 2;
						superClass = null;
					}
					else if (ClassUtils.isAssignable(paramType, superClass)) {
						result = result + 2;
						superClass = superClass.getSuperclass();
					}
					else {
						superClass = null;
					}
				}
				if (paramType.isInterface()) {
					result = result + 1;
				}
			}
		}
		return result;
	}

}
