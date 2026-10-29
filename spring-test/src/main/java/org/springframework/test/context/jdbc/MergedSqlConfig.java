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

package org.springframework.test.context.jdbc;

import java.lang.reflect.Array;
import java.util.Arrays;

import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.style.ToStringCreator;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.lang.Nullable;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.test.context.jdbc.SqlConfig.ErrorMode;
import org.springframework.test.context.jdbc.SqlConfig.TransactionMode;
import org.springframework.util.Assert;

/**
 * {@code MergedSqlConfig} 封装了通过 {@link Sql#config} 本地声明以及作为类级注解全局声明的<em>合并后的</em> {@link SqlConfig @SqlConfig} 属性。
 * <p>显式的本地配置属性会覆盖全局配置属性。
 *
 * @author Sam Brannen
 * @since 4.1
 * @see SqlConfig
 */
class MergedSqlConfig {

	private static final String COMMENT_PREFIX = "commentPrefix";

	private static final String COMMENT_PREFIXES = "commentPrefixes";


	private final String dataSource;

	private final String transactionManager;

	private final TransactionMode transactionMode;

	private final String encoding;

	private final String separator;

	private final String[] commentPrefixes;

	private final String blockCommentStartDelimiter;

	private final String blockCommentEndDelimiter;

	private final ErrorMode errorMode;


	/**
	 * 通过将提供的本地（可能是方法级）{@code @SqlConfig} 注解中的配置与在所提供的 {@code testClass} 上发现的类级配置合并，构造一个 {@code MergedSqlConfig} 实例。
	 * <p>本地配置覆盖类级配置。
	 * <p>如果测试类未标注 {@code @SqlConfig}，则不进行任何合并，本地配置“按原样”使用。
	 */
	MergedSqlConfig(SqlConfig localSqlConfig, Class<?> testClass) {
		Assert.notNull(localSqlConfig, "Local @SqlConfig must not be null");
		Assert.notNull(testClass, "testClass must not be null");

		AnnotationAttributes mergedAttributes = mergeAttributes(localSqlConfig, testClass);

		this.dataSource = mergedAttributes.getString("dataSource");
		this.transactionManager = mergedAttributes.getString("transactionManager");
		this.transactionMode = getEnum(mergedAttributes, "transactionMode", TransactionMode.DEFAULT,
				TransactionMode.INFERRED);
		this.encoding = mergedAttributes.getString("encoding");
		this.separator = getString(mergedAttributes, "separator", ScriptUtils.DEFAULT_STATEMENT_SEPARATOR);
		this.commentPrefixes = getCommentPrefixes(mergedAttributes);
		this.blockCommentStartDelimiter = getString(mergedAttributes, "blockCommentStartDelimiter",
				ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER);
		this.blockCommentEndDelimiter = getString(mergedAttributes, "blockCommentEndDelimiter",
				ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER);
		this.errorMode = getEnum(mergedAttributes, "errorMode", ErrorMode.DEFAULT, ErrorMode.FAIL_ON_ERROR);
	}

	private AnnotationAttributes mergeAttributes(SqlConfig localSqlConfig, Class<?> testClass) {
		AnnotationAttributes localAttributes = AnnotationUtils.getAnnotationAttributes(localSqlConfig, false, false);

		// Enforce comment prefix aliases within the local @SqlConfig.
		enforceCommentPrefixAliases(localAttributes);

		// Get global attributes, if any.
		SqlConfig globalSqlConfig = TestContextAnnotationUtils.findMergedAnnotation(testClass, SqlConfig.class);

		// Use local attributes only?
		if (globalSqlConfig == null) {
			return localAttributes;
		}

		AnnotationAttributes globalAttributes = AnnotationUtils.getAnnotationAttributes(globalSqlConfig, false, false);

		// Enforce comment prefix aliases within the global @SqlConfig.
		enforceCommentPrefixAliases(globalAttributes);

		for (String key : globalAttributes.keySet()) {
			Object value = localAttributes.get(key);
			if (isExplicitValue(value)) {
				// Override global attribute with local attribute.
				globalAttributes.put(key, value);

				// Ensure comment prefix aliases are honored during the merge.
				if (key.equals(COMMENT_PREFIX) && isEmptyArray(localAttributes.get(COMMENT_PREFIXES))) {
					globalAttributes.put(COMMENT_PREFIXES, value);
				}
				else if (key.equals(COMMENT_PREFIXES) && isEmptyString(localAttributes.get(COMMENT_PREFIX))) {
					globalAttributes.put(COMMENT_PREFIX, value);
				}
			}
		}
		return globalAttributes;
	}

	/**
	 * 获取 {@link javax.sql.DataSource} 的 bean 名称。
	 * @see SqlConfig#dataSource()
	 */
	String getDataSource() {
		return this.dataSource;
	}

	/**
	 * 获取 {@link org.springframework.transaction.PlatformTransactionManager} 的 bean 名称。
	 * @see SqlConfig#transactionManager()
	 */
	String getTransactionManager() {
		return this.transactionManager;
	}

