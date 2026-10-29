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
	 * @param con a JDBC connection. This is useful (and the purpose of this interface)
	 * if we need to do something RDBMS-specific with a proprietary Connection
	 * implementation class. This class conceals such proprietary details. However,
	 * it is best to avoid using such proprietary RDBMS features if possible.
	 * @return a Map of input parameters, keyed by name (never {@code null})
	 * @throws SQLException if an SQLException is encountered setting
	 * parameter values (that is, there's no need to catch SQLException)
	 */
	Map<String, ?> createMap(Connection con) throws SQLException;

}
