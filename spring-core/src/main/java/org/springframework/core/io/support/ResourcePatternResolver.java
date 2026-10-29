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

package org.springframework.core.io.support;

import java.io.IOException;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * 用于将位置模式（例如 Ant 风格的路径模式）
 * 解析为 {@link Resource} 对象的策略接口。
 *
 * <p>这是对 {@link org.springframework.core.io.ResourceLoader}
 * 接口的扩展。可以检查传入的 {@code ResourceLoader}（例如在上下文中运行时
 * 通过 {@link org.springframework.context.ResourceLoaderAware} 传入的
 * {@link org.springframework.context.ApplicationContext}）
 * 是否也实现了这个扩展接口。
 *
 * <p>{@link PathMatchingResourcePatternResolver} 是一个独立的实现，
 * 可在 {@code ApplicationContext} 之外使用，也被
 * {@link ResourceArrayPropertyEditor} 用于填充 {@code Resource} 数组类型的 Bean
 * 属性。
 *
 * <p>可用于任何类型的位置模式 &mdash; 例如，
 * {@code "/WEB-INF/*-context.xml"}。不过，输入模式必须与
 * 策略实现相匹配。此接口只规定转换方法，
 * 而不规定具体的模式格式。
 *
 * <p>此接口还为类路径中所有匹配的资源定义了一个 {@code "classpath*:"} 资源前缀。
 * 请注意，资源位置中也可以包含占位符 &mdash; 例如 {@code "/beans-*.xml"}。
 * JAR 文件或类路径中的不同目录可以包含多个
 * 同名文件。
 *
 * @author Juergen Hoeller
 * @since 1.0.2
 * @see org.springframework.core.io.Resource
 * @see org.springframework.core.io.ResourceLoader
 * @see org.springframework.context.ApplicationContext
 * @see org.springframework.context.ResourceLoaderAware
 */
public interface ResourcePatternResolver extends ResourceLoader {

	/**
	 * 类路径中所有匹配资源的伪 URL 前缀："classpath*:"
	 * <p>这与 ResourceLoader 的 classpath URL 前缀不同，它会
	 * 为给定名称（例如 "/beans.xml"）检索所有匹配的资源，
	 * 例如在所有已部署 JAR 文件的根目录中。
	 * @see org.springframework.core.io.ResourceLoader#CLASSPATH_URL_PREFIX
	 */
	String CLASSPATH_ALL_URL_PREFIX = "classpath*:";

	/**
	 * 将给定位置模式解析为 {@code Resource} 对象。
	 * <p>应尽可能避免指向同一物理资源的
	 * 重叠资源条目。结果应
	 * 具有集合语义。
	 * @param locationPattern the location pattern to resolve
	 * @return the corresponding {@code Resource} objects
	 * @throws IOException in case of I/O errors
	 */
	Resource[] getResources(String locationPattern) throws IOException;

}
