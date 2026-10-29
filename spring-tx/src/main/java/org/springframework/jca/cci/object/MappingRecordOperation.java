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

package org.springframework.jca.cci.object;

import java.sql.SQLException;

import javax.resource.ResourceException;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.InteractionSpec;
import javax.resource.cci.Record;
import javax.resource.cci.RecordFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * EIS 操作对象，期望映射的输入和输出对象，
 * 在 CCI Record 之间进行转换。
 *
 * <p>具体子类必须实现抽象方法
 * {@code createInputRecord(RecordFactory, Object)} 和
 * {@code extractOutputData(Record)}，分别从对象创建输入 Record
 * 和将输出 Record 转换为对象。
 *
 * @author Thierry Templier
 * @author Juergen Hoeller
 * @since 1.2
 * @see #createInputRecord(javax.resource.cci.RecordFactory, Object)
 * @see #extractOutputData(javax.resource.cci.Record)
 * @deprecated as of 5.3, in favor of specific data access APIs
 * (or native CCI usage if there is no alternative)
 */
@Deprecated
public abstract class MappingRecordOperation extends EisOperation {

	/**
	 * 允许作为 JavaBean 使用的构造器。
	 */
	public MappingRecordOperation() {
	}

	/**
	 * 便捷的构造器，接受 ConnectionFactory 和各规范
	 * （连接规范和交互规范）。
	 * @param connectionFactory the ConnectionFactory to use to obtain connections
	 */
	public MappingRecordOperation(ConnectionFactory connectionFactory, InteractionSpec interactionSpec) {
		getCciTemplate().setConnectionFactory(connectionFactory);
		setInteractionSpec(interactionSpec);
	}

	/**
	 * 设置用于创建默认输出 Record 的 RecordCreator。
	 * <p>默认为无：将调用 CCI 的 {@code Interaction.execute} 返回输出 Record 的变体。
	 * <p>如果你总是需要调用 CCI 的 {@code Interaction.execute} 并传入输出 Record 的变体，
	 * 请在此指定 RecordCreator。此 RecordCreator 将被调用以创建默认输出 Record 实例。
	 * @see javax.resource.cci.Interaction#execute(javax.resource.cci.InteractionSpec, Record)
	 * @see javax.resource.cci.Interaction#execute(javax.resource.cci.InteractionSpec, Record, Record)
	 * @see org.springframework.jca.cci.core.CciTemplate#setOutputRecordCreator
	 */
	public void setOutputRecordCreator(org.springframework.jca.cci.core.RecordCreator creator) {
		getCciTemplate().setOutputRecordCreator(creator);
	}

	/**
	 * 执行此操作对象封装的交互。
	 * @param inputObject the input data, to be converted to a Record
	 * by the {@code createInputRecord} method
	 * @return the output data extracted with the {@code extractOutputData} method
	 * @throws DataAccessException if there is any problem
	 * @see #createInputRecord
	 * @see #extractOutputData
	 */
	@Nullable
	public Object execute(Object inputObject) throws DataAccessException {
		InteractionSpec interactionSpec = getInteractionSpec();
		Assert.state(interactionSpec != null, "No InteractionSpec set");
		return getCciTemplate().execute(
				interactionSpec, new RecordCreatorImpl(inputObject), new RecordExtractorImpl());
	}


	/**
	 * 子类必须实现此方法，从传入 {@code execute} 方法的输入对象生成输入 Record。
	 * @param inputObject the passed-in input object
	 * @return the CCI input Record
	 * @throws ResourceException if thrown by a CCI method, to be auto-converted
	 * to a DataAccessException
	 * @see #execute(Object)
	 */
	protected abstract Record createInputRecord(RecordFactory recordFactory, Object inputObject)
			throws ResourceException, DataAccessException;

	/**
	 * 子类必须实现此方法，将 CCI 执行返回的 Record 转换为 {@code execute} 方法的结果对象。
	 * @param outputRecord the Record returned by CCI execution
	 * @return the result object
	 * @throws ResourceException if thrown by a CCI method, to be auto-converted
	 * to a DataAccessException
	 * @see #execute(Object)
	 */
	protected abstract Object extractOutputData(Record outputRecord)
			throws ResourceException, SQLException, DataAccessException;


	/**
	 * RecordCreator 的实现，调用外部类的 {@code createInputRecord} 方法。
	 */
	protected class RecordCreatorImpl implements org.springframework.jca.cci.core.RecordCreator {

		private final Object inputObject;

		public RecordCreatorImpl(Object inObject) {
			this.inputObject = inObject;
		}

		@Override
		public Record createRecord(RecordFactory recordFactory) throws ResourceException, DataAccessException {
			return createInputRecord(recordFactory, this.inputObject);
		}
	}


	/**
	 * RecordExtractor 的实现，调用外部类的 {@code extractOutputData} 方法。
	 */
	protected class RecordExtractorImpl implements org.springframework.jca.cci.core.RecordExtractor<Object> {

		@Override
		public Object extractData(Record record) throws ResourceException, SQLException, DataAccessException {
			return extractOutputData(record);
		}
	}

}
