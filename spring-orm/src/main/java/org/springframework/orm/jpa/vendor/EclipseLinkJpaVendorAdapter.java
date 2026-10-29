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

package org.springframework.orm.jpa.vendor;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.persistence.EntityManager;
import javax.persistence.spi.PersistenceProvider;

import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.eclipse.persistence.config.TargetDatabase;
import org.eclipse.persistence.jpa.JpaEntityManager;

import org.springframework.lang.Nullable;

/**
 * 适用于 Eclipse Persistence Services（EclipseLink）的
 * {@link org.springframework.orm.jpa.JpaVendorAdapter} 实现。
 * 基于 EclipseLink 2.7 开发并测试；在运行时向后兼容
 * EclipseLink 2.5 和 2.6。
 *
 * <p>暴露 EclipseLink 的持久化提供者和 EntityManager 扩展接口，
 * 并适配 {@link AbstractJpaVendorAdapter} 的通用配置设置。
 * 不支持带注解包的检测（通过
 * {@link org.springframework.orm.jpa.persistenceunit.SmartPersistenceUnitInfo#getManagedPackages()}），
 * 因为 EclipseLink 不使用包级元数据。
 *
 * @author Juergen Hoeller
 * @author Thomas Risberg
 * @since 2.5.2
 * @see EclipseLinkJpaDialect
 * @see org.eclipse.persistence.jpa.PersistenceProvider
 * @see org.eclipse.persistence.jpa.JpaEntityManager
 */
public class EclipseLinkJpaVendorAdapter extends AbstractJpaVendorAdapter {

	private final PersistenceProvider persistenceProvider = new org.eclipse.persistence.jpa.PersistenceProvider();

	private final EclipseLinkJpaDialect jpaDialect = new EclipseLinkJpaDialect();


	@Override
	public PersistenceProvider getPersistenceProvider() {
		return this.persistenceProvider;
	}

	@Override
	public Map<String, Object> getJpaPropertyMap() {
		Map<String, Object> jpaProperties = new HashMap<>();

		if (getDatabasePlatform() != null) {
			jpaProperties.put(PersistenceUnitProperties.TARGET_DATABASE, getDatabasePlatform());
		}
		else {
			String targetDatabase = determineTargetDatabaseName(getDatabase());
			if (targetDatabase != null) {
				jpaProperties.put(PersistenceUnitProperties.TARGET_DATABASE, targetDatabase);
			}
		}

		if (isGenerateDdl()) {
			jpaProperties.put(PersistenceUnitProperties.DDL_GENERATION,
					PersistenceUnitProperties.CREATE_ONLY);
			jpaProperties.put(PersistenceUnitProperties.DDL_GENERATION_MODE,
					PersistenceUnitProperties.DDL_DATABASE_GENERATION);
		}
		if (isShowSql()) {
			jpaProperties.put(PersistenceUnitProperties.CATEGORY_LOGGING_LEVEL_ +
					org.eclipse.persistence.logging.SessionLog.SQL, Level.FINE.toString());
			jpaProperties.put(PersistenceUnitProperties.LOGGING_PARAMETERS, Boolean.TRUE.toString());
		}

		return jpaProperties;
	}

	/**
	 * 确定给定数据库对应的 EclipseLink 目标数据库名称。
	 * @param database the specified database
	 * @return the EclipseLink target database name, or {@code null} if none found
	 */
	@Nullable
	protected String determineTargetDatabaseName(Database database) {
		switch (database) {
			case DB2: return TargetDatabase.DB2;
			case DERBY: return TargetDatabase.Derby;
			case HANA: return TargetDatabase.HANA;
			case HSQL: return TargetDatabase.HSQL;
			case INFORMIX: return TargetDatabase.Informix;
			case MYSQL: return TargetDatabase.MySQL;
			case ORACLE: return TargetDatabase.Oracle;
			case POSTGRESQL: return TargetDatabase.PostgreSQL;
			case SQL_SERVER: return TargetDatabase.SQLServer;
			case SYBASE: return TargetDatabase.Sybase;
			default: return null;
		}
	}

	@Override
	public EclipseLinkJpaDialect getJpaDialect() {
		return this.jpaDialect;
	}

	@Override
	public Class<? extends EntityManager> getEntityManagerInterface() {
		return JpaEntityManager.class;
	}

}
