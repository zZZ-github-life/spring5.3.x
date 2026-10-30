/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.r2dbc.connection.init;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.r2dbc.spi.Connection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 使用定义在外部资源中的 SQL 脚本填充、初始化或清理数据库。
 * <ul>
 * <li>调用 {@link #addScript} 添加单个 SQL 脚本位置。
 * <li>调用 {@link #addScripts} 添加多个 SQL 脚本位置。
 * <li>查阅此类中的 setter 方法以了解进一步的配置选项。
 * <li>调用 {@link #populate} 使用已配置的脚本初始化或清理数据库。
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
 * @author Mark Paluch
 * @since 5.3
 * @see ScriptUtils
 */
public class ResourceDatabasePopulator implements DatabasePopulator {

	List<Resource> scripts = new ArrayList<>();

	@Nullable
	private Charset sqlScriptEncoding;

	private String separator = ScriptUtils.DEFAULT_STATEMENT_SEPARATOR;

	private String[] commentPrefixes = ScriptUtils.DEFAULT_COMMENT_PREFIXES;

	private String blockCommentStartDelimiter = ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER;

	private String blockCommentEndDelimiter = ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER;

	private boolean continueOnError = false;

	private boolean ignoreFailedDrops = false;

	private DataBufferFactory dataBufferFactory = DefaultDataBufferFactory.sharedInstance;


	/**
	 * 使用默认设置创建一个新的 {@code ResourceDatabasePopulator}。
	 */
	public ResourceDatabasePopulator() {
	}

	/**
	 * 为所提供的脚本使用默认设置创建一个新的 {@code ResourceDatabasePopulator}。
	 * @param scripts 用于初始化或清理数据库的脚本
	 * （永不为 {@code null}）
	 */
	public ResourceDatabasePopulator(Resource... scripts) {
		setScripts(scripts);
	}

	/**
	 * 使用所提供的值构造一个新的 {@code ResourceDatabasePopulator}。
	 * @param continueOnError 标志，表示 SQL 中的所有失败都应记录日志，
	 * 但不导致失败
	 * @param ignoreFailedDrops 标志，表示可以忽略失败的 SQL {@code DROP}
	 * 语句
	 * @param sqlScriptEncoding 所提供 SQL 脚本的编码
	 * （可以是 {@code null} 或 <em>空</em>，表示使用平台编码）
	 * @param scripts 用于初始化或清理数据库的脚本
	 * （永不为 {@code null}）
	 */
	public ResourceDatabasePopulator(boolean continueOnError, boolean ignoreFailedDrops,
			@Nullable String sqlScriptEncoding, Resource... scripts) {

		this.continueOnError = continueOnError;
		this.ignoreFailedDrops = ignoreFailedDrops;
		setSqlScriptEncoding(sqlScriptEncoding);
		setScripts(scripts);
	}


	/**
	 * 添加一个用于初始化或清理数据库的待执行脚本。
	 * @param script SQL 脚本的路径（永不为 {@code null}）
	 */
	public void addScript(Resource script) {
		Assert.notNull(script, "'script' must not be null");
		this.scripts.add(script);
	}

	/**
	 * 添加多个用于初始化或清理数据库的待执行脚本。
	 * @param scripts 要执行的脚本（永不为 {@code null}）
	 */
	public void addScripts(Resource... scripts) {
		assertContentsOfScriptArray(scripts);
		this.scripts.addAll(Arrays.asList(scripts));
	}

	/**
	 * 设置用于初始化或清理数据库的待执行脚本，
	 * 替换之前添加的任何脚本。
	 * @param scripts 要执行的脚本（永不为 {@code null}）
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
	 * 指定已配置 SQL 脚本的编码，
	 * 前提是它与平台编码不同。
	 * @param sqlScriptEncoding 脚本中使用的编码
	 * （可以是 {@code null} 或空，表示使用平台编码）
	 * @see #addScript(Resource)
	 */
	public void setSqlScriptEncoding(@Nullable String sqlScriptEncoding) {
		setSqlScriptEncoding(StringUtils.hasText(sqlScriptEncoding) ? Charset.forName(sqlScriptEncoding) : null);
	}

	/**
	 * 指定已配置 SQL 脚本的编码，
	 * 前提是它与平台编码不同。
	 * @param sqlScriptEncoding 脚本中使用的编码
	 * （可以是 {@code null} 或空，表示使用平台编码）
	 * @see #addScript(Resource)
	 */
	public void setSqlScriptEncoding(@Nullable Charset sqlScriptEncoding) {
		this.sqlScriptEncoding = sqlScriptEncoding;
	}

	/**
	 * 指定语句分隔符（如果需要自定义）。
	 * <p>如果未指定，默认使用 {@code ";"}，最后则回退为 {@code "\n"}；
	 * 可以将其设置为 {@link ScriptUtils#EOF_STATEMENT_SEPARATOR}，
	 * 以表示每个脚本只包含一条不带分隔符的语句。
	 * @param separator 脚本语句分隔符
	 */
	public void setSeparator(String separator) {
		this.separator = separator;
	}

	/**
	 * 设置用于标识 SQL 脚本中单行注释的前缀。
	 * <p>默认值为 {@code "--"}。
	 * @param commentPrefix 单行注释的前缀
	 * @see #setCommentPrefixes(String...)
	 */
	public void setCommentPrefix(String commentPrefix) {
		Assert.hasText(commentPrefix, "'commentPrefix' must not be null or empty");
		this.commentPrefixes = new String[] { commentPrefix };
	}

	/**
	 * 设置用于标识 SQL 脚本中单行注释的各个前缀。
	 * <p>默认值为 {@code ["--"]}。
	 * @param commentPrefixes 单行注释的多个前缀
	 */
	public void setCommentPrefixes(String... commentPrefixes) {
		Assert.notEmpty(commentPrefixes, "'commentPrefixes' must not be null or empty");
		Assert.noNullElements(commentPrefixes, "'commentPrefixes' must not contain null elements");
		this.commentPrefixes = commentPrefixes;
	}

	/**
	 * 设置用于标识 SQL 脚本中块注释的起始分隔符。
	 * <p>默认值为 {@code "/*"}。
	 * @param blockCommentStartDelimiter 块注释的起始分隔符
	 * （永不为 {@code null} 或空）
	 * @see #setBlockCommentEndDelimiter
	 */
	public void setBlockCommentStartDelimiter(String blockCommentStartDelimiter) {
		Assert.hasText(blockCommentStartDelimiter, "'blockCommentStartDelimiter' must not be null or empty");
		this.blockCommentStartDelimiter = blockCommentStartDelimiter;
	}

	/**
	 * 设置用于标识 SQL 脚本中块注释的结束分隔符。
	 * <p>默认值为 <code>"*&#47;"</code>。
	 * @param blockCommentEndDelimiter 块注释的结束分隔符
	 * （永不为 {@code null} 或空）
	 * @see #setBlockCommentStartDelimiter
	 */
	public void setBlockCommentEndDelimiter(String blockCommentEndDelimiter) {
		Assert.hasText(blockCommentEndDelimiter, "'blockCommentEndDelimiter' must not be null or empty");
		this.blockCommentEndDelimiter = blockCommentEndDelimiter;
	}

	/**
	 * 指示是否应记录 SQL 中的所有失败但不将其视为失败的标志。
	 * <p>默认值为 {@code false}。
	 * @param continueOnError 如果脚本执行在出错时应继续进行，则为 {@code true}
	 */
	public void setContinueOnError(boolean continueOnError) {
		this.continueOnError = continueOnError;
	}

	/**
	 * 指示是否可以忽略失败的 SQL {@code DROP} 语句的标志。
	 * <p>这对于 SQL 方言不支持在 {@code DROP} 语句中使用
	 * {@code IF EXISTS} 子句的非嵌入式数据库很有用。
	 * <p>默认值为 {@code false}，这样如果 populator 被意外运行，
	 * 且脚本以 {@code DROP} 语句开头，它将会快速失败。
	 * @param ignoreFailedDrops 如果应忽略失败的 drop 语句，则为 {@code true}
	 */
	public void setIgnoreFailedDrops(boolean ignoreFailedDrops) {
		this.ignoreFailedDrops = ignoreFailedDrops;
	}

	/**
	 * 设置用于 {@link Resource} 加载的 {@link DataBufferFactory}。
	 * <p>默认值为 {@link DefaultDataBufferFactory}。
	 * @param dataBufferFactory 要使用的 {@link DataBufferFactory}，不得为 {@code null}
	 */
	public void setDataBufferFactory(DataBufferFactory dataBufferFactory) {
		Assert.notNull(dataBufferFactory, "DataBufferFactory must not be null");
		this.dataBufferFactory = dataBufferFactory;
	}


	@Override
	public Mono<Void> populate(Connection connection) throws ScriptException {
		Assert.notNull(connection, "Connection must not be null");
		return Flux.fromIterable(this.scripts).concatMap(resource -> {
			EncodedResource encodedScript = new EncodedResource(resource, this.sqlScriptEncoding);
			return ScriptUtils.executeSqlScript(connection, encodedScript, this.dataBufferFactory, this.continueOnError,
					this.ignoreFailedDrops, this.commentPrefixes, this.separator, this.blockCommentStartDelimiter,
					this.blockCommentEndDelimiter);
		}).then();
	}

}
