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

package org.springframework.context.annotation;

import java.util.function.Predicate;

import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.Nullable;

/**
 * 根据给定选择条件（通常是一个或多个注解属性）确定应导入哪些 @{@link Configuration} 类的类型应实现
 * 此接口。
 *
 * <p>{@link ImportSelector} 可以实现以下任意 {@link org.springframework.beans.factory.Aware Aware}
 * 接口，其各自的方法将在 {@link #selectImports} 之前调用：
 * <ul>
 * <li>{@link org.springframework.context.EnvironmentAware EnvironmentAware}</li>
 * <li>{@link org.springframework.beans.factory.BeanFactoryAware BeanFactoryAware}</li>
 * <li>{@link org.springframework.beans.factory.BeanClassLoaderAware BeanClassLoaderAware}</li>
 * <li>{@link org.springframework.context.ResourceLoaderAware ResourceLoaderAware}</li>
 * </ul>
 *
 * <p>或者，该类可以提供单个构造器，其参数为以下受支持参数类型中的一种或多种：
 * <ul>
 * <li>{@link org.springframework.core.env.Environment Environment}</li>
 * <li>{@link org.springframework.beans.factory.BeanFactory BeanFactory}</li>
 * <li>{@link java.lang.ClassLoader ClassLoader}</li>
 * <li>{@link org.springframework.core.io.ResourceLoader ResourceLoader}</li>
 * </ul>
 *
 * <p>{@code ImportSelector} 实现通常以与常规 {@code @Import} 注解相同的方式处理，不过也可以将导入
 * 选择推迟到所有 {@code @Configuration} 类处理完毕之后（详情请参见 {@link DeferredImportSelector}）。
 *
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see DeferredImportSelector
 * @see Import
 * @see ImportBeanDefinitionRegistrar
 * @see Configuration
 */
public interface ImportSelector {

	/**
	 * 根据导入方 @{@link Configuration} 类的 {@link AnnotationMetadata}，选择并返回应导入的类的名称。
	 * @return 类名；如果没有，则为空数组
	 */
	String[] selectImports(AnnotationMetadata importingClassMetadata);

	/**
	 * 返回一个用于从导入候选中排除类的谓词，该谓词将以传递方式应用于通过此选择器的导入所找到的所有类。
	 * <p>如果该谓词对给定的完全限定类名返回 {@code true}，则该类不会被视为导入的配置类，从而绕过类文件
	 * 加载以及元数据内省。
	 * @return 用于传递导入的配置类的全限定候选类名的过滤谓词；
	 * 如果没有，则为 {@code null}
	 * @since 5.2.4
	 */
	@Nullable
	default Predicate<String> getExclusionFilter() {
		return null;
	}

}
