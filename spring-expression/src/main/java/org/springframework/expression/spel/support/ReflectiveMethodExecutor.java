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

package org.springframework.expression.spel.support;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.TypedValue;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * 通过反射工作的 {@link MethodExecutor}。
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
public class ReflectiveMethodExecutor implements MethodExecutor {

	private final Method originalMethod;

	private final Method methodToInvoke;

	@Nullable
	private final Integer varargsPosition;

	private boolean computedPublicDeclaringClass = false;

	@Nullable
	private Class<?> publicDeclaringClass;

	private boolean argumentConversionOccurred = false;


	/**
	 * 为给定方法创建一个新的执行器。
	 * @param method the method to invoke
	 */
	public ReflectiveMethodExecutor(Method method) {
		this(method, null);
	}

	/**
	 * 为给定方法创建一个新的执行器。
	 * @param method the method to invoke
	 * @param targetClass the target class to invoke the method on
	 * @since 5.3.16
	 */
	public ReflectiveMethodExecutor(Method method, @Nullable Class<?> targetClass) {
		this.originalMethod = method;
		this.methodToInvoke = ClassUtils.getInterfaceMethodIfPossible(method, targetClass);
		if (method.isVarArgs()) {
			this.varargsPosition = method.getParameterCount() - 1;
		}
		else {
			this.varargsPosition = null;
		}
	}


	/**
	 * 返回此执行器所配置的原始方法。
	 */
	public final Method getMethod() {
		return this.originalMethod;
	}

	/**
	 * 在声明该方法的类层级中查找第一个声明该方法的公共类。有时反射方法发现逻辑会找到一个适合通过反射轻
	 * 松调用的方法，但在编译表达式时因可见性限制而无法从生成的代码中调用。例如，如果某个非公共类重写了
	 * toString()，此辅助方法会沿类型层级向上查找，以找到第一个声明该方法的公共类型（如果存在的话！）。
	 * 对于 toString()，它可能一直向上找到 Object。
	 */
	@Nullable
	public Class<?> getPublicDeclaringClass() {
		if (!this.computedPublicDeclaringClass) {
			this.publicDeclaringClass =
					discoverPublicDeclaringClass(this.originalMethod, this.originalMethod.getDeclaringClass());
			this.computedPublicDeclaringClass = true;
		}
		return this.publicDeclaringClass;
	}

	@Nullable
	private Class<?> discoverPublicDeclaringClass(Method method, Class<?> clazz) {
		if (Modifier.isPublic(clazz.getModifiers())) {
			try {
				clazz.getDeclaredMethod(method.getName(), method.getParameterTypes());
				return clazz;
			}
			catch (NoSuchMethodException ex) {
				// Continue below...
			}
		}
		if (clazz.getSuperclass() != null) {
			return discoverPublicDeclaringClass(method, clazz.getSuperclass());
		}
		return null;
	}

	public boolean didArgumentConversionOccur() {
		return this.argumentConversionOccurred;
	}


	@Override
	public TypedValue execute(EvaluationContext context, Object target, Object... arguments) throws AccessException {
		try {
			this.argumentConversionOccurred = ReflectionHelper.convertArguments(
					context.getTypeConverter(), arguments, this.originalMethod, this.varargsPosition);
			if (this.originalMethod.isVarArgs()) {
				arguments = ReflectionHelper.setupArgumentsForVarargsInvocation(
						this.originalMethod.getParameterTypes(), arguments);
			}
			ReflectionUtils.makeAccessible(this.methodToInvoke);
			Object value = this.methodToInvoke.invoke(target, arguments);
			return new TypedValue(value, new TypeDescriptor(new MethodParameter(this.originalMethod, -1)).narrow(value));
		}
		catch (Exception ex) {
			throw new AccessException("Problem invoking method: " + this.methodToInvoke, ex);
		}
	}

}
