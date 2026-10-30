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

package org.springframework.jdbc.datasource.embedded;

import javax.sql.DataSource;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.util.Assert;

/**
 * 一个构建器，提供用于构造嵌入式数据库的便捷 API。
 *
 * <h3>用法示例</h3>
 * <pre class="code">
 * EmbeddedDatabase db = new EmbeddedDatabaseBuilder()
 *     .generateUniqueName(true)
 *     .setType(H2)
 *     .setScriptEncoding("UTF-8")
 *     .ignoreFailedDrops(true)
 *     .addScript("schema.sql")
 *     .addScripts("user_data.sql", "country_data.sql")
 *     .build();
 *
 * // 对数据库执行操作（EmbeddedDatabase 继承自 javax.sql.DataSource）
 *
 * db.shutdown();
 * </pre>
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Dave Syer
 * @author Sam Brannen
 * @since 3.0
 * @see org.springframework.jdbc.datasource.init.ScriptUtils
 * @see org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
 * @see org.springframework.jdbc.datasource.init.DatabasePopulatorUtils
 */
public class EmbeddedDatabaseBuilder {

	private final EmbeddedDatabaseFactory databaseFactory;

	private final ResourceDatabasePopulator databasePopulator;

	private final ResourceLoader resourceLoader;


	/**
	 * 使用 {@link DefaultResourceLoader} 创建一个新的嵌入式数据库构建器。
	 */
	public EmbeddedDatabaseBuilder() {
		this(new DefaultResourceLoader());
	}

	/**
	 * 使用给定的 {@link ResourceLoader} 创建一个新的嵌入式数据库构建器。
	 * @param resourceLoader 要委托给它的 {@code ResourceLoader}
	 */
	public EmbeddedDatabaseBuilder(ResourceLoader resourceLoader) {
		this.databaseFactory = new EmbeddedDatabaseFactory();
		this.databasePopulator = new ResourceDatabasePopulator();
		this.databaseFactory.setDatabasePopulator(this.databasePopulator);
		this.resourceLoader = resourceLoader;
	}

	/**
	 * 指定是否应生成唯一 ID 并将其用作数据库名称。
	 * <p>如果此构建器的配置在单个 JVM 中的多个应用程序上下文之间被重用，
	 * 则应<em>启用</em>此标志（即设置为 {@code true}），
	 * 以确保每个应用程序上下文都获得自己的嵌入式数据库。
	 * <p>启用此标志将覆盖通过 {@link #setName} 设置的任何显式名称。
	 * @param flag 如果应生成唯一的数据库名称，则为 {@code true}
	 * @return {@code this}，以便进行方法链式调用
	 * @since 4.2
	 * @see #setName
	 */
	public EmbeddedDatabaseBuilder generateUniqueName(boolean flag) {
		this.databaseFactory.setGenerateUniqueDatabaseName(flag);
		return this;
	}

	/**
	 * 设置嵌入式数据库的名称。
	 * <p>如果未调用，则默认为 {@link EmbeddedDatabaseFactory#DEFAULT_DATABASE_NAME}。
	 * <p>如果 {@code generateUniqueName} 标志已设置为 {@code true}，则会被覆盖。
	 * @param databaseName 要构建的内嵌数据库的名称
	 * @return {@code this}，以便进行方法链式调用
	 * @see #generateUniqueName
	 */
	public EmbeddedDatabaseBuilder setName(String databaseName) {
		this.databaseFactory.setDatabaseName(databaseName);
		return this;
	}

	/**
	 * 设置嵌入式数据库的类型。
	 * <p>如果未调用，则默认为 HSQL。
	 * @param databaseType 要构建的内嵌数据库类型
	 * @return {@code this}，以便进行方法链式调用
	 */
	public EmbeddedDatabaseBuilder setType(EmbeddedDatabaseType databaseType) {
		this.databaseFactory.setDatabaseType(databaseType);
		return this;
	}

	/**
	 * 设置用于创建连接嵌入式数据库的 {@link DataSource} 实例的工厂。
	 * <p>默认为 {@link SimpleDriverDataSourceFactory}，但可以重写，
	 * 例如为了引入连接池。
	 * @return {@code this}，以便进行方法链式调用
	 * @since 4.0.3
	 */
	public EmbeddedDatabaseBuilder setDataSourceFactory(DataSourceFactory dataSourceFactory) {
		Assert.notNull(dataSourceFactory, "DataSourceFactory is required");
		this.databaseFactory.setDataSourceFactory(dataSourceFactory);
		return this;
	}

	/**
	 * 添加要执行的默认 SQL 脚本以填充数据库。
	 * <p>默认脚本为 {@code "schema.sql"}（用于创建数据库 schema）
	 * 和 {@code "data.sql"}（用于向数据库填充数据）。
	 * @return {@code this}，以便进行方法链式调用
	 */
	public EmbeddedDatabaseBuilder addDefaultScripts() {
		return addScripts("schema.sql", "data.sql");
	}

	/**
	 * 添加要执行的 SQL 脚本以初始化或填充数据库。
	 * @param script 要执行的脚本
	 * @return {@code this}，以便进行方法链式调用
	 */
	public EmbeddedDatabaseBuilder addScript(String script) {
		this.databasePopulator.addScript(this.resourceLoader.getResource(script));
		return this;
	}

