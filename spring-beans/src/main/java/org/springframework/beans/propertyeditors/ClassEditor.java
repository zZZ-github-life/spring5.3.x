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

package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;

import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * 用于 {@link Class java.lang.Class} 的属性编辑器，可直接填充 {@code Class} 属性，无需借助 String
 * 类名属性作为桥梁。
 *
 * <p>与标准的 {@link Class#forName(String)} 方法不同，它还支持 "java.lang.String[]" 风格的数组类名。
 *
 * @author Juergen Hoeller
 * @author Rick Evans
 * @since 13.05.2003
 * @see Class#forName
 * @see org.springframework.util.ClassUtils#forName(String, ClassLoader)
 */
public class ClassEditor extends PropertyEditorSupport {

	@Nullable
	private final ClassLoader classLoader;


	/**
	 * 使用线程上下文 ClassLoader 创建默认的 ClassEditor。
	 */
	public ClassEditor() {
		this(null);
	}

	/**
	 * 使用给定的 ClassLoader 创建默认的 ClassEditor。
	 * @param classLoader the ClassLoader to use
	 * (or {@code null} for the thread context ClassLoader)
	 */
	public ClassEditor(@Nullable ClassLoader classLoader) {
		this.classLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
	}


	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (StringUtils.hasText(text)) {
			setValue(ClassUtils.resolveClassName(text.trim(), this.classLoader));
		}
		else {
			setValue(null);
		}
	}

	@Override
	public String getAsText() {
		Class<?> clazz = (Class<?>) getValue();
		if (clazz != null) {
			return ClassUtils.getQualifiedName(clazz);
		}
		else {
			return "";
		}
	}

}
