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

package org.springframework.util;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.StringJoiner;
import java.util.StringTokenizer;
import java.util.TimeZone;

import org.springframework.lang.Nullable;

/**
 * 杂项 {@link String} 实用方法。
 *
 * <p>主要用于框架内部使用；如需更全面的 {@code String} 实用工具套件，请考虑使用
 * <a href="https://commons.apache.org/proper/commons-lang/">Apache 的 Commons Lang</a>。
 *
 * <p>此类提供了一些本应由核心 Java {@link String} 和 {@link StringBuilder} 类提供的简单功能。它还
 * 提供了易用的方法，用于在有分隔符的字符串（如 CSV 字符串）与集合和数组之间进行转换。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Keith Donald
 * @author Rob Harrop
 * @author Rick Evans
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @author Brian Clozel
 * @since 16 April 2001
 */
public abstract class StringUtils {

	private static final String[] EMPTY_STRING_ARRAY = {};

	private static final String FOLDER_SEPARATOR = "/";

	private static final char FOLDER_SEPARATOR_CHAR = '/';

	private static final String WINDOWS_FOLDER_SEPARATOR = "\\";

	private static final String TOP_PATH = "..";

	private static final String CURRENT_PATH = ".";

	private static final char EXTENSION_SEPARATOR = '.';

	private static final int DEFAULT_TRUNCATION_THRESHOLD = 100;

	private static final String TRUNCATION_SUFFIX = " (truncated)...";


	//---------------------------------------------------------------------
	// General convenience methods for working with Strings
	//---------------------------------------------------------------------

	/**
	 * 检查给定对象（可能是 {@code String}）是否为空。
	 * 这实际上是 {@code !hasLength(String)} 的快捷方式。
	 * <p>此方法接受任意 Object 作为参数，并将其与 {@code null} 和空 String 进行比较。因此，对于非 null
	 * 的非 String 对象，此方法将永远不会返回 {@code true}。
	 * <p>Object 签名适用于通用属性处理代码，这些代码通常处理 Strings，但一般必须遍历 Objects，因为属性
	 * 也可能例如是原始值对象。
	 * <p><b>注意：如果该对象事先已确定为 {@code String} 类型，请优先使用 {@link #hasLength(String)} 或
	 * {@link #hasText(String)}。</b>
	 * @param str the candidate object (possibly a {@code String})
	 * @since 3.2.1
	 * @deprecated as of 5.3, in favor of {@link #hasLength(String)} and
	 * {@link #hasText(String)} (or {@link ObjectUtils#isEmpty(Object)})
	 */
	@Deprecated
	public static boolean isEmpty(@Nullable Object str) {
		return (str == null || "".equals(str));
	}

	/**
	 * 检查给定的 {@code CharSequence} 既不是 {@code null}，长度也不为 0。
	 * <p>注意：对于完全由空白字符组成的 {@code CharSequence}，此方法返回 {@code true}。
	 * <p><pre class="code">
	 * StringUtils.hasLength(null) = false
	 * StringUtils.hasLength("") = false
	 * StringUtils.hasLength(" ") = true
	 * StringUtils.hasLength("Hello") = true
	 * </pre>
	 * @param str the {@code CharSequence} to check (may be {@code null})
	 * @return {@code true} if the {@code CharSequence} is not {@code null} and has length
	 * @see #hasLength(String)
	 * @see #hasText(CharSequence)
	 */
	public static boolean hasLength(@Nullable CharSequence str) {
		return (str != null && str.length() > 0);
	}

	/**
	 * 检查给定的 {@code String} 既不是 {@code null}，长度也不为 0。
	 * <p>注意：对于完全由空白字符组成的 {@code String}，此方法返回 {@code true}。
	 * @param str the {@code String} to check (may be {@code null})
	 * @return {@code true} if the {@code String} is not {@code null} and has length
	 * @see #hasLength(CharSequence)
	 * @see #hasText(String)
	 */
	public static boolean hasLength(@Nullable String str) {
		return (str != null && !str.isEmpty());
	}

	/**
	 * 检查给定的 {@code CharSequence} 是否包含实际的<em>文本</em>。
	 * <p>更具体地说，如果 {@code CharSequence} 不为 {@code null}、其长度大于 0，且包含至少一个非空白字
	 * 符，则此方法返回 {@code true}。
	 * <p><pre class="code">
	 * StringUtils.hasText(null) = false
	 * StringUtils.hasText("") = false
	 * StringUtils.hasText(" ") = false
	 * StringUtils.hasText("12345") = true
	 * StringUtils.hasText(" 12345 ") = true
	 * </pre>
	 * @param str the {@code CharSequence} to check (may be {@code null})
	 * @return {@code true} if the {@code CharSequence} is not {@code null},
	 * its length is greater than 0, and it does not contain whitespace only
	 * @see #hasText(String)
	 * @see #hasLength(CharSequence)
	 * @see Character#isWhitespace
	 */
	public static boolean hasText(@Nullable CharSequence str) {
		return (str != null && str.length() > 0 && containsText(str));
	}

	/**
	 * 检查给定的 {@code String} 是否包含实际的<em>文本</em>。
	 * <p>更具体地说，如果 {@code String} 不为 {@code null}、其长度大于 0，且包含至少一个非空白字符，则
	 * 此方法返回 {@code true}。
	 * @param str the {@code String} to check (may be {@code null})
	 * @return {@code true} if the {@code String} is not {@code null}, its
	 * length is greater than 0, and it does not contain whitespace only
	 * @see #hasText(CharSequence)
	 * @see #hasLength(String)
	 * @see Character#isWhitespace
	 */
	public static boolean hasText(@Nullable String str) {
		return (str != null && !str.isEmpty() && containsText(str));
	}

