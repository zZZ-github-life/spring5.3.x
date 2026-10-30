/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.io.File;
import java.io.IOException;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * 用于 {@code java.io.File} 的编辑器，可从 Spring 资源位置直接填充 File 属性。
 *
 * <p>支持 Spring 风格的 URL 表示法：任何完全限定的标准 URL（"file:"、"http:" 等）以及 Spring 特殊
 * 的 "classpath:" 伪 URL。
 *
 * <p><b>注意：</b>此编辑器的行为在 Spring 2.0 中已更改。以前，它直接从文件名创建 File 实例。从
 * Spring 2.0 起，它将标准的 Spring 资源位置作为输入；现在这与 URLEditor 和 InputStreamEditor 保持
 * 一致。
 *
 * <p><b>注意：</b>在 Spring 2.5 中做了以下修改。如果指定的文件名没有 URL 前缀或不是绝对路径，我们
 * 将尝试使用标准的 ResourceLoader 语义定位该文件。如果未找到该文件，则创建 File 实例，并假定该文件
 * 名指向相对文件位置。
 *
 * @author Juergen Hoeller
 * @author Thomas Risberg
 * @since 09.12.2003
 * @see java.io.File
 * @see org.springframework.core.io.ResourceEditor
 * @see org.springframework.core.io.ResourceLoader
 * @see URLEditor
 * @see InputStreamEditor
 */
public class FileEditor extends PropertyEditorSupport {

	private final ResourceEditor resourceEditor;


	/**
	 * 创建一个新的 FileEditor，底层使用默认的 ResourceEditor。
	 */
	public FileEditor() {
		this.resourceEditor = new ResourceEditor();
	}

	/**
	 * 创建一个新的 FileEditor，底层使用给定的 ResourceEditor。
	 * @param resourceEditor 要使用的 ResourceEditor
	 */
	public FileEditor(ResourceEditor resourceEditor) {
		Assert.notNull(resourceEditor, "ResourceEditor must not be null");
		this.resourceEditor = resourceEditor;
	}


	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (!StringUtils.hasText(text)) {
			setValue(null);
			return;
		}

		// Check whether we got an absolute file path without "file:" prefix.
		// For backwards compatibility, we'll consider those as straight file path.
		File file = null;
		if (!ResourceUtils.isUrl(text)) {
			file = new File(text);
			if (file.isAbsolute()) {
				setValue(file);
				return;
			}
		}

		// Proceed with standard resource location parsing.
		this.resourceEditor.setAsText(text);
		Resource resource = (Resource) this.resourceEditor.getValue();

		// If it's a URL or a path pointing to an existing resource, use it as-is.
		if (file == null || resource.exists()) {
			try {
				setValue(resource.getFile());
			}
			catch (IOException ex) {
				throw new IllegalArgumentException(
						"Could not retrieve file for " + resource + ": " + ex.getMessage());
			}
		}
		else {
			// Set a relative File reference and hope for the best.
			setValue(file);
		}
	}

	@Override
	public String getAsText() {
		File value = (File) getValue();
		return (value != null ? value.getPath() : "");
	}

}
