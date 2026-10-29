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
	 * @param script the ScriptSource for the script to evaluate
	 * @return the return value of the script, if any
	 * @throws ScriptCompilationException if the evaluator failed to read,
	 * compile or evaluate the script
	 */
	@Nullable
	Object evaluate(ScriptSource script) throws ScriptCompilationException;

	/**
	 * 使用给定的参数评估给定的脚本。
	 * @param script the ScriptSource for the script to evaluate
	 * @param arguments the key-value pairs to expose to the script,
	 * typically as script variables (may be {@code null} or empty)
	 * @return the return value of the script, if any
	 * @throws ScriptCompilationException if the evaluator failed to read,
	 * compile or evaluate the script
	 */
	@Nullable
	Object evaluate(ScriptSource script, @Nullable Map<String, Object> arguments) throws ScriptCompilationException;

}
