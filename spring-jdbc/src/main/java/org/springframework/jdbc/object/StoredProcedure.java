/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.jdbc.object;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterMapper;
import org.springframework.jdbc.core.SqlParameter;

/**
 * RDBMS 存储过程对象抽象的父类。
 * 本类是抽象类，子类应提供类型化的调用方法，并委托给提供的 {@link #execute} 方法。
 *
 * <p>继承的 {@link #setSql sql} 属性是 RDBMS 中存储过程的名称。
 *
 * @author Rod Johnson
 * @author Thomas Risberg
 */
public abstract class StoredProcedure extends SqlCall {

	/**
	 * 允许作为 bean 使用。
	 */
	protected StoredProcedure() {
	}

	/**
	 * 为存储过程创建新的对象包装器。
	 * @param ds the DataSource to use throughout the lifetime
	 * of this object to obtain connections
	 * @param name the name of the stored procedure in the database
	 */
	protected StoredProcedure(DataSource ds, String name) {
		setDataSource(ds);
		setSql(name);
	}

	/**
	 * 为存储过程创建新的对象包装器。
	 * @param jdbcTemplate the JdbcTemplate which wraps DataSource
	 * @param name the name of the stored procedure in the database
	 */
	protected StoredProcedure(JdbcTemplate jdbcTemplate, String name) {
		setJdbcTemplate(jdbcTemplate);
		setSql(name);
	}


	/**
	 * StoredProcedure 的参数 Map 默认允许包含未实际用作参数的额外条目。
	 */
	@Override
	protected boolean allowsUnusedParameters() {
		return true;
	}

	/**
	 * 声明一个参数。
	 * <p>声明为 {@code SqlParameter} 和 {@code SqlInOutParameter} 的参数始终用于
	 * 提供输入值。除此之外，任何声明为 {@code SqlOutParameter} 且提供了非 null
	 * 输入值的参数也将用作输入参数。
	 * <b>注意：调用 declareParameter 的顺序必须与它们在数据库存储过程参数列表中
	 * 出现的顺序一致。</b>
	 * <p>名称纯粹用于辅助映射。
	 * @param param the parameter object
	 * @throws InvalidDataAccessApiUsageException if the parameter has no name, or if the
	 * operation is already compiled, and hence cannot be configured further
	 */
	@Override
	public void declareParameter(SqlParameter param) throws InvalidDataAccessApiUsageException {
		if (param.getName() == null) {
			throw new InvalidDataAccessApiUsageException("Parameters to stored procedures must have names as well as types");
		}
		super.declareParameter(param);
	}

	/**
	 * 使用提供的参数值执行存储过程。这是一个便捷方法，传入参数值的顺序
	 * 必须与声明参数的顺序一致。
	 * @param inParams variable number of input parameters. Output parameters should
	 * not be included in this map. It is legal for values to be {@code null}, and this
	 * will produce the correct behavior using a NULL argument to the stored procedure.
	 * @return map of output params, keyed by name as in parameter declarations.
	 * Output parameters will appear here, with their values after the stored procedure
	 * has been called.
	 */
	public Map<String, Object> execute(Object... inParams) {
		Map<String, Object> paramsToUse = new HashMap<>();
		validateParameters(inParams);
		int i = 0;
		for (SqlParameter sqlParameter : getDeclaredParameters()) {
			if (sqlParameter.isInputValueProvided() && i < inParams.length) {
				paramsToUse.put(sqlParameter.getName(), inParams[i++]);
			}
		}
		return getJdbcTemplate().call(newCallableStatementCreator(paramsToUse), getDeclaredParameters());
	}

	/**
	 * 执行存储过程。子类应定义强类型的 execute 方法（使用有意义的名称），
	 * 由该方法调用此方法，填充输入 map 并从输出 map 中提取类型化的值。
	 * 子类的 execute 方法通常接受领域对象作为参数并返回结果值。
	 * 也可以返回 void。
	 * @param inParams map of input parameters, keyed by name as in parameter
	 * declarations. Output parameters need not (but can) be included in this map.
	 * It is legal for map entries to be {@code null}, and this will produce the
	 * correct behavior using a NULL argument to the stored procedure.
	 * @return map of output params, keyed by name as in parameter declarations.
	 * Output parameters will appear here, with their values after the
	 * stored procedure has been called.
	 */
	public Map<String, Object> execute(Map<String, ?> inParams) throws DataAccessException {
		validateParameters(inParams.values().toArray());
		return getJdbcTemplate().call(newCallableStatementCreator(inParams), getDeclaredParameters());
	}

	/**
	 * 执行存储过程。子类应定义强类型的 execute 方法（使用有意义的名称），
	 * 由该方法调用此方法，并传入一个用于填充输入 map 的 ParameterMapper。
	 * 由于 ParameterMapper 可以访问 Connection 对象，因此可以处理数据库特有的特性。
	 * execute 方法还负责从输出 map 中提取类型化的值。子类的 execute 方法通常
	 * 接受领域对象作为参数并返回结果值。也可以返回 void。
	 * @param inParamMapper map of input parameters, keyed by name as in parameter
	 * declarations. Output parameters need not (but can) be included in this map.
	 * It is legal for map entries to be {@code null}, and this will produce the correct
	 * behavior using a NULL argument to the stored procedure.
	 * @return map of output params, keyed by name as in parameter declarations.
	 * Output parameters will appear here, with their values after the
	 * stored procedure has been called.
	 */
	public Map<String, Object> execute(ParameterMapper inParamMapper) throws DataAccessException {
		checkCompiled();
		return getJdbcTemplate().call(newCallableStatementCreator(inParamMapper), getDeclaredParameters());
	}

}
