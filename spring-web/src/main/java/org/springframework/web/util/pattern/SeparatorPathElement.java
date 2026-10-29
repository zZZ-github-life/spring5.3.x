/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.util.pattern;

import org.springframework.web.util.pattern.PathPattern.MatchingContext;

/**
 * 分隔符路径元素。在模式 '/foo/bar' 中，两处 '/'
 * 将由 SeparatorPathElement 表示（如果使用的是默认分隔符 '/'）。
 *
 * @author Andy Clement
 * @since 5.0
 */
class SeparatorPathElement extends PathElement {

	SeparatorPathElement(int pos, char separator) {
		super(pos, separator);
	}


	/**
	 * 匹配分隔符很简单，基本上就是 candidateIndex 处的字符
	 * 必须是分隔符。
	 */
	@Override
	public boolean matches(int pathIndex, MatchingContext matchingContext) {
		if (pathIndex < matchingContext.pathLength && matchingContext.isSeparator(pathIndex)) {
			if (isNoMorePattern()) {
				if (matchingContext.determineRemainingPath) {
					matchingContext.remainingPathIndex = pathIndex + 1;
					return true;
				}
				else {
					return (pathIndex + 1 == matchingContext.pathLength);
				}
			}
			else {
				pathIndex++;
				return (this.next != null && this.next.matches(pathIndex, matchingContext));
			}
		}
		return false;
	}

	@Override
	public int getNormalizedLength() {
		return 1;
	}

	@Override
	public char[] getChars() {
		return new char[] {this.separator};
	}

	@Override
	public boolean isLiteral() {
		return true;
	}

	@Override
	public String toString() {
		return "Separator(" + this.separator + ")";
	}

}
