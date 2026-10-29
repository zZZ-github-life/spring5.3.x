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
import java.io.IOException;
import java.net.URL;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.util.Assert;

/**
 * 用于 {@code java.net.URL} 的编辑器，可直接填充 URL 属性，而无需使用 String 属性作为桥接。
 *
 * <p>支持 Spring 风格的 URL 表示法：任何完全限定的标准 URL（"file:"、"http:" 等）以及 Spring 特殊
 * 的 "classpath:" 伪 URL，还有 Spring 上下文相关的相对文件路径。
 *
 * <p>注意：URL 必须指定有效的协议，否则会在一开始就被拒绝。但是，目标资源在创建 URL 时不必一定存在；
 * 这取决于具体的资源类型。
 *
 * @author Juergen Hoeller
 * @since 15.12.2003
 * @see java.net.URL
 * @see org.springframework.core.io.ResourceEditor
 * @see org.springframework.core.io.ResourceLoader
 * @see FileEditor
 * @see InputStreamEditor
 */
public class URLEditor extends PropertyEditorSupport {

	private final ResourceEditor resourceEditor;


	/**
	 * 创建一个新的 URLEditor，底层使用默认的 ResourceEditor。
	 */
	public URLEditor() {
		this.resourceEditor = new ResourceEditor();
	}

	/**
	 * 创建一个新的 URLEditor，底层使用给定的 ResourceEditor。
	 * @param resourceEditor the ResourceEditor to use
	 */
	public URLEditor(ResourceEditor resourceEditor) {
		Assert.notNull(resourceEditor, "ResourceEditor must not be null");
		this.resourceEditor = resourceEditor;
	}


	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		this.resourceEditor.setAsText(text);
		Resource resource = (Resource) this.resourceEditor.getValue();
		try {
			setValue(resource != null ? resource.getURL() : null);
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Could not retrieve URL for " + resource + ": " + ex.getMessage());
		}
	}

	@Override
	public String getAsText() {
		URL value = (URL) getValue();
		return (value != null ? value.toExternalForm() : "");
	}

}
