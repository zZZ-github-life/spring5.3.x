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

package org.springframework.jca.cci.core;

import java.sql.SQLException;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.ConnectionSpec;
import javax.resource.cci.IndexedRecord;
import javax.resource.cci.Interaction;
import javax.resource.cci.InteractionSpec;
import javax.resource.cci.MappedRecord;
import javax.resource.cci.Record;
import javax.resource.cci.RecordFactory;
import javax.resource.cci.ResultSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * <b>这是 CCI 核心包中的中心类。</b>
 * 它简化了 CCI 的使用，并帮助避免常见错误。
 * 它执行核心的 CCI 工作流，将向 CCI 提供参数和提取结果的代码留给应用程序。
 * 此类执行 EIS 查询或更新，捕获 ResourceException 并将其转换为
 * {@code org.springframework.dao} 包中定义的通用异常层次结构。
 *
 * <p>使用此类的代码可以传入和接收 {@link javax.resource.cci.Record} 实例，
 * 或者实现回调接口来创建输入 Record 并从输出 Record（或 CCI ResultSet）中提取结果对象。
 *
 * <p>可以在服务实现中通过直接实例化并引用 ConnectionFactory 来使用，
 * 或者在应用程序上下文中准备并作为 bean 引用提供给服务。
 * 注意：ConnectionFactory 应始终在应用程序上下文中配置为 bean，
 * 第一种情况下直接提供给服务，第二种情况下提供给准备好的模板。
 *
 * @author Thierry Templier
 * @author Juergen Hoeller
 * @since 1.2
 * @see RecordCreator
 * @see RecordExtractor
 * @deprecated as of 5.3, in favor of specific data access APIs
 * (or native CCI usage if there is no alternative)
 */
@Deprecated
public class CciTemplate implements CciOperations {

	private final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private ConnectionFactory connectionFactory;

	@Nullable
	private ConnectionSpec connectionSpec;

	@Nullable
	private RecordCreator outputRecordCreator;


	/**
	 * 构造新的 CciTemplate，用于 bean 使用。
	 * <p>注意：使用实例前必须设置 ConnectionFactory。
	 * @see #setConnectionFactory
	 */
	public CciTemplate() {
	}

	/**
	 * 构造新的 CciTemplate，给定用于获取 Connection 的 ConnectionFactory。
	 * 注意：这将触发异常转换器的预先初始化。
	 * @param connectionFactory 要从中获取 Connection 的 JCA ConnectionFactory
	 */
	public CciTemplate(ConnectionFactory connectionFactory) {
		setConnectionFactory(connectionFactory);
		afterPropertiesSet();
	}

	/**
	 * 构造新的 CciTemplate，给定用于获取 Connection 的 ConnectionFactory。
	 * 注意：这将触发异常转换器的预先初始化。
	 * @param connectionFactory 要从中获取 Connection 的 JCA ConnectionFactory
	 * @param connectionSpec 用于获取 Connection 的 CCI ConnectionSpec
	 * （可以为 {@code null}）
	 */
	public CciTemplate(ConnectionFactory connectionFactory, @Nullable ConnectionSpec connectionSpec) {
		setConnectionFactory(connectionFactory);
		if (connectionSpec != null) {
			setConnectionSpec(connectionSpec);
		}
		afterPropertiesSet();
	}


