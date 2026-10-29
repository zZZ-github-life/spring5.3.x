/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.jdbc.support.incrementer;

import org.springframework.dao.DataAccessException;

/**
 * 定义递增任意数据存储字段最大值的契约的接口。其工作方式类似于序列号生成器。
 *
 * <p>典型实现可能使用标准 SQL、原生 RDBMS 序列或存储过程来完成此任务。
 *
 * @author Dmitriy Kopylenko
 * @author Jean-Pierre Pawlak
 * @author Juergen Hoeller
 */
public interface DataFieldMaxValueIncrementer {

	/**
	 * 以 int 形式递增数据存储字段的最大值。
	 * @return int next data store value such as <b>max + 1</b>
	 * @throws org.springframework.dao.DataAccessException in case of errors
	 */
	int nextIntValue() throws DataAccessException;

	/**
	 * 以 long 形式递增数据存储字段的最大值。
	 * @return int next data store value such as <b>max + 1</b>
	 * @throws org.springframework.dao.DataAccessException in case of errors
	 */
	long nextLongValue() throws DataAccessException;

	/**
	 * 以 String 形式递增数据存储字段的最大值。
	 * @return next data store value such as <b>max + 1</b>
	 * @throws org.springframework.dao.DataAccessException in case of errors
	 */
	String nextStringValue() throws DataAccessException;

}
