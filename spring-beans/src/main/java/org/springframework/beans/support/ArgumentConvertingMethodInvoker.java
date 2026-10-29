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

package org.springframework.beans.support;

import java.beans.PropertyEditor;
import java.lang.reflect.Method;

import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.TypeMismatchException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MethodInvoker;
import org.springframework.util.ReflectionUtils;

/**
 * {@link MethodInvoker} 的子类，它尝试通过 {@link TypeConverter} 为实际目标方法转换给定的参数。
 *
 * <p>支持灵活的参数转换，特别是用于调用特定的重载方法。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see org.springframework.beans.BeanWrapperImpl#convertIfNecessary
 */
public class ArgumentConvertingMethodInvoker extends MethodInvoker {

	@Nullable
	private TypeConverter typeConverter;

	private boolean useDefaultConverter = true;


	/**
	 * 设置用于参数类型转换的 TypeConverter。
	 * <p>默认是 {@link org.springframework.beans.SimpleTypeConverter}。可覆盖为任何 TypeConverter 实现，
	 * 通常是预先配置的 SimpleTypeConverter 或 BeanWrapperImpl 实例。
	 * @see org.springframework.beans.SimpleTypeConverter
	 * @see org.springframework.beans.BeanWrapperImpl
	 */
	public void setTypeConverter(@Nullable TypeConverter typeConverter) {
		this.typeConverter = typeConverter;
		this.useDefaultConverter = (typeConverter == null);
	}

	/**
	 * 返回用于参数类型转换的 TypeConverter。
	 * <p>如果需要直接访问底层的 PropertyEditors，可以将其强制转换为
	 * {@link org.springframework.beans.PropertyEditorRegistry}（前提是当前的 TypeConverter 实际实现了
	 * PropertyEditorRegistry 接口）。
	 */
	@Nullable
	public TypeConverter getTypeConverter() {
		if (this.typeConverter == null && this.useDefaultConverter) {
			this.typeConverter = getDefaultTypeConverter();
		}
		return this.typeConverter;
	}

	/**
	 * 获取此方法调用器的默认 TypeConverter。
	 * <p>在未指定显式 TypeConverter 时调用。默认实现会构建一个
	 * {@link org.springframework.beans.SimpleTypeConverter}。可以在子类中覆盖。
	 */
	protected TypeConverter getDefaultTypeConverter() {
		return new SimpleTypeConverter();
	}

	/**
	 * 为给定类型的所有属性注册给定的自定义属性编辑器。
	 * <p>通常与默认的 {@link org.springframework.beans.SimpleTypeConverter} 一起使用；也可用于任何实现
	 * 了 PropertyEditorRegistry 接口的 TypeConverter。
	 * @param requiredType type of the property
	 * @param propertyEditor editor to register
	 * @see #setTypeConverter
	 * @see org.springframework.beans.PropertyEditorRegistry#registerCustomEditor
	 */
	public void registerCustomEditor(Class<?> requiredType, PropertyEditor propertyEditor) {
		TypeConverter converter = getTypeConverter();
		if (!(converter instanceof PropertyEditorRegistry)) {
			throw new IllegalStateException(
					"TypeConverter does not implement PropertyEditorRegistry interface: " + converter);
		}
		((PropertyEditorRegistry) converter).registerCustomEditor(requiredType, propertyEditor);
	}


	/**
	 * 此实现会查找参数类型匹配的方法。
	 * @see #doFindMatchingMethod
	 */
	@Override
	protected Method findMatchingMethod() {
		Method matchingMethod = super.findMatchingMethod();
		// Second pass: look for method where arguments can be converted to parameter types.
		if (matchingMethod == null) {
			// Interpret argument array as individual method arguments.
			matchingMethod = doFindMatchingMethod(getArguments());
		}
		if (matchingMethod == null) {
			// Interpret argument array as single method argument of array type.
			matchingMethod = doFindMatchingMethod(new Object[] {getArguments()});
		}
		return matchingMethod;
	}

	/**
	 * 实际查找参数类型匹配的方法，即每个参数值都可以赋值给相应的参数类型。
	 * @param arguments the argument values to match against method parameters
	 * @return a matching method, or {@code null} if none
	 */
	@Nullable
	protected Method doFindMatchingMethod(Object[] arguments) {
		TypeConverter converter = getTypeConverter();
		if (converter != null) {
			String targetMethod = getTargetMethod();
			Method matchingMethod = null;
			int argCount = arguments.length;
			Class<?> targetClass = getTargetClass();
			Assert.state(targetClass != null, "No target class set");
			Method[] candidates = ReflectionUtils.getAllDeclaredMethods(targetClass);
			int minTypeDiffWeight = Integer.MAX_VALUE;
			Object[] argumentsToUse = null;
			for (Method candidate : candidates) {
				if (candidate.getName().equals(targetMethod)) {
					// Check if the inspected method has the correct number of parameters.
					int parameterCount = candidate.getParameterCount();
					if (parameterCount == argCount) {
						Class<?>[] paramTypes = candidate.getParameterTypes();
						Object[] convertedArguments = new Object[argCount];
						boolean match = true;
						for (int j = 0; j < argCount && match; j++) {
							// Verify that the supplied argument is assignable to the method parameter.
							try {
								convertedArguments[j] = converter.convertIfNecessary(arguments[j], paramTypes[j]);
							}
							catch (TypeMismatchException ex) {
								// Ignore -> simply doesn't match.
								match = false;
							}
						}
						if (match) {
							int typeDiffWeight = getTypeDifferenceWeight(paramTypes, convertedArguments);
							if (typeDiffWeight < minTypeDiffWeight) {
								minTypeDiffWeight = typeDiffWeight;
								matchingMethod = candidate;
								argumentsToUse = convertedArguments;
							}
						}
					}
				}
			}
			if (matchingMethod != null) {
				setArguments(argumentsToUse);
				return matchingMethod;
			}
		}
		return null;
	}

}