	private static boolean containsText(CharSequence str) {
		int strLen = str.length();
		for (int i = 0; i < strLen; i++) {
			if (!Character.isWhitespace(str.charAt(i))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 检查给定的 {@code CharSequence} 是否包含任何空白字符。
	 * @param str the {@code CharSequence} to check (may be {@code null})
	 * @return {@code true} if the {@code CharSequence} is not empty and
	 * contains at least 1 whitespace character
	 * @see Character#isWhitespace
	 */
	public static boolean containsWhitespace(@Nullable CharSequence str) {
		if (!hasLength(str)) {
			return false;
		}

		int strLen = str.length();
		for (int i = 0; i < strLen; i++) {
			if (Character.isWhitespace(str.charAt(i))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 检查给定的 {@code String} 是否包含任何空白字符。
	 * @param str the {@code String} to check (may be {@code null})
	 * @return {@code true} if the {@code String} is not empty and
	 * contains at least 1 whitespace character
	 * @see #containsWhitespace(CharSequence)
	 */
	public static boolean containsWhitespace(@Nullable String str) {
		return containsWhitespace((CharSequence) str);
	}

	/**
	 * 去除给定 {@code String} 的前导和尾随空白。
	 * @param str the {@code String} to check
	 * @return the trimmed {@code String}
	 * @see java.lang.Character#isWhitespace
	 */
	public static String trimWhitespace(String str) {
		if (!hasLength(str)) {
			return str;
		}

		int beginIndex = 0;
		int endIndex = str.length() - 1;

		while (beginIndex <= endIndex && Character.isWhitespace(str.charAt(beginIndex))) {
			beginIndex++;
		}

		while (endIndex > beginIndex && Character.isWhitespace(str.charAt(endIndex))) {
			endIndex--;
		}

		return str.substring(beginIndex, endIndex + 1);
	}

	/**
	 * 去除给定 {@code CharSequence} 中的<em>所有</em>空白：前导、尾随以及字符之间的空白。
	 * @param text the {@code CharSequence} to check
	 * @return the trimmed {@code CharSequence}
	 * @since 5.3.22
	 * @see #trimAllWhitespace(String)
	 * @see java.lang.Character#isWhitespace
	 */
	public static CharSequence trimAllWhitespace(CharSequence text) {
		if (!hasLength(text)) {
			return text;
		}

		int len = text.length();
		StringBuilder sb = new StringBuilder(text.length());
		for (int i = 0; i < len; i++) {
			char c = text.charAt(i);
			if (!Character.isWhitespace(c)) {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * 去除给定 {@code String} 中的<em>所有</em>空白：前导、尾随以及字符之间的空白。
	 * @param str the {@code String} to check
	 * @return the trimmed {@code String}
	 * @see #trimAllWhitespace(CharSequence)
	 * @see java.lang.Character#isWhitespace
	 */
	public static String trimAllWhitespace(String str) {
		if (str == null) {
			return null;
		}
		return trimAllWhitespace((CharSequence) str).toString();
	}

	/**
	 * 去除给定 {@code String} 的前导空白。
	 * @param str the {@code String} to check
	 * @return the trimmed {@code String}
	 * @see java.lang.Character#isWhitespace
	 */
	public static String trimLeadingWhitespace(String str) {
		if (!hasLength(str)) {
			return str;
		}

		int beginIdx = 0;
		while (beginIdx < str.length() && Character.isWhitespace(str.charAt(beginIdx))) {
			beginIdx++;
		}
		return str.substring(beginIdx);
	}

	/**
	 * 去除给定 {@code String} 的尾随空白。
	 * @param str the {@code String} to check
	 * @return the trimmed {@code String}
	 * @see java.lang.Character#isWhitespace
	 */
	public static String trimTrailingWhitespace(String str) {
		if (!hasLength(str)) {
			return str;
		}

		int endIdx = str.length() - 1;
		while (endIdx >= 0 && Character.isWhitespace(str.charAt(endIdx))) {
			endIdx--;
		}
		return str.substring(0, endIdx + 1);
	}

	/**
	 * 去除给定 {@code String} 中所有出现的指定前导字符。
	 * @param str the {@code String} to check
	 * @param leadingCharacter the leading character to be trimmed
	 * @return the trimmed {@code String}
	 */
	public static String trimLeadingCharacter(String str, char leadingCharacter) {
		if (!hasLength(str)) {
			return str;
		}

		int beginIdx = 0;
		while (beginIdx < str.length() && leadingCharacter == str.charAt(beginIdx)) {
			beginIdx++;
		}
		return str.substring(beginIdx);
	}

	/**
	 * 去除给定 {@code String} 中所有出现的指定尾随字符。
	 * @param str the {@code String} to check
	 * @param trailingCharacter the trailing character to be trimmed
	 * @return the trimmed {@code String}
	 */
	public static String trimTrailingCharacter(String str, char trailingCharacter) {
		if (!hasLength(str)) {
			return str;
		}

		int endIdx = str.length() - 1;
		while (endIdx >= 0 && trailingCharacter == str.charAt(endIdx)) {
			endIdx--;
		}
		return str.substring(0, endIdx + 1);
	}

	/**
	 * 测试给定 {@code String} 是否与给定的单个字符匹配。
	 * @param str the {@code String} to check
	 * @param singleCharacter the character to compare to
	 * @since 5.2.9
	 */
	public static boolean matchesCharacter(@Nullable String str, char singleCharacter) {
		return (str != null && str.length() == 1 && str.charAt(0) == singleCharacter);
	}

	/**
	 * 测试给定 {@code String} 是否以指定前缀开头，忽略大小写。
	 * @param str the {@code String} to check
	 * @param prefix the prefix to look for
	 * @see java.lang.String#startsWith
	 */
	public static boolean startsWithIgnoreCase(@Nullable String str, @Nullable String prefix) {
		return (str != null && prefix != null && str.length() >= prefix.length() &&
				str.regionMatches(true, 0, prefix, 0, prefix.length()));
	}

	/**
	 * 测试给定 {@code String} 是否以指定后缀结尾，忽略大小写。
	 * @param str the {@code String} to check
	 * @param suffix the suffix to look for
	 * @see java.lang.String#endsWith
	 */
	public static boolean endsWithIgnoreCase(@Nullable String str, @Nullable String suffix) {
		return (str != null && suffix != null && str.length() >= suffix.length() &&
				str.regionMatches(true, str.length() - suffix.length(), suffix, 0, suffix.length()));
	}

	/**
	 * 测试给定字符串是否在给定索引处与给定子字符串匹配。
	 * @param str the original string (or StringBuilder)
	 * @param index the index in the original string to start matching against
	 * @param substring the substring to match at the given index
	 */
	public static boolean substringMatch(CharSequence str, int index, CharSequence substring) {
		if (index + substring.length() > str.length()) {
			return false;
		}
		for (int i = 0; i < substring.length(); i++) {
			if (str.charAt(index + i) != substring.charAt(i)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 统计字符串 {@code str} 中子字符串 {@code sub} 出现的次数。
	 * @param str string to search in
	 * @param sub string to search for
	 */
	public static int countOccurrencesOf(String str, String sub) {
		if (!hasLength(str) || !hasLength(sub)) {
			return 0;
		}

		int count = 0;
		int pos = 0;
		int idx;
		while ((idx = str.indexOf(sub, pos)) != -1) {
			++count;
			pos = idx + sub.length();
		}
		return count;
	}

	/**
	 * 将字符串中某个子字符串的所有出现替换为另一个字符串。
	 * @param inString {@code String} to examine
	 * @param oldPattern {@code String} to replace
	 * @param newPattern {@code String} to insert
	 * @return a {@code String} with the replacements
	 */
	public static String replace(String inString, String oldPattern, @Nullable String newPattern) {
		if (!hasLength(inString) || !hasLength(oldPattern) || newPattern == null) {
			return inString;
		}
		int index = inString.indexOf(oldPattern);
		if (index == -1) {
			// no occurrence -> can return input as-is
			return inString;
		}

		int capacity = inString.length();
		if (newPattern.length() > oldPattern.length()) {
			capacity += 16;
		}
		StringBuilder sb = new StringBuilder(capacity);

		int pos = 0;  // our position in the old string
		int patLen = oldPattern.length();
		while (index >= 0) {
			sb.append(inString, pos, index);
			sb.append(newPattern);
			pos = index + patLen;
			index = inString.indexOf(oldPattern, pos);
		}

		// append any characters to the right of a match
		sb.append(inString, pos, inString.length());
		return sb.toString();
	}

	/**
	 * 删除给定子字符串的所有出现。
	 * @param inString the original {@code String}
	 * @param pattern the pattern to delete all occurrences of
	 * @return the resulting {@code String}
	 */
	public static String delete(String inString, String pattern) {
		return replace(inString, pattern, "");
	}

	/**
	 * 删除给定 {@code String} 中的任意字符。
	 * @param inString the original {@code String}
	 * @param charsToDelete a set of characters to delete.
	 * E.g. "az\n" will delete 'a's, 'z's and new lines.
	 * @return the resulting {@code String}
	 */
	public static String deleteAny(String inString, @Nullable String charsToDelete) {
		if (!hasLength(inString) || !hasLength(charsToDelete)) {
			return inString;
		}

		int lastCharIndex = 0;
		char[] result = new char[inString.length()];
		for (int i = 0; i < inString.length(); i++) {
			char c = inString.charAt(i);
			if (charsToDelete.indexOf(c) == -1) {
				result[lastCharIndex++] = c;
			}
		}
		if (lastCharIndex == inString.length()) {
			return inString;
		}
		return new String(result, 0, lastCharIndex);
	}

	//---------------------------------------------------------------------
	// Convenience methods for working with formatted Strings
	//---------------------------------------------------------------------

	/**
	 * 用单引号将给定 {@code String} 括起来。
	 * @param str the input {@code String} (e.g. "myString")
	 * @return the quoted {@code String} (e.g. "'myString'"),
	 * or {@code null} if the input was {@code null}
	 */
	@Nullable
	public static String quote(@Nullable String str) {
		return (str != null ? "'" + str + "'" : null);
	}

	/**
	 * 如果给定 Object 是 {@code String}，则将其转换为用单引号括起来的 {@code String}；否则保持 Object
	 * 原样。
	 * @param obj the input Object (e.g. "myString")
	 * @return the quoted {@code String} (e.g. "'myString'"),
	 * or the input object as-is if not a {@code String}
	 */
	@Nullable
	public static Object quoteIfString(@Nullable Object obj) {
		return (obj instanceof String ? quote((String) obj) : obj);
	}

	/**
	 * 对由 '.' 点字符限定的字符串进行去限定。例如，"this.name.is.qualified" 返回 "qualified"。
	 * @param qualifiedName the qualified name
	 */
	public static String unqualify(String qualifiedName) {
		return unqualify(qualifiedName, '.');
	}

	/**
	 * 对由分隔符字符限定的字符串进行去限定。例如，"this:name:is:qualified" 在使用 ':' 分隔符时返回
	 * "qualified"。
	 * @param qualifiedName the qualified name
	 * @param separator the separator
	 */
	public static String unqualify(String qualifiedName, char separator) {
		return qualifiedName.substring(qualifiedName.lastIndexOf(separator) + 1);
	}

	/**
	 * 将 {@code String} 首字母大写，依据 {@link Character#toUpperCase(char)} 将第一个字母转换为大写。
	 * 其他字母不变。
	 * @param str the {@code String} to capitalize
	 * @return the capitalized {@code String}
	 */
	public static String capitalize(String str) {
		return changeFirstCharacterCase(str, true);
	}

	/**
	 * 将 {@code String} 首字母小写，依据 {@link Character#toLowerCase(char)} 将第一个字母转换为小写。
	 * 其他字母不变。
	 * @param str the {@code String} to uncapitalize
	 * @return the uncapitalized {@code String}
	 */
	public static String uncapitalize(String str) {
		return changeFirstCharacterCase(str, false);
	}

	private static String changeFirstCharacterCase(String str, boolean capitalize) {
		if (!hasLength(str)) {
			return str;
		}

		char baseChar = str.charAt(0);
		char updatedChar;
		if (capitalize) {
			updatedChar = Character.toUpperCase(baseChar);
		}
		else {
			updatedChar = Character.toLowerCase(baseChar);
		}
		if (baseChar == updatedChar) {
			return str;
		}

		char[] chars = str.toCharArray();
		chars[0] = updatedChar;
		return new String(chars);
	}

	/**
	 * 从给定的 Java 资源路径中提取文件名，例如 {@code "mypath/myfile.txt" &rarr; "myfile.txt"}。
	 * @param path the file path (may be {@code null})
	 * @return the extracted filename, or {@code null} if none
	 */
	@Nullable
	public static String getFilename(@Nullable String path) {
		if (path == null) {
			return null;
		}

		int separatorIndex = path.lastIndexOf(FOLDER_SEPARATOR_CHAR);
		return (separatorIndex != -1 ? path.substring(separatorIndex + 1) : path);
	}

	/**
	 * 从给定的 Java 资源路径中提取文件扩展名，例如 "mypath/myfile.txt" &rarr; "txt"。
	 * @param path the file path (may be {@code null})
	 * @return the extracted filename extension, or {@code null} if none
	 */
	@Nullable
	public static String getFilenameExtension(@Nullable String path) {
		if (path == null) {
			return null;
		}

		int extIndex = path.lastIndexOf(EXTENSION_SEPARATOR);
		if (extIndex == -1) {
			return null;
		}

		int folderIndex = path.lastIndexOf(FOLDER_SEPARATOR_CHAR);
		if (folderIndex > extIndex) {
			return null;
		}

		return path.substring(extIndex + 1);
	}

	/**
	 * 从给定的 Java 资源路径中去除文件扩展名，例如 "mypath/myfile.txt" &rarr; "mypath/myfile"。
	 * @param path the file path
	 * @return the path with stripped filename extension
	 */
	public static String stripFilenameExtension(String path) {
		int extIndex = path.lastIndexOf(EXTENSION_SEPARATOR);
		if (extIndex == -1) {
			return path;
		}

		int folderIndex = path.lastIndexOf(FOLDER_SEPARATOR_CHAR);
		if (folderIndex > extIndex) {
			return path;
		}

		return path.substring(0, extIndex);
	}

	/**
	 * 将给定相对路径应用到给定 Java 资源路径，假定使用标准的 Java 文件夹分隔符（即 "/" 分隔符）。
	 * @param path the path to start from (usually a full file path)
	 * @param relativePath the relative path to apply
	 * (relative to the full file path above)
	 * @return the full file path that results from applying the relative path
	 */
	public static String applyRelativePath(String path, String relativePath) {
		int separatorIndex = path.lastIndexOf(FOLDER_SEPARATOR_CHAR);
		if (separatorIndex != -1) {
			String newPath = path.substring(0, separatorIndex);
			if (!relativePath.startsWith(FOLDER_SEPARATOR)) {
				newPath += FOLDER_SEPARATOR_CHAR;
			}
			return newPath + relativePath;
		}
		else {
			return relativePath;
		}
	}

	/**
	 * 通过抑制 "path/.." 和内部简单点之类的序列来规范化路径。
	 * <p>结果便于进行路径比较。对于其他用途，请注意 Windows 分隔符（"\"）会被替换为简单斜杠。
	 * <p><strong>注意</strong>，在安全上下文中不应依赖 {@code cleanPath}。应使用其他机制来防止路径遍历
	 * 问题。
	 * @param path the original path
	 * @return the normalized path
	 */
	public static String cleanPath(String path) {
		if (!hasLength(path)) {
			return path;
		}

		String normalizedPath = replace(path, WINDOWS_FOLDER_SEPARATOR, FOLDER_SEPARATOR);
		String pathToUse = normalizedPath;

		// Shortcut if there is no work to do
		if (pathToUse.indexOf('.') == -1) {
			return pathToUse;
		}

		// Strip prefix from path to analyze, to not treat it as part of the
		// first path element. This is necessary to correctly parse paths like
		// "file:core/../core/io/Resource.class", where the ".." should just
		// strip the first "core" directory while keeping the "file:" prefix.
		int prefixIndex = pathToUse.indexOf(':');
		String prefix = "";
		if (prefixIndex != -1) {
			prefix = pathToUse.substring(0, prefixIndex + 1);
			if (prefix.contains(FOLDER_SEPARATOR)) {
				prefix = "";
			}
			else {
				pathToUse = pathToUse.substring(prefixIndex + 1);
			}
		}
		if (pathToUse.startsWith(FOLDER_SEPARATOR)) {
			prefix = prefix + FOLDER_SEPARATOR;
			pathToUse = pathToUse.substring(1);
		}

		String[] pathArray = delimitedListToStringArray(pathToUse, FOLDER_SEPARATOR);
		// we never require more elements than pathArray and in the common case the same number
		Deque<String> pathElements = new ArrayDeque<>(pathArray.length);
		int tops = 0;

		for (int i = pathArray.length - 1; i >= 0; i--) {
			String element = pathArray[i];
			if (CURRENT_PATH.equals(element)) {
				// Points to current directory - drop it.
			}
			else if (TOP_PATH.equals(element)) {
				// Registering top path found.
				tops++;
			}
			else {
				if (tops > 0) {
					// Merging path element with element corresponding to top path.
					tops--;
				}
				else {
					// Normal path element found.
					pathElements.addFirst(element);
				}
			}
		}

		// All path elements stayed the same - shortcut
		if (pathArray.length == pathElements.size()) {
			return normalizedPath;
		}
		// Remaining top paths need to be retained.
		for (int i = 0; i < tops; i++) {
			pathElements.addFirst(TOP_PATH);
		}
		// If nothing else left, at least explicitly point to current path.
		if (pathElements.size() == 1 && pathElements.getLast().isEmpty() && !prefix.endsWith(FOLDER_SEPARATOR)) {
			pathElements.addFirst(CURRENT_PATH);
		}

		final String joined = collectionToDelimitedString(pathElements, FOLDER_SEPARATOR);
		// avoid string concatenation with empty prefix
		return prefix.isEmpty() ? joined : prefix + joined;
	}

	/**
	 * 在两个路径规范化之后比较它们。
	 * @param path1 first path for comparison
	 * @param path2 second path for comparison
	 * @return whether the two paths are equivalent after normalization
	 */
	public static boolean pathEquals(String path1, String path2) {
		return cleanPath(path1).equals(cleanPath(path2));
	}

	/**
	 * 解码给定的已编码 URI 组件值。基于以下规则：
	 * <ul>
	 * <li>字母数字字符 {@code "a"} 到 {@code "z"}、{@code "A"} 到 {@code "Z"}，
	 * 以及 {@code "0"} 到 {@code "9"} 保持不变。</li>
	 * <li>特殊字符 {@code "-"}、{@code "_"}、{@code "."} 和 {@code "*"} 保持不变。</li>
	 * <li>序列 "{@code %<i>xy</i>}" 被解释为字符的十六进制表示。</li>
	 * </ul>
	 * @param source the encoded String
	 * @param charset the character set
	 * @return the decoded value
	 * @throws IllegalArgumentException when the given source contains invalid encoded sequences
	 * @since 5.0
	 * @see java.net.URLDecoder#decode(String, String)
	 */
	public static String uriDecode(String source, Charset charset) {
		int length = source.length();
		if (length == 0) {
			return source;
		}
		Assert.notNull(charset, "Charset must not be null");

		ByteArrayOutputStream baos = new ByteArrayOutputStream(length);
		boolean changed = false;
		for (int i = 0; i < length; i++) {
			int ch = source.charAt(i);
			if (ch == '%') {
				if (i + 2 < length) {
					char hex1 = source.charAt(i + 1);
					char hex2 = source.charAt(i + 2);
					int u = Character.digit(hex1, 16);
					int l = Character.digit(hex2, 16);
					if (u == -1 || l == -1) {
						throw new IllegalArgumentException("Invalid encoded sequence \"" + source.substring(i) + "\"");
					}
					baos.write((char) ((u << 4) + l));
					i += 2;
					changed = true;
				}
				else {
					throw new IllegalArgumentException("Invalid encoded sequence \"" + source.substring(i) + "\"");
				}
			}
			else {
				baos.write(ch);
			}
		}
		return (changed ? StreamUtils.copyToString(baos, charset) : source);
	}

	/**
	 * 将给定的 {@code String} 值解析为 {@link Locale}，同时接受 {@link Locale#toString} 格式以及由
	 * {@link Locale#forLanguageTag} 指定的 BCP 47 语言标记。
	 * @param localeValue the locale value: following either {@code Locale's}
	 * {@code toString()} format ("en", "en_UK", etc), also accepting spaces as
	 * separators (as an alternative to underscores), or BCP 47 (e.g. "en-UK")
	 * @return a corresponding {@code Locale} instance, or {@code null} if none
	 * @throws IllegalArgumentException in case of an invalid locale specification
	 * @since 5.0.4
	 * @see #parseLocaleString
	 * @see Locale#forLanguageTag
	 */
	@Nullable
	public static Locale parseLocale(String localeValue) {
		String[] tokens = tokenizeLocaleSource(localeValue);
		if (tokens.length == 1) {
			validateLocalePart(localeValue);
			Locale resolved = Locale.forLanguageTag(localeValue);
			if (resolved.getLanguage().length() > 0) {
				return resolved;
			}
		}
		return parseLocaleTokens(localeValue, tokens);
	}

	/**
	 * 将给定的 {@code String} 表示解析为 {@link Locale}。
	 * <p>对于许多解析场景，这是 {@link Locale#toString Locale 的 toString} 在宽松意义上的逆操作。此方
	 * 法并不追求严格符合 {@code Locale} 设计规范；它更像是专门针对典型的 Spring 解析需求而定制的。
	 * <p><b>注意：此委托不接受 BCP 47 语言标记格式。如需对两种格式进行宽松解析，请使用
	 * {@link #parseLocale}。</b>
	 * @param localeString the locale {@code String}: following {@code Locale's}
	 * {@code toString()} format ("en", "en_UK", etc), also accepting spaces as
	 * separators (as an alternative to underscores)
	 * @return a corresponding {@code Locale} instance, or {@code null} if none
	 * @throws IllegalArgumentException in case of an invalid locale specification
	 */
	@Nullable
	public static Locale parseLocaleString(String localeString) {
		return parseLocaleTokens(localeString, tokenizeLocaleSource(localeString));
	}

	private static String[] tokenizeLocaleSource(String localeSource) {
		return tokenizeToStringArray(localeSource, "_ ", false, false);
	}

	@Nullable
	private static Locale parseLocaleTokens(String localeString, String[] tokens) {
		String language = (tokens.length > 0 ? tokens[0] : "");
		String country = (tokens.length > 1 ? tokens[1] : "");
		validateLocalePart(language);
		validateLocalePart(country);

		String variant = "";
		if (tokens.length > 2) {
			// There is definitely a variant, and it is everything after the country
			// code sans the separator between the country code and the variant.
			int endIndexOfCountryCode = localeString.indexOf(country, language.length()) + country.length();
			// Strip off any leading '_' and whitespace, what's left is the variant.
			variant = trimLeadingWhitespace(localeString.substring(endIndexOfCountryCode));
			if (variant.startsWith("_")) {
				variant = trimLeadingCharacter(variant, '_');
			}
		}

		if (variant.isEmpty() && country.startsWith("#")) {
			variant = country;
			country = "";
		}

		return (language.length() > 0 ? new Locale(language, country, variant) : null);
	}

	private static void validateLocalePart(String localePart) {
		for (int i = 0; i < localePart.length(); i++) {
			char ch = localePart.charAt(i);
			if (ch != ' ' && ch != '_' && ch != '-' && ch != '#' && !Character.isLetterOrDigit(ch)) {
				throw new IllegalArgumentException(
						"Locale part \"" + localePart + "\" contains invalid characters");
			}
		}
	}

	/**
	 * 确定符合 RFC 3066 的语言标记，用于 HTTP 的 "Accept-Language" 头。
	 * @param locale the Locale to transform to a language tag
	 * @return the RFC 3066 compliant language tag as {@code String}
	 * @deprecated as of 5.0.4, in favor of {@link Locale#toLanguageTag()}
	 */
	@Deprecated
	public static String toLanguageTag(Locale locale) {
		return locale.getLanguage() + (hasText(locale.getCountry()) ? "-" + locale.getCountry() : "");
	}

	/**
	 * 将给定的 {@code timeZoneString} 值解析为 {@link TimeZone}。
	 * @param timeZoneString the time zone {@code String}, following {@link TimeZone#getTimeZone(String)}
	 * but throwing {@link IllegalArgumentException} in case of an invalid time zone specification
	 * @return a corresponding {@link TimeZone} instance
	 * @throws IllegalArgumentException in case of an invalid time zone specification
	 */
	public static TimeZone parseTimeZoneString(String timeZoneString) {
		TimeZone timeZone = TimeZone.getTimeZone(timeZoneString);
		if ("GMT".equals(timeZone.getID()) && !timeZoneString.startsWith("GMT")) {
			// We don't want that GMT fallback...
			throw new IllegalArgumentException("Invalid time zone specification '" + timeZoneString + "'");
		}
		return timeZone;
	}


	//---------------------------------------------------------------------
	// Convenience methods for working with String arrays
	//---------------------------------------------------------------------

	/**
	 * 将给定的 {@link Collection} 复制到 {@code String} 数组中。
	 * <p>{@code Collection} 只能包含 {@code String} 元素。
	 * @param collection the {@code Collection} to copy
	 * (potentially {@code null} or empty)
	 * @return the resulting {@code String} array
	 */
	public static String[] toStringArray(@Nullable Collection<String> collection) {
		return (!CollectionUtils.isEmpty(collection) ? collection.toArray(EMPTY_STRING_ARRAY) : EMPTY_STRING_ARRAY);
	}

	/**
	 * 将给定的 {@link Enumeration} 复制到 {@code String} 数组中。
	 * <p>{@code Enumeration} 只能包含 {@code String} 元素。
	 * @param enumeration the {@code Enumeration} to copy
	 * (potentially {@code null} or empty)
	 * @return the resulting {@code String} array
	 */
	public static String[] toStringArray(@Nullable Enumeration<String> enumeration) {
		return (enumeration != null ? toStringArray(Collections.list(enumeration)) : EMPTY_STRING_ARRAY);
	}

	/**
	 * 将给定的 {@code String} 追加到给定的 {@code String} 数组，返回一个新数组，其中包含输入数组的内容
	 * 加上给定的 {@code String}。
	 * @param array the array to append to (can be {@code null})
	 * @param str the {@code String} to append
	 * @return the new array (never {@code null})
	 */
	public static String[] addStringToArray(@Nullable String[] array, String str) {
		if (ObjectUtils.isEmpty(array)) {
			return new String[] {str};
		}

		String[] newArr = new String[array.length + 1];
		System.arraycopy(array, 0, newArr, 0, array.length);
		newArr[array.length] = str;
		return newArr;
	}

	/**
	 * 将给定的 {@code String} 数组合并为一个，重叠的数组元素会被包含两次。
	 * <p>原始数组中元素的顺序保持不变。
	 * @param array1 the first array (can be {@code null})
	 * @param array2 the second array (can be {@code null})
	 * @return the new array ({@code null} if both given arrays were {@code null})
	 */
	@Nullable
	public static String[] concatenateStringArrays(@Nullable String[] array1, @Nullable String[] array2) {
		if (ObjectUtils.isEmpty(array1)) {
			return array2;
		}
		if (ObjectUtils.isEmpty(array2)) {
			return array1;
		}

		String[] newArr = new String[array1.length + array2.length];
		System.arraycopy(array1, 0, newArr, 0, array1.length);
		System.arraycopy(array2, 0, newArr, array1.length, array2.length);
		return newArr;
	}

	/**
	 * 将给定的 {@code String} 数组合并为一个，重叠的数组元素只包含一次。
	 * <p>原始数组中元素的顺序保持不变（重叠元素除外，它们只在首次出现时被包含）。
	 * @param array1 the first array (can be {@code null})
	 * @param array2 the second array (can be {@code null})
	 * @return the new array ({@code null} if both given arrays were {@code null})
	 * @deprecated as of 4.3.15, in favor of manual merging via {@link LinkedHashSet}
	 * (with every entry included at most once, even entries within the first array)
	 */
	@Deprecated
	@Nullable
	public static String[] mergeStringArrays(@Nullable String[] array1, @Nullable String[] array2) {
		if (ObjectUtils.isEmpty(array1)) {
			return array2;
		}
		if (ObjectUtils.isEmpty(array2)) {
			return array1;
		}

		List<String> result = new ArrayList<>(Arrays.asList(array1));
		for (String str : array2) {
			if (!result.contains(str)) {
				result.add(str);
			}
		}
		return toStringArray(result);
	}

	/**
	 * 如有必要，对给定的 {@code String} 数组进行排序。
	 * @param array the original array (potentially empty)
	 * @return the array in sorted form (never {@code null})
	 */
	public static String[] sortStringArray(String[] array) {
		if (ObjectUtils.isEmpty(array)) {
			return array;
		}

		Arrays.sort(array);
		return array;
	}

	/**
	 * 去除给定 {@code String} 数组中各元素的空白，对每个非 null 元素调用 {@code String.trim()}。
	 * @param array the original {@code String} array (potentially empty)
	 * @return the resulting array (of the same size) with trimmed elements
	 */
	public static String[] trimArrayElements(String[] array) {
		if (ObjectUtils.isEmpty(array)) {
			return array;
		}

		String[] result = new String[array.length];
		for (int i = 0; i < array.length; i++) {
			String element = array[i];
			result[i] = (element != null ? element.trim() : null);
		}
		return result;
	}

	/**
	 * 从给定数组中移除重复的字符串。
	 * <p>从 4.2 开始，它会保留原始顺序，因为它使用了 {@link LinkedHashSet}。
	 * @param array the {@code String} array (potentially empty)
	 * @return an array without duplicates, in natural sort order
	 */
	public static String[] removeDuplicateStrings(String[] array) {
		if (ObjectUtils.isEmpty(array)) {
			return array;
		}

		Set<String> set = new LinkedHashSet<>(Arrays.asList(array));
		return toStringArray(set);
	}

	/**
	 * 在分隔符第一次出现处拆分 {@code String}。结果中不包含该分隔符。
	 * @param toSplit the string to split (potentially {@code null} or empty)
	 * @param delimiter to split the string up with (potentially {@code null} or empty)
	 * @return a two element array with index 0 being before the delimiter, and
	 * index 1 being after the delimiter (neither element includes the delimiter);
	 * or {@code null} if the delimiter wasn't found in the given input {@code String}
	 */
	@Nullable
	public static String[] split(@Nullable String toSplit, @Nullable String delimiter) {
		if (!hasLength(toSplit) || !hasLength(delimiter)) {
			return null;
		}
		int offset = toSplit.indexOf(delimiter);
		if (offset < 0) {
			return null;
		}

		String beforeDelimiter = toSplit.substring(0, offset);
		String afterDelimiter = toSplit.substring(offset + delimiter.length());
		return new String[] {beforeDelimiter, afterDelimiter};
	}

	/**
	 * 接收一个字符串数组，并根据给定的分隔符拆分每个元素。然后生成一个 {@code Properties} 实例，分隔符
	 * 左侧提供键，分隔符右侧提供值。
	 * <p>在将该键和值添加到 {@code Properties} 之前，会去除二者的空白。
	 * @param array the array to process
	 * @param delimiter to split each element using (typically the equals symbol)
	 * @return a {@code Properties} instance representing the array contents,
	 * or {@code null} if the array to process was {@code null} or empty
	 */
	@Nullable
	public static Properties splitArrayElementsIntoProperties(String[] array, String delimiter) {
		return splitArrayElementsIntoProperties(array, delimiter, null);
	}

	/**
	 * 接收一个字符串数组，并根据给定的分隔符拆分每个元素。然后生成一个 {@code Properties} 实例，分隔符
	 * 左侧提供键，分隔符右侧提供值。
	 * <p>在将该键和值添加到 {@code Properties} 实例之前，会去除二者的空白。
	 * @param array the array to process
	 * @param delimiter to split each element using (typically the equals symbol)
	 * @param charsToDelete one or more characters to remove from each element
	 * prior to attempting the split operation (typically the quotation mark
	 * symbol), or {@code null} if no removal should occur
	 * @return a {@code Properties} instance representing the array contents,
	 * or {@code null} if the array to process was {@code null} or empty
	 */
	@Nullable
	public static Properties splitArrayElementsIntoProperties(
			String[] array, String delimiter, @Nullable String charsToDelete) {

		if (ObjectUtils.isEmpty(array)) {
			return null;
		}

		Properties result = new Properties();
		for (String element : array) {
			if (charsToDelete != null) {
				element = deleteAny(element, charsToDelete);
			}
			String[] splittedElement = split(element, delimiter);
			if (splittedElement == null) {
				continue;
			}
			result.setProperty(splittedElement[0].trim(), splittedElement[1].trim());
		}
		return result;
	}

	/**
	 * 通过 {@link StringTokenizer} 将给定的 {@code String} 分词为 {@code String} 数组。
	 * <p>去除标记（token）的空白并忽略空标记。
	 * <p>给定的 {@code delimiters} 字符串可以包含任意数量的分隔字符。每个这样的字符都可用于分隔标记。
	 * 分隔符始终是单个字符；对于多字符分隔符，请考虑使用 {@link #delimitedListToStringArray}。
	 * @param str the {@code String} to tokenize (potentially {@code null} or empty)
	 * @param delimiters the delimiter characters, assembled as a {@code String}
	 * (each of the characters is individually considered as a delimiter)
	 * @return an array of the tokens
	 * @see java.util.StringTokenizer
	 * @see String#trim()
	 * @see #delimitedListToStringArray
	 */
	public static String[] tokenizeToStringArray(@Nullable String str, String delimiters) {
		return tokenizeToStringArray(str, delimiters, true, true);
	}

	/**
	 * 通过 {@link StringTokenizer} 将给定的 {@code String} 分词为 {@code String} 数组。
	 * <p>给定的 {@code delimiters} 字符串可以包含任意数量的分隔字符。每个这样的字符都可用于分隔标记。
	 * 分隔符始终是单个字符；对于多字符分隔符，请考虑使用 {@link #delimitedListToStringArray}。
	 * @param str the {@code String} to tokenize (potentially {@code null} or empty)
	 * @param delimiters the delimiter characters, assembled as a {@code String}
	 * (each of the characters is individually considered as a delimiter)
	 * @param trimTokens trim the tokens via {@link String#trim()}
	 * @param ignoreEmptyTokens omit empty tokens from the result array
	 * (only applies to tokens that are empty after trimming; StringTokenizer
	 * will not consider subsequent delimiters as token in the first place).
	 * @return an array of the tokens
	 * @see java.util.StringTokenizer
	 * @see String#trim()
	 * @see #delimitedListToStringArray
	 */
	public static String[] tokenizeToStringArray(
			@Nullable String str, String delimiters, boolean trimTokens, boolean ignoreEmptyTokens) {

		if (str == null) {
			return EMPTY_STRING_ARRAY;
		}

		StringTokenizer st = new StringTokenizer(str, delimiters);
		List<String> tokens = new ArrayList<>();
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if (trimTokens) {
				token = token.trim();
			}
			if (!ignoreEmptyTokens || token.length() > 0) {
				tokens.add(token);
			}
		}
		return toStringArray(tokens);
	}

	/**
	 * 接收一个作为分隔列表的 {@code String}，并将其转换为 {@code String} 数组。
	 * <p>单个 {@code delimiter} 可以由多个字符组成，但与 {@link #tokenizeToStringArray} 相反，它仍会被
	 * 视为一个分隔字符串，而不是一组可能的分隔字符。
	 * @param str the input {@code String} (potentially {@code null} or empty)
	 * @param delimiter the delimiter between elements (this is a single delimiter,
	 * rather than a bunch individual delimiter characters)
	 * @return an array of the tokens in the list
	 * @see #tokenizeToStringArray
	 */
	public static String[] delimitedListToStringArray(@Nullable String str, @Nullable String delimiter) {
		return delimitedListToStringArray(str, delimiter, null);
	}

	/**
	 * 接收一个作为分隔列表的 {@code String}，并将其转换为 {@code String} 数组。
	 * <p>单个 {@code delimiter} 可以由多个字符组成，但与 {@link #tokenizeToStringArray} 相反，它仍会被
	 * 视为一个分隔字符串，而不是一组可能的分隔字符。
	 * @param str the input {@code String} (potentially {@code null} or empty)
	 * @param delimiter the delimiter between elements (this is a single delimiter,
	 * rather than a bunch individual delimiter characters)
	 * @param charsToDelete a set of characters to delete; useful for deleting unwanted
	 * line breaks: e.g. "\r\n\f" will delete all new lines and line feeds in a {@code String}
	 * @return an array of the tokens in the list
	 * @see #tokenizeToStringArray
	 */
	public static String[] delimitedListToStringArray(
			@Nullable String str, @Nullable String delimiter, @Nullable String charsToDelete) {

		if (str == null) {
			return EMPTY_STRING_ARRAY;
		}
		if (delimiter == null) {
			return new String[] {str};
		}

		List<String> result = new ArrayList<>();
		if (delimiter.isEmpty()) {
			for (int i = 0; i < str.length(); i++) {
				result.add(deleteAny(str.substring(i, i + 1), charsToDelete));
			}
		}
		else {
			int pos = 0;
			int delPos;
			while ((delPos = str.indexOf(delimiter, pos)) != -1) {
				result.add(deleteAny(str.substring(pos, delPos), charsToDelete));
				pos = delPos + delimiter.length();
			}
			if (str.length() > 0 && pos <= str.length()) {
				// Add rest of String, but not in case of empty input.
				result.add(deleteAny(str.substring(pos), charsToDelete));
			}
		}
		return toStringArray(result);
	}

	/**
	 * 将以逗号分隔的列表（例如 CSV 文件中的一行）转换为字符串数组。
	 * @param str the input {@code String} (potentially {@code null} or empty)
	 * @return an array of strings, or the empty array in case of empty input
	 */
	public static String[] commaDelimitedListToStringArray(@Nullable String str) {
		return delimitedListToStringArray(str, ",");
	}

	/**
	 * 将以逗号分隔的列表（例如 CSV 文件中的一行）转换为集合。
	 * <p>请注意，这会抑制重复项；从 4.2 开始，返回集合中的元素将按照 {@link LinkedHashSet} 保留原始顺
	 * 序。
	 * @param str the input {@code String} (potentially {@code null} or empty)
	 * @return a set of {@code String} entries in the list
	 * @see #removeDuplicateStrings(String[])
	 */
	public static Set<String> commaDelimitedListToSet(@Nullable String str) {
		String[] tokens = commaDelimitedListToStringArray(str);
		return new LinkedHashSet<>(Arrays.asList(tokens));
	}

	/**
	 * 将 {@link Collection} 转换为带分隔符的 {@code String}（例如 CSV）。
	 * <p>适用于 {@code toString()} 实现。
	 * @param coll the {@code Collection} to convert (potentially {@code null} or empty)
	 * @param delim the delimiter to use (typically a ",")
	 * @param prefix the {@code String} to start each element with
	 * @param suffix the {@code String} to end each element with
	 * @return the delimited {@code String}
	 */
	public static String collectionToDelimitedString(
			@Nullable Collection<?> coll, String delim, String prefix, String suffix) {

		if (CollectionUtils.isEmpty(coll)) {
			return "";
		}

		int totalLength = coll.size() * (prefix.length() + suffix.length()) + (coll.size() - 1) * delim.length();
		for (Object element : coll) {
			totalLength += String.valueOf(element).length();
		}

		StringBuilder sb = new StringBuilder(totalLength);
		Iterator<?> it = coll.iterator();
		while (it.hasNext()) {
			sb.append(prefix).append(it.next()).append(suffix);
			if (it.hasNext()) {
				sb.append(delim);
			}
		}
		return sb.toString();
	}

	/**
	 * 将 {@code Collection} 转换为带分隔符的 {@code String}（例如 CSV）。
	 * <p>适用于 {@code toString()} 实现。
	 * @param coll the {@code Collection} to convert (potentially {@code null} or empty)
	 * @param delim the delimiter to use (typically a ",")
	 * @return the delimited {@code String}
	 */
	public static String collectionToDelimitedString(@Nullable Collection<?> coll, String delim) {
		return collectionToDelimitedString(coll, delim, "", "");
	}

	/**
	 * 将 {@code Collection} 转换为带分隔符的 {@code String}（例如 CSV）。
	 * <p>适用于 {@code toString()} 实现。
	 * @param coll the {@code Collection} to convert (potentially {@code null} or empty)
	 * @return the delimited {@code String}
	 */
	public static String collectionToCommaDelimitedString(@Nullable Collection<?> coll) {
		return collectionToDelimitedString(coll, ",");
	}

	/**
	 * 将 {@code String} 数组转换为带分隔符的 {@code String}（例如 CSV）。
	 * <p>适用于 {@code toString()} 实现。
	 * @param arr the array to display (potentially {@code null} or empty)
	 * @param delim the delimiter to use (typically a ",")
	 * @return the delimited {@code String}
	 */
	public static String arrayToDelimitedString(@Nullable Object[] arr, String delim) {
		if (ObjectUtils.isEmpty(arr)) {
			return "";
		}
		if (arr.length == 1) {
			return ObjectUtils.nullSafeToString(arr[0]);
		}

		StringJoiner sj = new StringJoiner(delim);
		for (Object elem : arr) {
			sj.add(String.valueOf(elem));
		}
		return sj.toString();
	}

	/**
	 * 将 {@code String} 数组转换为以逗号分隔的 {@code String}（即 CSV）。
	 * <p>适用于 {@code toString()} 实现。
	 * @param arr the array to display (potentially {@code null} or empty)
	 * @return the delimited {@code String}
	 */
	public static String arrayToCommaDelimitedString(@Nullable Object[] arr) {
		return arrayToDelimitedString(arr, ",");
	}

	/**
	 * 截断提供的 {@link CharSequence}。
	 * <p>委托给 {@link #truncate(CharSequence, int)}，并将 {@code 100} 作为阈值。
	 * @param charSequence the {@code CharSequence} to truncate
	 * @return a truncated string, or a string representation of the original
	 * {@code CharSequence} if its length does not exceed the threshold
	 * @since 5.3.27
	 */
	public static String truncate(CharSequence charSequence) {
		return truncate(charSequence, DEFAULT_TRUNCATION_THRESHOLD);
	}

	/**
	 * 截断提供的 {@link CharSequence}。
	 * <p>如果 {@code CharSequence} 的长度大于阈值，此方法会返回该 {@code CharSequence} 的一个
	 * {@linkplain CharSequence#subSequence(int, int) 子序列}（至多到阈值），并附加后缀
	 * {@code " (truncated)..."}。否则，此方法返回 {@code charSequence.toString()}。
	 * @param charSequence the {@code CharSequence} to truncate
	 * @param threshold the maximum length after which to truncate; must be a
	 * positive number
	 * @return a truncated string, or a string representation of the original
	 * {@code CharSequence} if its length does not exceed the threshold
	 * @since 5.3.27
	 */
	public static String truncate(CharSequence charSequence, int threshold) {
		Assert.isTrue(threshold > 0,
				() -> "Truncation threshold must be a positive number: " + threshold);
		if (charSequence.length() > threshold) {
			return charSequence.subSequence(0, threshold) + TRUNCATION_SUFFIX;
		}
		return charSequence.toString();
	}

}
