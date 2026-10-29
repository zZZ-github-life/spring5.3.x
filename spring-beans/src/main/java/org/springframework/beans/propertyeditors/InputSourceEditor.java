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

package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.io.IOException;

import org.xml.sax.InputSource;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.util.Assert;

/**
 * 用于 {@code org.xml.sax.InputSource} 的编辑器，可将 Spring 资源位置 String 转换为 SAX
 * InputSource 对象。
 *
 * <p>支持 Spring 风格的 URL 表示法：任何完全限定的标准 URL（"file:"、"http:" 等）以及 Spring 特殊
 * 的 "classpath:" 伪 URL。
 *
 * @author Juergen Hoeller
 * @since 3.0.3
 * @see org.xml.sax.InputSource
 * @see org.springframework.core.io.ResourceEditor
 * @see org.springframework.core.io.ResourceLoader
 * @see URLEditor
 * @see FileEditor
 */
public class InputSourceEditor extends PropertyEditorSupport {

	private final ResourceEditor resourceEditor;


	/**
	 * 创建一个新的 InputSourceEditor，底层使用默认的 ResourceEditor。
	 */
	public InputSourceEditor() {
		this.resourceEditor = new ResourceEditor();
	}

	/**
	 * 创建一个新的 InputSourceEditor，底层使用给定的 ResourceEditor。
	 * @param resourceEditor the ResourceEditor to use
	 */
	public InputSourceEditor(ResourceEditor resourceEditor) {
		Assert.notNull(resourceEditor, "ResourceEditor must not be null");
		this.resourceEditor = resourceEditor;
	}


	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		this.resourceEditor.setAsText(text);
		Resource resource = (Resource) this.resourceEditor.getValue();
		try {
			setValue(resource != null ? new InputSource(resource.getURL().toString()) : null);
		}
		catch (IOException ex) {
			throw new IllegalArgumentException(
					"Could not retrieve URL for " + resource + ": " + ex.getMessage());
		}
	}

	@Override
	public String getAsText() {
		InputSource value = (InputSource) getValue();
		return (value != null ? value.getSystemId() : "");
	}

}
