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

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DerbyTenSevenDialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HANAColumnStoreDialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.Informix10Dialect;
import org.hibernate.dialect.MySQL57Dialect;
import org.hibernate.dialect.Oracle12cDialect;
import org.hibernate.dialect.PostgreSQL95Dialect;
import org.hibernate.dialect.SQLServer2012Dialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;

import org.springframework.lang.Nullable;

/**
 * 适用于 Hibernate EntityManager 的
 * {@link org.springframework.orm.jpa.JpaVendorAdapter} 实现。
 * 基于 Hibernate 5.3 和 5.4 开发并测试；在运行时尽最大努力
 * 向后兼容 Hibernate 5.2。
 *
 * <p>暴露 Hibernate 的持久化提供者和 Hibernate 的 Session 作为扩展的
 * EntityManager 接口，并适配 {@link AbstractJpaVendorAdapter} 的通用配置设置。
 * 还支持带注解包的检测（通过
 * {@link org.springframework.orm.jpa.persistenceunit.SmartPersistenceUnitInfo#getManagedPackages()}），
 * 例如包含 Hibernate {@link org.hibernate.annotations.FilterDef} 注解的包，
 * 以及不依赖 {@code persistence.xml} 的 Spring 驱动实体扫描
 * （{@link org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean#setPackagesToScan}）。
 *
 * <p><b>关于 {@code HibernateJpaVendorAdapter} 与原生 Hibernate 设置的说明：</b>
 * 此适配器上的某些设置可能与原生 Hibernate 配置规则或自定义 Hibernate 属性冲突。
 * 例如，请只指定 {@link #setDatabase} 或 Hibernate 的
 * "hibernate.dialect_resolvers" 属性之一，而不要同时指定两者。
 * 另外，请注意 Hibernate 的连接释放模式：此适配器倾向于 {@code ON_CLOSE} 行为，
 * 与 {@link HibernateJpaDialect#setPrepareConnection} 保持一致，
 * 至少对于非 JTA 场景是如此；你可以通过相应的原生 Hibernate 属性覆盖这一点。
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @since 2.0
 * @see HibernateJpaDialect
 */
public class HibernateJpaVendorAdapter extends AbstractJpaVendorAdapter {

	private final HibernateJpaDialect jpaDialect = new HibernateJpaDialect();

	private final PersistenceProvider persistenceProvider;

	private final Class<? extends EntityManagerFactory> entityManagerFactoryInterface;

	private final Class<? extends EntityManager> entityManagerInterface;


	public HibernateJpaVendorAdapter() {
		this.persistenceProvider = new SpringHibernateJpaPersistenceProvider();
		this.entityManagerFactoryInterface = SessionFactory.class;  // as of Spring 5.3
		this.entityManagerInterface = Session.class;  // as of Spring 5.3
	}


	/**
	 * 设置是否准备事务性 Hibernate Session 底层的 JDBC Connection，
	 * 即是否将事务特定的隔离级别和/或事务的只读标志
	 * 应用到该底层 JDBC Connection。
	 * <p>详情请参见 {@link HibernateJpaDialect#setPrepareConnection(boolean)}。
	 * 这只是传递给 {@code HibernateJpaDialect} 的便捷标志。
	 * <p>在 Hibernate 5.1+ 上，此标志默认仍为 {@code true}，
	 * 与之前的 Hibernate 版本保持一致。在这种情况下，厂商适配器会手动强制
	 * Hibernate 的新连接处理模式 {@code DELAYED_ACQUISITION_AND_HOLD}，
	 * 除非用户指定的连接处理模式属性另有指示；
	 * 将此标志切换为 {@code false} 可避免这种干预。
	 * <p><b>注意：对于事务类型为 JTA 的持久化单元（例如在 WebLogic 上），
	 * 连接释放模式绝不会偏离提供者的默认值，
	 * 即不会被此标志强制为 {@code DELAYED_ACQUISITION_AND_HOLD}。</b>
	 * 或者，在这种情况下将 Hibernate 的 "hibernate.connection.handling_mode"
	 * 属性设置为 "DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION"，
	 * 甚至设置为 "DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT"。
	 * @since 4.3.1
	 * @see PersistenceUnitInfo#getTransactionType()
	 * @see #getJpaPropertyMap(PersistenceUnitInfo)
	 * @see HibernateJpaDialect#beginTransaction
	 */
	public void setPrepareConnection(boolean prepareConnection) {
		this.jpaDialect.setPrepareConnection(prepareConnection);
	}


	@Override
	public PersistenceProvider getPersistenceProvider() {
		return this.persistenceProvider;
	}

	@Override
	public String getPersistenceProviderRootPackage() {
		return "org.hibernate";
	}

	@Override
	public Map<String, Object> getJpaPropertyMap(PersistenceUnitInfo pui) {
		return buildJpaPropertyMap(this.jpaDialect.prepareConnection &&
				pui.getTransactionType() != PersistenceUnitTransactionType.JTA);
	}

	@Override
	public Map<String, Object> getJpaPropertyMap() {
		return buildJpaPropertyMap(this.jpaDialect.prepareConnection);
	}

	private Map<String, Object> buildJpaPropertyMap(boolean connectionReleaseOnClose) {
		Map<String, Object> jpaProperties = new HashMap<>();

		if (getDatabasePlatform() != null) {
			jpaProperties.put(AvailableSettings.DIALECT, getDatabasePlatform());
		}
		else {
			Class<?> databaseDialectClass = determineDatabaseDialectClass(getDatabase());
			if (databaseDialectClass != null) {
				jpaProperties.put(AvailableSettings.DIALECT, databaseDialectClass.getName());
			}
		}

		if (isGenerateDdl()) {
			jpaProperties.put(AvailableSettings.HBM2DDL_AUTO, "update");
		}
		if (isShowSql()) {
			jpaProperties.put(AvailableSettings.SHOW_SQL, "true");
		}

		if (connectionReleaseOnClose) {
			jpaProperties.put(AvailableSettings.CONNECTION_HANDLING,
					PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_HOLD);
		}

		return jpaProperties;
	}

	/**
	 * 确定给定目标数据库对应的 Hibernate 数据库方言类。
	 * @param database 目标数据库
	 * @return Hibernate 数据库方言类；如果未找到，则为 {@code null}
	 */
	@Nullable
	protected Class<?> determineDatabaseDialectClass(Database database) {
		switch (database) {
			case DB2: return DB2Dialect.class;
			case DERBY: return DerbyTenSevenDialect.class;
			case H2: return H2Dialect.class;
			case HANA: return HANAColumnStoreDialect.class;
			case HSQL: return HSQLDialect.class;
			case INFORMIX: return Informix10Dialect.class;
			case MYSQL: return MySQL57Dialect.class;
			case ORACLE: return Oracle12cDialect.class;
			case POSTGRESQL: return PostgreSQL95Dialect.class;
			case SQL_SERVER: return SQLServer2012Dialect.class;
			case SYBASE: return SybaseDialect.class;
			default: return null;
		}
	}

	@Override
	public HibernateJpaDialect getJpaDialect() {
		return this.jpaDialect;
	}

	@Override
	public Class<? extends EntityManagerFactory> getEntityManagerFactoryInterface() {
		return this.entityManagerFactoryInterface;
	}

	@Override
	public Class<? extends EntityManager> getEntityManagerInterface() {
		return this.entityManagerInterface;
	}

}
