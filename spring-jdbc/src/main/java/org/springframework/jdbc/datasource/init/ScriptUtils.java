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

package org.springframework.jdbc.datasource.init;

import java.io.IOException;
import java.io.LineNumberReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 用于结合 JDBC 处理 SQL 脚本的通用工具方法。
 *
 * <p>主要用于框架内部使用。
 *
 * @author Thomas Risberg
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @author Keith Donald
 * @author Dave Syer
 * @author Chris Beams
 * @author Oliver Gierke
 * @author Chris Baldwin
 * @author Nicolas Debeissat
 * @author Phillip Webb
 * @since 4.0.3
 * @see org.springframework.r2dbc.connection.init.ScriptUtils
 */
public abstract class ScriptUtils {

	/**
	 * SQL 脚本中的默认语句分隔符：{@code ";"}。
	 */
	public static final String DEFAULT_STATEMENT_SEPARATOR = ";";

	/**
	 * SQL 脚本中的回退语句分隔符：{@code "\n"}。
	 * <p>当给定的脚本中既未提供自定义分隔符，也不存在
	 * {@link #DEFAULT_STATEMENT_SEPARATOR} 时使用。
	 */
	public static final String FALLBACK_STATEMENT_SEPARATOR = "\n";

	/**
	 * 文件末尾（EOF）SQL 语句分隔符：{@code "^^^ END OF SCRIPT ^^^"}。
	 * <p>该值可以作为 {@code separator} 传递给 {@link
	 * #executeSqlScript(Connection, EncodedResource, boolean, boolean, String, String, String, String)}，
	 * 表示某个 SQL 脚本只包含一条语句（可能跨多行）且没有显式的语句分隔符。
	 * 注意，这样的脚本实际上不应包含此值；它只是一个 <em>虚拟</em> 的语句分隔符。
	 */
	public static final String EOF_STATEMENT_SEPARATOR = "^^^ END OF SCRIPT ^^^";

	/**
	 * SQL 脚本中单行注释的默认前缀：{@code "--"}。
	 */
	public static final String DEFAULT_COMMENT_PREFIX = "--";

	/**
	 * SQL 脚本中单行注释的默认前缀：{@code ["--"]}。
	 * @since 5.2
	 */
	public static final String[] DEFAULT_COMMENT_PREFIXES = {DEFAULT_COMMENT_PREFIX};

	/**
	 * SQL 脚本中块注释的默认起始分隔符：{@code "/*"}。
	 */
	public static final String DEFAULT_BLOCK_COMMENT_START_DELIMITER = "/*";

	/**
	 * SQL 脚本中块注释的默认结束分隔符：<code>"*&#47;"</code>。
	 */
	public static final String DEFAULT_BLOCK_COMMENT_END_DELIMITER = "*/";


	private static final Log logger = LogFactory.getLog(ScriptUtils.class);


	/**
	 * 使用语句分隔符、注释分隔符和异常处理标志的默认设置执行给定的 SQL 脚本。
	 * <p>在依次执行所提供的脚本中的各条语句之前，语句分隔符和注释将被移除。
	 * <p><strong>警告</strong>：此方法 <em>不会</em> 释放提供的 {@link Connection}。
	 * @param connection the JDBC connection to use to execute the script; already
	 * configured and ready to use
	 * @param resource the resource to load the SQL script from; encoded with the
	 * current platform's default encoding
	 * @throws ScriptException if an error occurred while executing the SQL script
	 * @see #executeSqlScript(Connection, EncodedResource, boolean, boolean, String, String, String, String)
	 * @see #DEFAULT_STATEMENT_SEPARATOR
	 * @see #DEFAULT_COMMENT_PREFIX
	 * @see #DEFAULT_BLOCK_COMMENT_START_DELIMITER
	 * @see #DEFAULT_BLOCK_COMMENT_END_DELIMITER
	 * @see org.springframework.jdbc.datasource.DataSourceUtils#getConnection
	 * @see org.springframework.jdbc.datasource.DataSourceUtils#releaseConnection
	 */
	public static void executeSqlScript(Connection connection, Resource resource) throws ScriptException {
		executeSqlScript(connection, new EncodedResource(resource));
	}

