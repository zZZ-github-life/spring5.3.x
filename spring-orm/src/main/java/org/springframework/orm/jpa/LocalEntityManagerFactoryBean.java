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

package org.springframework.orm.jpa;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;
import javax.persistence.spi.PersistenceProvider;

/**
 * 按照 JPA 标准<i>独立</i>引导契约创建 JPA
 * {@link javax.persistence.EntityManagerFactory} 的
 * {@link org.springframework.beans.factory.FactoryBean}。
 * 这是在 Spring 应用上下文中设置共享 JPA EntityManagerFactory 的最简单方式；
 * 然后可以通过依赖注入将 EntityManagerFactory 传递给基于 JPA 的 DAO。
 * 请注意，切换到 JNDI 查找或 {@link LocalContainerEntityManagerFactoryBean}
 * 定义只是配置上的变化！
 *
 * <p>根据 JPA 独立引导契约，配置设置通常从位于类路径中的
 * {@code META-INF/persistence.xml} 配置文件读取。此外，
 * 大多数 JPA 提供者需要一个特殊的 VM agent（在 JVM 启动时指定）
 * 才能对应用程序类进行插桩。有关设置详情，
 * 请参阅 Java Persistence API 规范和你的提供者文档。
 *
 * <p>此 EntityManagerFactory 引导方式适用于仅使用 JPA 进行数据访问的独立应用程序。
 * 如果你希望为外部 DataSource 和/或跨多个资源的全局事务配置持久化提供者，
 * 则需要将其部署到完整的 Java EE 应用服务器中并通过 JNDI 访问部署的
 * EntityManagerFactory，或者使用 Spring 的
 * {@link LocalContainerEntityManagerFactoryBean} 并按照 JPA 容器契约
 * 进行适当的本地配置。
 *
 * <p><b>注意：</b>此 FactoryBean 在能够传递给 JPA 提供者的配置方面
 * 能力有限。如果你需要更灵活的配置，例如向 JPA 提供者传递
 * Spring 管理的 JDBC DataSource，请考虑改用 Spring 更强大的
 * {@link LocalContainerEntityManagerFactoryBean}。
 *
 * <p><b>注意：自 Spring 5.0 起，Spring 的 JPA 支持要求 JPA 2.1 或更高版本。</b>
 * 基于 JPA 1.0/2.0 的应用程序仍然受支持；不过，运行时需要符合 JPA 2.1 的
 * 持久化提供者。
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @since 2.0
 * @see #setJpaProperties
 * @see #setJpaVendorAdapter
 * @see JpaTransactionManager#setEntityManagerFactory
 * @see LocalContainerEntityManagerFactoryBean
 * @see org.springframework.jndi.JndiObjectFactoryBean
 * @see org.springframework.orm.jpa.support.SharedEntityManagerBean
 * @see javax.persistence.Persistence#createEntityManagerFactory
 * @see javax.persistence.spi.PersistenceProvider#createEntityManagerFactory
 */
@SuppressWarnings("serial")
public class LocalEntityManagerFactoryBean extends AbstractEntityManagerFactoryBean {

	/**
	 * 为给定的配置初始化 EntityManagerFactory。
	 * @throws javax.persistence.PersistenceException 如果发生 JPA 初始化错误
	 */
	@Override
	protected EntityManagerFactory createNativeEntityManagerFactory() throws PersistenceException {
		if (logger.isDebugEnabled()) {
			logger.debug("Building JPA EntityManagerFactory for persistence unit '" + getPersistenceUnitName() + "'");
		}
		PersistenceProvider provider = getPersistenceProvider();
		if (provider != null) {
			// Create EntityManagerFactory directly through PersistenceProvider.
			EntityManagerFactory emf = provider.createEntityManagerFactory(getPersistenceUnitName(), getJpaPropertyMap());
			if (emf == null) {
				throw new IllegalStateException(
						"PersistenceProvider [" + provider + "] did not return an EntityManagerFactory for name '" +
						getPersistenceUnitName() + "'");
			}
			return emf;
		}
		else {
			// Let JPA perform its standard PersistenceProvider autodetection.
			return Persistence.createEntityManagerFactory(getPersistenceUnitName(), getJpaPropertyMap());
		}
	}

}
