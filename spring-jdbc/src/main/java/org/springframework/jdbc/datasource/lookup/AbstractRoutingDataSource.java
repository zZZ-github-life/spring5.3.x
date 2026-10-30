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

package org.springframework.jdbc.datasource.lookup;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * 抽象的 {@link javax.sql.DataSource} 实现，它根据查找键将 {@link #getConnection()}
 * 调用路由到各个目标 DataSource 之一。查找键通常（但并非必然）
 * 是通过某个绑定到线程的事务上下文确定的。
 *
 * @author Juergen Hoeller
 * @since 2.0.1
 * @see #setTargetDataSources
 * @see #setDefaultTargetDataSource
 * @see #determineCurrentLookupKey()
 */
public abstract class AbstractRoutingDataSource extends AbstractDataSource implements InitializingBean {

	@Nullable
	private Map<Object, Object> targetDataSources;

	@Nullable
	private Object defaultTargetDataSource;

	private boolean lenientFallback = true;

	private DataSourceLookup dataSourceLookup = new JndiDataSourceLookup();

	@Nullable
	private Map<Object, DataSource> resolvedDataSources;

	@Nullable
	private DataSource resolvedDefaultDataSource;


	/**
	 * 指定目标 DataSource 的映射，以查找键作为键。
	 * 映射的值可以是相应的 {@link javax.sql.DataSource} 实例，
	 * 也可以是数据源名称字符串（通过 {@link #setDataSourceLookup DataSourceLookup} 解析）。
	 * <p>键可以是任意类型；本类只实现通用的查找过程。
	 * 具体的键表示形式将由 {@link #resolveSpecifiedLookupKey(Object)}
	 * 和 {@link #determineCurrentLookupKey()} 处理。
	 */
	public void setTargetDataSources(Map<Object, Object> targetDataSources) {
		this.targetDataSources = targetDataSources;
	}

	/**
	 * 指定默认的目标 DataSource（如果有）。
	 * <p>映射的值可以是相应的 {@link javax.sql.DataSource} 实例，
	 * 也可以是数据源名称字符串（通过 {@link #setDataSourceLookup DataSourceLookup} 解析）。
	 * <p>如果带键的 {@link #setTargetDataSources targetDataSources} 中没有任何项
	 * 与 {@link #determineCurrentLookupKey()} 的当前查找键匹配，
	 * 则将使用此 DataSource 作为目标。
	 */
	public void setDefaultTargetDataSource(Object defaultTargetDataSource) {
		this.defaultTargetDataSource = defaultTargetDataSource;
	}

	/**
	 * 指定在无法为当前查找键找到特定 DataSource 时，
	 * 是否对默认 DataSource 应用宽松回退。
	 * <p>默认值为 "true"，即接受在目标 DataSource 映射中没有对应条目的查找键 ——
	 * 在这种情况下只需回退到默认 DataSource。
	 * <p>如果希望仅在查找键为 {@code null} 时才应用回退，请将此标志切换为 "false"。
	 * 此时没有对应 DataSource 条目的查找键将导致 IllegalStateException。
	 * @see #setTargetDataSources
	 * @see #setDefaultTargetDataSource
	 * @see #determineCurrentLookupKey()
	 */
	public void setLenientFallback(boolean lenientFallback) {
		this.lenientFallback = lenientFallback;
	}

	/**
	 * 设置用于解析 {@link #setTargetDataSources targetDataSources} 映射中
	 * 数据源名称字符串的 DataSourceLookup 实现。
	 * <p>默认是一个 {@link JndiDataSourceLookup}，允许直接指定应用服务器
	 * DataSource 的 JNDI 名称。
	 */
	public void setDataSourceLookup(@Nullable DataSourceLookup dataSourceLookup) {
		this.dataSourceLookup = (dataSourceLookup != null ? dataSourceLookup : new JndiDataSourceLookup());
	}


	@Override
	public void afterPropertiesSet() {
		if (this.targetDataSources == null) {
			throw new IllegalArgumentException("Property 'targetDataSources' is required");
		}
		this.resolvedDataSources = CollectionUtils.newHashMap(this.targetDataSources.size());
		this.targetDataSources.forEach((key, value) -> {
			Object lookupKey = resolveSpecifiedLookupKey(key);
			DataSource dataSource = resolveSpecifiedDataSource(value);
			this.resolvedDataSources.put(lookupKey, dataSource);
		});
		if (this.defaultTargetDataSource != null) {
			this.resolvedDefaultDataSource = resolveSpecifiedDataSource(this.defaultTargetDataSource);
		}
	}

	/**
	 * 将 {@link #setTargetDataSources targetDataSources} 映射中指定的
	 * 给定查找键对象解析为实际用于与
	 * {@link #determineCurrentLookupKey() 当前查找键} 匹配的查找键。
	 * <p>默认实现只是按原样返回给定的键。
	 * @param lookupKey 用户指定的查找键对象
	 * @return 匹配所需的查找键
	 */
	protected Object resolveSpecifiedLookupKey(Object lookupKey) {
		return lookupKey;
	}

	/**
	 * 将指定的数据源对象解析为 DataSource 实例。
	 * <p>默认实现处理 DataSource 实例和数据源名称
	 * （通过 {@link #setDataSourceLookup DataSourceLookup} 解析）。
	 * @param dataSource {@link #setTargetDataSources targetDataSources} 映射中指定的数据源值对象
	 * @return 解析后的 DataSource（永不为 {@code null}）
	 * @throws IllegalArgumentException 如果值类型不受支持
	 */
	protected DataSource resolveSpecifiedDataSource(Object dataSource) throws IllegalArgumentException {
		if (dataSource instanceof DataSource) {
			return (DataSource) dataSource;
		}
		else if (dataSource instanceof String) {
			return this.dataSourceLookup.getDataSource((String) dataSource);
		}
		else {
			throw new IllegalArgumentException(
					"Illegal data source value - only [javax.sql.DataSource] and String supported: " + dataSource);
		}
	}

	/**
	 * 返回此路由器管理的已解析目标 DataSource。
	 * @return 解析后的查找键与 DataSource 的不可修改映射
	 * @throws IllegalStateException 如果目标 DataSource 尚未解析
	 * @since 5.2.9
	 * @see #setTargetDataSources
	 */
	public Map<Object, DataSource> getResolvedDataSources() {
		Assert.state(this.resolvedDataSources != null, "DataSources not resolved yet - call afterPropertiesSet");
		return Collections.unmodifiableMap(this.resolvedDataSources);
	}

	/**
	 * 返回已解析的默认目标 DataSource（如果有）。
	 * @return 默认 DataSource，如果没有或尚未解析则为 {@code null}
	 * @since 5.2.9
	 * @see #setDefaultTargetDataSource
	 */
	@Nullable
	public DataSource getResolvedDefaultDataSource() {
		return this.resolvedDefaultDataSource;
	}


	@Override
	public Connection getConnection() throws SQLException {
		return determineTargetDataSource().getConnection();
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return determineTargetDataSource().getConnection(username, password);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> iface) throws SQLException {
		if (iface.isInstance(this)) {
			return (T) this;
		}
		return determineTargetDataSource().unwrap(iface);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return (iface.isInstance(this) || determineTargetDataSource().isWrapperFor(iface));
	}

	/**
	 * 获取当前的目标 DataSource。确定
	 * {@link #determineCurrentLookupKey() 当前查找键}，
	 * 在 {@link #setTargetDataSources targetDataSources} 映射中执行查找，
	 * 必要时回退到指定的
	 * {@link #setDefaultTargetDataSource 默认目标 DataSource}。
	 * @see #determineCurrentLookupKey()
	 */
	protected DataSource determineTargetDataSource() {
		Assert.notNull(this.resolvedDataSources, "DataSource router not initialized");
		Object lookupKey = determineCurrentLookupKey();
		DataSource dataSource = this.resolvedDataSources.get(lookupKey);
		if (dataSource == null && (this.lenientFallback || lookupKey == null)) {
			dataSource = this.resolvedDefaultDataSource;
		}
		if (dataSource == null) {
			throw new IllegalStateException("Cannot determine target DataSource for lookup key [" + lookupKey + "]");
		}
		return dataSource;
	}

	/**
	 * 确定当前的查找键。通常将其实现为检查绑定到线程的事务上下文。
	 * <p>允许使用任意键。返回的键需要与存储的查找键类型匹配，
	 * 该类型由 {@link #resolveSpecifiedLookupKey} 方法解析。
	 */
	@Nullable
	protected abstract Object determineCurrentLookupKey();

}