	/**
	 * 使用语句分隔符、注释分隔符和异常处理标志的默认设置执行给定的 SQL 脚本。
	 * <p>在依次执行所提供的脚本中的各条语句之前，语句分隔符和注释将被移除。
	 * <p><strong>警告</strong>：此方法 <em>不会</em> 释放提供的 {@link Connection}。
	 * @param connection the JDBC connection to use to execute the script; already
	 * configured and ready to use
	 * @param resource the resource (potentially associated with a specific encoding)
	 * to load the SQL script from
	 * @throws ScriptException if an error occurred while executing the SQL script
	 * @see #executeSqlScript(Connection, EncodedResource, boolean, boolean, String, String, String, String)
	 * @see #DEFAULT_STATEMENT_SEPARATOR
	 * @see #DEFAULT_COMMENT_PREFIX
	 * @see #DEFAULT_BLOCK_COMMENT_START_DELIMITER
	 * @see #DEFAULT_BLOCK_COMMENT_END_DELIMITER
	 * @see org.springframework.jdbc.datasource.DataSourceUtils#getConnection
	 * @see org.springframework.jdbc.datasource.DataSourceUtils#releaseConnection
	 */
	public static void executeSqlScript(Connection connection, EncodedResource resource) throws ScriptException {
		executeSqlScript(connection, resource, false, false, DEFAULT_COMMENT_PREFIX, DEFAULT_STATEMENT_SEPARATOR,
				DEFAULT_BLOCK_COMMENT_START_DELIMITER, DEFAULT_BLOCK_COMMENT_END_DELIMITER);
	}

	/**
	 * 执行给定的 SQL 脚本。
	 * <p>在依次执行所提供的脚本中的各条语句之前，语句分隔符和注释将被移除。
	 * <p><strong>警告</strong>：此方法 <em>不会</em> 释放提供的 {@link Connection}。
	 * @param connection the JDBC connection to use to execute the script; already
	 * configured and ready to use
	 * @param resource the resource (potentially associated with a specific encoding)
	 * to load the SQL script from
	 * @param continueOnError whether to continue without throwing an exception
	 * in the event of an error
	 * @param ignoreFailedDrops whether to continue in the event of specifically
	 * an error on a {@code DROP} statement
	 * @param commentPrefix the prefix that identifies single-line comments in the
	 * SQL script (typically "--")
	 * @param separator the script statement separator; defaults to
	 * {@value #DEFAULT_STATEMENT_SEPARATOR} if not specified and falls back to
	 * {@value #FALLBACK_STATEMENT_SEPARATOR} as a last resort; may be set to
	 * {@value #EOF_STATEMENT_SEPARATOR} to signal that the script contains a
	 * single statement without a separator
	 * @param blockCommentStartDelimiter the <em>start</em> block comment delimiter
	 * @param blockCommentEndDelimiter the <em>end</em> block comment delimiter
	 * @throws ScriptException if an error occurred while executing the SQL script
	 * @see #DEFAULT_STATEMENT_SEPARATOR
	 * @see #FALLBACK_STATEMENT_SEPARATOR
	 * @see #EOF_STATEMENT_SEPARATOR
	 * @see org.springframework.jdbc.datasource.DataSourceUtils#getConnection
	 * @see org.springframework.jdbc.datasource.DataSourceUtils#releaseConnection
	 */
	public static void executeSqlScript(Connection connection, EncodedResource resource, boolean continueOnError,
			boolean ignoreFailedDrops, String commentPrefix, @Nullable String separator,
			String blockCommentStartDelimiter, String blockCommentEndDelimiter) throws ScriptException {

		executeSqlScript(connection, resource, continueOnError, ignoreFailedDrops,
				new String[] { commentPrefix }, separator, blockCommentStartDelimiter,
				blockCommentEndDelimiter);
	}

