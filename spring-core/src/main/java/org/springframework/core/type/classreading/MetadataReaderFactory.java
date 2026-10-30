/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.core.type.classreading;

import java.io.IOException;

import org.springframework.core.io.Resource;

/**
 * 用于 {@link MetadataReader} 实例的工厂接口。允许按原始资源缓存 MetadataReader。
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see SimpleMetadataReaderFactory
 * @see CachingMetadataReaderFactory
 */
public interface MetadataReaderFactory {

	/**
	 * 获取给定类名的 MetadataReader。
	 * @param className 类名（将被解析为 ".class" 文件）
	 * @return ClassReader 实例的持有者（永不为 {@code null}）
	 * @throws IOException 如果发生 I/O 失败
	 */
	MetadataReader getMetadataReader(String className) throws IOException;

	/**
	 * 获取给定资源的 MetadataReader。
	 * @param resource 资源（指向 ".class" 文件）
	 * @return ClassReader 实例的持有者（永不为 {@code null}）
	 * @throws IOException 如果发生 I/O 失败
	 */
	MetadataReader getMetadataReader(Resource resource) throws IOException;

}
