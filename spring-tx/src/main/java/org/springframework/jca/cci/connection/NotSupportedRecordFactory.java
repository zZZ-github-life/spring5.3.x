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

package org.springframework.jca.cci.connection;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.IndexedRecord;
import javax.resource.cci.MappedRecord;
import javax.resource.cci.RecordFactory;

/**
 * CCI RecordFactory 接口的实现，始终抛出 NotSupportedException。
 *
 * <p>用作 RecordFactory 参数的占位符（例如由 RecordCreator 回调定义），
 * 特别是当连接器的 {@code ConnectionFactory.getRecordFactory()} 实现
 * 在早期就抛出 NotSupportedException，而不是从 RecordFactory 的方法中抛出异常时。
 *
 * @author Juergen Hoeller
 * @since 1.2.4
 * @see org.springframework.jca.cci.core.RecordCreator#createRecord(javax.resource.cci.RecordFactory)
 * @see org.springframework.jca.cci.core.CciTemplate#getRecordFactory(javax.resource.cci.ConnectionFactory)
 * @see javax.resource.cci.ConnectionFactory#getRecordFactory()
 * @see javax.resource.NotSupportedException
 * @deprecated as of 5.3, in favor of specific data access APIs
 * (or native CCI usage if there is no alternative)
 */
@Deprecated
public class NotSupportedRecordFactory implements RecordFactory {

	@Override
	public MappedRecord createMappedRecord(String name) throws ResourceException {
		throw new NotSupportedException("The RecordFactory facility is not supported by the connector");
	}

	@Override
	public IndexedRecord createIndexedRecord(String name) throws ResourceException {
		throw new NotSupportedException("The RecordFactory facility is not supported by the connector");
	}

}
