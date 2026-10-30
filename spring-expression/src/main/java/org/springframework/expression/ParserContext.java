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

package org.springframework.expression;

/**
 * 提供给表达式解析器的输入，可影响表达式的解析/编译流程。
 *
 * @author Keith Donald
 * @author Andy Clement
 * @since 3.0
 */
public interface ParserContext {

	/**
	 * 正在解析的表达式是否为模板。模板表达式由可与可求值块混合的字面文本组成。一些示例：
	 * <pre class="code">
	 * 	   Some literal text
	 *     Hello #{name.firstName}!
	 *     #{3 + 4}
	 * </pre>
	 * @return 如果表达式是模板则返回 true，否则返回 false
	 */
	boolean isTemplate();

	/**
	 * 对于模板表达式，返回用于标识字符串中表达式块起始位置的前缀。例如："${"
	 * @return 标识表达式起始位置的前缀
	 */
	String getExpressionPrefix();

	/**
	 * 对于模板表达式，返回用于标识字符串中表达式块结束位置的前缀。例如："}"
	 * @return 标识表达式结束位置的后缀
	 */
	String getExpressionSuffix();


	/**
	 * 默认的 ParserContext 实现，启用模板表达式解析模式。表达式前缀为 "#{"，表达式后缀为 "}"。
	 * @see #isTemplate()
	 */
	ParserContext TEMPLATE_EXPRESSION = new ParserContext() {

		@Override
		public boolean isTemplate() {
			return true;
		}

		@Override
		public String getExpressionPrefix() {
			return "#{";
		}

		@Override
		public String getExpressionSuffix() {
			return "}";
		}
	};

}