	/**
	 * 添加多个要执行的 SQL 脚本以初始化或填充数据库。
	 * @param scripts 要执行的脚本
	 * @return {@code this}，以便进行方法链式调用
	 * @since 4.0.3
	 */
	public EmbeddedDatabaseBuilder addScripts(String... scripts) {
		for (String script : scripts) {
			addScript(script);
		}
		return this;
	}

	/**
	 * 指定所有 SQL 脚本中使用的字符编码（如果与平台编码不同）。
	 * @param scriptEncoding 脚本中使用的编码
	 * @return {@code this}，以便进行方法链式调用
	 * @since 4.0.3
	 */
	public EmbeddedDatabaseBuilder setScriptEncoding(String scriptEncoding) {
		this.databasePopulator.setSqlScriptEncoding(scriptEncoding);
		return this;
	}

	/**
	 * 指定所有 SQL 脚本中使用的语句分隔符（如果是自定义分隔符）。
	 * <p>如果未指定，则默认为 {@code ";"}，最后的手段是回退到 {@code "\n"}；
	 * 可以设置为 {@link ScriptUtils#EOF_STATEMENT_SEPARATOR}，
	 * 以表示每个脚本只包含一个不带分隔符的语句。
	 * @param separator 语句分隔符
	 * @return {@code this}，以便进行方法链式调用
	 * @since 4.0.3
	 */
	public EmbeddedDatabaseBuilder setSeparator(String separator) {
		this.databasePopulator.setSeparator(separator);
		return this;
	}

	/**
	 * 指定所有 SQL 脚本中使用的单行注释前缀。
	 * <p>默认为 {@code "--"}。
	 * @param commentPrefix 单行注释的前缀
	 * @return {@code this}，以便进行方法链式调用
	 * @since 4.0.3
	 * @see #setCommentPrefixes(String...)
	 */
	public EmbeddedDatabaseBuilder setCommentPrefix(String commentPrefix) {
		this.databasePopulator.setCommentPrefix(commentPrefix);
		return this;
	}

	/**
	 * 指定用于标识所有 SQL 脚本中单行注释的前缀。
	 * <p>默认为 {@code ["--"]}。
	 * @param commentPrefixes 单行注释的前缀
	 * @return {@code this}，以便进行方法链式调用
	 * @since 5.2
	 */
	public EmbeddedDatabaseBuilder setCommentPrefixes(String... commentPrefixes) {
		this.databasePopulator.setCommentPrefixes(commentPrefixes);
		return this;
	}

	/**
	 * 指定所有 SQL 脚本中块注释的起始分隔符。
	 * <p>默认为 {@code "/*"}。
	 * @param blockCommentStartDelimiter 块注释的起始分隔符
	 * @return {@code this}，以便进行方法链式调用
	 * @since 4.0.3
	 * @see #setBlockCommentEndDelimiter
	 */
	public EmbeddedDatabaseBuilder setBlockCommentStartDelimiter(String blockCommentStartDelimiter) {
		this.databasePopulator.setBlockCommentStartDelimiter(blockCommentStartDelimiter);
		return this;
	}

	/**
	 * 指定所有 SQL 脚本中块注释的结束分隔符。
	 * <p>默认为 <code>"*&#47;"</code>。
	 * @param blockCommentEndDelimiter 块注释的结束分隔符
	 * @return {@code this}，以便进行方法链式调用
	 * @since 4.0.3
	 * @see #setBlockCommentStartDelimiter
	 */
	public EmbeddedDatabaseBuilder setBlockCommentEndDelimiter(String blockCommentEndDelimiter) {
		this.databasePopulator.setBlockCommentEndDelimiter(blockCommentEndDelimiter);
		return this;
	}

	/**
	 * 指定执行 SQL 脚本时发生的所有失败都应记录日志，但不应导致失败。
	 * <p>默认为 {@code false}。
	 * @param flag 如果脚本执行应在出错时继续，则为 {@code true}
	 * @return {@code this}，以便进行方法链式调用
	 * @since 4.0.3
	 */
	public EmbeddedDatabaseBuilder continueOnError(boolean flag) {
		this.databasePopulator.setContinueOnError(flag);
		return this;
	}

	/**
	 * 指定可以忽略所执行脚本中失败的 SQL {@code DROP} 语句。
	 * <p>这对于 SQL 方言不支持在 {@code DROP} 语句中使用 {@code IF EXISTS}
	 * 子句的数据库很有用。
	 * <p>默认值为 {@code false}，因此如果脚本以 {@code DROP} 语句开头，
	 * {@link #build 构建}将快速失败。
	 * @param flag 如果应忽略失败的 drop 语句，则为 {@code true}
	 * @return {@code this}，以便进行方法链式调用
	 * @since 4.0.3
	 */
	public EmbeddedDatabaseBuilder ignoreFailedDrops(boolean flag) {
		this.databasePopulator.setIgnoreFailedDrops(flag);
		return this;
	}

	/**
	 * 构建嵌入式数据库。
	 * @return 该内嵌数据库
	 */
	public EmbeddedDatabase build() {
		return this.databaseFactory.getDatabase();
	}

}
