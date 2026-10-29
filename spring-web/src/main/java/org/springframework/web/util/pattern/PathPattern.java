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

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.springframework.http.server.PathContainer;
import org.springframework.http.server.PathContainer.Element;
import org.springframework.http.server.PathContainer.Separator;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * 解析后的路径模式的表示。包含用于快速匹配的路径元素链，
 * 并累积计算得到的状态以便快速比较模式。
 *
 * <p>{@code PathPattern} 使用以下规则匹配 URL 路径：<br>
 * <ul>
 * <li>{@code ?} 匹配一个字符</li>
 * <li>{@code *} 匹配路径段内零个或多个字符</li>
 * <li>{@code **} 匹配零个或多个<em>路径段</em>，直到路径末尾</li>
 * <li><code>{spring}</code> 匹配一个<em>路径段</em>，并将其捕获为名为 "spring" 的变量</li>
 * <li><code>{spring:[a-z]+}</code> 将正则表达式 {@code [a-z]+} 匹配为名为 "spring" 的路径变量</li>
 * <li><code>{*spring}</code> 匹配零个或多个<em>路径段</em>，直到路径末尾，
 * 并将其捕获为名为 "spring" 的变量</li>
 * </ul>
 *
 * <p><strong>注意：</strong>与
 * {@link org.springframework.util.AntPathMatcher} 不同，{@code **} 仅支持
 * 出现在模式末尾。例如 {@code /pages/{**}} 是合法的，但
 * {@code /pages/{**}/details} 不合法。捕获变体
 * <code>{*spring}</code> 也是如此。这样做的目的是在比较模式的
 * 具体性时消除歧义。
 *
 * <h3>示例</h3>
 * <ul>
 * <li>{@code /pages/t?st.html} &mdash; 匹配 {@code /pages/test.html} 以及
 * {@code /pages/tXst.html}，但不匹配 {@code /pages/toast.html}</li>
 * <li>{@code /resources/*.png} &mdash; 匹配 {@code resources} 目录下的
 * 所有 {@code .png} 文件</li>
 * <li><code>/resources/&#42;&#42;</code> &mdash; 匹配
 * {@code /resources/} 路径下的所有文件，包括 {@code /resources/image.png}
 * 和 {@code /resources/css/spring.css}</li>
 * <li><code>/resources/{&#42;path}</code> &mdash; 匹配
 * {@code /resources/} 下的所有文件以及 {@code /resources}，并将
 * 其相对路径捕获到名为 "path" 的变量中；{@code /resources/image.png}
 * 将以 "path" &rarr; "/image.png" 匹配，而 {@code /resources/css/spring.css}
 * 将以 "path" &rarr; "/css/spring.css" 匹配</li>
 * <li><code>/resources/{filename:\\w+}.dat</code> 将匹配 {@code /resources/spring.dat}，
 * 并将值 {@code "spring"} 赋给 {@code filename} 变量</li>
 * </ul>
 *
 * @author Andy Clement
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see PathContainer
 */
public class PathPattern implements Comparable<PathPattern> {

	private static final PathContainer EMPTY_PATH = PathContainer.parsePath("");

	/**
	 * 按如下具体性规则对模式进行排序的比较器：
	 * <ol>
	 * <li>null 实例排最后。
	 * <li>捕获所有内容的模式排最后。
	 * <li>如果两个模式都捕获所有内容，则考虑长度（更长者优先）。
	 * <li>比较通配符和捕获变量数量（更少者优先）。
	 * <li>考虑长度（更长者优先）
	 * </ol>
	 */
	public static final Comparator<PathPattern> SPECIFICITY_COMPARATOR =
			Comparator.nullsLast(
					Comparator.<PathPattern>
							comparingInt(p -> p.isCatchAll() ? 1 : 0)
							.thenComparingInt(p -> p.isCatchAll() ? scoreByNormalizedLength(p) : 0)
							.thenComparingInt(PathPattern::getScore)
							.thenComparingInt(PathPattern::scoreByNormalizedLength)
			);


	/** 解析后的模式文本。 */
	private final String patternString;

	/** 用于构造此模式的解析器。 */
	private final PathPatternParser parser;

	/** 解析模式时使用的选项。 */
	private final PathContainer.Options pathOptions;

	/** 如果此模式没有末尾斜杠，则允许候选路径包含末尾斜杠并仍成功匹配。 */
	private final boolean matchOptionalTrailingSeparator;

	/** 是否以区分大小写的方式匹配候选路径？（区分大小写设置于解析时）。 */
	private final boolean caseSensitive;

	/** 此模式解析后的路径元素链中的第一个路径元素。 */
	@Nullable
	private final PathElement head;

	/** 此模式中捕获的变量数量。 */
	private int capturedVariableCount;

	/**
	 * 规范化长度试图度量模式的“活动”部分。它假定所有捕获变量的
	 * 规范化长度均为 1 来计算。实际上，这意味着更改变量名的长度
	 * 不会改变模式活动部分的长度。在比较两个模式时很有用。
	 */
	private int normalizedLength;

	/**
	 * 模式是否以 '&lt;separator&gt;' 结尾。
	 */
	private boolean endsWithSeparatorWildcard = false;

	/**
	 * 分数用于快速比较模式。不同的模式组成部分被赋予不同的
	 * 权重。“分数越低”越具体。当前权重：
	 * <ul>
	 * <li>捕获变量权重为 1
	 * <li>通配符权重为 100
	 * </ul>
	 */
	private int score;

	/** 模式是否以 {*...} 结尾。 */
	private boolean catchAll = false;


	PathPattern(String patternText, PathPatternParser parser, @Nullable PathElement head) {
		this.patternString = patternText;
		this.parser = parser;
		this.pathOptions = parser.getPathOptions();
		this.matchOptionalTrailingSeparator = parser.isMatchOptionalTrailingSeparator();
		this.caseSensitive = parser.isCaseSensitive();
		this.head = head;

		// Compute fields for fast comparison
		PathElement elem = head;
		while (elem != null) {
			this.capturedVariableCount += elem.getCaptureCount();
			this.normalizedLength += elem.getNormalizedLength();
			this.score += elem.getScore();
			if (elem instanceof CaptureTheRestPathElement || elem instanceof WildcardTheRestPathElement) {
				this.catchAll = true;
			}
			if (elem instanceof SeparatorPathElement && elem.next instanceof WildcardPathElement && elem.next.next == null) {
				this.endsWithSeparatorWildcard = true;
			}
			elem = elem.next;
		}
	}


	/**
	 * 返回解析以创建此 PathPattern 的原始字符串。
	 */
	public String getPatternString() {
		return this.patternString;
	}

	/**
	 * 模式字符串是否包含需要使用 {@link #matches(PathContainer)} 的模式语法，
	 * 或者它是否是可以直接与其他字符串比较的普通字符串。
	 * @since 5.2
	 */
	public boolean hasPatternSyntax() {
		return (this.score > 0 || this.catchAll || this.patternString.indexOf('?') != -1);
	}

	/**
	 * 此模式是否匹配给定路径。
	 * @param pathContainer the candidate path to attempt to match against
	 * @return {@code true} if the path matches this pattern
	 */
	public boolean matches(PathContainer pathContainer) {
		if (this.head == null) {
			return !hasLength(pathContainer) ||
				(this.matchOptionalTrailingSeparator && pathContainerIsJustSeparator(pathContainer));
		}
		else if (!hasLength(pathContainer)) {
			if (this.head instanceof WildcardTheRestPathElement || this.head instanceof CaptureTheRestPathElement) {
				pathContainer = EMPTY_PATH; // Will allow CaptureTheRest to bind the variable to empty
			}
			else {
				return false;
			}
		}
		MatchingContext matchingContext = new MatchingContext(pathContainer, false);
		return this.head.matches(0, matchingContext);
	}

	/**
	 * 将此模式与给定的 URI 路径匹配，并返回提取的 URI 模板变量
	 * 以及路径参数（矩阵变量）。
	 * @param pathContainer the candidate path to attempt to match against
	 * @return info object with the extracted variables, or {@code null} for no match
	 */
	@Nullable
	public PathMatchInfo matchAndExtract(PathContainer pathContainer) {
		if (this.head == null) {
			return (hasLength(pathContainer) &&
					!(this.matchOptionalTrailingSeparator && pathContainerIsJustSeparator(pathContainer)) ?
					null : PathMatchInfo.EMPTY);
		}
		else if (!hasLength(pathContainer)) {
			if (this.head instanceof WildcardTheRestPathElement || this.head instanceof CaptureTheRestPathElement) {
				pathContainer = EMPTY_PATH; // Will allow CaptureTheRest to bind the variable to empty
			}
			else {
				return null;
			}
		}
		MatchingContext matchingContext = new MatchingContext(pathContainer, true);
		return this.head.matches(0, matchingContext) ? matchingContext.getPathMatchResult() : null;
	}

	/**
	 * 匹配给定路径的开头，并返回此模式未覆盖的剩余部分。
	 * 这对于匹配嵌套路由很有用，因为路径会在每一层级逐步匹配。
	 * @param pathContainer the candidate path to attempt to match against
	 * @return info object with the match result or {@code null} for no match
	 */
	@Nullable
	public PathRemainingMatchInfo matchStartOfPath(PathContainer pathContainer) {
		if (this.head == null) {
			return new PathRemainingMatchInfo(EMPTY_PATH, pathContainer);
		}
		else if (!hasLength(pathContainer)) {
			return null;
		}

		MatchingContext matchingContext = new MatchingContext(pathContainer, true);
		matchingContext.setMatchAllowExtraPath();
		boolean matches = this.head.matches(0, matchingContext);
		if (!matches) {
			return null;
		}
		else {
			PathContainer pathMatched;
			PathContainer pathRemaining;
			if (matchingContext.remainingPathIndex == pathContainer.elements().size()) {
				pathMatched = pathContainer;
				pathRemaining = EMPTY_PATH;
			}
			else {
				pathMatched = pathContainer.subPath(0, matchingContext.remainingPathIndex);
				pathRemaining = pathContainer.subPath(matchingContext.remainingPathIndex);
			}
			return new PathRemainingMatchInfo(pathMatched, pathRemaining, matchingContext.getPathMatchResult());
		}
	}

	/**
	 * 确定给定路径中由模式映射的部分。
	 * <p>例如：<ul>
	 * <li>'{@code /docs/cvs/commit.html}' 和 '{@code /docs/cvs/commit.html} &rarr; ''</li>
	 * <li>'{@code /docs/*}' 和 '{@code /docs/cvs/commit}' &rarr; '{@code cvs/commit}'</li>
	 * <li>'{@code /docs/cvs/*.html}' 和 '{@code /docs/cvs/commit.html} &rarr; '{@code commit.html}'</li>
	 * <li>'{@code /docs/**}' 和 '{@code /docs/cvs/commit} &rarr; '{@code cvs/commit}'</li>
	 * </ul>
	 * <p><b>说明：</b>
	 * <ul>
	 * <li>假定 {@link #matches} 对同一路径返回 {@code true}，但
	 * <strong>不会</strong>强制校验这一点。
	 * <li>返回结果中重复出现的分隔符会被移除
	 * <li>返回结果中开头和末尾的分隔符会被移除
	 * </ul>
	 * @param path a path that matches this pattern
	 * @return the subset of the path that is matched by pattern or "" if none
	 * of it is matched by pattern elements
	 */
	public PathContainer extractPathWithinPattern(PathContainer path) {
		List<Element> pathElements = path.elements();
		int pathElementsCount = pathElements.size();

		int startIndex = 0;
		// Find first path element that is not a separator or a literal (i.e. the first pattern based element)
		PathElement elem = this.head;
		while (elem != null) {
			if (!elem.isLiteral()) {
				break;
			}
			elem = elem.next;
			startIndex++;
		}
		if (elem == null) {
			// There is no pattern piece
			return PathContainer.parsePath("");
		}

		// Skip leading separators that would be in the result
		while (startIndex < pathElementsCount && (pathElements.get(startIndex) instanceof Separator)) {
			startIndex++;
		}

		int endIndex = pathElements.size();
		// Skip trailing separators that would be in the result
		while (endIndex > 0 && (pathElements.get(endIndex - 1) instanceof Separator)) {
			endIndex--;
		}

		boolean multipleAdjacentSeparators = false;
		for (int i = startIndex; i < (endIndex - 1); i++) {
			if ((pathElements.get(i) instanceof Separator) && (pathElements.get(i+1) instanceof Separator)) {
				multipleAdjacentSeparators=true;
				break;
			}
		}

		PathContainer resultPath = null;
		if (multipleAdjacentSeparators) {
			// Need to rebuild the path without the duplicate adjacent separators
			StringBuilder sb = new StringBuilder();
			int i = startIndex;
			while (i < endIndex) {
				Element e = pathElements.get(i++);
				sb.append(e.value());
				if (e instanceof Separator) {
					while (i < endIndex && (pathElements.get(i) instanceof Separator)) {
						i++;
					}
				}
			}
			resultPath = PathContainer.parsePath(sb.toString(), this.pathOptions);
		}
		else if (startIndex >= endIndex) {
			resultPath = PathContainer.parsePath("");
		}
		else {
			resultPath = path.subPath(startIndex, endIndex);
		}
		return resultPath;
	}

	/**
	 * 将此模式与提供的模式进行比较：如果此模式比提供的模式更具体、
	 * 相同或更不具体，则分别返回 -1、0、+1。目标是让更具体的模式排在最前面。
	 */
	@Override
	public int compareTo(@Nullable PathPattern otherPattern) {
		int result = SPECIFICITY_COMPARATOR.compare(this, otherPattern);
		return (result == 0 && otherPattern != null ?
				this.patternString.compareTo(otherPattern.patternString) : result);
	}

	/**
	 * 将此模式与另一个模式组合。
	 */
	public PathPattern combine(PathPattern pattern2string) {
		// If one of them is empty the result is the other. If both empty the result is ""
		if (!StringUtils.hasLength(this.patternString)) {
			if (!StringUtils.hasLength(pattern2string.patternString)) {
				return this.parser.parse("");
			}
			else {
				return pattern2string;
			}
		}
		else if (!StringUtils.hasLength(pattern2string.patternString)) {
			return this;
		}

		// /* + /hotel => /hotel
		// /*.* + /*.html => /*.html
		// However:
		// /usr + /user => /usr/user
		// /{foo} + /bar => /{foo}/bar
		if (!this.patternString.equals(pattern2string.patternString) && this.capturedVariableCount == 0 &&
				matches(PathContainer.parsePath(pattern2string.patternString))) {
			return pattern2string;
		}

		// /hotels/* + /booking => /hotels/booking
		// /hotels/* + booking => /hotels/booking
		if (this.endsWithSeparatorWildcard) {
			return this.parser.parse(concat(
					this.patternString.substring(0, this.patternString.length() - 2),
					pattern2string.patternString));
		}

		// /hotels + /booking => /hotels/booking
		// /hotels + booking => /hotels/booking
		int starDotPos1 = this.patternString.indexOf("*.");  // Are there any file prefix/suffix things to consider?
		if (this.capturedVariableCount != 0 || starDotPos1 == -1 || getSeparator() == '.') {
			return this.parser.parse(concat(this.patternString, pattern2string.patternString));
		}

		// /*.html + /hotel => /hotel.html
		// /*.html + /hotel.* => /hotel.html
		String firstExtension = this.patternString.substring(starDotPos1 + 1);  // looking for the first extension
		String p2string = pattern2string.patternString;
		int dotPos2 = p2string.indexOf('.');
		String file2 = (dotPos2 == -1 ? p2string : p2string.substring(0, dotPos2));
		String secondExtension = (dotPos2 == -1 ? "" : p2string.substring(dotPos2));
		boolean firstExtensionWild = (firstExtension.equals(".*") || firstExtension.isEmpty());
		boolean secondExtensionWild = (secondExtension.equals(".*") || secondExtension.isEmpty());
		if (!firstExtensionWild && !secondExtensionWild) {
			throw new IllegalArgumentException(
					"Cannot combine patterns: " + this.patternString + " and " + pattern2string);
		}
		return this.parser.parse(file2 + (firstExtensionWild ? secondExtension : firstExtension));
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (!(other instanceof PathPattern)) {
			return false;
		}
		PathPattern otherPattern = (PathPattern) other;
		return (this.patternString.equals(otherPattern.getPatternString()) &&
				getSeparator() == otherPattern.getSeparator() &&
				this.caseSensitive == otherPattern.caseSensitive);
	}

	@Override
	public int hashCode() {
		return (this.patternString.hashCode() + getSeparator()) * 17 + (this.caseSensitive ? 1 : 0);
	}

	@Override
	public String toString() {
		return this.patternString;
	}


	int getScore() {
		return this.score;
	}

	boolean isCatchAll() {
		return this.catchAll;
	}

	/**
	 * 规范化长度试图度量模式的“活动”部分。它假定所有捕获变量
	 * 的规范化长度均为 1 来计算。实际上，这意味着更改变量名的长度
	 * 不会改变模式活动部分的长度。在比较两个模式时很有用。
	 */
	int getNormalizedLength() {
		return this.normalizedLength;
	}

	char getSeparator() {
		return this.pathOptions.separator();
	}

	int getCapturedVariableCount() {
		return this.capturedVariableCount;
	}

	String toChainString() {
		StringJoiner stringJoiner = new StringJoiner(" ");
		PathElement pe = this.head;
		while (pe != null) {
			stringJoiner.add(pe.toString());
			pe = pe.next;
		}
		return stringJoiner.toString();
	}

	/**
	 * 返回通过遍历路径元素链构建的模式字符串形式。
	 * @return the string form of the pattern
	 */
	String computePatternString() {
		StringBuilder sb = new StringBuilder();
		PathElement pe = this.head;
		while (pe != null) {
			sb.append(pe.getChars());
			pe = pe.next;
		}
		return sb.toString();
	}

	@Nullable
	PathElement getHeadSection() {
		return this.head;
	}

	/**
	 * 将两个路径连接在一起，必要时包含分隔符。
	 * 多余的分隔符会被移除（如果第一个路径以分隔符结尾
	 * 且第二个路径以分隔符开头）。
	 * @param path1 first path
	 * @param path2 second path
	 * @return joined path that may include separator if necessary
	 */
	private String concat(String path1, String path2) {
		boolean path1EndsWithSeparator = (path1.charAt(path1.length() - 1) == getSeparator());
		boolean path2StartsWithSeparator = (path2.charAt(0) == getSeparator());
		if (path1EndsWithSeparator && path2StartsWithSeparator) {
			return path1 + path2.substring(1);
		}
		else if (path1EndsWithSeparator || path2StartsWithSeparator) {
			return path1 + path2;
		}
		else {
			return path1 + getSeparator() + path2;
		}
	}

	/**
	 * 返回容器是否非 null 且元素数大于零。
	 * @param container a path container
	 * @return {@code true} has more than zero elements
	 */
	private boolean hasLength(@Nullable PathContainer container) {
		return container != null && container.elements().size() > 0;
	}

	private static int scoreByNormalizedLength(PathPattern pattern) {
		return -pattern.getNormalizedLength();
	}

	private boolean pathContainerIsJustSeparator(PathContainer pathContainer) {
		return pathContainer.value().length() == 1 &&
				pathContainer.value().charAt(0) == getSeparator();
	}


	/**
	 * 用于保存基于模式为给定匹配路径提取的 URI 变量和
	 * 路径参数（矩阵变量）的持有者。
	 */
	public static class PathMatchInfo {

		private static final PathMatchInfo EMPTY = new PathMatchInfo(Collections.emptyMap(), Collections.emptyMap());

		private final Map<String, String> uriVariables;

		private final Map<String, MultiValueMap<String, String>> matrixVariables;

		PathMatchInfo(Map<String, String> uriVars, @Nullable Map<String, MultiValueMap<String, String>> matrixVars) {
			this.uriVariables = Collections.unmodifiableMap(uriVars);
			this.matrixVariables = (matrixVars != null ?
					Collections.unmodifiableMap(matrixVars) : Collections.emptyMap());
		}

		/**
		 * 返回提取的 URI 变量。
		 */
		public Map<String, String> getUriVariables() {
			return this.uriVariables;
		}

		/**
		 * 返回按路径段划分的矩阵变量映射，以 URI
		 * 变量名作为键。
		 */
		public Map<String, MultiValueMap<String, String>> getMatrixVariables() {
			return this.matrixVariables;
		}

		@Override
		public String toString() {
			return "PathMatchInfo[uriVariables=" + this.uriVariables + ", " +
					"matrixVariables=" + this.matrixVariables + "]";
		}
	}


	/**
	 * 保存模式开头匹配结果的持有者。
	 * 提供对未与该模式匹配的剩余路径的访问，以及在已匹配的
	 * 第一部分中绑定的任何变量。
	 */
	public static class PathRemainingMatchInfo {

		private final PathContainer pathMatched;

		private final PathContainer pathRemaining;

		private final PathMatchInfo pathMatchInfo;


		PathRemainingMatchInfo(PathContainer pathMatched, PathContainer pathRemaining) {
			this(pathMatched, pathRemaining, PathMatchInfo.EMPTY);
		}

		PathRemainingMatchInfo(PathContainer pathMatched, PathContainer pathRemaining,
				PathMatchInfo pathMatchInfo) {
			this.pathRemaining = pathRemaining;
			this.pathMatched = pathMatched;
			this.pathMatchInfo = pathMatchInfo;
		}

		/**
		 * 返回路径中由模式匹配的部分。
		 * @since 5.3
		 */
		public PathContainer getPathMatched() {
			return this.pathMatched;
		}

		/**
		 * 返回路径中未由模式匹配的部分。
		 */
		public PathContainer getPathRemaining() {
			return this.pathRemaining;
		}

		/**
		 * 返回在成功匹配的路径部分中绑定的变量，或空映射。
		 */
		public Map<String, String> getUriVariables() {
			return this.pathMatchInfo.getUriVariables();
		}

		/**
		 * 返回每个已绑定变量的路径参数。
		 */
		public Map<String, MultiValueMap<String, String>> getMatrixVariables() {
			return this.pathMatchInfo.getMatrixVariables();
		}
	}


	/**
	 * 封装尝试匹配时的上下文。包含一些固定状态（例如当前正在
	 * 考虑匹配的候选路径），还包含一些用于提取变量的累加器。
	 */
	class MatchingContext {

		final PathContainer candidate;

		final List<Element> pathElements;

		final int pathLength;

		@Nullable
		private Map<String, String> extractedUriVariables;

		@Nullable
		private Map<String, MultiValueMap<String, String>> extractedMatrixVariables;

		boolean extractingVariables;

		boolean determineRemainingPath = false;

		// if determineRemaining is true, this is set to the position in
		// the candidate where the pattern finished matching - i.e. it
		// points to the remaining path that wasn't consumed
		int remainingPathIndex;

		public MatchingContext(PathContainer pathContainer, boolean extractVariables) {
			this.candidate = pathContainer;
			this.pathElements = pathContainer.elements();
			this.pathLength = this.pathElements.size();
			this.extractingVariables = extractVariables;
		}

		public void setMatchAllowExtraPath() {
			this.determineRemainingPath = true;
		}

		public boolean isMatchOptionalTrailingSeparator() {
			return matchOptionalTrailingSeparator;
		}

		public void set(String key, String value, MultiValueMap<String,String> parameters) {
			if (this.extractedUriVariables == null) {
				this.extractedUriVariables = new HashMap<>();
			}
			this.extractedUriVariables.put(key, value);

			if (!parameters.isEmpty()) {
				if (this.extractedMatrixVariables == null) {
					this.extractedMatrixVariables = new HashMap<>();
				}
				this.extractedMatrixVariables.put(key, CollectionUtils.unmodifiableMultiValueMap(parameters));
			}
		}

		public PathMatchInfo getPathMatchResult() {
			if (this.extractedUriVariables == null) {
				return PathMatchInfo.EMPTY;
			}
			else {
				return new PathMatchInfo(this.extractedUriVariables, this.extractedMatrixVariables);
			}
		}

		/**
		 * 返回指定索引处的元素是否为分隔符。
		 * @param pathIndex possible index of a separator
		 * @return {@code true} if element is a separator
		 */
		boolean isSeparator(int pathIndex) {
			return this.pathElements.get(pathIndex) instanceof Separator;
		}

		/**
		 * 返回指定元素的解码值。
		 * @param pathIndex path element index
		 * @return the decoded value
		 */
		String pathElementValue(int pathIndex) {
			Element element = (pathIndex < this.pathLength) ? this.pathElements.get(pathIndex) : null;
			if (element instanceof PathContainer.PathSegment) {
				return ((PathContainer.PathSegment)element).valueToMatch();
			}
			return "";
		}
	}

}
