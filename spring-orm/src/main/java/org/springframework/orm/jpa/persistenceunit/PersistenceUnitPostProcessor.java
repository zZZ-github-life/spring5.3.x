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

/**
 * 用于后处理 JPA PersistenceUnitInfo 的回调接口。
 * 实现可以注册到 DefaultPersistenceUnitManager，
 * 或通过 LocalContainerEntityManagerFactoryBean 注册。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see DefaultPersistenceUnitManager#setPersistenceUnitPostProcessors
 * @see org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean#setPersistenceUnitPostProcessors
 */
public interface PersistenceUnitPostProcessor {

	/**
	 * 对给定的 PersistenceUnitInfo 进行后处理，例如注册
	 * 更多的实体类和 jar 文件。
	 * @param pui 选定的 PersistenceUnitInfo，读取自 {@code persistence.xml}。
	 * 以 MutablePersistenceUnitInfo 形式传入。
	 */
	void postProcessPersistenceUnitInfo(MutablePersistenceUnitInfo pui);

}
