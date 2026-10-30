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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * 任何需要访问 JPA {@link EntityManagerFactory} 的类的基类，
 * 通常用于获取 JPA {@link EntityManager}。定义公共属性。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see EntityManagerFactoryUtils
 */
public abstract class EntityManagerFactoryAccessor implements BeanFactoryAware {

	/** 可供子类使用的日志记录器。 */
	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private EntityManagerFactory entityManagerFactory;

	@Nullable
	private String persistenceUnitName;

	private final Map<String, Object> jpaPropertyMap = new HashMap<>();


	/**
	 * 设置应用于创建 EntityManager 的 JPA EntityManagerFactory。
	 * @see javax.persistence.EntityManagerFactory#createEntityManager()
	 * @see javax.persistence.EntityManagerFactory#createEntityManager(java.util.Map)
	 */
	public void setEntityManagerFactory(@Nullable EntityManagerFactory emf) {
		this.entityManagerFactory = emf;
	}

	/**
	 * 返回应用于创建 EntityManager 的 JPA EntityManagerFactory。
	 */
	@Nullable
	public EntityManagerFactory getEntityManagerFactory() {
		return this.entityManagerFactory;
	}

	/**
	 * 获取供实际使用的 EntityManagerFactory。
	 * @return EntityManagerFactory（永不为 {@code null}）
	 * @throws IllegalStateException 如果未设置 EntityManagerFactory
	 * @since 5.0
	 */
	protected final EntityManagerFactory obtainEntityManagerFactory() {
		EntityManagerFactory emf = getEntityManagerFactory();
		Assert.state(emf != null, "No EntityManagerFactory set");
		return emf;
	}

	/**
	 * 设置要访问其 EntityManagerFactory 的持久化单元名称。
	 * <p>这是直接引用指定 EntityManagerFactory 的替代方式，
	 * 改为按持久化单元名称解析它。如果既未指定 EntityManagerFactory
	 * 也未指定持久化单元名称，则将通过查找唯一的 EntityManagerFactory
	 * 类型 bean 来获取一个默认的 EntityManagerFactory。
	 * @see #setEntityManagerFactory
	 */
	public void setPersistenceUnitName(@Nullable String persistenceUnitName) {
		this.persistenceUnitName = persistenceUnitName;
	}

	/**
	 * 返回要访问其 EntityManagerFactory 的持久化单元名称（如果有）。
	 */
	@Nullable
	public String getPersistenceUnitName() {
		return this.persistenceUnitName;
	}

	/**
	 * 指定要传入 {@code EntityManagerFactory.createEntityManager(Map)} 的
	 * JPA 属性（如果有）。
	 * <p>可以通过 String 类型的 "value"（由 PropertiesEditor 解析）
	 * 或 XML bean 定义中的 "props" 元素来填充。
	 * @see javax.persistence.EntityManagerFactory#createEntityManager(java.util.Map)
	 */
	public void setJpaProperties(Properties jpaProperties) {
		CollectionUtils.mergePropertiesIntoMap(jpaProperties, this.jpaPropertyMap);
	}

	/**
	 * 以 Map 形式指定要传入 {@code EntityManagerFactory.createEntityManager(Map)}
	 * 的 JPA 属性（如果有）。
	 * <p>可以通过 XML bean 定义中的 "map" 或 "props" 元素来填充。
	 * @see javax.persistence.EntityManagerFactory#createEntityManager(java.util.Map)
	 */
	public void setJpaPropertyMap(@Nullable Map<String, Object> jpaProperties) {
		if (jpaProperties != null) {
			this.jpaPropertyMap.putAll(jpaProperties);
		}
	}

	/**
	 * 允许以 {@code Map} 方式访问要传递给持久化提供者的 JPA 属性，
	 * 并可以选择添加或覆盖特定条目。
	 * <p>适用于直接指定条目，例如通过 {@code jpaPropertyMap[myKey]}。
	 */
	public Map<String, Object> getJpaPropertyMap() {
		return this.jpaPropertyMap;
	}

	/**
	 * 如果未显式设置 EntityManagerFactory，则按持久化单元名称获取它。
	 * 如果未指定持久化单元，则回退到默认的 EntityManagerFactory bean。
	 * @see #setPersistenceUnitName
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (getEntityManagerFactory() == null) {
			if (!(beanFactory instanceof ListableBeanFactory)) {
				throw new IllegalStateException("Cannot retrieve EntityManagerFactory by persistence unit name " +
						"in a non-listable BeanFactory: " + beanFactory);
			}
			ListableBeanFactory lbf = (ListableBeanFactory) beanFactory;
			setEntityManagerFactory(EntityManagerFactoryUtils.findEntityManagerFactory(lbf, getPersistenceUnitName()));
		}
	}


	/**
	 * 从此访问器的 EntityManagerFactory 获取一个新的 EntityManager。
	 * <p>可以在子类中重写，以创建特定的 EntityManager 变体。
	 * @return 新的 EntityManager
	 * @throws IllegalStateException 如果此访问器未配置 EntityManagerFactory
	 * @see javax.persistence.EntityManagerFactory#createEntityManager()
	 * @see javax.persistence.EntityManagerFactory#createEntityManager(java.util.Map)
	 */
	protected EntityManager createEntityManager() throws IllegalStateException {
		EntityManagerFactory emf = obtainEntityManagerFactory();
		Map<String, Object> properties = getJpaPropertyMap();
		return (!CollectionUtils.isEmpty(properties) ? emf.createEntityManager(properties) : emf.createEntityManager());
	}

	/**
	 * 获取此访问器的 EntityManagerFactory 的事务性 EntityManager（如果有）。
	 * @return 事务性 EntityManager；如果没有则返回 {@code null}
	 * @throws IllegalStateException 如果此访问器未配置 EntityManagerFactory
	 * @see EntityManagerFactoryUtils#getTransactionalEntityManager(javax.persistence.EntityManagerFactory)
	 * @see EntityManagerFactoryUtils#getTransactionalEntityManager(javax.persistence.EntityManagerFactory, java.util.Map)
	 */
	@Nullable
	protected EntityManager getTransactionalEntityManager() throws IllegalStateException{
		EntityManagerFactory emf = obtainEntityManagerFactory();
		return EntityManagerFactoryUtils.getTransactionalEntityManager(emf, getJpaPropertyMap());
	}

}
