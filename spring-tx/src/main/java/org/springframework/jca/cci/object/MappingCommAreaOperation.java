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

import java.io.IOException;

import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.InteractionSpec;
import javax.resource.cci.Record;
import javax.resource.cci.RecordFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataRetrievalFailureException;

/**
 * 用于访问 COMMAREA 记录的 EIS 操作对象。
 * 通用 MappingRecordOperation 类的子类。
 *
 * @author Thierry Templier
 * @since 1.2
 * @deprecated as of 5.3, in favor of specific data access APIs
 * (or native CCI usage if there is no alternative)
 */
@Deprecated
public abstract class MappingCommAreaOperation extends MappingRecordOperation {

	/**
	 * 创建新的 MappingCommAreaQuery。
	 * @see #setConnectionFactory
	 * @see #setInteractionSpec
	 */
	public MappingCommAreaOperation() {
	}

	/**
	 * 创建新的 MappingCommAreaQuery。
	 * @param connectionFactory the ConnectionFactory to use to obtain connections
	 * @param interactionSpec specification to configure the interaction
	 */
	public MappingCommAreaOperation(ConnectionFactory connectionFactory, InteractionSpec interactionSpec) {
		super(connectionFactory, interactionSpec);
	}


	@Override
	protected final Record createInputRecord(RecordFactory recordFactory, Object inObject) {
		try {
			return new org.springframework.jca.cci.core.support.CommAreaRecord(objectToBytes(inObject));
		}
		catch (IOException ex) {
			throw new DataRetrievalFailureException("I/O exception during bytes conversion", ex);
		}
	}

	@Override
	protected final Object extractOutputData(Record record) throws DataAccessException {
		org.springframework.jca.cci.core.support.CommAreaRecord commAreaRecord =
				(org.springframework.jca.cci.core.support.CommAreaRecord) record;
		try {
			return bytesToObject(commAreaRecord.toByteArray());
		}
		catch (IOException ex) {
			throw new DataRetrievalFailureException("I/O exception during bytes conversion", ex);
		}
	}


	/**
	 * 将对象转换为 COMMAREA 字节的方法。
	 * @param inObject the input data
	 * @return the COMMAREA's bytes
	 * @throws IOException if thrown by I/O methods
	 * @throws DataAccessException if conversion failed
	 */
	protected abstract byte[] objectToBytes(Object inObject) throws IOException, DataAccessException;

	/**
	 * 将 COMMAREA 字节转换为对象的方法。
	 * @param bytes the COMMAREA's bytes
	 * @return the output data
	 * @throws IOException if thrown by I/O methods
	 * @throws DataAccessException if conversion failed
	 */
	protected abstract Object bytesToObject(byte[] bytes) throws IOException, DataAccessException;

}
