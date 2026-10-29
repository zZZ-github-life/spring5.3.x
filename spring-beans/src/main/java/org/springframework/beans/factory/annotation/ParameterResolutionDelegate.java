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

package org.springframework.beans.factory.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 用于在外部管理的构造器和方法上解析可自动装配参数的公共委托类。
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 5.2
 * @see #isAutowirable
 * @see #resolveDependency
 */
public final class ParameterResolutionDelegate {

	private static final AnnotatedElement EMPTY_ANNOTATED_ELEMENT = new AnnotatedElement() {
		@Override
		@Nullable
		public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
			return null;
		}
		@Override
		public Annotation[] getAnnotations() {
			return new Annotation[0];
		}
		@Override
		public Annotation[] getDeclaredAnnotations() {
			return new Annotation[0];
		}
	};


	private ParameterResolutionDelegate() {
	}


	/**
	 * 确定提供的 {@link Parameter} 是否<em>可能</em>从 {@link AutowireCapableBeanFactory} 自动装配。
	 * <p>如果提供的参数被 {@link Autowired @Autowired}、{@link Qualifier @Qualifier} 或
	 * {@link Value @Value} 注解或元注解标注，则返回 {@code true}。
	 * <p>请注意，即使此方法返回 {@code false}，{@link #resolveDependency} 仍可能解析所提供的参数的依赖。
	 *
	 * @param parameter the parameter whose dependency should be autowired
	 * (must not be {@code null})
	 * @param parameterIndex the index of the parameter in the constructor or method
	 * that declares the parameter
	 * @see #resolveDependency
	 */
	public static boolean isAutowirable(Parameter parameter, int parameterIndex) {
		Assert.notNull(parameter, "Parameter must not be null");
		AnnotatedElement annotatedParameter = getEffectiveAnnotatedParameter(parameter, parameterIndex);
		return (AnnotatedElementUtils.hasAnnotation(annotatedParameter, Autowired.class) ||
				AnnotatedElementUtils.hasAnnotation(annotatedParameter, Qualifier.class) ||
				AnnotatedElementUtils.hasAnnotation(annotatedParameter, Value.class));
	}

	/**
	 * 从提供的 {@link AutowireCapableBeanFactory} 中解析所提供的 {@link Parameter} 的依赖。
	 * <p>为单个方法参数提供全面的自动装配支持，与 Spring 对自动装配字段和方法的依赖注入设施相当，包括
	 * 支持 {@link Autowired @Autowired}、{@link Qualifier @Qualifier} 和 {@link Value @Value}，并支持
	 * {@code @Value} 声明中的属性占位符和 SpEL 表达式。
	 * <p>除非该参数被 {@link Autowired @Autowired} 注解或元注解标注且
	 * {@link Autowired#required required} 标志设为 {@code false}，否则该依赖是必需的。
	 * <p>如果未声明显式的<em>限定符</em>，则将使用参数名称作为限定符来消除歧义。
	 * @param parameter the parameter whose dependency should be resolved (must not be
	 * {@code null})
	 * @param parameterIndex the index of the parameter in the constructor or method
	 * that declares the parameter
	 * @param containingClass the concrete class that contains the parameter; this may
	 * differ from the class that declares the parameter in that it may be a subclass
	 * thereof, potentially substituting type variables (must not be {@code null})
	 * @param beanFactory the {@code AutowireCapableBeanFactory} from which to resolve
	 * the dependency (must not be {@code null})
	 * @return the resolved object, or {@code null} if none found
	 * @throws BeansException if dependency resolution failed
	 * @see #isAutowirable
	 * @see Autowired#required
	 * @see SynthesizingMethodParameter#forExecutable(Executable, int)
	 * @see AutowireCapableBeanFactory#resolveDependency(DependencyDescriptor, String)
	 */
	@Nullable
	public static Object resolveDependency(
			Parameter parameter, int parameterIndex, Class<?> containingClass, AutowireCapableBeanFactory beanFactory)
			throws BeansException {

		Assert.notNull(parameter, "Parameter must not be null");
		Assert.notNull(containingClass, "Containing class must not be null");
		Assert.notNull(beanFactory, "AutowireCapableBeanFactory must not be null");

		AnnotatedElement annotatedParameter = getEffectiveAnnotatedParameter(parameter, parameterIndex);
		Autowired autowired = AnnotatedElementUtils.findMergedAnnotation(annotatedParameter, Autowired.class);
		boolean required = (autowired == null || autowired.required());

		MethodParameter methodParameter = SynthesizingMethodParameter.forExecutable(
				parameter.getDeclaringExecutable(), parameterIndex);
		DependencyDescriptor descriptor = new DependencyDescriptor(methodParameter, required);
		descriptor.setContainingClass(containingClass);
		return beanFactory.resolveDependency(descriptor, null);
	}

	/**
	 * 由于 JDK 9 之前的 JDK 版本中 {@code javac} 存在一个缺陷，对于内部类构造器，直接在
	 * {@link Parameter} 上查找注解会失败。
	 * <h4>JDK &lt; 9 中 javac 的缺陷</h4>
	 * <p>编译后字节码中的参数注解数组会缺少内部类构造器隐式<em>外围实例</em>参数对应的条目。
	 * <h4>变通方案</h4>
	 * <p>此方法通过允许调用者访问前一个 {@link Parameter} 对象（即 {@code index - 1}）上的注解，为这一
	 * 差一错误提供了变通方案。如果提供的 {@code index} 为零，此方法返回一个空的
	 * {@code AnnotatedElement}。
	 * <h4>警告</h4>
	 * <p>此方法返回的 {@code AnnotatedElement} 绝不应被强制转换并当作 {@code Parameter} 处理，因为其元
	 * 数据（例如 {@link Parameter#getName()}、{@link Parameter#getType()} 等）将与内部类构造器中给定索
	 * 引处所声明参数的不匹配。
	 * @return the supplied {@code parameter} or the <em>effective</em> {@code Parameter}
	 * if the aforementioned bug is in effect
	 */
	private static AnnotatedElement getEffectiveAnnotatedParameter(Parameter parameter, int index) {
		Executable executable = parameter.getDeclaringExecutable();
		if (executable instanceof Constructor && ClassUtils.isInnerClass(executable.getDeclaringClass()) &&
				executable.getParameterAnnotations().length == executable.getParameterCount() - 1) {
			// Bug in javac in JDK <9: annotation array excludes enclosing instance parameter
			// for inner classes, so access it with the actual parameter index lowered by 1
			return (index == 0 ? EMPTY_ANNOTATED_ELEMENT : executable.getParameters()[index - 1]);
		}
		return parameter;
	}

}
