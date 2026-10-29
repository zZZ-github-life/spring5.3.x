/*
 * Copyright 2002-2021 the original author or authors.
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

import org.springframework.http.server.PathContainer;
import org.springframework.http.server.PathContainer.Element;
import org.springframework.web.util.pattern.PathPattern.MatchingContext;

/**
 * 通配符路径元素。在模式 '/foo/&ast;/goo' 中，* 由
 * WildcardPathElement 表示。在路径中间，它至少匹配一个字符；
 * 但在路径末尾，它可以匹配零个字符。
 *
 * @author Andy Clement
 * @since 5.0
 */
class WildcardPathElement extends PathElement {

	public WildcardPathElement(int pos, char separator) {
		super(pos, separator);
	}


	/**
	 * 对 WildcardPathElement 的匹配非常直接。从 candidateIndex 开始
	 * 扫描候选路径，查找下一个分隔符或候选路径的末尾。
	 */
	@Override
	public boolean matches(int pathIndex, MatchingContext matchingContext) {
		String segmentData = null;
		// Assert if it exists it is a segment
		if (pathIndex < matchingContext.pathLength) {
			Element element = matchingContext.pathElements.get(pathIndex);
			if (!(element instanceof PathContainer.PathSegment)) {
				// Should not match a separator
				return false;
			}
			segmentData = ((PathContainer.PathSegment)element).valueToMatch();
			pathIndex++;
		}

		if (isNoMorePattern()) {
			if (matchingContext.determineRemainingPath) {
				matchingContext.remainingPathIndex = pathIndex;
				return true;
			}
			else {
				if (pathIndex == matchingContext.pathLength) {
					// and the path data has run out too
					return true;
				}
				else {
					return (matchingContext.isMatchOptionalTrailingSeparator() &&  // if optional slash is on...
							segmentData != null && segmentData.length() > 0 &&  // and there is at least one character to match the *...
							(pathIndex + 1) == matchingContext.pathLength &&   // and the next path element is the end of the candidate...
							matchingContext.isSeparator(pathIndex));  // and the final element is a separator
				}
			}
		}
		else {
			// Within a path (e.g. /aa/*/bb) there must be at least one character to match the wildcard
			if (segmentData == null || segmentData.length() == 0) {
				return false;
			}
			return (this.next != null && this.next.matches(pathIndex, matchingContext));
		}
	}

	@Override
	public int getNormalizedLength() {
		return 1;
	}

	@Override
	public char[] getChars() {
		return new char[] {'*'};
	}

	@Override
	public int getWildcardCount() {
		return 1;
	}

	@Override
	public int getScore() {
		return WILDCARD_WEIGHT;
	}


	@Override
	public String toString() {
		return "Wildcard(*)";
	}

}
