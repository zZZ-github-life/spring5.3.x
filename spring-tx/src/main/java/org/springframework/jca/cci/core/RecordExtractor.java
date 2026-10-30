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

import javax.resource.ResourceException;
import javax.resource.cci.Record;

import org.springframework.dao.DataAccessException;
import org.springframework.lang.Nullable;

/**
 * 用于从 CCI Record 实例中提取结果对象的回调接口。
 *
 * <p>用于 CciTemplate 中输出对象的创建。或者，输出 Record 也可以原样返回给客户端代码。
 * 如果执行结果是 CCI ResultSet，你几乎总是需要实现 RecordExtractor，
 * 以便以受管理的方式读取 ResultSet，在读取 ResultSet 时 CCI Connection 仍然打开。
 *
 * <p>此接口的实现执行实际的结果提取工作，但不需要担心异常处理。
 * ResourceException 将由 CciTemplate 类正确捕获和处理。
 *
 * @author Thierry Templier
 * @author Juergen Hoeller
 * @since 1.2
 * @param <T> 结果类型
 * @see CciTemplate#execute(javax.resource.cci.InteractionSpec, Record, RecordExtractor)
 * @see CciTemplate#execute(javax.resource.cci.InteractionSpec, RecordCreator, RecordExtractor)
 * @see javax.resource.cci.ResultSet
 * @deprecated as of 5.3, in favor of specific data access APIs
 * (or native CCI usage if there is no alternative)
 */
@Deprecated
@FunctionalInterface
public interface RecordExtractor<T> {

	/**
	 * 处理给定 Record 中的数据，创建相应的结果对象。
	 * @param record 要从中提取数据的 Record
	 * （可能是 CCI ResultSet）
	 * @return 任意结果对象；如果没有则为 {@code null}
	 * （在后一种情况下，提取器通常是有状态的）
	 * @throws ResourceException 如果由 CCI 方法抛出，
	 * 将自动转换为 DataAccessException
	 * @throws SQLException 如果由 ResultSet 方法抛出，
	 * 将自动转换为 DataAccessException
	 * @throws DataAccessException 在出现自定义异常的情况下
	 * @see javax.resource.cci.ResultSet
	 */
	@Nullable
	T extractData(Record record) throws ResourceException, SQLException, DataAccessException;

}
