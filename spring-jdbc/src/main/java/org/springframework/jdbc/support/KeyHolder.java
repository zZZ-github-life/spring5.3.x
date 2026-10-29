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

import java.util.List;
import java.util.Map;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.lang.Nullable;

/**
 * 用于获取键的接口，通常用于获取 JDBC 插入语句可能返回的自动生成的主键。
 *
 * <p>此接口的实现可以保存任意数量的键。一般情况下，键以 List 形式返回，
 * 其中每一行键对应一个 Map。
 *
 * <p>大多数应用程序在插入语句中每行只使用一个键，并且一次只处理一行。
 * 在这些情况下，只需调用 {@link #getKey() getKey} 或
 * {@link #getKeyAs(Class) getKeyAs} 即可获取键。{@code getKey} 返回的值是
 * {@link Number}，这是自动生成主键的常用类型。对于任何其他类型的自动生成主键，
 * 请改用 {@code getKeyAs}。
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author Slawomir Dymitrow
 * @since 1.1
 * @see org.springframework.jdbc.core.JdbcTemplate
 * @see org.springframework.jdbc.object.SqlUpdate
 */
public interface KeyHolder {

	/**
	 * 从第一个 map 中获取第一个条目，假定只有一个条目、只有一个 map，
	 * 且该条目是数字。这是典型情况：单个数字型的生成主键。
	 * <p>键保存在 Map 的 List 中，其中列表中的每个条目表示每一行的键。
	 * 如果存在多个列，则 Map 也会有多个条目。如果此方法在 map 或列表中发现
	 * 多个条目（意味着返回了多个键），则抛出 InvalidDataAccessApiUsageException。
	 * @return the generated key as a number
	 * @throws InvalidDataAccessApiUsageException if multiple keys are encountered
	 * @see #getKeyAs(Class)
	 */
	@Nullable
	Number getKey() throws InvalidDataAccessApiUsageException;

	/**
	 * 从第一个 map 中获取第一个条目，假定只有一个条目、只有一个 map，
	 * 且该条目是指定类型的实例。这是常见情况：指定类型的单个生成主键。
	 * <p>键保存在 Map 的 List 中，其中列表中的每个条目表示每一行的键。
	 * 如果存在多个列，则 Map 也会有多个条目。如果此方法在 map 或列表中发现
	 * 多个条目（意味着返回了多个键），则抛出 InvalidDataAccessApiUsageException。
	 * @param keyType the type of the auto-generated key
	 * @return the generated key as an instance of specified type
	 * @throws InvalidDataAccessApiUsageException if multiple keys are encountered
	 * @since 5.3
	 * @see #getKey()
	 */
	@Nullable
	<T> T getKeyAs(Class<T> keyType) throws InvalidDataAccessApiUsageException;

	/**
	 * 获取第一个键 map。
	 * <p>如果列表中有多个条目（意味着多行返回了键），
	 * 则抛出 InvalidDataAccessApiUsageException。
	 * @return the Map of generated keys for a single row
	 * @throws InvalidDataAccessApiUsageException if keys for multiple rows are encountered
	 */
	@Nullable
	Map<String, Object> getKeys() throws InvalidDataAccessApiUsageException;

	/**
	 * 返回对包含键的 List 的引用。
	 * <p>可用于提取多行的键（不常见的情况），也可用于添加新的键 map。
	 * @return the List for the generated keys, with each entry representing
	 * an individual row through a Map of column names and key values
	 */
	List<Map<String, Object>> getKeyList();

}