	/**
	 * 获取 {@link TransactionMode}。
	 * @see SqlConfig#transactionMode()
	 */
	TransactionMode getTransactionMode() {
		return this.transactionMode;
	}

	/**
	 * 获取 SQL 脚本的编码（如果与平台编码不同）。
	 * @see SqlConfig#encoding()
	 */
	String getEncoding() {
		return this.encoding;
	}

	/**
	 * 获取用于分隔 SQL 脚本中各个语句的字符串。
	 * @see SqlConfig#separator()
	 */
	String getSeparator() {
		return this.separator;
	}

	/**
	 * 获取用于标识 SQL 脚本中单行注释的前缀。
	 * @since 5.2
	 * @see SqlConfig#commentPrefixes()
	 */
	String[] getCommentPrefixes() {
		return this.commentPrefixes;
	}

	/**
	 * 获取用于标识 SQL 脚本中块注释的起始分隔符。
	 * @see SqlConfig#blockCommentStartDelimiter()
	 */
	String getBlockCommentStartDelimiter() {
		return this.blockCommentStartDelimiter;
	}

	/**
	 * 获取用于标识 SQL 脚本中块注释的结束分隔符。
	 * @see SqlConfig#blockCommentEndDelimiter()
	 */
	String getBlockCommentEndDelimiter() {
		return this.blockCommentEndDelimiter;
	}

	/**
	 * 获取 {@link ErrorMode}。
	 * @see SqlConfig#errorMode()
	 */
	ErrorMode getErrorMode() {
		return this.errorMode;
	}

	/**
	 * 提供合并后的 SQL 脚本配置的字符串表示形式。
	 */
	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("dataSource", this.dataSource)
				.append("transactionManager", this.transactionManager)
				.append("transactionMode", this.transactionMode)
				.append("encoding", this.encoding)
				.append("separator", this.separator)
				.append("commentPrefixes", this.commentPrefixes)
				.append("blockCommentStartDelimiter", this.blockCommentStartDelimiter)
				.append("blockCommentEndDelimiter", this.blockCommentEndDelimiter)
				.append("errorMode", this.errorMode)
				.toString();
	}


	private static <E extends Enum<?>> E getEnum(AnnotationAttributes attributes, String attributeName,
			E inheritedOrDefaultValue, E defaultValue) {

		E value = attributes.getEnum(attributeName);
		if (value == inheritedOrDefaultValue) {
			value = defaultValue;
		}
		return value;
	}

	private static String getString(AnnotationAttributes attributes, String attributeName, String defaultValue) {
		String value = attributes.getString(attributeName);
		if (value.isEmpty()) {
			value = defaultValue;
		}
		return value;
	}

	private static void enforceCommentPrefixAliases(AnnotationAttributes attributes) {
		String commentPrefix = attributes.getString(COMMENT_PREFIX);
		String[] commentPrefixes = attributes.getStringArray(COMMENT_PREFIXES);

		boolean explicitCommentPrefix = !commentPrefix.isEmpty();
		boolean explicitCommentPrefixes = (commentPrefixes.length != 0);
		Assert.isTrue(!(explicitCommentPrefix && explicitCommentPrefixes),
			"You may declare the 'commentPrefix' or 'commentPrefixes' attribute in @SqlConfig but not both");

		if (explicitCommentPrefix) {
			Assert.hasText(commentPrefix, "@SqlConfig(commentPrefix) must contain text");
			attributes.put(COMMENT_PREFIXES, new String[] { commentPrefix });
		}
		else if (explicitCommentPrefixes) {
			for (String prefix : commentPrefixes) {
				Assert.hasText(prefix, "@SqlConfig(commentPrefixes) must not contain empty prefixes");
			}
			attributes.put(COMMENT_PREFIX, commentPrefixes);
		}
		else {
			// We know commentPrefixes is an empty array, so make sure commentPrefix
			// is set to that as well in order to honor the alias contract.
			attributes.put(COMMENT_PREFIX, commentPrefixes);
		}
	}

	private static String[] getCommentPrefixes(AnnotationAttributes attributes) {
		String[] commentPrefix = attributes.getStringArray(COMMENT_PREFIX);
		String[] commentPrefixes = attributes.getStringArray(COMMENT_PREFIXES);

		Assert.state(Arrays.equals(commentPrefix, commentPrefixes),
			"Failed to properly handle 'commentPrefix' and 'commentPrefixes' aliases");

		return (commentPrefixes.length != 0 ? commentPrefixes : ScriptUtils.DEFAULT_COMMENT_PREFIXES);
	}

	/**
	 * 确定所提供的值是否为显式值（即不是默认值）。
	 */
	private static boolean isExplicitValue(@Nullable Object value) {
		return !(isEmptyString(value) ||
				isEmptyArray(value) ||
				value == TransactionMode.DEFAULT ||
				value == ErrorMode.DEFAULT);
	}

	private static boolean isEmptyString(@Nullable Object value) {
		return (value instanceof String && ((String) value).isEmpty());
	}

	private static boolean isEmptyArray(@Nullable Object value) {
		return (value != null && value.getClass().isArray() && Array.getLength(value) == 0);
	}

}
