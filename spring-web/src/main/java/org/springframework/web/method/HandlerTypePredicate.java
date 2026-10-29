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

package org.springframework.web.method;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * 一个 {@code Predicate}，当以下选择器中的<strong>任意一个</strong>匹配时，
 * 即匹配请求处理组件类型：
 * <ul>
 * <li>基础包 —— 用于按处理器所在的包进行选择。
 * <li>可赋值类型 —— 用于按处理器的父类型进行选择。
 * <li>注解 —— 用于选择以特定方式标注的处理器。
 * </ul>
 * <p>可以使用 {@link Predicate} 上的组合方法：
 * <pre class="code">
 * Predicate&lt;Class&lt;?&gt;&gt; predicate =
 * 		HandlerTypePredicate.forAnnotation(RestController.class)
 * 				.and(HandlerTypePredicate.forBasePackage("org.example"));
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @since 5.1
 */
public final class HandlerTypePredicate implements Predicate<Class<?>> {

	private final Set<String> basePackages;

	private final List<Class<?>> assignableTypes;

	private final List<Class<? extends Annotation>> annotations;


	/**
	 * 私有构造函数。请参阅静态工厂方法。
	 */
	private HandlerTypePredicate(Set<String> basePackages, List<Class<?>> assignableTypes,
			List<Class<? extends Annotation>> annotations) {

		this.basePackages = Collections.unmodifiableSet(basePackages);
		this.assignableTypes = Collections.unmodifiableList(assignableTypes);
		this.annotations = Collections.unmodifiableList(annotations);
	}


	@Override
	public boolean test(@Nullable Class<?> controllerType) {
		if (!hasSelectors()) {
			return true;
		}
		else if (controllerType != null) {
			for (String basePackage : this.basePackages) {
				if (controllerType.getName().startsWith(basePackage)) {
					return true;
				}
			}
			for (Class<?> clazz : this.assignableTypes) {
				if (ClassUtils.isAssignable(clazz, controllerType)) {
					return true;
				}
			}
			for (Class<? extends Annotation> annotationClass : this.annotations) {
				if (AnnotationUtils.findAnnotation(controllerType, annotationClass) != null) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean hasSelectors() {
		return (!this.basePackages.isEmpty() || !this.assignableTypes.isEmpty() || !this.annotations.isEmpty());
	}


	// Static factory methods

	/**
	 * 适用于任意处理器的 {@code Predicate}。
	 */
	public static HandlerTypePredicate forAnyHandlerType() {
		return new HandlerTypePredicate(
				Collections.emptySet(), Collections.emptyList(), Collections.emptyList());
	}

	/**
	 * 匹配声明在基础包下的处理器，例如 "org.example"。
	 * @param packages one or more base package names
	 */
	public static HandlerTypePredicate forBasePackage(String... packages) {
		return new Builder().basePackage(packages).build();
	}

	/**
	 * {@link #forBasePackage(String...)} 的类型安全替代方法，
	 * 通过类来指定基础包。
	 * @param packageClasses one or more base package classes
	 */
	public static HandlerTypePredicate forBasePackageClass(Class<?>... packageClasses) {
		return new Builder().basePackageClass(packageClasses).build();
	}

	/**
	 * 匹配可赋值给给定类型的处理器。
	 * @param types one or more handler supertypes
	 */
	public static HandlerTypePredicate forAssignableType(Class<?>... types) {
		return new Builder().assignableType(types).build();
	}

	/**
	 * 匹配标注了特定注解的处理器。
	 * @param annotations one or more annotations to check for
	 */
	@SafeVarargs
	public static HandlerTypePredicate forAnnotation(Class<? extends Annotation>... annotations) {
		return new Builder().annotation(annotations).build();
	}

	/**
	 * 返回 {@code HandlerTypePredicate} 的构建器。
	 */
	public static Builder builder() {
		return new Builder();
	}


	/**
	 * {@link HandlerTypePredicate} 的构建器。
	 */
	public static class Builder {

		private final Set<String> basePackages = new LinkedHashSet<>();

		private final List<Class<?>> assignableTypes = new ArrayList<>();

		private final List<Class<? extends Annotation>> annotations = new ArrayList<>();

		/**
		 * 匹配声明在基础包下的处理器，例如 "org.example"。
		 * @param packages one or more base package classes
		 */
		public Builder basePackage(String... packages) {
			Arrays.stream(packages).filter(StringUtils::hasText).forEach(this::addBasePackage);
			return this;
		}

		/**
		 * {@link #forBasePackage(String...)} 的类型安全替代方法，
		 * 通过类来指定基础包。
		 * @param packageClasses one or more base package names
		 */
		public Builder basePackageClass(Class<?>... packageClasses) {
			Arrays.stream(packageClasses).forEach(clazz -> addBasePackage(ClassUtils.getPackageName(clazz)));
			return this;
		}

		private void addBasePackage(String basePackage) {
			this.basePackages.add(basePackage.endsWith(".") ? basePackage : basePackage + ".");
		}

		/**
		 * 匹配可赋值给给定类型的处理器。
		 * @param types one or more handler supertypes
		 */
		public Builder assignableType(Class<?>... types) {
			this.assignableTypes.addAll(Arrays.asList(types));
			return this;
		}

		/**
		 * 匹配标注了给定注解之一中的任意注解的类型。
		 * @param annotations one or more annotations to check for
		 */
		@SuppressWarnings("unchecked")
		public final Builder annotation(Class<? extends Annotation>... annotations) {
			this.annotations.addAll(Arrays.asList(annotations));
			return this;
		}

		public HandlerTypePredicate build() {
			return new HandlerTypePredicate(this.basePackages, this.assignableTypes, this.annotations);
		}
	}

}
