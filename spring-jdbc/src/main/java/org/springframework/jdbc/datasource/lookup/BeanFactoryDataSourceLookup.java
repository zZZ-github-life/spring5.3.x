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

package org.springframework.jdbc.datasource.lookup;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 基于 Spring {@link BeanFactory} 的 {@link DataSourceLookup} 实现。
 *
 * <p>将查找由 bean 名称标识的 Spring 管理的 bean，
 * 并要求它们是 {@code javax.sql.DataSource} 类型。
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.beans.factory.BeanFactory
 */
public class BeanFactoryDataSourceLookup implements DataSourceLookup, BeanFactoryAware {

	@Nullable
	private BeanFactory beanFactory;


	/**
	 * 创建 {@link BeanFactoryDataSourceLookup} 类的新实例。
	 * <p>必须通过 {@code setBeanFactory} 设置要访问的 BeanFactory。
	 * @see #setBeanFactory
	 */
	public BeanFactoryDataSourceLookup() {
	}

	/**
	 * 创建 {@link BeanFactoryDataSourceLookup} 类的新实例。
	 * <p>如果此对象由 Spring IoC 容器创建，则使用此构造函数是多余的，
	 * 因为提供的 {@link BeanFactory} 将被创建它的 {@link BeanFactory} 替换
	 * （参见 {@link BeanFactoryAware} 契约）。因此，仅在 Spring IoC 容器之外
	 * 使用本类时才使用此构造函数。
	 * @param beanFactory 用于查找 {@link DataSource DataSources} 的 Bean 工厂
	 */
	public BeanFactoryDataSourceLookup(BeanFactory beanFactory) {
		Assert.notNull(beanFactory, "BeanFactory is required");
		this.beanFactory = beanFactory;
	}


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	@Override
	public DataSource getDataSource(String dataSourceName) throws DataSourceLookupFailureException {
		Assert.state(this.beanFactory != null, "BeanFactory is required");
		try {
			return this.beanFactory.getBean(dataSourceName, DataSource.class);
		}
		catch (BeansException ex) {
			throw new DataSourceLookupFailureException(
					"Failed to look up DataSource bean with name '" + dataSourceName + "'", ex);
		}
	}

}
