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

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.Method;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ObjectUtils;

/**
 * 用于确定哪些注解作为其他注解容器的策略。{@link #standardRepeatables()} 方法提供了默认策略，它遵
 * 循 Java 的 {@link Repeatable @Repeatable} 支持，应适用于大多数场景。
 *
 * <p>可以使用 {@link #of} 方法为不希望使用 {@link Repeatable @Repeatable} 的注解注册关系。
 *
 * <p>要完全禁用可重复支持，请使用 {@link #none()}。
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 5.2
 */
public abstract class RepeatableContainers {

	@Nullable
	private final RepeatableContainers parent;


	private RepeatableContainers(@Nullable RepeatableContainers parent) {
		this.parent = parent;
	}


	/**
	 * 在被包含注解与可重复注解之间添加一个额外的显式关系。
	 * @param container 容器类型
	 * @param repeatable 所含的可重复类型
	 * @return 一个新的 {@link RepeatableContainers} 实例
	 */
	public RepeatableContainers and(Class<? extends Annotation> container,
			Class<? extends Annotation> repeatable) {

		return new ExplicitRepeatableContainer(this, repeatable, container);
	}

	@Nullable
	Annotation[] findRepeatedAnnotations(Annotation annotation) {
		if (this.parent == null) {
			return null;
		}
		return this.parent.findRepeatedAnnotations(annotation);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (other == this) {
			return true;
		}
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		return ObjectUtils.nullSafeEquals(this.parent, ((RepeatableContainers) other).parent);
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(this.parent);
	}


	/**
	 * 创建一个使用 Java 的 {@link Repeatable @Repeatable} 注解进行搜索的 {@link RepeatableContainers}
	 * 实例。
	 * @return 一个 {@link RepeatableContainers} 实例
	 */
	public static RepeatableContainers standardRepeatables() {
		return StandardRepeatableContainers.INSTANCE;
	}

	/**
	 * 创建一个使用预定义的可重复类型和容器类型的 {@link RepeatableContainers} 实例。
	 * @param repeatable 所含的可重复注解类型
	 * @param container 容器注解类型或 {@code null}。如果指定，
	 * 该注解必须声明一个返回可重复注解数组的 {@code value} 属性。
	 * 如果未指定，则将通过检查 {@code repeatable} 上的
	 * {@code @Repeatable} 注解来推断容器。
	 * @return 一个 {@link RepeatableContainers} 实例
	 * @throws IllegalArgumentException 如果提供的容器类型为 {@code null} 且注解类型不是可重复注解
	 * @throws AnnotationConfigurationException 如果提供的容器类型不是正确配置的可重复注解容器
	 */
	public static RepeatableContainers of(
			Class<? extends Annotation> repeatable, @Nullable Class<? extends Annotation> container) {

		return new ExplicitRepeatableContainer(null, repeatable, container);
	}

	/**
	 * 创建一个不展开任何可重复注解的 {@link RepeatableContainers} 实例。
	 * @return 一个 {@link RepeatableContainers} 实例
	 */
	public static RepeatableContainers none() {
		return NoRepeatableContainers.INSTANCE;
	}


	/**
	 * 标准的 {@link RepeatableContainers} 实现，使用 Java 的 {@link Repeatable @Repeatable} 注解进行搜
	 * 索。
	 */
	private static class StandardRepeatableContainers extends RepeatableContainers {

		private static final Map<Class<? extends Annotation>, Object> cache = new ConcurrentReferenceHashMap<>();

		private static final Object NONE = new Object();

		private static StandardRepeatableContainers INSTANCE = new StandardRepeatableContainers();

		StandardRepeatableContainers() {
			super(null);
		}

		@Override
		@Nullable
		Annotation[] findRepeatedAnnotations(Annotation annotation) {
			Method method = getRepeatedAnnotationsMethod(annotation.annotationType());
			if (method != null) {
				return (Annotation[]) AnnotationUtils.invokeAnnotationMethod(method, annotation);
			}
			return super.findRepeatedAnnotations(annotation);
		}