	/**
	 * 执行给定的 SQL 脚本。
	 * <p>在依次执行所提供的脚本中的各条语句之前，语句分隔符和注释将被移除。
	 * <p><strong>警告</strong>：此方法 <em>不会</em> 释放提供的 {@link Connection}。
	 * @param connection the JDBC connection to use to execute the script; already
	 * configured and ready to use
	 * @param resource the resource (potentially associated with a specific encoding)
	 * to load the SQL script from
	 * @param continueOnError whether to continue without throwing an exception
	 * in the event of an error
	 * @param ignoreFailedDrops whether to continue in the event of specifically
	 * an error on a {@code DROP} statement
	 * @param commentPrefixes the prefixes that identify single-line comments in the
	 * SQL script (typically "--")
	 * @param separator the script statement separator; defaults to
	 * {@value #DEFAULT_STATEMENT_SEPARATOR} if not specified and falls back to
	 * {@value #FALLBACK_STATEMENT_SEPARATOR} as a last resort; may be set to
	 * {@value #EOF_STATEMENT_SEPARATOR} to signal that the script contains a
	 * single statement without a separator
	 * @param blockCommentStartDelimiter the <em>start</em> block comment delimiter
	 * @param blockCommentEndDelimiter the <em>end</em> block comment delimiter
	 * @throws ScriptException if an error occurred while executing the SQL script
	 * @since 5.2
	 * @see #DEFAULT_STATEMENT_SEPARATOR
	 * @see #FALLBACK_STATEMENT_SEPARATOR
	 * @see #EOF_STATEMENT_SEPARATOR
	 * @see org.springframework.jdbc.datasource.DataSourceUtils#getConnection
	 * @see org.springframework.jdbc.datasource.DataSourceUtils#releaseConnection
	 */
	public static void executeSqlScript(Connection connection, EncodedResource resource, boolean continueOnError,
			boolean ignoreFailedDrops, String[] commentPrefixes, @Nullable String separator,
			String blockCommentStartDelimiter, String blockCommentEndDelimiter) throws ScriptException {

		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Executing SQL script from " + resource);
			}
			long startTime = System.currentTimeMillis();

			String script;
			try {
				script = readScript(resource, separator, commentPrefixes, blockCommentEndDelimiter);
			}
			catch (IOException ex) {
				throw new CannotReadScriptException(resource, ex);
			}

			if (separator == null) {
				separator = DEFAULT_STATEMENT_SEPARATOR;
			}
			if (!EOF_STATEMENT_SEPARATOR.equals(separator) &&
					!containsStatementSeparator(resource, script, separator, commentPrefixes,
						blockCommentStartDelimiter, blockCommentEndDelimiter)) {
				separator = FALLBACK_STATEMENT_SEPARATOR;
			}

			List<String> statements = new ArrayList<>();
			splitSqlScript(resource, script, separator, commentPrefixes, blockCommentStartDelimiter,
					blockCommentEndDelimiter, statements);

			int stmtNumber = 0;
			Statement stmt = connection.createStatement();
			try {
				for (String statement : statements) {
					stmtNumber++;
					try {
						stmt.execute(statement);
						int rowsAffected = stmt.getUpdateCount();
						if (logger.isDebugEnabled()) {
							logger.debug(rowsAffected + " returned as update count for SQL: " + statement);
							SQLWarning warningToLog = stmt.getWarnings();
							while (warningToLog != null) {
								logger.debug("SQLWarning ignored: SQL state '" + warningToLog.getSQLState() +
										"', error code '" + warningToLog.getErrorCode() +
										"', message [" + warningToLog.getMessage() + "]");
								warningToLog = warningToLog.getNextWarning();
							}
						}
					}
					catch (SQLException ex) {
						boolean dropStatement = StringUtils.startsWithIgnoreCase(statement.trim(), "drop");
						if (continueOnError || (dropStatement && ignoreFailedDrops)) {
							if (logger.isDebugEnabled()) {
								logger.debug(ScriptStatementFailedException.buildErrorMessage(statement, stmtNumber, resource), ex);
							}
						}
						else {
							throw new ScriptStatementFailedException(statement, stmtNumber, resource, ex);
						}
					}
				}
			}
			finally {
				try {
					stmt.close();
				}
				catch (Throwable ex) {
					logger.trace("Could not close JDBC Statement", ex);
				}
			}

