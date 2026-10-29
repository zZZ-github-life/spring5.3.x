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

package org.springframework.core.type.filter;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * 一个简单的 {@link TypeFilter}，它匹配带有给定注解的类，同时还会检查继承的注解。
 *
 * <p>默认情况下，匹配逻辑与
 * {@link AnnotationUtils#getAnnotation(java.lang.reflect.AnnotatedElement, Class)} 一致，支持单层
 * 元注解上<em>存在</em>或<em>元存在</em>的注解。可以禁用对元注解的搜索。同样，也可以选择启用对接口
 * 上注解的搜索。详情请参阅本类中的各个构造函数。
 *
 * @author Mark Fisher
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 2.5
 */
public class AnnotationTypeFilter extends AbstractTypeHierarchyTraversingFilter {

	private final Class<? extends Annotation> annotationType;

	private final boolean considerMetaAnnotations;


	/**
	 * 为给定的注解类型创建一个新的 {@code AnnotationTypeFilter}。
	 * <p>该过滤器还会匹配元注解。要禁用元注解匹配，请使用接受 '{@code considerMetaAnnotations}' 参数的
	 * 构造函数。
	 * <p>该过滤器不会匹配接口。
	 * @param annotationType the annotation type to match
	 */
	public AnnotationTypeFilter(Class<? extends Annotation> annotationType) {
		this(annotationType, true, false);
	}

	/**
	 * 为给定的注解类型创建一个新的 {@code AnnotationTypeFilter}。
	 * <p>该过滤器不会匹配接口。
	 * @param annotationType the annotation type to match
	 * @param considerMetaAnnotations whether to also match on meta-annotations
	 */
	public AnnotationTypeFilter(Class<? extends Annotation> annotationType, boolean considerMetaAnnotations) {
		this(annotationType, considerMetaAnnotations, false);
	}

	/**
	 * 为给定的注解类型创建一个新的 {@code AnnotationTypeFilter}。
	 * @param annotationType the annotation type to match
	 * @param considerMetaAnnotations whether to also match on meta-annotations
	 * @param considerInterfaces whether to also match interfaces
	 */
	public AnnotationTypeFilter(
			Class<? extends Annotation> annotationType, boolean considerMetaAnnotations, boolean considerInterfaces) {

		super(annotationType.isAnnotationPresent(Inherited.class), considerInterfaces);
		this.annotationType = annotationType;
		this.considerMetaAnnotations = considerMetaAnnotations;
	}

	/**
	 * 返回此实例用于筛选候选对象的 {@link Annotation}。
	 * @since 5.0
	 */
	public final Class<? extends Annotation> getAnnotationType() {
		return this.annotationType;
	}

	@Override
	protected boolean matchSelf(MetadataReader metadataReader) {
		AnnotationMetadata metadata = metadataReader.getAnnotationMetadata();
		return metadata.hasAnnotation(this.annotationType.getName()) ||
				(this.considerMetaAnnotations && metadata.hasMetaAnnotation(this.annotationType.getName()));
	}

	@Override
	@Nullable
	protected Boolean matchSuperClass(String superClassName) {
		return hasAnnotation(superClassName);
	}

	@Override
	@Nullable
	protected Boolean matchInterface(String interfaceName) {
		return hasAnnotation(interfaceName);
	}

	@Nullable
	protected Boolean hasAnnotation(String typeName) {
		if (Object.class.getName().equals(typeName)) {
			return false;
		}
		else if (typeName.startsWith("java")) {
			if (!this.annotationType.getName().startsWith("java")) {
				// Standard Java types do not have non-standard annotations on them ->
				// skip any load attempt, in particular for Java language interfaces.
				return false;
			}
			try {
				Class<?> clazz = ClassUtils.forName(typeName, getClass().getClassLoader());
				return ((this.considerMetaAnnotations ? AnnotationUtils.getAnnotation(clazz, this.annotationType) :
						clazz.getAnnotation(this.annotationType)) != null);
			}
			catch (Throwable ex) {
				// Class not regularly loadable - can't determine a match that way.
			}
		}
		return null;
	}

}
