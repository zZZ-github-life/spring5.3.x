/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.orm.jpa.support;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.orm.jpa.EntityManagerFactoryAccessor;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.util.Assert;

/**
 * {@link FactoryBean}，为给定的 EntityManagerFactory 暴露共享的 JPA
 * {@link javax.persistence.EntityManager} 引用。通常用于由
 * {@link org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean}
 * 创建的 EntityManagerFactory，作为通过 JNDI 查找 Java EE 服务器
 * EntityManager 引用的直接替代方案。
 *
 * <p>共享的 EntityManager 的行为与从应用服务器 JNDI 环境获取的
 * EntityManager 完全一致（符合 JPA 规范）。它会将所有调用委托给当前的
 * 事务性 EntityManager（如果有）；否则，它将在每次操作时回退到
 * 新创建的 EntityManager。
 *
 * <p>可以传递给期望共享 EntityManager 引用而非 EntityManagerFactory 的 DAO。
 * 请注意，Spring 的 {@link org.springframework.orm.jpa.JpaTransactionManager}
 * 始终需要 EntityManagerFactory 才能创建新的事务性 EntityManager 实例。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #setEntityManagerFactory
 * @see #setEntityManagerInterface
 * @see org.springframework.orm.jpa.LocalEntityManagerFactoryBean
 * @see org.springframework.orm.jpa.JpaTransactionManager
 */
public class SharedEntityManagerBean extends EntityManagerFactoryAccessor
		implements FactoryBean<EntityManager>, InitializingBean {

	@Nullable
	private Class<? extends EntityManager> entityManagerInterface;

	private boolean synchronizedWithTransaction = true;

	@Nullable
	private EntityManager shared;


	/**
	 * 指定要暴露的 EntityManager 接口。
	 * <p>默认为 EntityManagerFactoryInfo 所定义的 EntityManager 接口（如果可用）。
	 * 否则，将使用标准的 {@code javax.persistence.EntityManager} 接口。
	 * @see org.springframework.orm.jpa.EntityManagerFactoryInfo#getEntityManagerInterface()
	 * @see javax.persistence.EntityManager
	 */
	public void setEntityManagerInterface(Class<? extends EntityManager> entityManagerInterface) {
		Assert.notNull(entityManagerInterface, "'entityManagerInterface' must not be null");
		this.entityManagerInterface = entityManagerInterface;
	}

	/**
	 * 设置是否自动加入正在进行的事务（依据 JPA 2.1 SynchronizationType 规则）。
	 * 默认值为 "true"。
	 */
	public void setSynchronizedWithTransaction(boolean synchronizedWithTransaction) {
		this.synchronizedWithTransaction = synchronizedWithTransaction;
	}


	@Override
	public final void afterPropertiesSet() {
		EntityManagerFactory emf = getEntityManagerFactory();
		if (emf == null) {
			throw new IllegalArgumentException("'entityManagerFactory' or 'persistenceUnitName' is required");
		}
		if (emf instanceof EntityManagerFactoryInfo) {
			EntityManagerFactoryInfo emfInfo = (EntityManagerFactoryInfo) emf;
			if (this.entityManagerInterface == null) {
				this.entityManagerInterface = emfInfo.getEntityManagerInterface();
				if (this.entityManagerInterface == null) {
					this.entityManagerInterface = EntityManager.class;
				}
			}
		}
		else {
			if (this.entityManagerInterface == null) {
				this.entityManagerInterface = EntityManager.class;
			}
		}
		this.shared = SharedEntityManagerCreator.createSharedEntityManager(
				emf, getJpaPropertyMap(), this.synchronizedWithTransaction, this.entityManagerInterface);
	}


	@Override
	@Nullable
	public EntityManager getObject() {
		return this.shared;
	}

	@Override
	public Class<? extends EntityManager> getObjectType() {
		return (this.entityManagerInterface != null ? this.entityManagerInterface : EntityManager.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
