/*
 * Copyright 2002-2019 the original author or authors.
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

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 使用定义在外部资源中的 SQL 脚本填充、初始化或清理数据库。
 *
 * <ul>
 * <li>调用 {@link #addScript} 添加单个 SQL 脚本位置。
 * <li>调用 {@link #addScripts} 添加多个 SQL 脚本位置。
 * <li>查阅本类中的 setter 方法以了解进一步的配置选项。
 * <li>调用 {@link #populate} 或 {@link #execute}，使用已配置的脚本初始化或清理数据库。
 * </ul>
 *
 * @author Keith Donald
 * @author Dave Syer
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Oliver Gierke
 * @author Sam Brannen
 * @author Chris Baldwin
 * @author Phillip Webb
 * @since 3.0
 * @see DatabasePopulatorUtils
 * @see ScriptUtils
 */
public class ResourceDatabasePopulator implements DatabasePopulator {

	List<Resource> scripts = new ArrayList<>();

	@Nullable
	private String sqlScriptEncoding;

	private String separator = ScriptUtils.DEFAULT_STATEMENT_SEPARATOR;

	private String[] commentPrefixes = ScriptUtils.DEFAULT_COMMENT_PREFIXES;

	private String blockCommentStartDelimiter = ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER;

	private String blockCommentEndDelimiter = ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER;

	private boolean continueOnError = false;

	private boolean ignoreFailedDrops = false;


	/**
	 * 使用默认设置构造一个新的 {@code ResourceDatabasePopulator}。
	 * @since 4.0.3
	 */
	public ResourceDatabasePopulator() {
	}

	/**
	 * 使用默认设置针对提供的脚本构造一个新的 {@code ResourceDatabasePopulator}。
	 * @param scripts the scripts to execute to initialize or clean up the database
	 * (never {@code null})
	 * @since 4.0.3
	 */
	public ResourceDatabasePopulator(Resource... scripts) {
		setScripts(scripts);
	}

	/**
	 * 使用提供的值构造一个新的 {@code ResourceDatabasePopulator}。
	 * @param continueOnError flag to indicate that all failures in SQL should be
	 * logged but not cause a failure
	 * @param ignoreFailedDrops flag to indicate that a failed SQL {@code DROP}
	 * statement can be ignored
	 * @param sqlScriptEncoding the encoding for the supplied SQL scripts
	 * (may be {@code null} or <em>empty</em> to indicate platform encoding)
	 * @param scripts the scripts to execute to initialize or clean up the database
	 * (never {@code null})
	 * @since 4.0.3
	 */
	public ResourceDatabasePopulator(boolean continueOnError, boolean ignoreFailedDrops,
			@Nullable String sqlScriptEncoding, Resource... scripts) {

		this.continueOnError = continueOnError;
		this.ignoreFailedDrops = ignoreFailedDrops;
		setSqlScriptEncoding(sqlScriptEncoding);
		setScripts(scripts);
	}


	/**
	 * 添加一个用于初始化或清理数据库的脚本。
	 * @param script the path to an SQL script (never {@code null})
	 */
	public void addScript(Resource script) {
		Assert.notNull(script, "'script' must not be null");
		this.scripts.add(script);
	}

	/**
	 * 添加多个用于初始化或清理数据库的脚本。
	 * @param scripts the scripts to execute (never {@code null})
	 */
	public void addScripts(Resource... scripts) {
		assertContentsOfScriptArray(scripts);
		this.scripts.addAll(Arrays.asList(scripts));
	}

	/**
	 * 设置用于初始化或清理数据库的脚本，替换之前添加的所有脚本。
	 * @param scripts the scripts to execute (never {@code null})
	 */
	public void setScripts(Resource... scripts) {
		assertContentsOfScriptArray(scripts);
		// Ensure that the list is modifiable
		this.scripts = new ArrayList<>(Arrays.asList(scripts));
	}

	private void assertContentsOfScriptArray(Resource... scripts) {
		Assert.notNull(scripts, "'scripts' must not be null");
		Assert.noNullElements(scripts, "'scripts' must not contain null elements");
	}

	/**
	 * 指定所配置 SQL 脚本的编码（如果与平台编码不同）。
	 * @param sqlScriptEncoding the encoding used in scripts
	 * (may be {@code null} or empty to indicate platform encoding)
	 * @see #addScript(Resource)
	 */
	public void setSqlScriptEncoding(@Nullable String sqlScriptEncoding) {
		this.sqlScriptEncoding = (StringUtils.hasText(sqlScriptEncoding) ? sqlScriptEncoding : null);
	}