	/**
	 * 设置此模板应获取 Connection 的 CCI ConnectionFactory。
	 */
	public void setConnectionFactory(@Nullable ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	/**
	 * 返回此模板使用的 CCI ConnectionFactory。
	 */
	@Nullable
	public ConnectionFactory getConnectionFactory() {
		return this.connectionFactory;
	}

	private ConnectionFactory obtainConnectionFactory() {
		ConnectionFactory connectionFactory = getConnectionFactory();
		Assert.state(connectionFactory != null, "No ConnectionFactory set");
		return connectionFactory;
	}

	/**
	 * 设置此模板实例应获取 Connection 的 CCI ConnectionSpec。
	 */
	public void setConnectionSpec(@Nullable ConnectionSpec connectionSpec) {
		this.connectionSpec = connectionSpec;
	}

	/**
	 * 返回此模板使用的 CCI ConnectionSpec（如果有的话）。
	 */
	@Nullable
	public ConnectionSpec getConnectionSpec() {
		return this.connectionSpec;
	}

	/**
	 * 设置用于创建默认输出 Record 的 RecordCreator。
	 * <p>默认为无：当没有显式的输出 Record 传入 {@code execute} 方法时，
	 * 将调用 CCI 的 {@code Interaction.execute} 返回输出 Record 的变体。
	 * <p>如果你总是需要调用 CCI 的 {@code Interaction.execute} 并传入输出 Record 的变体，
	 * 请在此指定 RecordCreator。除非有显式指定的输出 Record，否则 CciTemplate 将
	 * 调用此 RecordCreator 创建默认输出 Record 实例。
	 * @see javax.resource.cci.Interaction#execute(javax.resource.cci.InteractionSpec, Record)
	 * @see javax.resource.cci.Interaction#execute(javax.resource.cci.InteractionSpec, Record, Record)
	 */
	public void setOutputRecordCreator(@Nullable RecordCreator creator) {
		this.outputRecordCreator = creator;
	}

	/**
	 * 返回用于创建默认输出 Record 的 RecordCreator。
	 */
	@Nullable
	public RecordCreator getOutputRecordCreator() {
		return this.outputRecordCreator;
	}

	public void afterPropertiesSet() {
		if (getConnectionFactory() == null) {
			throw new IllegalArgumentException("Property 'connectionFactory' is required");
		}
	}


	/**
	 * 从此模板实例创建一个派生模板，
	 * 继承 ConnectionFactory 和其他设置，但覆盖用于获取 Connection 的 ConnectionSpec。
	 * @param connectionSpec 派生模板实例应为其获取 Connection 的
	 * CCI ConnectionSpec
	 * @return 派生的模板实例
	 * @see #setConnectionSpec
	 */
	public CciTemplate getDerivedTemplate(ConnectionSpec connectionSpec) {
		CciTemplate derived = new CciTemplate(obtainConnectionFactory(), connectionSpec);
		RecordCreator recordCreator = getOutputRecordCreator();
		if (recordCreator != null) {
			derived.setOutputRecordCreator(recordCreator);
		}
		return derived;
	}


	@Override
	@Nullable
	public <T> T execute(ConnectionCallback<T> action) throws DataAccessException {
		Assert.notNull(action, "Callback object must not be null");
		ConnectionFactory connectionFactory = obtainConnectionFactory();
		Connection con = org.springframework.jca.cci.connection.ConnectionFactoryUtils.getConnection(
				connectionFactory, getConnectionSpec());
		try {
			return action.doInConnection(con, connectionFactory);
		}
		catch (NotSupportedException ex) {
			throw new org.springframework.jca.cci.CciOperationNotSupportedException(
					"CCI operation not supported by connector", ex);
		}
		catch (ResourceException ex) {
			throw new DataAccessResourceFailureException("CCI operation failed", ex);
		}
		catch (SQLException ex) {
			throw new org.springframework.jca.cci.InvalidResultSetAccessException(
					"Parsing of CCI ResultSet failed", ex);
		}
		finally {
			org.springframework.jca.cci.connection.ConnectionFactoryUtils.releaseConnection(
					con, getConnectionFactory());
		}
	}

	@Override
	@Nullable
	public <T> T execute(final InteractionCallback<T> action) throws DataAccessException {
		Assert.notNull(action, "Callback object must not be null");
		return execute((ConnectionCallback<T>) (connection, connectionFactory) -> {
			Interaction interaction = connection.createInteraction();
			try {
				return action.doInInteraction(interaction, connectionFactory);
			}
			finally {
				closeInteraction(interaction);
			}
		});
	}

	@Override
	@Nullable
	public Record execute(InteractionSpec spec, Record inputRecord) throws DataAccessException {
		return doExecute(spec, inputRecord, null, new SimpleRecordExtractor());
	}

	@Override
	public void execute(InteractionSpec spec, Record inputRecord, Record outputRecord) throws DataAccessException {
		doExecute(spec, inputRecord, outputRecord, null);
	}

	@Override
	public Record execute(InteractionSpec spec, RecordCreator inputCreator) throws DataAccessException {
		Record output = doExecute(spec, createRecord(inputCreator), null, new SimpleRecordExtractor());
		Assert.state(output != null, "Invalid output record");
		return output;
	}

	@Override
	public <T> T execute(InteractionSpec spec, Record inputRecord, RecordExtractor<T> outputExtractor)
			throws DataAccessException {

		return doExecute(spec, inputRecord, null, outputExtractor);
	}

	@Override
	public <T> T execute(InteractionSpec spec, RecordCreator inputCreator, RecordExtractor<T> outputExtractor)
			throws DataAccessException {

		return doExecute(spec, createRecord(inputCreator), null, outputExtractor);
	}

	/**
	 * 使用 CCI 在 EIS 上执行指定的交互。所有其他交互执行方法都通过此方法。
	 * @param spec 定义该交互的 CCI InteractionSpec 实例
	 * （特定于连接器）
	 * @param inputRecord 输入记录
	 * @param outputRecord 输出记录（可以为 {@code null}）
	 * @param outputExtractor 将输出记录转换为结果对象的对象
	 * @return 使用 RecordExtractor 对象提取的输出数据
	 * @throws DataAccessException 如果出现任何问题
	 */
	@Nullable
	protected <T> T doExecute(
			final InteractionSpec spec, final Record inputRecord, @Nullable final Record outputRecord,
			@Nullable final RecordExtractor<T> outputExtractor) throws DataAccessException {

		return execute((InteractionCallback<T>) (interaction, connectionFactory) -> {
			Record outputRecordToUse = outputRecord;
			try {
				if (outputRecord != null || getOutputRecordCreator() != null) {
					// Use the CCI execute method with output record as parameter.
					if (outputRecord == null) {
						RecordFactory recordFactory = getRecordFactory(connectionFactory);
						outputRecordToUse = getOutputRecordCreator().createRecord(recordFactory);
					}
					interaction.execute(spec, inputRecord, outputRecordToUse);
				}
				else {
					outputRecordToUse = interaction.execute(spec, inputRecord);
				}
				return (outputExtractor != null ? outputExtractor.extractData(outputRecordToUse) : null);
			}
			finally {
				if (outputRecordToUse instanceof ResultSet) {
					closeResultSet((ResultSet) outputRecordToUse);
				}
			}
		});
	}


	/**
	 * 通过 ConnectionFactory 的 RecordFactory 创建索引 Record。
	 * @param name 记录的名称
	 * @return 该 Record
	 * @throws DataAccessException 如果 Record 创建失败
	 * @see #getRecordFactory(javax.resource.cci.ConnectionFactory)
	 * @see javax.resource.cci.RecordFactory#createIndexedRecord(String)
	 */
	public IndexedRecord createIndexedRecord(String name) throws DataAccessException {
		try {
			RecordFactory recordFactory = getRecordFactory(obtainConnectionFactory());
			return recordFactory.createIndexedRecord(name);
		}
		catch (NotSupportedException ex) {
			throw new org.springframework.jca.cci.RecordTypeNotSupportedException(
					"Creation of indexed Record not supported by connector", ex);
		}
		catch (ResourceException ex) {
			throw new org.springframework.jca.cci.CannotCreateRecordException(
					"Creation of indexed Record failed", ex);
		}
	}

	/**
	 * 从 ConnectionFactory 的 RecordFactory 创建映射 Record。
	 * @param name 记录名称
	 * @return 该 Record
	 * @throws DataAccessException 如果 Record 创建失败
	 * @see #getRecordFactory(javax.resource.cci.ConnectionFactory)
	 * @see javax.resource.cci.RecordFactory#createMappedRecord(String)
	 */
	public MappedRecord createMappedRecord(String name) throws DataAccessException {
		try {
			RecordFactory recordFactory = getRecordFactory(obtainConnectionFactory());
			return recordFactory.createMappedRecord(name);
		}
		catch (NotSupportedException ex) {
			throw new org.springframework.jca.cci.RecordTypeNotSupportedException(
					"Creation of mapped Record not supported by connector", ex);
		}
		catch (ResourceException ex) {
			throw new org.springframework.jca.cci.CannotCreateRecordException(
					"Creation of mapped Record failed", ex);
		}
	}

	/**
	 * 调用给定的 RecordCreator，将 JCA ResourceException 转换为
	 * Spring 的 DataAccessException 层次结构。
	 * @param recordCreator 要调用的 RecordCreator
	 * @return 创建的 Record
	 * @throws DataAccessException 如果 Record 创建失败
	 * @see #getRecordFactory(javax.resource.cci.ConnectionFactory)
	 * @see RecordCreator#createRecord(javax.resource.cci.RecordFactory)
	 */
	protected Record createRecord(RecordCreator recordCreator) throws DataAccessException {
		try {
			RecordFactory recordFactory = getRecordFactory(obtainConnectionFactory());
			return recordCreator.createRecord(recordFactory);
		}
		catch (NotSupportedException ex) {
			throw new org.springframework.jca.cci.RecordTypeNotSupportedException(
					"Creation of the desired Record type not supported by connector", ex);
		}
		catch (ResourceException ex) {
			throw new org.springframework.jca.cci.CannotCreateRecordException(
					"Creation of the desired Record failed", ex);
		}
	}

	/**
	 * 返回给定 ConnectionFactory 的 RecordFactory。
	 * <p>默认实现在可用时返回连接器的 RecordFactory，否则回退到
	 * NotSupportedRecordFactory 占位符。这允许在任何情况下都以非空的
	 * RecordFactory 引用调用 RecordCreator 回调。
	 * @param connectionFactory 该 CCI ConnectionFactory
	 * @return 该 ConnectionFactory 对应的 CCI RecordFactory
	 * @throws ResourceException 如果由 CCI 方法抛出
	 * @see org.springframework.jca.cci.connection.NotSupportedRecordFactory
	 */
	protected RecordFactory getRecordFactory(ConnectionFactory connectionFactory) throws ResourceException {
		try {
			return connectionFactory.getRecordFactory();
		}
		catch (NotSupportedException ex) {
			return new org.springframework.jca.cci.connection.NotSupportedRecordFactory();
		}
	}


	/**
	 * 关闭给定的 CCI Interaction 并忽略任何抛出的异常。
	 * 这对于手动 CCI 代码中的典型 finally 块很有用。
	 * @param interaction 要关闭的 CCI Interaction
	 * @see javax.resource.cci.Interaction#close()
	 */
	private void closeInteraction(@Nullable Interaction interaction) {
		if (interaction != null) {
			try {
				interaction.close();
			}
			catch (ResourceException ex) {
				logger.trace("Could not close CCI Interaction", ex);
			}
			catch (Throwable ex) {
				// We don't trust the CCI driver: It might throw RuntimeException or Error.
				logger.trace("Unexpected exception on closing CCI Interaction", ex);
			}
		}
	}

	/**
	 * 关闭给定的 CCI ResultSet 并忽略任何抛出的异常。
	 * 这对于手动 CCI 代码中的典型 finally 块很有用。
	 * @param resultSet 要关闭的 CCI ResultSet
	 * @see javax.resource.cci.ResultSet#close()
	 */
	private void closeResultSet(@Nullable ResultSet resultSet) {
		if (resultSet != null) {
			try {
				resultSet.close();
			}
			catch (SQLException ex) {
				logger.trace("Could not close CCI ResultSet", ex);
			}
			catch (Throwable ex) {
				// We don't trust the CCI driver: It might throw RuntimeException or Error.
				logger.trace("Unexpected exception on closing CCI ResultSet", ex);
			}
		}
	}


	private static class SimpleRecordExtractor implements RecordExtractor<Record> {

		@Override
		public Record extractData(Record record) {
			return record;
		}
	}

}
