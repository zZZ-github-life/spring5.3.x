/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.beans.factory.parsing;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 在解析过程中已处理的导入的表示形式。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see ReaderEventListener#importProcessed(ImportDefinition)
 */
public class ImportDefinition implements BeanMetadataElement {

	private final String importedResource;

	@Nullable
	private final Resource[] actualResources;

	@Nullable
	private final Object source;


	/**
	 * 创建一个新的 ImportDefinition。
	 * @param importedResource the location of the imported resource
	 */
	public ImportDefinition(String importedResource) {
		this(importedResource, null, null);
	}

	/**
	 * 创建一个新的 ImportDefinition。
	 * @param importedResource the location of the imported resource
	 * @param source the source object (may be {@code null})
	 */
	public ImportDefinition(String importedResource, @Nullable Object source) {
		this(importedResource, null, source);
	}

	/**
	 * 创建一个新的 ImportDefinition。
	 * @param importedResource the location of the imported resource
	 * @param source the source object (may be {@code null})
	 */
	public ImportDefinition(String importedResource, @Nullable Resource[] actualResources, @Nullable Object source) {
		Assert.notNull(importedResource, "Imported resource must not be null");
		this.importedResource = importedResource;
		this.actualResources = actualResources;
		this.source = source;
	}


	/**
	 * 返回导入资源的位置。
	 */
	public final String getImportedResource() {
		return this.importedResource;
	}

	@Nullable
	public final Resource[] getActualResources() {
		return this.actualResources;
	}

	@Override
	@Nullable
	public final Object getSource() {
		return this.source;
	}

}
