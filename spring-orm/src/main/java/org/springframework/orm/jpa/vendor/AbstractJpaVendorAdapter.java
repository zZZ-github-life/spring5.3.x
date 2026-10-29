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

package org.springframework.orm.jpa.vendor;

import java.util.Collections;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitInfo;

import org.springframework.lang.Nullable;
import org.springframework.orm.jpa.JpaDialect;
import org.springframework.orm.jpa.JpaVendorAdapter;

/**
 * 抽象的 {@link JpaVendorAdapter} 实现，定义了通用属性，
 * 由具体子类转换为厂商特定的 JPA 属性。
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @since 2.0
 */
public abstract class AbstractJpaVendorAdapter implements JpaVendorAdapter {

	private Database database = Database.DEFAULT;

	@Nullable
	private String databasePlatform;

	private boolean generateDdl = false;

	private boolean showSql = false;


	/**
	 * 指定要操作的目标数据库，取值为 {@code Database} 枚举中的值：
	 * DB2、DERBY、H2、HANA、HSQL、INFORMIX、MYSQL、ORACLE、POSTGRESQL、SQL_SERVER、SYBASE
	 * <p><b>注意：</b>此设置将覆盖 JPA 提供者的默认算法。
	 * 自定义厂商属性仍可微调数据库方言。不过，仍可能存在冲突：
	 * 例如，请只指定此设置或 Hibernate 的 "hibernate.dialect_resolvers" 属性之一，
	 * 而不要同时指定两者。
	 */
	public void setDatabase(Database database) {
		this.database = database;
	}

	/**
	 * 返回要操作的目标数据库。
	 */
	protected Database getDatabase() {
		return this.database;
	}

	/**
	 * 指定要操作的目标数据库的名称。
	 * 支持的取值为厂商相关的平台标识符。
	 */
	public void setDatabasePlatform(@Nullable String databasePlatform) {
		this.databasePlatform = databasePlatform;
	}

	/**
	 * 返回要操作的目标数据库的名称。
	 */
	@Nullable
	protected String getDatabasePlatform() {
		return this.databasePlatform;
	}

	/**
	 * 设置是否在 EntityManagerFactory 初始化之后生成 DDL，
	 * 即创建/更新所有相关的表。
	 * <p>请注意，此标志的确切语义取决于底层持久化提供者。
	 * 如需更高级的功能，请通过 "jpaProperties" 指定相应的
	 * 厂商特定设置。
	 * <p><b>注意：在同时设置 JPA 2.1 的
	 * {@code javax.persistence.schema-generation.database.action} 属性时，
	 * 不要将此标志设置为 'true'。</b>
	 * 这两种 schema 生成机制（标准 JPA 与提供者原生方式）互斥，
	 * 例如在 Hibernate 5 中就是如此。
	 * @see org.springframework.orm.jpa.AbstractEntityManagerFactoryBean#setJpaProperties
	 */
	public void setGenerateDdl(boolean generateDdl) {
		this.generateDdl = generateDdl;
	}

	/**
	 * 返回是否在 EntityManagerFactory 初始化之后生成 DDL，
	 * 即创建/更新所有相关的表。
	 */
	protected boolean isGenerateDdl() {
		return this.generateDdl;
	}

	/**
	 * 设置是否在日志（或控制台）中显示 SQL。
	 * <p>如需更具体的日志配置，请通过 "jpaProperties" 指定相应的
	 * 厂商特定设置。
	 * @see org.springframework.orm.jpa.AbstractEntityManagerFactoryBean#setJpaProperties
	 */
	public void setShowSql(boolean showSql) {
		this.showSql = showSql;
	}

	/**
	 * 返回是否在日志（或控制台）中显示 SQL。
	 */
	protected boolean isShowSql() {
		return this.showSql;
	}


	@Override
	@Nullable
	public String getPersistenceProviderRootPackage() {
		return null;
	}

	@Override
	public Map<String, ?> getJpaPropertyMap(PersistenceUnitInfo pui) {
		return getJpaPropertyMap();
	}

	@Override
	public Map<String, ?> getJpaPropertyMap() {
		return Collections.emptyMap();
	}

	@Override
	@Nullable
	public JpaDialect getJpaDialect() {
		return null;
	}

	@Override
	public Class<? extends EntityManagerFactory> getEntityManagerFactoryInterface() {
		return EntityManagerFactory.class;
	}

	@Override
	public Class<? extends EntityManager> getEntityManagerInterface() {
		return EntityManager.class;
	}

	@Override
	public void postProcessEntityManagerFactory(EntityManagerFactory emf) {
	}

	@Override
	public void postProcessEntityManager(EntityManager em) {
	}

}
