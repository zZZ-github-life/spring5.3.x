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

package org.springframework.jdbc.support;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 简单接口，用于将复杂类型设置为语句参数。
 *
 * <p>实现类负责设置实际值的具体工作。它们必须实现回调方法 {@code setValue}，
 * 该方法可以抛出 SQLException，这些异常会被调用代码捕获并转换。如果需要创建
 * 任何数据库特定的对象，该回调方法可以通过给定的 PreparedStatement 对象
 * 访问底层的 Connection。
 *
 * @author Juergen Hoeller
 * @since 2.5.6
 * @see org.springframework.jdbc.core.SqlTypeValue
 * @see org.springframework.jdbc.core.DisposableSqlTypeValue
 */
public interface SqlValue {

	/**
	 * 在给定的 PreparedStatement 上设置值。
	 * @param ps 要进行操作的 PreparedStatement
	 * @param paramIndex 需要为其设置值的参数索引
	 * @throws SQLException 在设置参数值时遇到 SQLException
	 */
	void setValue(PreparedStatement ps, int paramIndex)	throws SQLException;

	/**
	 * 清理此值对象持有的资源。
	 */
	void cleanup();

}
