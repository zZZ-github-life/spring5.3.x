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

import java.util.Collections;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;

import org.springframework.lang.Nullable;

/**
 * SPI 接口，允许将供应商特定的行为插入 Spring 的 EntityManagerFactory 创建器。
 * 作为所有供应商特定属性的单一配置点。
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @since 2.0
 * @see AbstractEntityManagerFactoryBean#setJpaVendorAdapter
 */
public interface JpaVendorAdapter {

	/**
	 * 返回供应商特定的持久化提供者。
	 */
	PersistenceProvider getPersistenceProvider();

	/**
	 * 返回持久化提供者根包的名称（例如 "oracle.toplink.essentials"）。
	 * 用于在临时类覆盖时排除提供者类。
	 * @since 2.5.2
	 */
	@Nullable
	default String getPersistenceProviderRootPackage() {
		return null;
	}

	/**
	 * 返回给定持久化单元的供应商特定 JPA 属性 Map，
	 * 通常基于此 JpaVendorAdapter 实例中的设置。
	 * <p>请注意，EntityManagerFactory bean 上可能还定义了其他 JPA 属性，
	 * 这些属性可能会覆盖此处指定的个别 JPA 属性值。
	 * <p>此实现将非单元相关的属性委托给 {@link #getJpaPropertyMap()}。
	 * 实际上，只有在确实需要针对单元特定特征（例如事务类型）做出反应时，
	 * 才需要实现此基于 PersistenceUnitInfo 的变体。
	 * <p><b>注意：</b>仅在存在 {@link PersistenceUnitInfo} 的
	 * Java EE 风格容器引导场景（即 {@link LocalContainerEntityManagerFactoryBean}）中
	 * 才会调用此变体。在通过 {@link javax.persistence.Persistence}
	 * 进行简单 Java SE 风格引导的场景（即 {@link LocalEntityManagerFactoryBean}）中，
	 * 将直接调用无参数的 {@link #getJpaPropertyMap()} 变体。
	 * @param pui 当前持久化单元的 PersistenceUnitInfo
	 * @return JPA 属性的 Map，采用标准 JPA 引导设施所接受的形式；
	 * 如果没有要暴露的属性，则为空 Map
	 * @since 4.3.13
	 * @see PersistenceUnitInfo#getTransactionType()
	 * @see javax.persistence.spi.PersistenceProvider#createContainerEntityManagerFactory(PersistenceUnitInfo, Map)
	 */
	default Map<String, ?> getJpaPropertyMap(PersistenceUnitInfo pui) {
		return getJpaPropertyMap();
	}

	/**
	 * 返回供应商特定的 JPA 属性 Map，
	 * 通常基于此 JpaVendorAdapter 实例中的设置。
	 * <p>请注意，EntityManagerFactory bean 上可能还定义了其他 JPA 属性，
	 * 这些属性可能会覆盖此处指定的个别 JPA 属性值。
	 * @return JPA 属性的 Map，采用标准 JPA 引导设施所接受的形式；
	 * 如果没有要暴露的属性，则为空 Map
	 * @see javax.persistence.Persistence#createEntityManagerFactory(String, Map)
	 */
	default Map<String, ?> getJpaPropertyMap() {
		return Collections.emptyMap();
	}

	/**
	 * 返回此提供者的供应商特定 JpaDialect 实现，
	 * 如果没有则返回 {@code null}。
	 */
	@Nullable
	default JpaDialect getJpaDialect() {
		return null;
	}

	/**
	 * 返回 EntityManagerFactory 代理应实现的供应商特定 EntityManagerFactory 接口。
	 * <p>如果提供者不提供任何 EntityManagerFactory 扩展，
	 * 适配器只需在此处返回标准的
	 * {@link javax.persistence.EntityManagerFactory} 类。
	 * @since 2.5.2
	 */
	default Class<? extends EntityManagerFactory> getEntityManagerFactoryInterface() {
		return EntityManagerFactory.class;
	}

	/**
	 * 返回此提供者的 EntityManager 将实现的供应商特定 EntityManager 接口。
	 * <p>如果提供者不提供任何 EntityManager 扩展，
	 * 适配器只需在此处返回标准的
	 * {@link javax.persistence.EntityManager} 类。
	 */
	default Class<? extends EntityManager> getEntityManagerInterface() {
		return EntityManager.class;
	}

	/**
	 * 可选回调，用于在激活使用之前对原生 EntityManagerFactory 进行后处理。
	 * <p>可用于触发供应商特定的初始化过程。
	 * 虽然预计大多数提供者不会使用此回调，
	 * 但它作为通用的扩展钩子包含在此处。
	 */
	default void postProcessEntityManagerFactory(EntityManagerFactory emf) {
	}

	/**
	 * 可选回调，用于在激活使用之前对原生 EntityManager 进行后处理。
	 * <p>可用于在每个新的 EntityManager 上设置供应商特定的参数，
	 * 例如 Hibernate 过滤器。
	 * @since 5.3
	 */
	default void postProcessEntityManager(EntityManager em) {
	}

}