			long elapsedTime = System.currentTimeMillis() - startTime;
			if (logger.isDebugEnabled()) {
				logger.debug("Executed SQL script from " + resource + " in " + elapsedTime + " ms.");
			}
		}
		catch (Exception ex) {
			if (ex instanceof ScriptException) {
				throw (ScriptException) ex;
			}
			throw new UncategorizedScriptException(
				"Failed to execute database script from resource [" + resource + "]", ex);
		}
	}

	/**
	 * 从提供的资源中读取脚本，使用给定的注释前缀和语句分隔符，
	 * 并构建包含各行的 {@code String}。
	 * <p>以其中一个注释前缀 <em>开头</em> 的行会从结果中排除；
	 * 但位于其他位置的行注释 &mdash; 例如语句内部的注释 &mdash; 则会包含在结果中。
	 * @param resource the {@code EncodedResource} containing the script
	 * to be processed
	 * @param separator the statement separator in the SQL script (typically ";")
	 * @param commentPrefixes the prefixes that identify comments in the SQL script
	 * (typically "--")
	 * @param blockCommentEndDelimiter the <em>end</em> block comment delimiter
	 * @return a {@code String} containing the script lines
	 * @throws IOException in case of I/O errors
	 */
	static String readScript(EncodedResource resource, @Nullable String separator,
			String[] commentPrefixes, String blockCommentEndDelimiter) throws IOException {

		try (LineNumberReader lnr = new LineNumberReader(resource.getReader())) {
			return readScript(lnr, commentPrefixes, separator, blockCommentEndDelimiter);
		}
	}

	/**
	 * 从提供的 {@code LineNumberReader} 中读取脚本，使用给定的注释前缀和语句分隔符，
	 * 并构建包含各行的 {@code String}。
	 * <p>以该注释前缀 <em>开头</em> 的行会从结果中排除；
	 * 但位于其他位置的行注释 &mdash; 例如语句内部的注释 &mdash; 则会包含在结果中。
	 * @param lineNumberReader the {@code LineNumberReader} containing the script
	 * to be processed
	 * @param commentPrefix the prefix that identifies comments in the SQL script
	 * (typically "--")
	 * @param separator the statement separator in the SQL script (typically ";")
	 * @param blockCommentEndDelimiter the <em>end</em> block comment delimiter
	 * @return a {@code String} containing the script lines
	 * @throws IOException in case of I/O errors
	 * @deprecated as of Spring Framework 5.2.16 with no plans for replacement.
	 * This is an internal API and will likely be removed in Spring Framework 6.0.
	 */
	@Deprecated
	public static String readScript(LineNumberReader lineNumberReader, @Nullable String commentPrefix,
			@Nullable String separator, @Nullable String blockCommentEndDelimiter) throws IOException {

		String[] commentPrefixes = (commentPrefix != null) ? new String[] { commentPrefix } : null;
		return readScript(lineNumberReader, commentPrefixes, separator, blockCommentEndDelimiter);
	}

	/**
	 * 从提供的 {@code LineNumberReader} 中读取脚本，使用给定的注释前缀和语句分隔符，
	 * 并构建包含各行的 {@code String}。
	 * <p>以其中一个注释前缀 <em>开头</em> 的行会从结果中排除；
	 * 但位于其他位置的行注释 &mdash; 例如语句内部的注释 &mdash; 则会包含在结果中。
	 * @param lineNumberReader the {@code LineNumberReader} containing the script
	 * to be processed
	 * @param commentPrefixes the prefixes that identify comments in the SQL script
	 * (typically "--")
	 * @param separator the statement separator in the SQL script (typically ";")
	 * @param blockCommentEndDelimiter the <em>end</em> block comment delimiter
	 * @return a {@code String} containing the script lines
	 * @throws IOException in case of I/O errors
	 * @since 5.2
	 * @deprecated as of Spring Framework 5.2.16 with no plans for replacement.
	 * This is an internal API and will likely be removed in Spring Framework 6.0.
	 */
	@Deprecated
	public static String readScript(LineNumberReader lineNumberReader, @Nullable String[] commentPrefixes,
			@Nullable String separator, @Nullable String blockCommentEndDelimiter) throws IOException {

		String currentStatement = lineNumberReader.readLine();
		StringBuilder scriptBuilder = new StringBuilder();
		while (currentStatement != null) {
			if ((blockCommentEndDelimiter != null && currentStatement.contains(blockCommentEndDelimiter)) ||
				(commentPrefixes != null && !startsWithAny(currentStatement, commentPrefixes, 0))) {
				if (scriptBuilder.length() > 0) {
					scriptBuilder.append('\n');
				}
				scriptBuilder.append(currentStatement);
			}
			currentStatement = lineNumberReader.readLine();
		}
		appendSeparatorToScriptIfNecessary(scriptBuilder, separator);
		return scriptBuilder.toString();
	}

	private static void appendSeparatorToScriptIfNecessary(StringBuilder scriptBuilder, @Nullable String separator) {
		if (separator == null) {
			return;
		}
		String trimmed = separator.trim();
		if (trimmed.length() == separator.length()) {
			return;
		}
		// separator ends in whitespace, so we might want to see if the script is trying
		// to end the same way
		if (scriptBuilder.lastIndexOf(trimmed) == scriptBuilder.length() - trimmed.length()) {
			scriptBuilder.append(separator.substring(trimmed.length()));
		}
	}

	/**
	 * 判断提供的 SQL 脚本是否包含指定的分隔符。
	 * <p>此方法旨在用于查找分隔各条 SQL 语句的字符串 &mdash; 例如字符 ';'。
	 * <p>如果分隔符出现在由单引号（{@code '}）或双引号（{@code "}）括起来的
	 * <em>字面量</em> 文本块中、通过反斜杠（{@code \}）转义，
	 * 或者位于单行注释或块注释中，则脚本中出现的该分隔符将被忽略。
	 * @param script the SQL script to search within
	 * @param delimiter the statement delimiter to search for
	 * @see #DEFAULT_COMMENT_PREFIXES
	 * @see #DEFAULT_BLOCK_COMMENT_START_DELIMITER
	 * @see #DEFAULT_BLOCK_COMMENT_END_DELIMITER
	 * @deprecated as of Spring Framework 5.2.16 with no plans for replacement.
	 * This is an internal API and will likely be removed in Spring Framework 6.0.
	 */
	@Deprecated
	public static boolean containsSqlScriptDelimiters(String script, String delimiter) {
		return containsStatementSeparator(null, script, delimiter, DEFAULT_COMMENT_PREFIXES,
			DEFAULT_BLOCK_COMMENT_START_DELIMITER, DEFAULT_BLOCK_COMMENT_END_DELIMITER);
	}

	/**
	 * 判断提供的 SQL 脚本是否包含指定的语句分隔符。
	 * <p>此方法旨在用于查找分隔各条 SQL 语句的字符串 &mdash; 例如字符 ';'。
	 * <p>如果分隔符出现在由单引号（{@code '}）或双引号（{@code "}）括起来的
	 * <em>字面量</em> 文本块中、通过反斜杠（{@code \}）转义，
	 * 或者位于单行注释或块注释中，则脚本中出现的该分隔符将被忽略。
	 * @param resource the resource from which the script was read, or {@code null}
	 * if unknown
	 * @param script the SQL script to search within
	 * @param separator the statement separator to search for
	 * @param commentPrefixes the prefixes that identify single-line comments
	 * (typically {@code "--"})
	 * @param blockCommentStartDelimiter the <em>start</em> block comment delimiter
	 * (typically {@code "/*"})
	 * @param blockCommentEndDelimiter the <em>end</em> block comment delimiter
	 * (typically <code>"*&#47;"</code>)
	 * @since 5.2.16
	 */
	private static boolean containsStatementSeparator(@Nullable EncodedResource resource, String script,
			String separator, String[] commentPrefixes, String blockCommentStartDelimiter,
			String blockCommentEndDelimiter) throws ScriptException {

		boolean inSingleQuote = false;
		boolean inDoubleQuote = false;
		boolean inEscape = false;

		for (int i = 0; i < script.length(); i++) {
			char c = script.charAt(i);
			if (inEscape) {
				inEscape = false;
				continue;
			}
			// MySQL style escapes
			if (c == '\\') {
				inEscape = true;
				continue;
			}
			if (!inDoubleQuote && (c == '\'')) {
				inSingleQuote = !inSingleQuote;
			}
			else if (!inSingleQuote && (c == '"')) {
				inDoubleQuote = !inDoubleQuote;
			}
			if (!inSingleQuote && !inDoubleQuote) {
				if (script.startsWith(separator, i)) {
					return true;
				}
				else if (startsWithAny(script, commentPrefixes, i)) {
					// Skip over any content from the start of the comment to the EOL
					int indexOfNextNewline = script.indexOf('\n', i);
					if (indexOfNextNewline > i) {
						i = indexOfNextNewline;
						continue;
					}
					else {
						// If there's no EOL, we must be at the end of the script, so stop here.
						break;
					}
				}
				else if (script.startsWith(blockCommentStartDelimiter, i)) {
					// Skip over any block comments
					int indexOfCommentEnd = script.indexOf(blockCommentEndDelimiter, i);
					if (indexOfCommentEnd > i) {
						i = indexOfCommentEnd + blockCommentEndDelimiter.length() - 1;
						continue;
					}
					else {
						throw new ScriptParseException(
								"Missing block comment end delimiter: " + blockCommentEndDelimiter, resource);
					}
				}
			}
		}

		return false;
	}

	/**
	 * 将 SQL 脚本拆分为由所提供的分隔字符分隔的独立语句。每条独立语句都会被添加到
	 * 所提供的 {@code List} 中。
	 * <p>在脚本中，{@value #DEFAULT_COMMENT_PREFIX} 将用作注释前缀；
	 * 以该注释前缀开头并延伸到行尾的任何文本都会从输出中省略。同样，
	 * {@value #DEFAULT_BLOCK_COMMENT_START_DELIMITER} 和
	 * {@value #DEFAULT_BLOCK_COMMENT_END_DELIMITER} 将分别用作
	 * 块注释的 <em>起始</em> 和 <em>结束</em> 分隔符：块注释中包含的任何文本
	 * 都会从输出中省略。此外，多个相邻的空白字符将被合并为一个空格。
	 * @param script the SQL script
	 * @param separator character separating each statement (typically a ';')
	 * @param statements the list that will contain the individual statements
	 * @throws ScriptException if an error occurred while splitting the SQL script
	 * @see #splitSqlScript(String, String, List)
	 * @see #splitSqlScript(EncodedResource, String, String, String, String, String, List)
	 * @deprecated as of Spring Framework 5.2.16 with no plans for replacement.
	 * This is an internal API and will likely be removed in Spring Framework 6.0.
	 */
	@Deprecated
	public static void splitSqlScript(String script, char separator, List<String> statements) throws ScriptException {
		splitSqlScript(script, String.valueOf(separator), statements);
	}

	/**
	 * 将 SQL 脚本拆分为由所提供的分隔字符串分隔的独立语句。每条独立语句都会被添加到
	 * 所提供的 {@code List} 中。
	 * <p>在脚本中，{@value #DEFAULT_COMMENT_PREFIX} 将用作注释前缀；
	 * 以该注释前缀开头并延伸到行尾的任何文本都会从输出中省略。同样，
	 * {@value #DEFAULT_BLOCK_COMMENT_START_DELIMITER} 和
	 * {@value #DEFAULT_BLOCK_COMMENT_END_DELIMITER} 将分别用作
	 * 块注释的 <em>起始</em> 和 <em>结束</em> 分隔符：块注释中包含的任何文本
	 * 都会从输出中省略。此外，多个相邻的空白字符将被合并为一个空格。
	 * @param script the SQL script
	 * @param separator text separating each statement
	 * (typically a ';' or newline character)
	 * @param statements the list that will contain the individual statements
	 * @throws ScriptException if an error occurred while splitting the SQL script
	 * @see #splitSqlScript(String, char, List)
	 * @see #splitSqlScript(EncodedResource, String, String, String, String, String, List)
	 * @deprecated as of Spring Framework 5.2.16 with no plans for replacement.
	 * This is an internal API and will likely be removed in Spring Framework 6.0.
	 */
	@Deprecated
	public static void splitSqlScript(String script, String separator, List<String> statements) throws ScriptException {
		splitSqlScript(null, script, separator, DEFAULT_COMMENT_PREFIX, DEFAULT_BLOCK_COMMENT_START_DELIMITER,
				DEFAULT_BLOCK_COMMENT_END_DELIMITER, statements);
	}

	/**
	 * 将 SQL 脚本拆分为由所提供的分隔字符串分隔的独立语句。每条独立语句都会被添加到
	 * 所提供的 {@code List} 中。
	 * <p>在脚本中，将遵循所提供的 {@code commentPrefix}：
	 * 以该注释前缀开头并延伸到行尾的任何文本都会从输出中省略。同样，
	 * 将遵循所提供的 {@code blockCommentStartDelimiter} 和
	 * {@code blockCommentEndDelimiter} 分隔符：块注释中包含的任何文本
	 * 都会从输出中省略。此外，多个相邻的空白字符将被合并为一个空格。
	 * @param resource the resource from which the script was read
	 * @param script the SQL script
	 * @param separator text separating each statement
	 * (typically a ';' or newline character)
	 * @param commentPrefix the prefix that identifies SQL line comments
	 * (typically "--")
	 * @param blockCommentStartDelimiter the <em>start</em> block comment delimiter;
	 * never {@code null} or empty
	 * @param blockCommentEndDelimiter the <em>end</em> block comment delimiter;
	 * never {@code null} or empty
	 * @param statements the list that will contain the individual statements
	 * @throws ScriptException if an error occurred while splitting the SQL script
	 * @deprecated as of Spring Framework 5.2.16 with no plans for replacement.
	 * This is an internal API and will likely be removed in Spring Framework 6.0.
	 */
	@Deprecated
	public static void splitSqlScript(@Nullable EncodedResource resource, String script,
			String separator, String commentPrefix, String blockCommentStartDelimiter,
			String blockCommentEndDelimiter, List<String> statements) throws ScriptException {

		Assert.hasText(commentPrefix, "'commentPrefix' must not be null or empty");
		splitSqlScript(resource, script, separator, new String[] { commentPrefix },
				blockCommentStartDelimiter, blockCommentEndDelimiter, statements);
	}

	/**
	 * 将 SQL 脚本拆分为由所提供的分隔字符串分隔的独立语句。每条独立语句都会被添加到
	 * 所提供的 {@code List} 中。
	 * <p>在脚本中，将遵循所提供的 {@code commentPrefixes}：
	 * 以其中一个注释前缀开头并延伸到行尾的任何文本都会从输出中省略。同样，
	 * 将遵循所提供的 {@code blockCommentStartDelimiter} 和
	 * {@code blockCommentEndDelimiter} 分隔符：块注释中包含的任何文本
	 * 都会从输出中省略。此外，多个相邻的空白字符将被合并为一个空格。
	 * @param resource the resource from which the script was read
	 * @param script the SQL script
	 * @param separator text separating each statement
	 * (typically a ';' or newline character)
	 * @param commentPrefixes the prefixes that identify SQL line comments
	 * (typically "--")
	 * @param blockCommentStartDelimiter the <em>start</em> block comment delimiter;
	 * never {@code null} or empty
	 * @param blockCommentEndDelimiter the <em>end</em> block comment delimiter;
	 * never {@code null} or empty
	 * @param statements the list that will contain the individual statements
	 * @throws ScriptException if an error occurred while splitting the SQL script
	 * @since 5.2
	 * @deprecated as of Spring Framework 5.2.16 with no plans for replacement.
	 * This is an internal API and will likely be removed in Spring Framework 6.0.
	 */
	@Deprecated
	public static void splitSqlScript(@Nullable EncodedResource resource, String script,
			String separator, String[] commentPrefixes, String blockCommentStartDelimiter,
			String blockCommentEndDelimiter, List<String> statements) throws ScriptException {

		Assert.hasText(script, "'script' must not be null or empty");
		Assert.notNull(separator, "'separator' must not be null");
		Assert.notEmpty(commentPrefixes, "'commentPrefixes' must not be null or empty");
		for (String commentPrefix : commentPrefixes) {
			Assert.hasText(commentPrefix, "'commentPrefixes' must not contain null or empty elements");
		}
		Assert.hasText(blockCommentStartDelimiter, "'blockCommentStartDelimiter' must not be null or empty");
		Assert.hasText(blockCommentEndDelimiter, "'blockCommentEndDelimiter' must not be null or empty");

		StringBuilder sb = new StringBuilder();
		boolean inSingleQuote = false;
		boolean inDoubleQuote = false;
		boolean inEscape = false;

		for (int i = 0; i < script.length(); i++) {
			char c = script.charAt(i);
			if (inEscape) {
				inEscape = false;
				sb.append(c);
				continue;
			}
			// MySQL style escapes
			if (c == '\\') {
				inEscape = true;
				sb.append(c);
				continue;
			}
			if (!inDoubleQuote && (c == '\'')) {
				inSingleQuote = !inSingleQuote;
			}
			else if (!inSingleQuote && (c == '"')) {
				inDoubleQuote = !inDoubleQuote;
			}
			if (!inSingleQuote && !inDoubleQuote) {
				if (script.startsWith(separator, i)) {
					// We've reached the end of the current statement
					if (sb.length() > 0) {
						statements.add(sb.toString());
						sb = new StringBuilder();
					}
					i += separator.length() - 1;
					continue;
				}
				else if (startsWithAny(script, commentPrefixes, i)) {
					// Skip over any content from the start of the comment to the EOL
					int indexOfNextNewline = script.indexOf('\n', i);
					if (indexOfNextNewline > i) {
						i = indexOfNextNewline;
						continue;
					}
					else {
						// If there's no EOL, we must be at the end of the script, so stop here.
						break;
					}
				}
				else if (script.startsWith(blockCommentStartDelimiter, i)) {
					// Skip over any block comments
					int indexOfCommentEnd = script.indexOf(blockCommentEndDelimiter, i);
					if (indexOfCommentEnd > i) {
						i = indexOfCommentEnd + blockCommentEndDelimiter.length() - 1;
						continue;
					}
					else {
						throw new ScriptParseException(
								"Missing block comment end delimiter: " + blockCommentEndDelimiter, resource);
					}
				}
				else if (c == ' ' || c == '\r' || c == '\n' || c == '\t') {
					// Avoid multiple adjacent whitespace characters
					if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ') {
						c = ' ';
					}
					else {
						continue;
					}
				}
			}
			sb.append(c);
		}

		if (StringUtils.hasText(sb)) {
			statements.add(sb.toString());
		}
	}

	private static boolean startsWithAny(String script, String[] prefixes, int offset) {
		for (String prefix : prefixes) {
			if (script.startsWith(prefix, offset)) {
				return true;
			}
		}
		return false;
	}

}