	/**
	 * 指定语句分隔符（如果需要自定义）。
	 * <p>如果未指定，默认为 {@code ";"}，最后会回退到 {@code "\n"}；
	 * 可以设置为 {@link ScriptUtils#EOF_STATEMENT_SEPARATOR}，
	 * 表示每个脚本只包含一条语句且没有分隔符。
	 * @param separator the script statement separator
	 */
	public void setSeparator(String separator) {
		this.separator = separator;
	}

	/**
	 * 设置用于标识 SQL 脚本中单行注释的前缀。
	 * <p>默认为 {@code "--"}。
	 * @param commentPrefix the prefix for single-line comments
	 * @see #setCommentPrefixes(String...)
	 */
	public void setCommentPrefix(String commentPrefix) {
		Assert.hasText(commentPrefix, "'commentPrefix' must not be null or empty");
		this.commentPrefixes = new String[] { commentPrefix };
	}

	/**
	 * 设置用于标识 SQL 脚本中单行注释的各个前缀。
	 * <p>默认为 {@code ["--"]}。
	 * @param commentPrefixes the prefixes for single-line comments
	 * @since 5.2
	 */
	public void setCommentPrefixes(String... commentPrefixes) {
		Assert.notEmpty(commentPrefixes, "'commentPrefixes' must not be null or empty");
		Assert.noNullElements(commentPrefixes, "'commentPrefixes' must not contain null elements");
		this.commentPrefixes = commentPrefixes;
	}

	/**
	 * 设置用于标识 SQL 脚本中块注释的起始分隔符。
	 * <p>默认为 {@code "/*"}。
	 * @param blockCommentStartDelimiter the start delimiter for block comments
	 * (never {@code null} or empty)
	 * @since 4.0.3
	 * @see #setBlockCommentEndDelimiter
	 */
	public void setBlockCommentStartDelimiter(String blockCommentStartDelimiter) {
		Assert.hasText(blockCommentStartDelimiter, "'blockCommentStartDelimiter' must not be null or empty");
		this.blockCommentStartDelimiter = blockCommentStartDelimiter;
	}

	/**
	 * 设置用于标识 SQL 脚本中块注释的结束分隔符。
	 * <p>默认为 <code>"*&#47;"</code>。
	 * @param blockCommentEndDelimiter the end delimiter for block comments
	 * (never {@code null} or empty)
	 * @since 4.0.3
	 * @see #setBlockCommentStartDelimiter
	 */
	public void setBlockCommentEndDelimiter(String blockCommentEndDelimiter) {
		Assert.hasText(blockCommentEndDelimiter, "'blockCommentEndDelimiter' must not be null or empty");
		this.blockCommentEndDelimiter = blockCommentEndDelimiter;
	}

	/**
	 * 用于指示是否应将 SQL 中的所有失败记录到日志而不导致失败的标志。
	 * <p>默认为 {@code false}。
	 * @param continueOnError {@code true} if script execution should continue on error
	 */
	public void setContinueOnError(boolean continueOnError) {
		this.continueOnError = continueOnError;
	}

	/**
	 * 用于指示是否可以忽略失败的 SQL {@code DROP} 语句的标志。
	 * <p>这对于 SQL 方言不支持在 {@code DROP} 语句中使用 {@code IF EXISTS}
	 * 子句的非嵌入式数据库很有用。
	 * <p>默认值为 {@code false}，因此如果填充器被意外运行，而脚本以
	 * {@code DROP} 语句开头，它将会快速失败。
	 * @param ignoreFailedDrops {@code true} if failed drop statements should be ignored
	 */
	public void setIgnoreFailedDrops(boolean ignoreFailedDrops) {
		this.ignoreFailedDrops = ignoreFailedDrops;
	}


	/**
	 * {@inheritDoc}
	 * @see #execute(DataSource)
	 */
	@Override
	public void populate(Connection connection) throws ScriptException {
		Assert.notNull(connection, "'connection' must not be null");
		for (Resource script : this.scripts) {
			EncodedResource encodedScript = new EncodedResource(script, this.sqlScriptEncoding);
			ScriptUtils.executeSqlScript(connection, encodedScript, this.continueOnError, this.ignoreFailedDrops,
					this.commentPrefixes, this.separator, this.blockCommentStartDelimiter, this.blockCommentEndDelimiter);
		}
	}

	/**
	 * 针对给定的 {@link DataSource} 执行此 {@code ResourceDatabasePopulator}。
	 * <p>委托给 {@link DatabasePopulatorUtils#execute}。
	 * @param dataSource the {@code DataSource} to execute against (never {@code null})
	 * @throws ScriptException if an error occurs
	 * @since 4.1
	 * @see #populate(Connection)
	 */
	public void execute(DataSource dataSource) throws ScriptException {
		DatabasePopulatorUtils.execute(this, dataSource);
	}

}