		@Nullable
		private static Method getRepeatedAnnotationsMethod(Class<? extends Annotation> annotationType) {
			Object result = cache.computeIfAbsent(annotationType,
					StandardRepeatableContainers::computeRepeatedAnnotationsMethod);
			return (result != NONE ? (Method) result : null);
		}

		private static Object computeRepeatedAnnotationsMethod(Class<? extends Annotation> annotationType) {
			AttributeMethods methods = AttributeMethods.forAnnotationType(annotationType);
			Method method = methods.get(MergedAnnotation.VALUE);
			if (method != null) {
				Class<?> returnType = method.getReturnType();
				if (returnType.isArray()) {
					Class<?> componentType = returnType.getComponentType();
					if (Annotation.class.isAssignableFrom(componentType) &&
							componentType.isAnnotationPresent(Repeatable.class)) {
						return method;
					}
				}
			}
			return NONE;
		}
	}


	/**
	 * 单个显式映射。
	 */
	private static class ExplicitRepeatableContainer extends RepeatableContainers {

		private final Class<? extends Annotation> repeatable;

		private final Class<? extends Annotation> container;

		private final Method valueMethod;

		ExplicitRepeatableContainer(@Nullable RepeatableContainers parent,
				Class<? extends Annotation> repeatable, @Nullable Class<? extends Annotation> container) {

			super(parent);
			Assert.notNull(repeatable, "Repeatable must not be null");
			if (container == null) {
				container = deduceContainer(repeatable);
			}
			Method valueMethod = AttributeMethods.forAnnotationType(container).get(MergedAnnotation.VALUE);
			try {
				if (valueMethod == null) {
					throw new NoSuchMethodException("No value method found");
				}
				Class<?> returnType = valueMethod.getReturnType();
				if (!returnType.isArray() || returnType.getComponentType() != repeatable) {
					throw new AnnotationConfigurationException("Container type [" +
							container.getName() +
							"] must declare a 'value' attribute for an array of type [" +
							repeatable.getName() + "]");
				}
			}
			catch (AnnotationConfigurationException ex) {
				throw ex;
			}
			catch (Throwable ex) {
				throw new AnnotationConfigurationException(
						"Invalid declaration of container type [" + container.getName() +
								"] for repeatable annotation [" + repeatable.getName() + "]",
						ex);
			}
			this.repeatable = repeatable;
			this.container = container;
			this.valueMethod = valueMethod;
		}

		private Class<? extends Annotation> deduceContainer(Class<? extends Annotation> repeatable) {
			Repeatable annotation = repeatable.getAnnotation(Repeatable.class);
			Assert.notNull(annotation, () -> "Annotation type must be a repeatable annotation: " +
						"failed to resolve container type for " + repeatable.getName());
			return annotation.value();
		}

		@Override
		@Nullable
		Annotation[] findRepeatedAnnotations(Annotation annotation) {
			if (this.container.isAssignableFrom(annotation.annotationType())) {
				return (Annotation[]) AnnotationUtils.invokeAnnotationMethod(this.valueMethod, annotation);
			}
			return super.findRepeatedAnnotations(annotation);
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (!super.equals(other)) {
				return false;
			}
			ExplicitRepeatableContainer otherErc = (ExplicitRepeatableContainer) other;
			return (this.container.equals(otherErc.container) && this.repeatable.equals(otherErc.repeatable));
		}

		@Override
		public int hashCode() {
			int hashCode = super.hashCode();
			hashCode = 31 * hashCode + this.container.hashCode();
			hashCode = 31 * hashCode + this.repeatable.hashCode();
			return hashCode;
		}
	}


	/**
	 * 没有可重复容器。
	 */
	private static class NoRepeatableContainers extends RepeatableContainers {

		private static NoRepeatableContainers INSTANCE = new NoRepeatableContainers();

		NoRepeatableContainers() {
			super(null);
		}
	}

}
