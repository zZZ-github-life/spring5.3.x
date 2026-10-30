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

package org.springframework.orm.jpa;

import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.sql.DataSource;

import org.springframework.lang.Nullable;

/**
 * Spring 管理的 JPA {@link EntityManagerFactory} 的元数据接口。
 *
 * <p>可以通过将 EntityManagerFactory 句柄强制转换为该接口，
 * 从 Spring 管理的 EntityManagerFactory 代理中获取此功能。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 */
public interface EntityManagerFactoryInfo {

	/**
	 * 返回创建底层 EntityManagerFactory 所使用的底层 PersistenceProvider。
	 * @return 用于创建此 EntityManagerFactory 的 PersistenceProvider，
	 * 或 {@code null}（如果使用标准 JPA 提供者自动检测过程配置了该 EntityManagerFactory）
	 */
	@Nullable
	PersistenceProvider getPersistenceProvider();

	/**
	 * 返回创建此 EntityManagerFactory 所使用的 PersistenceUnitInfo
	 * （如果使用了容器内 API）。
	 * @return 用于创建此 EntityManagerFactory 的 PersistenceUnitInfo，
	 * 或 {@code null}（如果未使用容器内约定配置该 EntityManagerFactory）
	 */
	@Nullable
	PersistenceUnitInfo getPersistenceUnitInfo();

	/**
	 * 返回创建此 EntityManagerFactory 所使用的持久化单元名称，
	 * 如果是未命名的默认值则返回 {@code null}。
	 * <p>如果 {@code getPersistenceUnitInfo()} 返回非 null，
	 * 则 {@code getPersistenceUnitName()} 的结果必须等于
	 * {@code PersistenceUnitInfo.getPersistenceUnitName()} 返回的值。
	 * @see #getPersistenceUnitInfo()
	 * @see javax.persistence.spi.PersistenceUnitInfo#getPersistenceUnitName()
	 */
	@Nullable
	String getPersistenceUnitName();

	/**
	 * 返回此 EntityManagerFactory 从中获取 JDBC 连接的 JDBC DataSource。
	 * @return JDBC DataSource；如果未知则为 {@code null}
	 */
	@Nullable
	DataSource getDataSource();

	/**
	 * 返回此工厂的 EntityManager 将实现的（可能为供应商特定的）
	 * EntityManager 接口。
	 * <p>返回 {@code null} 表示应进行自动检测：
	 * 或者基于目标 {@code EntityManager} 实例，
	 * 或者简单地默认为 {@code javax.persistence.EntityManager}。
	 */
	@Nullable
	Class<? extends EntityManager> getEntityManagerInterface();

	/**
	 * 返回此 EntityManagerFactory 的供应商特定的 JpaDialect 实现，
	 * 如果未知则返回 {@code null}。
	 */
	@Nullable
	JpaDialect getJpaDialect();

	/**
	 * 返回加载应用程序 bean 所用的 ClassLoader。
	 * <p>代理将在此 ClassLoader 中生成。
	 */
	ClassLoader getBeanClassLoader();

	/**
	 * 返回原始的底层 EntityManagerFactory。
	 * @return 未经修饰的 EntityManagerFactory（永不为 {@code null}）
	 */
	EntityManagerFactory getNativeEntityManagerFactory();

	/**
	 * 创建一个原生 JPA EntityManager，用作应用程序级 EntityManager 句柄
	 * 背后的框架管理资源。
	 * <p>这会从底层 {@link #getNativeEntityManagerFactory() 原生 EntityManagerFactory}
	 * 中暴露一个原生 {@code EntityManager}，并考虑
	 * {@link JpaVendorAdapter#postProcessEntityManager(EntityManager)}。
	 * @since 5.3
	 * @see #getNativeEntityManagerFactory()
	 * @see EntityManagerFactory#createEntityManager()
	 */
	EntityManager createNativeEntityManager(@Nullable Map<?, ?> properties);

}
