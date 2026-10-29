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

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.springframework.core.MethodParameter;

/**
 * 一种 {@link MethodParameter} 变体，用于合成通过 {@link AliasFor @AliasFor} 声明属性别名的注解。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.2
 * @see AnnotationUtils#synthesizeAnnotation
 * @see AnnotationUtils#synthesizeAnnotationArray
 */
public class SynthesizingMethodParameter extends MethodParameter {

	/**
	 * 为给定的方法创建一个新的 {@code SynthesizingMethodParameter}，嵌套层级为 1。
	 * @param method the Method to specify a parameter for
	 * @param parameterIndex the index of the parameter: -1 for the method
	 * return type; 0 for the first method parameter; 1 for the second method
	 * parameter, etc.
	 */
	public SynthesizingMethodParameter(Method method, int parameterIndex) {
		super(method, parameterIndex);
	}

	/**
	 * 为给定的方法创建一个新的 {@code SynthesizingMethodParameter}。
	 * @param method the Method to specify a parameter for
	 * @param parameterIndex the index of the parameter: -1 for the method
	 * return type; 0 for the first method parameter; 1 for the second method
	 * parameter, etc.
	 * @param nestingLevel the nesting level of the target type
	 * (typically 1; e.g. in case of a List of Lists, 1 would indicate the
	 * nested List, whereas 2 would indicate the element of the nested List)
	 */
	public SynthesizingMethodParameter(Method method, int parameterIndex, int nestingLevel) {
		super(method, parameterIndex, nestingLevel);
	}

	/**
	 * 为给定的构造函数创建一个新的 {@code SynthesizingMethodParameter}，嵌套层级为 1。
	 * @param constructor the Constructor to specify a parameter for
	 * @param parameterIndex the index of the parameter
	 */
	public SynthesizingMethodParameter(Constructor<?> constructor, int parameterIndex) {
		super(constructor, parameterIndex);
	}

	/**
	 * 为给定的构造函数创建一个新的 {@code SynthesizingMethodParameter}。
	 * @param constructor the Constructor to specify a parameter for
	 * @param parameterIndex the index of the parameter
	 * @param nestingLevel the nesting level of the target type
	 * (typically 1; e.g. in case of a List of Lists, 1 would indicate the
	 * nested List, whereas 2 would indicate the element of the nested List)
	 */
	public SynthesizingMethodParameter(Constructor<?> constructor, int parameterIndex, int nestingLevel) {
		super(constructor, parameterIndex, nestingLevel);
	}

	/**
	 * 复制构造函数，基于原始对象所处的相同元数据和缓存状态，生成一个独立的
	 * {@code SynthesizingMethodParameter}。
	 * @param original the original SynthesizingMethodParameter object to copy from
	 */
	protected SynthesizingMethodParameter(SynthesizingMethodParameter original) {
		super(original);
	}


	@Override
	protected <A extends Annotation> A adaptAnnotation(A annotation) {
		return AnnotationUtils.synthesizeAnnotation(annotation, getAnnotatedElement());
	}

	@Override
	protected Annotation[] adaptAnnotationArray(Annotation[] annotations) {
		return AnnotationUtils.synthesizeAnnotationArray(annotations, getAnnotatedElement());
	}

	@Override
	public SynthesizingMethodParameter clone() {
		return new SynthesizingMethodParameter(this);
	}


	/**
	 * 为给定的方法或构造函数创建一个新的 SynthesizingMethodParameter。
	 * <p>这是一个便捷工厂方法，适用于以通用方式处理 Method 或 Constructor 引用的场景。
	 * @param executable the Method or Constructor to specify a parameter for
	 * @param parameterIndex the index of the parameter
	 * @return the corresponding SynthesizingMethodParameter instance
	 * @since 5.0
	 */
	public static SynthesizingMethodParameter forExecutable(Executable executable, int parameterIndex) {
		if (executable instanceof Method) {
			return new SynthesizingMethodParameter((Method) executable, parameterIndex);
		}
		else if (executable instanceof Constructor) {
			return new SynthesizingMethodParameter((Constructor<?>) executable, parameterIndex);
		}
		else {
			throw new IllegalArgumentException("Not a Method/Constructor: " + executable);
		}
	}

	/**
	 * 为给定的参数描述符创建一个新的 SynthesizingMethodParameter。
	 * <p>这是一个便捷工厂方法，适用于已经获得 Java 8 {@link Parameter} 描述符的场景。
	 * @param parameter the parameter descriptor
	 * @return the corresponding SynthesizingMethodParameter instance
	 * @since 5.0
	 */
	public static SynthesizingMethodParameter forParameter(Parameter parameter) {
		return forExecutable(parameter.getDeclaringExecutable(), findParameterIndex(parameter));
	}

}
