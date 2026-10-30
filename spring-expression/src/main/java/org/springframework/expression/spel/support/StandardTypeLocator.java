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

package org.springframework.expression.spel.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypeLocator;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * {@link TypeLocator} 的简单实现，使用上下文 ClassLoader（或在其上设置的任意 ClassLoader）。它支持
 * “众所周知”的包：因此，如果无法找到某个类型，它会尝试通过已注册的导入来定位它。
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
public class StandardTypeLocator implements TypeLocator {

	@Nullable
	private final ClassLoader classLoader;

	private final List<String> knownPackagePrefixes = new ArrayList<>(1);


	/**
	 * 为默认 ClassLoader（通常是线程上下文 ClassLoader）创建一个 StandardTypeLocator。
	 */
	public StandardTypeLocator() {
		this(ClassUtils.getDefaultClassLoader());
	}

	/**
	 * 为给定的 ClassLoader 创建一个 StandardTypeLocator。
	 * @param classLoader 要委托的 ClassLoader
	 */
	public StandardTypeLocator(@Nullable ClassLoader classLoader) {
		this.classLoader = classLoader;
		// Similar to when writing regular Java code, it only knows about java.lang by default
		registerImport("java.lang");
	}


	/**
	 * 注册一个新的导入前缀，用于搜索未限定类型。预期格式类似于“java.lang”。
	 * @param prefix 要注册的前缀
	 */
	public void registerImport(String prefix) {
		this.knownPackagePrefixes.add(prefix);
	}

	/**
	 * 从该定位器的导入列表中移除指定的前缀。
	 * @param prefix 要移除的前缀
	 */
	public void removeImport(String prefix) {
		this.knownPackagePrefixes.remove(prefix);
	}

	/**
	 * 返回在此 StandardTypeLocator 中注册的所有导入前缀的列表。
	 * @return 已注册的导入前缀列表
	 */
	public List<String> getImportPrefixes() {
		return Collections.unmodifiableList(this.knownPackagePrefixes);
	}


	/**
	 * 查找（可能未限定的）类型引用——先按原样使用类型名，如果找不到该类型名，再尝试任何已注册的前缀。
	 * @param typeName 要定位的类型
	 * @return 该类型的 Class 对象
	 * @throws EvaluationException 如果找不到该类型
	 */
	@Override
	public Class<?> findType(String typeName) throws EvaluationException {
		String nameToLookup = typeName;
		try {
			return ClassUtils.forName(nameToLookup, this.classLoader);
		}
		catch (ClassNotFoundException ey) {
			// try any registered prefixes before giving up
		}
		for (String prefix : this.knownPackagePrefixes) {
			try {
				nameToLookup = prefix + '.' + typeName;
				return ClassUtils.forName(nameToLookup, this.classLoader);
			}
			catch (ClassNotFoundException ex) {
				// might be a different prefix
			}
		}
		throw new SpelEvaluationException(SpelMessage.TYPE_NOT_FOUND, typeName);
	}

}
