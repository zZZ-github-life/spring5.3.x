/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.expression.common;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.ParserContext;
import org.springframework.lang.Nullable;

/**
 * 能够理解模板的表达式解析器。不提供一流模板支持的表达式解析器可以通过继承它来获得该能力。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Andy Clement
 * @since 3.0
 */
public abstract class TemplateAwareExpressionParser implements ExpressionParser {

	@Override
	public Expression parseExpression(String expressionString) throws ParseException {
		return parseExpression(expressionString, null);
	}

	@Override
	public Expression parseExpression(String expressionString, @Nullable ParserContext context) throws ParseException {
		if (context != null && context.isTemplate()) {
			return parseTemplate(expressionString, context);
		}
		else {
			return doParseExpression(expressionString, context);
		}
	}


	private Expression parseTemplate(String expressionString, ParserContext context) throws ParseException {
		if (expressionString.isEmpty()) {
			return new LiteralExpression("");
		}

		Expression[] expressions = parseExpressions(expressionString, context);
		if (expressions.length == 1) {
			return expressions[0];
		}
		else {
			return new CompositeStringExpression(expressionString, expressions);
		}
	}

	/**
	 * 使用所配置的解析器解析给定表达式字符串的辅助方法。表达式字符串可以包含任意数量的表达式，它们都放
	 * 在 "${...}" 标记中。例如："foo${expr0}bar${expr1}"。静态文本部分也会作为仅返回该静态文本的
	 * Expression 返回。因此，对返回的所有表达式求值并拼接结果，就能得到完整的求值后字符串。只对找到的
	 * 最外层分隔符进行解包，所以字符串 'hello ${foo${abc}}' 会被拆分为 'hello ' 和 'foo${abc}'。这意味
	 * 着将 ${..} 作为其功能一部分的表达式语言也能毫无问题地得到支持。解析过程会考虑内嵌表达式的结构。
	 * 它假定表达式中的圆括号 '('、方括号 '[' 和花括号 '}' 必须成对出现，除非它们位于字符串字面量中，而
	 * 字符串字面量以单引号 ' 开始和结束。
	 * @param expressionString 表达式字符串
	 * @return 解析得到的表达式
	 * @throws ParseException 当表达式无法解析时
	 */
	private Expression[] parseExpressions(String expressionString, ParserContext context) throws ParseException {
		List<Expression> expressions = new ArrayList<>();
		String prefix = context.getExpressionPrefix();
		String suffix = context.getExpressionSuffix();
		int startIdx = 0;

		while (startIdx < expressionString.length()) {
			int prefixIndex = expressionString.indexOf(prefix, startIdx);
			if (prefixIndex >= startIdx) {
				// an inner expression was found - this is a composite
				if (prefixIndex > startIdx) {
					expressions.add(new LiteralExpression(expressionString.substring(startIdx, prefixIndex)));
				}
				int afterPrefixIndex = prefixIndex + prefix.length();
				int suffixIndex = skipToCorrectEndSuffix(suffix, expressionString, afterPrefixIndex);
				if (suffixIndex == -1) {
					throw new ParseException(expressionString, prefixIndex,
							"No ending suffix '" + suffix + "' for expression starting at character " +
							prefixIndex + ": " + expressionString.substring(prefixIndex));
				}
				if (suffixIndex == afterPrefixIndex) {
					throw new ParseException(expressionString, prefixIndex,
							"No expression defined within delimiter '" + prefix + suffix +
							"' at character " + prefixIndex);
				}
				String expr = expressionString.substring(prefixIndex + prefix.length(), suffixIndex);
				expr = expr.trim();
				if (expr.isEmpty()) {
					throw new ParseException(expressionString, prefixIndex,
							"No expression defined within delimiter '" + prefix + suffix +
							"' at character " + prefixIndex);
				}
				expressions.add(doParseExpression(expr, context));
				startIdx = suffixIndex + suffix.length();
			}
			else {
				// no more ${expressions} found in string, add rest as static text
				expressions.add(new LiteralExpression(expressionString.substring(startIdx)));
				startIdx = expressionString.length();
			}
		}

		return expressions.toArray(new Expression[0]);
	}

