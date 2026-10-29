/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.core;

import org.springframework.lang.Nullable;

/**
 * 用于暴露 Spring 版本的类。它从 jar 文件中获取 "Implementation-Version" 清单属性。
 *
 * <p>请注意，某些 ClassLoader 不会暴露包元数据，因此此类可能无法在所有环境中确定 Spring 版本。请考
 * 虑改用基于反射的检查 &mdash; 例如，检查你打算调用的某个 Spring 5.2 特定方法是否存在。
 *
 * @author Juergen Hoeller
 * @since 1.1
 */
public final class SpringVersion {

	private SpringVersion() {
	}


	/**
	 * 返回当前 Spring 代码库的完整版本字符串，如果无法确定则返回 {@code null}。
	 * @see Package#getImplementationVersion()
	 */
	@Nullable
	public static String getVersion() {
		Package pkg = SpringVersion.class.getPackage();
		return (pkg != null ? pkg.getImplementationVersion() : null);
	}

}
