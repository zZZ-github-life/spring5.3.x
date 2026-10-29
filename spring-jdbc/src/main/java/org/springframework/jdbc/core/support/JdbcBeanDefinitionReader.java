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

package org.springframework.jdbc.core.support;

import java.util.Properties;

import javax.sql.DataSource;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 基于给定的 SQL 语句从数据库表中读取值的 Bean 定义读取器。
 *
 * <p>期望有分别以 String 形式表示 bean 名称、属性名称和值的列。
 * 每一列的格式与 PropertiesBeanDefinitionReader 所识别的属性格式相同。
 *
 * <p><b>注意：</b>这主要用作自定义的基于 JDBC 的 bean 定义读取器的示例。
 * 它并不旨在提供全面的功能。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #loadBeanDefinitions
 * @see org.springframework.beans.factory.support.PropertiesBeanDefinitionReader
 * @deprecated as of 5.3, in favor of Spring's common bean definition formats
 * and/or custom reader implementations
 */
@Deprecated
public class JdbcBeanDefinitionReader {

	private final org.springframework.beans.factory.support.PropertiesBeanDefinitionReader propReader;

	@Nullable
	private JdbcTemplate jdbcTemplate;


	/**
	 * 为给定的 bean 工厂创建一个新的 JdbcBeanDefinitionReader，
	 * 底层使用默认的 PropertiesBeanDefinitionReader。
	 * <p>仍然需要设置 DataSource 或 JdbcTemplate。
	 * @see #setDataSource
	 * @see #setJdbcTemplate
	 */
	public JdbcBeanDefinitionReader(BeanDefinitionRegistry beanFactory) {
		this.propReader = new org.springframework.beans.factory.support.PropertiesBeanDefinitionReader(beanFactory);
	}

	/**
	 * 创建一个新的 JdbcBeanDefinitionReader，委托给
	 * 底层给定的 PropertiesBeanDefinitionReader。
	 * <p>仍然需要设置 DataSource 或 JdbcTemplate。
	 * @see #setDataSource
	 * @see #setJdbcTemplate
	 */
	public JdbcBeanDefinitionReader(org.springframework.beans.factory.support.PropertiesBeanDefinitionReader reader) {
		Assert.notNull(reader, "Bean definition reader must not be null");
		this.propReader = reader;
	}


	/**
	 * 设置用于获取数据库连接的 DataSource。
	 * 将使用给定的 DataSource 隐式创建一个新的 JdbcTemplate。
	 */
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	/**
	 * 设置此 bean 工厂要使用的 JdbcTemplate。
	 * 其中包含 DataSource、SQLExceptionTranslator 等设置。
	 */
	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		Assert.notNull(jdbcTemplate, "JdbcTemplate must not be null");
		this.jdbcTemplate = jdbcTemplate;
	}


	/**
	 * 通过给定的 SQL 字符串从数据库加载 bean 定义。
	 * @param sql the SQL query to use for loading bean definitions.
	 * The first three columns must be bean name, property name and value.
	 * Any join and any other columns are permitted: e.g.
	 * {@code SELECT BEAN_NAME, PROPERTY, VALUE FROM CONFIG WHERE CONFIG.APP_ID = 1}
	 * It's also possible to perform a join. Column names are not significant --
	 * only the ordering of these first three columns.
	 */
	public void loadBeanDefinitions(String sql) {
		Assert.notNull(this.jdbcTemplate, "Not fully configured - specify DataSource or JdbcTemplate");
		final Properties props = new Properties();
		this.jdbcTemplate.query(sql, rs -> {
			String beanName = rs.getString(1);
			String property = rs.getString(2);
			String value = rs.getString(3);
			// Make a properties entry by combining bean name and property.
			props.setProperty(beanName + '.' + property, value);
		});
		this.propReader.registerBeanDefinitions(props);
	}

}
