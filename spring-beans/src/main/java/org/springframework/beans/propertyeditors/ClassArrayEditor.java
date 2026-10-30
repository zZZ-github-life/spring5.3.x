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
import java.util.StringJoiner;

import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 用于 {@link Class Classes} 数组的属性编辑器，可直接填充 {@code Class[]} 属性，而无需使用
 * {@code String} 类名属性作为桥梁。
 *
 * <p>与标准的 {@link Class#forName(String)} 方法不同，它还支持 "java.lang.String[]" 风格的数组类名。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public class ClassArrayEditor extends PropertyEditorSupport {

	@Nullable
	private final ClassLoader classLoader;


	/**
	 * 使用线程上下文 {@code ClassLoader} 创建默认的 {@code ClassEditor}。
	 */
	public ClassArrayEditor() {
		this(null);
	}

	/**
	 * 使用给定的 {@code ClassLoader} 创建默认的 {@code ClassArrayEditor}。
	 * @param classLoader 要使用的 {@code ClassLoader}
	 * （或传入 {@code null} 以使用线程上下文 {@code ClassLoader}）
	 */
	public ClassArrayEditor(@Nullable ClassLoader classLoader) {
		this.classLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
	}


	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (StringUtils.hasText(text)) {
			String[] classNames = StringUtils.commaDelimitedListToStringArray(text);
			Class<?>[] classes = new Class<?>[classNames.length];
			for (int i = 0; i < classNames.length; i++) {
				String className = classNames[i].trim();
				classes[i] = ClassUtils.resolveClassName(className, this.classLoader);
			}
			setValue(classes);
		}
		else {
			setValue(null);
		}
	}

	@Override
	public String getAsText() {
		Class<?>[] classes = (Class[]) getValue();
		if (ObjectUtils.isEmpty(classes)) {
			return "";
		}
		StringJoiner sj = new StringJoiner(",");
		for (Class<?> klass : classes) {
			sj.add(ClassUtils.getQualifiedName(klass));
		}
		return sj.toString();
	}

}
