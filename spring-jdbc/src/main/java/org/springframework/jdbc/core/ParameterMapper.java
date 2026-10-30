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

package org.springframework.jdbc.core;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * 当需要根据连接自定义参数时实现此接口。
 * 我们可能需要这样做来利用仅在特定Connection类型中可用的专有功能。
 *
 * @author Rod Johnson
 * @author Thomas Risberg
 * @see CallableStatementCreatorFactory#newCallableStatementCreator(ParameterMapper)
 * @see org.springframework.jdbc.object.StoredProcedure#execute(ParameterMapper)
 */
@FunctionalInterface
public interface ParameterMapper {

	/**
	 * 创建输入参数的Map，以名称为键。
	 * @param con 一个 JDBC 连接。如果需要对专有的 Connection 实现类执行
	 * 特定于 RDBMS 的操作，这会很有用（这也是本接口的目的）。
	 * 本类会隐藏此类专有细节。不过，最好尽可能避免使用此类专有的 RDBMS 特性。
	 * @return 以名称为键的输入参数 Map（永不为 {@code null}）
	 * @throws SQLException 如果在设置参数值时遇到 SQLException
	 * （也就是说，无需捕获 SQLException）
	 */
	Map<String, ?> createMap(Connection con) throws SQLException;

}
