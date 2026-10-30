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

package org.springframework.jdbc.support;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * JdbcUtils 类使用的回调接口。该接口的实现负责提取数据库元数据的实际工作，
 * 但无需关心异常处理。SQLException 将由 JdbcUtils 类捕获并正确处理。
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @param <T> 结果类型
 * @see JdbcUtils#extractDatabaseMetaData(javax.sql.DataSource, DatabaseMetaDataCallback)
 */
@FunctionalInterface
public interface DatabaseMetaDataCallback<T> {

	/**
	 * 实现类必须实现此方法以处理传入的元数据。
	 * 具体选择做什么由实现类自行决定。
	 * @param dbmd 要处理的 DatabaseMetaData
	 * @return 从元数据中提取的结果对象
	 * （可以是任意对象，取决于实现的需要）
	 * @throws SQLException 如果在获取列值时遇到 SQLException
	 * （也就是说，无需捕获 SQLException）
	 * @throws MetaDataAccessException 在提取元数据时发生其他失败（例如反射失败）
	 */
	T processMetaData(DatabaseMetaData dbmd) throws SQLException, MetaDataAccessException;

}
