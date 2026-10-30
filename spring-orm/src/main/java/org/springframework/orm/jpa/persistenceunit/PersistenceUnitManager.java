/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.orm.jpa.persistenceunit;

import javax.persistence.spi.PersistenceUnitInfo;

/**
 * 定义查找和管理 JPA PersistenceUnitInfo 抽象的接口。
 * 由 {@link org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean}
 * 用于获取 {@link javax.persistence.spi.PersistenceUnitInfo}，
 * 以构建具体的 {@link javax.persistence.EntityManagerFactory}。
 *
 * <p>获取 PersistenceUnitInfo 实例的过程是独占的。
 * 一旦 PersistenceUnitInfo 实例已被获取，就不能再用于后续调用。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see DefaultPersistenceUnitManager
 * @see org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean#setPersistenceUnitManager
 */
public interface PersistenceUnitManager {

	/**
	 * 从此管理器获取默认的 PersistenceUnitInfo。
	 * @return PersistenceUnitInfo（永不为 {@code null}）
	 * @throws IllegalStateException 如果未定义默认的 PersistenceUnitInfo，或它已被获取
	 */
	PersistenceUnitInfo obtainDefaultPersistenceUnitInfo() throws IllegalStateException;

	/**
	 * 从此管理器获取指定的 PersistenceUnitInfo。
	 * @param persistenceUnitName 所需持久化单元的名称
	 * @return PersistenceUnitInfo（永不为 {@code null}）
	 * @throws IllegalArgumentException 如果未定义具有给定名称的 PersistenceUnitInfo
	 * @throws IllegalStateException 如果具有给定名称的 PersistenceUnitInfo 已被获取
	 */
	PersistenceUnitInfo obtainPersistenceUnitInfo(String persistenceUnitName)
			throws IllegalArgumentException, IllegalStateException;

}
