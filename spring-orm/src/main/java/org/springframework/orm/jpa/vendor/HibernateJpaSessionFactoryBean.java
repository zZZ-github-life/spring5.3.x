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

package org.springframework.orm.jpa.vendor;

import java.lang.reflect.Method;

import javax.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.Nullable;
import org.springframework.orm.jpa.EntityManagerFactoryAccessor;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * 简单的 {@code FactoryBean}，用于暴露由 Hibernate 支撑的 JPA
 * {@link EntityManagerFactory} 背后的 {@link SessionFactory}。
 *
 * <p>主要用于通过 {@link #setPersistenceUnitName "persistenceUnitName"}
 * bean 属性按 JPA 持久化单元名称解析 SessionFactory。
 *
 * <p>请注意，对于简单场景，你也可以直接声明一个工厂方法：
 *
 * <pre class="code">
 * &lt;bean id="sessionFactory" factory-bean="entityManagerFactory" factory-method="getSessionFactory"/&gt;
 * </pre>
 *
 * <p>从 JPA 2.1 开始，{@link EntityManagerFactory#unwrap} 也提供了一种不错的方式，
 * 特别是在配置类安排中：
 *
 * <pre class="code">
 * &#064;Bean
 * public SessionFactory sessionFactory(@Qualifier("entityManagerFactory") EntityManagerFactory emf) {
 *     return emf.unwrap(SessionFactory.class);
 * }
 * </pre>
 *
 * 请注意：自 Hibernate 5.2 起，其 {@code SessionFactory} 接口改为扩展 JPA 的
 * {@code EntityManagerFactory}，因此按类型注入时可能会发生冲突，
 * 原始工厂和自定义的 {@code SessionFactory} 都会匹配 {@code EntityManagerFactory}。
 * 此时建议为原始工厂显式指定限定符（如上所示）。
 *
 * @author Juergen Hoeller
 * @since 3.1
 * @see #setPersistenceUnitName
 * @see #setEntityManagerFactory
 * @deprecated as of Spring Framework 4.3.12 against Hibernate 5.2, in favor of a custom solution
 * based on {@link EntityManagerFactory#unwrap} with explicit qualifiers and/or primary markers
 */
@Deprecated
public class HibernateJpaSessionFactoryBean extends EntityManagerFactoryAccessor implements FactoryBean<SessionFactory> {

	@Override
	@Nullable
	public SessionFactory getObject() {
		EntityManagerFactory emf = getEntityManagerFactory();
		Assert.state(emf != null, "EntityManagerFactory must not be null");
		try {
			Method getSessionFactory = emf.getClass().getMethod("getSessionFactory");
			return (SessionFactory) ReflectionUtils.invokeMethod(getSessionFactory, emf);
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalStateException("No compatible Hibernate EntityManagerFactory found: " + ex);
		}
	}

	@Override
	public Class<?> getObjectType() {
		return SessionFactory.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
