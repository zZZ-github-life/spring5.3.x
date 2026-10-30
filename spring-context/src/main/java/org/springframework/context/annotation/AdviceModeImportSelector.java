/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.context.annotation;

import java.lang.annotation.Annotation;

import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 用于 {@link ImportSelector} 实现的便捷基类，这些实现基于注解（例如 {@code @Enable*} 注解）中的
 * {@link AdviceMode} 值来选择导入。
 *
 * @author Chris Beams
 * @since 3.1
 * @param <A> 包含 {@linkplain #getAdviceModeAttributeName() AdviceMode 属性}的注解
 */
public abstract class AdviceModeImportSelector<A extends Annotation> implements ImportSelector {

	/**
	 * 默认的通知模式属性名称。
	 */
	public static final String DEFAULT_ADVICE_MODE_ATTRIBUTE_NAME = "mode";


	/**
	 * 由泛型类型 {@code A} 所指定注解的 {@link AdviceMode} 属性名称。默认为
	 * {@value #DEFAULT_ADVICE_MODE_ATTRIBUTE_NAME}，但子类可以覆盖以进行定制。
	 */
	protected String getAdviceModeAttributeName() {
		return DEFAULT_ADVICE_MODE_ATTRIBUTE_NAME;
	}

	/**
	 * 此实现从泛型元数据中解析注解类型，并验证 (a) 该注解确实存在于导入的 {@code @Configuration} 类上，
	 * 以及 (b) 给定注解具有类型为 {@link AdviceMode} 的
	 * {@linkplain #getAdviceModeAttributeName() 通知模式属性}。
	 *
	 * <p>随后会调用 {@link #selectImports(AdviceMode)} 方法，允许具体实现以安全便捷的方式选择导入。
	 * @throws IllegalArgumentException 如果导入的 {@code @Configuration} 类上不存在预期的注解 {@code A}，
	 * 或者 {@link #selectImports(AdviceMode)} 返回 {@code null}
	 */
	@Override
	public final String[] selectImports(AnnotationMetadata importingClassMetadata) {
		Class<?> annType = GenericTypeResolver.resolveTypeArgument(getClass(), AdviceModeImportSelector.class);
		Assert.state(annType != null, "Unresolvable type argument for AdviceModeImportSelector");

		AnnotationAttributes attributes = AnnotationConfigUtils.attributesFor(importingClassMetadata, annType);
		if (attributes == null) {
			throw new IllegalArgumentException(String.format(
					"@%s is not present on importing class '%s' as expected",
					annType.getSimpleName(), importingClassMetadata.getClassName()));
		}

		AdviceMode adviceMode = attributes.getEnum(getAdviceModeAttributeName());
		String[] imports = selectImports(adviceMode);
		if (imports == null) {
			throw new IllegalArgumentException("Unknown AdviceMode: " + adviceMode);
		}
		return imports;
	}

	/**
	 * 根据给定的 {@code AdviceMode} 确定应导入哪些类。
	 *
	 * <p>从此方法返回 {@code null} 表示无法处理该 {@code AdviceMode} 或其未知，并且应抛出
	 * {@code IllegalArgumentException}。
	 * @param adviceMode 通过泛型指定的注解的
	 * {@linkplain #getAdviceModeAttributeName() 通知模式属性}值。
	 * @return 包含要导入的类的数组（如果没有则为空数组；
	 * 如果给定的 {@code AdviceMode} 未知，则为 {@code null}）
	 */
	@Nullable
	protected abstract String[] selectImports(AdviceMode adviceMode);

}
