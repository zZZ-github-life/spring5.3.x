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
	 * @param method 要指定参数的 Method
	 * @param parameterIndex 参数索引：-1 表示方法的返回类型；
	 * 0 表示第一个方法参数；1 表示第二个方法参数，依此类推
	 */
	public SynthesizingMethodParameter(Method method, int parameterIndex) {
		super(method, parameterIndex);
	}

	/**
	 * 为给定的方法创建一个新的 {@code SynthesizingMethodParameter}。
	 * @param method 要指定参数的 Method
	 * @param parameterIndex 参数索引：-1 表示方法的返回类型；
	 * 0 表示第一个方法参数；1 表示第二个方法参数，依此类推
	 * @param nestingLevel 目标类型的嵌套层级（通常为 1；例如对于 List 的 List，
	 * 1 表示嵌套的 List，而 2 表示嵌套 List 的元素）
	 */
	public SynthesizingMethodParameter(Method method, int parameterIndex, int nestingLevel) {
		super(method, parameterIndex, nestingLevel);
	}

	/**
	 * 为给定的构造函数创建一个新的 {@code SynthesizingMethodParameter}，嵌套层级为 1。
	 * @param constructor 要指定参数的 Constructor
	 * @param parameterIndex 参数的索引
	 */
	public SynthesizingMethodParameter(Constructor<?> constructor, int parameterIndex) {
		super(constructor, parameterIndex);
	}

	/**
	 * 为给定的构造函数创建一个新的 {@code SynthesizingMethodParameter}。
	 * @param constructor 要指定参数的 Constructor
	 * @param parameterIndex 参数的索引
	 * @param nestingLevel 目标类型的嵌套层级（通常为 1；例如对于 List 的 List，
	 * 1 表示嵌套的 List，而 2 表示嵌套 List 的元素）
	 */
	public SynthesizingMethodParameter(Constructor<?> constructor, int parameterIndex, int nestingLevel) {
		super(constructor, parameterIndex, nestingLevel);
	}

	/**
	 * 复制构造函数，基于原始对象所处的相同元数据和缓存状态，生成一个独立的
	 * {@code SynthesizingMethodParameter}。
	 * @param original 要从中复制的原始 SynthesizingMethodParameter 对象
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
	 * @param executable 要指定参数的 Method 或 Constructor
	 * @param parameterIndex 参数的索引
	 * @return 对应的 SynthesizingMethodParameter 实例
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
	 * @param parameter 参数描述符
	 * @return 对应的 SynthesizingMethodParameter 实例
	 * @since 5.0
	 */
	public static SynthesizingMethodParameter forParameter(Parameter parameter) {
		return forExecutable(parameter.getDeclaringExecutable(), findParameterIndex(parameter));
	}

}
