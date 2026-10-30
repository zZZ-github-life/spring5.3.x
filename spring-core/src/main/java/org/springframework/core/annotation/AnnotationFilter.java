/*
 * Copyright 2002-2021 the original author or authors.
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

/**
 * 可用于过滤特定注解类型的回调接口。
 *
 * <p>请注意，{@link MergedAnnotations} 模型（本接口正是为此而设计）始终根据 {@link #PLAIN} 过滤器
 * 忽略 lang 注解（出于效率原因）。任何附加过滤器乃至自定义过滤器实现都在此边界内生效，并且只能在此
 * 基础上进一步收窄。
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @since 5.2
 * @see MergedAnnotations
 */
@FunctionalInterface
public interface AnnotationFilter {

	/**
	 * 匹配 {@code java.lang} 和 {@code org.springframework.lang} 包及其子包中注解的
	 * {@link AnnotationFilter}。
	 * <p>这是 {@link MergedAnnotations} 模型中的默认过滤器。
	 */
	AnnotationFilter PLAIN = packages("java.lang", "org.springframework.lang");

	/**
	 * 匹配 {@code java} 和 {@code javax} 包及其子包中注解的 {@link AnnotationFilter}。
	 */
	AnnotationFilter JAVA = packages("java", "javax");

	/**
	 * 始终匹配的 {@link AnnotationFilter}，可在完全不预期存在相关注解类型时使用。
	 */
	AnnotationFilter ALL = new AnnotationFilter() {
		@Override
		public boolean matches(Annotation annotation) {
			return true;
		}
		@Override
		public boolean matches(Class<?> type) {
			return true;
		}
		@Override
		public boolean matches(String typeName) {
			return true;
		}
		@Override
		public String toString() {
			return "All annotations filtered";
		}
	};

	/**
	 * 从不匹配的 {@link AnnotationFilter}，可在不需要过滤时使用（允许存在任何注解类型）。
	 * @see #PLAIN
	 * @deprecated as of 5.2.6 since the {@link MergedAnnotations} model
	 * always ignores lang annotations according to the {@link #PLAIN} filter
	 * (for efficiency reasons)
	 */
	@Deprecated
	AnnotationFilter NONE = new AnnotationFilter() {
		@Override
		public boolean matches(Annotation annotation) {
			return false;
		}
		@Override
		public boolean matches(Class<?> type) {
			return false;
		}
		@Override
		public boolean matches(String typeName) {
			return false;
		}
		@Override
		public String toString() {
			return "No annotation filtering";
		}
	};


	/**
	 * 测试给定注解是否匹配该过滤器。
	 * @param annotation 要测试的注解
	 * @return 如果注解匹配则返回 {@code true}
	 */
	default boolean matches(Annotation annotation) {
		return matches(annotation.annotationType());
	}

	/**
	 * 测试给定类型是否匹配该过滤器。
	 * @param type 要测试的注解类型
	 * @return 如果注解匹配则返回 {@code true}
	 */
	default boolean matches(Class<?> type) {
		return matches(type.getName());
	}

	/**
	 * 测试给定类型名称是否匹配该过滤器。
	 * @param typeName 要测试的注解类型的全限定类名
	 * @return 如果注解匹配则返回 {@code true}
	 */
	boolean matches(String typeName);


	/**
	 * 创建一个新的 {@link AnnotationFilter}，用于匹配指定包中的注解。
	 * @param packages 应当匹配的注解包
	 * @return 一个新的 {@link AnnotationFilter} 实例
	 */
	static AnnotationFilter packages(String... packages) {
		return new PackagesAnnotationFilter(packages);
	}

}
