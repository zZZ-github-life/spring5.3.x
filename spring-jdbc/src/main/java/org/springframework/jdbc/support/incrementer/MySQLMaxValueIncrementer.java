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

package org.springframework.jdbc.support.incrementer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;

/**
 * {@link DataFieldMaxValueIncrementer}，通过相当于自增列的方式递增给定 MySQL 表的最大值。
 * 注意：如果使用此类，你的 MySQL 键列应 <i>不</i> 为自增列，因为序列表会完成这项工作。
 *
 * <p>序列保存在一张表中；每个需要自动生成键的表都应有一张对应的序列表。
 * 序列表使用的存储引擎可以是 MYISAM 或 INNODB，因为序列是使用单独的连接分配的，
 * 不会受任何其他可能正在进行的事务影响。
 *
 * <p>示例：
 *
 * <pre class="code">
 * create table tab (id int unsigned not null primary key, text varchar(100));
 * create table tab_sequence (value int not null);
 * insert into tab_sequence values(0);</pre>
 *
 * <p>如果设置了 {@code cacheSize}，中间值将直接从缓存提供，而无需查询数据库。
 * 如果服务器或应用停止、崩溃，或者事务回滚，未使用的值将永远不会被提供。
 * 因此编号中的最大空洞大小即为 {@code cacheSize} 的值。
 *
 * <p>通过将 "useNewConnection" 属性设置为 false，可以为递增器避免获取新连接。
 * 在这种情况下，定义递增器表时你 <i>必须</i> 使用像 MYISAM 这样的非事务性存储引擎。
 *
 * <p>从 Spring Framework 5.3.7 开始，{@code MySQLMaxValueIncrementer} 兼容
 * <a href="https://dev.mysql.com/doc/refman/8.0/en/mysql-tips.html#safe-updates">MySQL safe updates mode</a>。
 *
 * @author Jean-Pierre Pawlak
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class MySQLMaxValueIncrementer extends AbstractColumnMaxValueIncrementer {

	/** 用于获取新序列值的 SQL 字符串。 */
	private static final String VALUE_SQL = "select last_insert_id()";

	/** 下一个要提供的 ID。 */
	private long nextId = 0;

	/** 要提供的最大 ID。 */
	private long maxId = 0;

	/** 是否为此递增器使用新连接。 */
	private boolean useNewConnection = true;


	/**
	 * 用于 bean 属性风格用法的默认构造函数。
	 * @see #setDataSource
	 * @see #setIncrementerName
	 * @see #setColumnName
	 */
	public MySQLMaxValueIncrementer() {
	}

	/**
	 * 便捷构造函数。
	 * @param dataSource 要使用的 DataSource
	 * @param incrementerName 要使用的序列表的名称
	 * @param columnName 要使用的序列表中的列名
	 */
	public MySQLMaxValueIncrementer(DataSource dataSource, String incrementerName, String columnName) {
		super(dataSource, incrementerName, columnName);
	}


	/**
	 * 设置是否为此递增器使用新连接。
	 * <p>{@code true} 对于支持事务性存储引擎是必需的，
	 * 此时递增操作使用一个隔离的独立事务。
	 * 如果序列表的存储引擎是非事务性的（如 MYISAM），则 {@code false} 就足够了，
	 * 这样可以避免为递增操作额外获取一个 {@code Connection}。
	 * <p>自 Spring Framework 5.0 起，默认值为 {@code true}。
	 * @since 4.3.6
	 * @see DataSource#getConnection()
	 */
	public void setUseNewConnection(boolean useNewConnection) {
		this.useNewConnection = useNewConnection;
	}


	@Override
	protected synchronized long getNextKey() throws DataAccessException {
		if (this.maxId == this.nextId) {
			/*
			* If useNewConnection is true, then we obtain a non-managed connection so our modifications
			* are handled in a separate transaction. If it is false, then we use the current transaction's
			* connection relying on the use of a non-transactional storage engine like MYISAM for the
			* incrementer table. We also use straight JDBC code because we need to make sure that the insert
			* and select are performed on the same connection (otherwise we can't be sure that last_insert_id()
			* returned the correct value).
			*/
			Connection con = null;
			Statement stmt = null;
			boolean mustRestoreAutoCommit = false;
			try {
				if (this.useNewConnection) {
					con = getDataSource().getConnection();
					if (con.getAutoCommit()) {
						mustRestoreAutoCommit = true;
						con.setAutoCommit(false);
					}
				}
				else {
					con = DataSourceUtils.getConnection(getDataSource());
				}
				stmt = con.createStatement();
				if (!this.useNewConnection) {
					DataSourceUtils.applyTransactionTimeout(stmt, getDataSource());
				}
				// Increment the sequence column...
				String columnName = getColumnName();
				try {
					stmt.executeUpdate("update " + getIncrementerName() + " set " + columnName +
							" = last_insert_id(" + columnName + " + " + getCacheSize() + ") limit 1");
				}
				catch (SQLException ex) {
					throw new DataAccessResourceFailureException("Could not increment " + columnName + " for " +
							getIncrementerName() + " sequence table", ex);
				}
				// Retrieve the new max of the sequence column...
				ResultSet rs = stmt.executeQuery(VALUE_SQL);
				try {
					if (!rs.next()) {
						throw new DataAccessResourceFailureException("last_insert_id() failed after executing an update");
					}
					this.maxId = rs.getLong(1);
				}
				finally {
					JdbcUtils.closeResultSet(rs);
				}
				this.nextId = this.maxId - getCacheSize() + 1;
			}
			catch (SQLException ex) {
				throw new DataAccessResourceFailureException("Could not obtain last_insert_id()", ex);
			}
			finally {
				JdbcUtils.closeStatement(stmt);
				if (con != null) {
					if (this.useNewConnection) {
						try {
							con.commit();
							if (mustRestoreAutoCommit) {
								con.setAutoCommit(true);
							}
						}
						catch (SQLException ignore) {
							throw new DataAccessResourceFailureException(
									"Unable to commit new sequence value changes for " + getIncrementerName());
						}
						JdbcUtils.closeConnection(con);
					}
					else {
						DataSourceUtils.releaseConnection(con, getDataSource());
					}
				}
			}
		}
		else {
			this.nextId++;
		}
		return this.nextId;
	}

}
