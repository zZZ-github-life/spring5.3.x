/*
 * Copyright 2002-2015 the original author or authors.
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

import java.util.Map;

import org.springframework.lang.Nullable;

/**
 * Spring 用于评估脚本的策略接口。
 *
 * <p>除了特定语言的实现之外，Spring 还附带了一个基于标准 {@code javax.script} 包（JSR-223）的版本：
 * {@link org.springframework.scripting.support.StandardScriptEvaluator}。
 *
 * @author Juergen Hoeller
 * @author Costin Leau
 * @since 4.0
 */
public interface ScriptEvaluator {

	/**
	 * 评估给定的脚本。
	 * @param script 要评估脚本的 ScriptSource
	 * @return 脚本的返回值（如果有）
	 * @throws ScriptCompilationException 如果评估器读取、编译或评估脚本失败
	 */
	@Nullable
	Object evaluate(ScriptSource script) throws ScriptCompilationException;

	/**
	 * 使用给定的参数评估给定的脚本。
	 * @param script 要评估脚本的 ScriptSource
	 * @param arguments 要暴露给脚本的键值对，通常作为脚本变量
	 * （可为 {@code null} 或为空）
	 * @return 脚本的返回值（如果有）
	 * @throws ScriptCompilationException 如果评估器读取、编译或评估脚本失败
	 */
	@Nullable
	Object evaluate(ScriptSource script, @Nullable Map<String, Object> arguments) throws ScriptCompilationException;

}
