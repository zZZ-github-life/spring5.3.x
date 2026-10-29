/*
 * Copyright 2002-2016 the original author or authors.
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

import javax.resource.ResourceException;
import javax.resource.cci.Record;
import javax.resource.cci.RecordFactory;

import org.springframework.dao.DataAccessException;

/**
 * 用于创建 CCI Record 实例的回调接口，通常基于传入的 CCI RecordFactory。
 *
 * <p>用于 CciTemplate 中输入 Record 的创建。或者，也可以将 Record 实例
 * 直接传入 CciTemplate 的相应 {@code execute} 方法，无论是手动实例化的
 * 还是通过 CciTemplate 的 Record 工厂方法创建的。
 *
 * <P>还用于在 CciTemplate 中创建默认输出 Record。
 * 当 JCA 连接器需要显式的输出 Record 实例，但不应将输出 Record 传入
 * CciTemplate 的 {@code execute} 方法时，这很有用。
 *
 * @author Thierry Templier
 * @author Juergen Hoeller
 * @since 1.2
 * @see CciTemplate#execute(javax.resource.cci.InteractionSpec, RecordCreator)
 * @see CciTemplate#execute(javax.resource.cci.InteractionSpec, RecordCreator, RecordExtractor)
 * @see CciTemplate#createIndexedRecord(String)
 * @see CciTemplate#createMappedRecord(String)
 * @see CciTemplate#setOutputRecordCreator(RecordCreator)
 * @deprecated as of 5.3, in favor of specific data access APIs
 * (or native CCI usage if there is no alternative)
 */
@Deprecated
@FunctionalInterface
public interface RecordCreator {

	/**
	 * 创建 CCI Record 实例，通常基于传入的 CCI RecordFactory。
	 * <p>用作 CciTemplate 的 {@code execute} 方法的<i>输入</i>创建器时，
	 * 此方法应创建<i>已填充</i>的 Record 实例。用作<i>输出</i> Record 创建器时，
	 * 应返回<i>空</i>的 Record 实例。
	 * @param recordFactory the CCI RecordFactory (never {@code null}, but not guaranteed to be
	 * supported by the connector: its create methods might throw NotSupportedException)
	 * @return the Record instance
	 * @throws ResourceException if thrown by a CCI method, to be auto-converted
	 * to a DataAccessException
	 * @throws DataAccessException in case of custom exceptions
	 */
	Record createRecord(RecordFactory recordFactory) throws ResourceException, DataAccessException;

}
