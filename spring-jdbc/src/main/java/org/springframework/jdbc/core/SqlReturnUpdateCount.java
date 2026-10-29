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

import java.sql.Types;

/**
 * 表示存储过程调用返回的更新计数。
 *
 * <p>返回的更新计数 - 与所有存储过程参数一样 - <b>必须</b>有名称。
 *
 * @author Thomas Risberg
 */
public class SqlReturnUpdateCount extends SqlParameter {

	/**
	 * 创建新的SqlReturnUpdateCount。
	 * @param name the name of the parameter, as used in input and output maps
	 */
	public SqlReturnUpdateCount(String name) {
		super(name, Types.INTEGER);
	}


	/**
	 * 此实现始终返回{@code false}。
	 */
	@Override
	public boolean isInputValueProvided() {
		return false;
	}

	/**
	 * 此实现始终返回{@code true}。
	 */
	@Override
	public boolean isResultsParameter() {
		return true;
	}

}