	/**
	 * 如果能在提供的表达式字符串中的指定位置找到指定后缀，则返回 true。
	 * @param expressionString 可能包含该后缀的表达式字符串
	 * @param pos 检查该后缀的起始位置
	 * @param suffix 后缀字符串
	 */
	private boolean isSuffixHere(String expressionString, int pos, String suffix) {
		int suffixPosition = 0;
		for (int i = 0; i < suffix.length() && pos < expressionString.length(); i++) {
			if (expressionString.charAt(pos++) != suffix.charAt(suffixPosition++)) {
				return false;
			}
		}
		if (suffixPosition != suffix.length()) {
			// the expressionString ran out before the suffix could entirely be found
			return false;
		}
		return true;
	}

	/**
	 * 处理嵌套，例如 '${...${...}}'，其中第一个 ${ 的正确结束位置是最后的 }。
	 * @param suffix 后缀
	 * @param expressionString 表达式字符串
	 * @param afterPrefixIndex 最近找到的前缀位置，
	 * 正在为其寻找匹配的结束后缀
	 * @return 正确匹配的 nextSuffix 的位置，
	 * 如果找不到则为 -1
	 */
	private int skipToCorrectEndSuffix(String suffix, String expressionString, int afterPrefixIndex)
			throws ParseException {

		// Chew on the expression text - relying on the rules:
		// brackets must be in pairs: () [] {}
		// string literals are "..." or '...' and these may contain unmatched brackets
		int pos = afterPrefixIndex;
		int maxlen = expressionString.length();
		int nextSuffix = expressionString.indexOf(suffix, afterPrefixIndex);
		if (nextSuffix == -1) {
			return -1; // the suffix is missing
		}
		Deque<Bracket> stack = new ArrayDeque<>();
		while (pos < maxlen) {
			if (isSuffixHere(expressionString, pos, suffix) && stack.isEmpty()) {
				break;
			}
			char ch = expressionString.charAt(pos);
			switch (ch) {
				case '{':
				case '[':
				case '(':
					stack.push(new Bracket(ch, pos));
					break;
				case '}':
				case ']':
				case ')':
					if (stack.isEmpty()) {
						throw new ParseException(expressionString, pos, "Found closing '" + ch +
								"' at position " + pos + " without an opening '" +
								Bracket.theOpenBracketFor(ch) + "'");
					}
					Bracket p = stack.pop();
					if (!p.compatibleWithCloseBracket(ch)) {
						throw new ParseException(expressionString, pos, "Found closing '" + ch +
								"' at position " + pos + " but most recent opening is '" + p.bracket +
								"' at position " + p.pos);
					}
					break;
				case '\'':
				case '"':
					// jump to the end of the literal
					int endLiteral = expressionString.indexOf(ch, pos + 1);
					if (endLiteral == -1) {
						throw new ParseException(expressionString, pos,
								"Found non terminating string literal starting at position " + pos);
					}
					pos = endLiteral;
					break;
			}
			pos++;
		}
		if (!stack.isEmpty()) {
			Bracket p = stack.pop();
			throw new ParseException(expressionString, p.pos, "Missing closing '" +
					Bracket.theCloseBracketFor(p.bracket) + "' for '" + p.bracket + "' at position " + p.pos);
		}
		if (!isSuffixHere(expressionString, pos, suffix)) {
			return -1;
		}
		return pos;
	}


	/**
	 * 实际解析表达式字符串并返回 Expression 对象。
	 * @param expressionString 要解析的原始表达式字符串
	 * @param context 用于影响此表达式解析过程的上下文（可选）
	 * @return 用于已解析表达式的求值器
	 * @throws ParseException 解析过程中发生异常
	 */
	protected abstract Expression doParseExpression(String expressionString, @Nullable ParserContext context)
			throws ParseException;


	/**
	 * 用于捕获一种括号类型及其在表达式中出现的位置。当因为找不到相关的结束括号而需要报告错误时，会使用
	 * 该位置信息。Bracket 用于描述：方括号 []、圆括号 () 和花括号 {}。
	 */
	private static class Bracket {

		char bracket;

		int pos;

		Bracket(char bracket, int pos) {
			this.bracket = bracket;
			this.pos = pos;
		}

		boolean compatibleWithCloseBracket(char closeBracket) {
			if (this.bracket == '{') {
				return closeBracket == '}';
			}
			else if (this.bracket == '[') {
				return closeBracket == ']';
			}
			return closeBracket == ')';
		}

		static char theOpenBracketFor(char closeBracket) {
			if (closeBracket == '}') {
				return '{';
			}
			else if (closeBracket == ']') {
				return '[';
			}
			return '(';
		}

		static char theCloseBracketFor(char openBracket) {
			if (openBracket == '{') {
				return '}';
			}
			else if (openBracket == '[') {
				return ']';
			}
			return ')';
		}
	}

}
