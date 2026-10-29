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

import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.pattern.PathPattern.MatchingContext;

/**
 * 用于表示路径模式的 AST 节点的公共父类型。
 *
 * @author Andy Clement
 * @author Brian Clozel
 * @since 5.0
 */
abstract class PathElement {

	// Score related
	protected static final int WILDCARD_WEIGHT = 100;

	protected static final int CAPTURE_VARIABLE_WEIGHT = 1;

	protected static final MultiValueMap<String,String> NO_PARAMETERS = new LinkedMultiValueMap<>();

	// Position in the pattern where this path element starts
	protected final int pos;

	// The separator used in this path pattern
	protected final char separator;

	// The next path element in the chain
	@Nullable
	protected PathElement next;

	// The previous path element in the chain
	@Nullable
	protected PathElement prev;


	/**
	 * 创建一个新的路径元素。
	 * @param pos the position where this path element starts in the pattern data
	 * @param separator the separator in use in the path pattern
	 */
	PathElement(int pos, char separator) {
		this.pos = pos;
		this.separator = separator;
	}


	/**
	 * 尝试匹配此路径元素。
	 * @param candidatePos the current position within the candidate path
	 * @param matchingContext encapsulates context for the match including the candidate
	 * @return {@code true} if it matches, otherwise {@code false}
	 */
	public abstract boolean matches(int candidatePos, MatchingContext matchingContext);

	/**
	 * 返回路径元素的长度，其中捕获部分按一个字符计算。
	 * @return the normalized length
	 */
	public abstract int getNormalizedLength();

	public abstract char[] getChars();

	/**
	 * 返回此路径元素捕获的变量数量。
	 */
	public int getCaptureCount() {
		return 0;
	}

	/**
	 * 返回路径元素中通配符元素（*、?）的数量。
	 */
	public int getWildcardCount() {
		return 0;
	}

	/**
	 * 返回此 PathElement 的分数，组合分数用于比较解析后的模式。
	 */
	public int getScore() {
		return 0;
	}

	/**
	 * 返回此 PathElement 是否可以与另一个元素进行严格的
	 * {@link String#compareTo(String) 比较}以用于匹配。
	 */
	public boolean isLiteral() {
		return false;
	}

	/**
	 * 返回模式中是否已没有更多 PathElement。
	 * @return {@code true} if the there are no more elements
	 */
	protected final boolean isNoMorePattern() {
		return this.next == null;
	}

}
