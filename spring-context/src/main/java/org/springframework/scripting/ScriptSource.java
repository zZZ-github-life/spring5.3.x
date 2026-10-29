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

package org.springframework.scripting;

import java.io.IOException;

import org.springframework.lang.Nullable;

/**
 * 定义脚本源的接口。跟踪底层脚本是否已被修改。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public interface ScriptSource {

	/**
	 * 检索当前的脚本源文本，以 String 形式返回。
	 * @return the script text
	 * @throws IOException if script retrieval failed
	 */
	String getScriptAsString() throws IOException;

	/**
	 * 指示自上次调用 {@link #getScriptAsString()} 以来底层脚本数据是否已被修改。如果尚未读取脚本，则返
	 * 回 {@code true}。
	 * @return whether the script data has been modified
	 */
	boolean isModified();

	/**
	 * 确定底层脚本的类名。
	 * @return the suggested class name, or {@code null} if none available
	 */
	@Nullable
	String suggestedClassName();

}
